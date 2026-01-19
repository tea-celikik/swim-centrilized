package swim.centrilized;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;

public class NodeMain {

    private static final String HEARTBEAT_QUEUE = "HEARTBEAT.queue";
    private static final String MEMBERSHIP_EXCHANGE = "membership_exchange";
    private static final long INTERVAL_MS = 2000;

    public static void main(String[] args) {
        // kolku nodes da se startuvaat, primer 5
        int n = (args.length > 0) ? Integer.parseInt(args[0]) : 5;

        String prefix = (args.length > 1) ? args[1] : "node";

        System.out.println("Starting " + n + " nodes...");

        for (int i = 1; i <= n; i++) {
            String nodeId = prefix + "-" + i;
            Thread t = new Thread(() -> runSingleNode(nodeId), "Thread-" + nodeId);
            t.start();
        }
    }

    private static void runSingleNode(String nodeId) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");

            try (Connection connection = factory.newConnection();
                 Channel channel = connection.createChannel()) {

                channel.queueDeclare(HEARTBEAT_QUEUE, false, false, false, null);
                channel.exchangeDeclare(MEMBERSHIP_EXCHANGE, BuiltinExchangeType.FANOUT);

                // sekoj jazol si ima svoja membership redica
                String membershipQueue = "membership." + nodeId;
                channel.queueDeclare(membershipQueue, false, false, false, null);
                channel.queueBind(membershipQueue, MEMBERSHIP_EXCHANGE, "");

                // primanje membership updates od Monitor
                DeliverCallback membershipCallback = (consumerTag, delivery) -> {
                    String update = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    System.out.println("[" + nodeId + "] received membership_update: " + update);
                };
                channel.basicConsume(membershipQueue, true, membershipCallback, consumerTag -> {});

                System.out.println("[" + nodeId + "] started. Sending heartbeats every " + (INTERVAL_MS / 1000) + "s...");

                // Heartbeat loop
                long startTime = System.currentTimeMillis();

                while (true) {
                    long now = System.currentTimeMillis();

                    // simuliranje na pad na jazol posle 10 sekundi, primer node-3
                    if (nodeId.equals("node-3") && now - startTime > 10_000) {
                        System.out.println("[" + nodeId + "] stopped sending heartbeats!");
                        break; // izleguva od heartbeat loop, ne isprakje povekje heartbeats
                    }

                    String msg = nodeId + ":" + System.currentTimeMillis();
                    channel.basicPublish("", HEARTBEAT_QUEUE, null,
                            msg.getBytes(StandardCharsets.UTF_8));

                    System.out.println("[" + nodeId + "] sent HEARTBEAT: " + msg);
                    Thread.sleep(INTERVAL_MS);
                }

            }
        } catch (Exception e) {
            System.out.println("[" + nodeId + "] ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
