package swim.centrilized;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MonitorMain {

    private static final String queueName = "HEARTBEAT.queue";
    private static final long suspectMs = 4000; //ako nema 4 sek heartbeat, suspect
    private static final long deadMs = 8000; //ako nema 8 sek heartbeat, dead
    private static final String membershipExchange = "membership_exchange";

    //status za nodes ot
    enum Status { ALIVE, SUSPECT, DEAD }

    static class NodeInfo {
        volatile long lastSeen;
        volatile Status status;

        NodeInfo(long lastSeen, Status status) {
            this.lastSeen = lastSeen;
            this.status = status;
        }
    }

    private static final Map<String, NodeInfo> nodes = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        //konekcija do RabbitMQ
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        //da se osiguri deka queue postoi
        channel.queueDeclare(queueName, false, false, false, null);
        channel.exchangeDeclare(membershipExchange, BuiltinExchangeType.FANOUT);

        System.out.println("Monitor is waiting for heartbeats...");

        //callback koga poraka kje stigne
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            String[] parts = message.split(":");
            String nodeId = parts[0];
            long timeStamp = Long.parseLong(parts[1]);

            nodes.compute(nodeId, (k, v) ->  {
                if (v == null) { // koga kje dojde nov node so heartbeat, za da ne padne v.lastSeen
                    return new NodeInfo(timeStamp, Status.ALIVE);
                }
                v.lastSeen = timeStamp;
                v.status = Status.ALIVE; //ako pak prakja heartbeat, znaci ziv e
                return v;
            });

            System.out.println("Heartbeat from " + nodeId + " at " + timeStamp);
        };

        //pocnuva da slusa
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});

        final Channel publishChannel = channel;

        Thread statusChecker = new Thread(() -> {
            while (true) {
                long now = System.currentTimeMillis();

                nodes.forEach((nodeId, info) -> {
                    long difference = now - info.lastSeen;

                    Status newStatus;
                    if (difference >= deadMs) {
                        newStatus = Status.DEAD;
                    } else if (difference >= suspectMs) {
                        newStatus = Status.SUSPECT;
                    } else {
                        newStatus = Status.ALIVE;
                    }

                    if (newStatus != info.status) {
                        info.status = newStatus;
                        System.out.println("Status change: " + nodeId + "-> " + newStatus + " (lastSeen " + difference + "ms ago)");
                        //prakjame update do site nodes za posledniot node
                        String update = nodeId + "|" + newStatus + "|" + now;
                        try {
                            publishChannel.basicPublish(membershipExchange, "", null, update.getBytes(StandardCharsets.UTF_8));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("Sent membership_update: " + update);
                    }
                });

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        });

        statusChecker.start();
    }
}
