package org.swim.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardStore store;

    public DashboardController(DashboardStore store) {
        this.store = store;
    }

    // API za status na jazli
    @GetMapping("/nodes")
    public List<DashboardStore.NodeView> getNodes() {
        return store.getNodes();
    }

    //
    // API za log komunikacija
    @GetMapping("/events")
    public List<DashboardStore.Event> getEvents() {
        return store.getEvents();
    }
}
