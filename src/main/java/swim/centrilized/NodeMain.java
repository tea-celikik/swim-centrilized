package swim.centrilized;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;

public class NodeMain {

    private static final String queueName = "HEARTBEAT.queue";
    private static final String nodeId = "node-1";
    private static final long intervalMs = 2000;
    private static final String membershipExchange = "membership_exchange";

    public static void main(String[] args) throws Exception {
        //konekcija do RabbitMQ
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost"); // ако RabbitMQ е на друга машина, тука ја менуваш адресата

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            //da se osiguri deka queue postoi
            channel.queueDeclare(queueName, false, false, false, null);
            channel.exchangeDeclare(membershipExchange, BuiltinExchangeType.FANOUT);

            //sekoj node ima svoj queue za slusanje na updates
            String membershipQueue = "membership." + nodeId;
            channel.queueDeclare(membershipQueue, false, false, false, null);
            channel.queueBind(membershipQueue, membershipExchange, "");

            System.out.println("Node started. Sending heartbeats every " + (intervalMs / 1000) + "s...");

            //slusanje na updates
            DeliverCallback membershipCallBack = (consummerTag, delivery) -> {
                String update = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("Node received membership_update: " + update);
            };

            channel.basicConsume(membershipQueue, true, membershipCallBack, consummerTag -> {});
            //node prakja heartbeat na sekoi 2 sekundi kontinuirano
            while (true) {
                String msg = nodeId + ":" + System.currentTimeMillis();
                channel.basicPublish("", queueName, null, msg.getBytes(StandardCharsets.UTF_8));
                System.out.println("Node sent HEARTBEAT: " + msg);
                Thread.sleep(intervalMs);
            }
        }
    }
}
