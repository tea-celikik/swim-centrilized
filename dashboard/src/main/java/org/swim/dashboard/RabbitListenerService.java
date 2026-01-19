package org.swim.dashboard;

import com.rabbitmq.client.*;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.nio.charset.StandardCharsets;

@Service
public class RabbitListenerService {

    private static final String HEARTBEAT_QUEUE = "HEARTBEAT.queue";
    private static final String MEMBERSHIP_EXCHANGE = "membership_exchange";
    private static final String DASHBOARD_MEMBERSHIP_QUEUE = "dashboard.membership";

    private final DashboardStore store;

    private Connection connection;
    private Channel channel;

    public RabbitListenerService(DashboardStore store) {
        this.store = store;
    }

    @PostConstruct
    public void start() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        connection = factory.newConnection();
        channel = connection.createChannel();

        // heartbeat queue za sekoj slucaj i tuka da go ima
        channel.queueDeclare(HEARTBEAT_QUEUE, false, false, false, null);

        // membership exchange + dashboard queue za da prima fanout poraki
        channel.exchangeDeclare(MEMBERSHIP_EXCHANGE, BuiltinExchangeType.FANOUT);
        channel.queueDeclare(DASHBOARD_MEMBERSHIP_QUEUE, false, false, false, null);
        channel.queueBind(DASHBOARD_MEMBERSHIP_QUEUE, MEMBERSHIP_EXCHANGE, "");

        // slusanje heartbeats
        DeliverCallback hbCallback = (consumerTag, delivery) -> {
            String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
            // ocekuvano: nodeId:timestamp
            String[] parts = msg.split(":");
            if (parts.length != 2) return;

            String nodeId = parts[0];
            long ts;
            try { ts = Long.parseLong(parts[1]); } catch (NumberFormatException e) { return; }

            store.onHeartbeat(nodeId, ts);
        };
        channel.basicConsume(HEARTBEAT_QUEUE, true, hbCallback, consumerTag -> {});

        // slusanje membership updates
        DeliverCallback muCallback = (consumerTag, delivery) -> {
            String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
            // ocekuvano: nodeId|STATUS|timestamp
            String[] parts = msg.split("\\|");
            if (parts.length != 3) return;

            String nodeId = parts[0];
            DashboardStore.Status status;
            try { status = DashboardStore.Status.valueOf(parts[1]); } catch (Exception e) { return; }

            long ts;
            try { ts = Long.parseLong(parts[2]); } catch (NumberFormatException e) { return; }

            store.onMembershipUpdate(nodeId, status, ts);
        };
        channel.basicConsume(DASHBOARD_MEMBERSHIP_QUEUE, true, muCallback, consumerTag -> {});

        System.out.println("RabbitListenerService started: listening to heartbeats + membership updates");
    }

    @PreDestroy
    public void stop() {
        try { if (channel != null) channel.close(); } catch (Exception ignored) {}
        try { if (connection != null) connection.close(); } catch (Exception ignored) {}
    }
}
