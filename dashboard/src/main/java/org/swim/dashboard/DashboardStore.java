package org.swim.dashboard;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class DashboardStore {

    public enum Status { ALIVE, SUSPECT, DEAD }

    // nodeId info
    private final Map<String, NodeView> nodes = new ConcurrentHashMap<>();

    // log od posledni nastani
    private final Deque<Event> events = new ConcurrentLinkedDeque<>();
    private final int maxEvents = 200;

    public static class NodeView {
        public String nodeId;
        public Status status;
        public long lastSeen;

        public NodeView(String nodeId, Status status, long lastSeen) {
            this.nodeId = nodeId;
            this.status = status;
            this.lastSeen = lastSeen;
        }
    }

    public static class Event {
        public long time;
        public String type;   // HEARTBEAT ili MEMBERSHIP_UPDATE
        public String message;

        public Event(long time, String type, String message) {
            this.time = time;
            this.type = type;
            this.message = message;
        }
    }

    // Update methods

    public void onHeartbeat(String nodeId, long ts) {
        nodes.compute(nodeId, (k, v) -> {
            if (v == null) return new NodeView(nodeId, Status.ALIVE, ts);
            v.lastSeen = ts;
            v.status = Status.ALIVE; // ako stignal heartbeat, se vrakja vo ALIVE
            return v;
        });
        addEvent("HEARTBEAT", nodeId + ":" + ts);
    }


    public void onMembershipUpdate(String nodeId, Status status, long ts) {
        nodes.compute(nodeId, (k, v) -> {
            if (v == null) return new NodeView(nodeId, status, ts);
            v.status = status;
            v.lastSeen = ts;
            return v;
        });
        addEvent("MEMBERSHIP_UPDATE", nodeId + "|" + status + "|" + ts);
    }

    private void addEvent(String type, String msg) {
        events.addFirst(new Event(System.currentTimeMillis(), type, msg));
        while (events.size() > maxEvents) {
            events.removeLast();
        }
    }

    // Read methods

    public List<NodeView> getNodes() {
        List<NodeView> list = new ArrayList<>(nodes.values());
        list.sort(Comparator.comparing(n -> n.nodeId));
        return list;
    }

    public List<Event> getEvents() {
        return new ArrayList<>(events);
    }
}
