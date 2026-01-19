function formatTime(ms) {
    const d = new Date(ms);
    return d.toLocaleTimeString();
}

function formatAgo(lastSeen) {
    const diff = Date.now() - lastSeen;
    if (diff < 1000) return diff + "ms ago";
    return Math.floor(diff / 1000) + "s ago";
}

async function loadNodes() {
    const res = await fetch("/api/nodes");
    const nodes = await res.json();

    const body = document.getElementById("nodesBody");
    body.innerHTML = "";

    if (!nodes.length) {
        body.innerHTML = `<tr><td colspan="3">No nodes yet</td></tr>`;
        return;
    }

    for (const n of nodes) {
        const status = n.status || "ALIVE";
        const tr = document.createElement("tr");
        tr.innerHTML = `
      <td>${n.nodeId}</td>
      <td><span class="status ${status}">${status}</span></td>
      <td>${formatTime(n.lastSeen)} (${formatAgo(n.lastSeen)})</td>
    `;
        body.appendChild(tr);
    }
}

async function loadEvents() {
    const res = await fetch("/api/events");
    const events = await res.json();

    const div = document.getElementById("events");
    div.innerHTML = "";

    if (!events.length) {
        div.textContent = "No events yet";
        return;
    }

    for (const e of events) {
        const line = document.createElement("div");
        line.className = "event";
        line.textContent = `[${formatTime(e.time)}] ${e.type}: ${e.message}`;
        div.appendChild(line);
    }
}

async function refresh() {
    try {
        await loadNodes();
        await loadEvents();
    } catch (err) {
        console.error(err);
    }
}

// prv pat
refresh();
// na sekoja 1 sekunda
setInterval(refresh, 1000);
