# SWIM-Centralized Membership Monitor

Demonstration of a **centralized membership and failure detection system** inspired by the SWIM protocol, implemented using **Java** and **RabbitMQ**.

---

## Description

This project demonstrates a **centralized architecture** for node membership monitoring:

- Multiple **nodes** periodically send heartbeat messages.
- A central **Monitor** receives heartbeats and tracks active nodes.
- If a node stops sending heartbeats within a timeout period, it is marked as **SUSPECT** or **DEAD**.
- Membership updates are broadcast back to all nodes.

The goal of the project is to clearly show how **heartbeat-based failure detection** works in a centralized distributed system model.

---

## Architecture Overview

- **Node**
  - Sends heartbeat messages every fixed interval
  - Listens for membership updates from the Monitor

- **Monitor**
  - Receives heartbeats from all nodes
  - Maintains a membership table
  - Detects failed nodes using timeouts
  - Broadcasts membership changes

---

## Technologies and Tools

- **Programming Language:** Java
- **Messaging System:** RabbitMQ
- **Docker + Docker Compose**
- **Build Tool:** Maven
- **Protocol Concept:** SWIM (centralized variant)

## RabbitMQ (Docker Compose)

This project uses RabbitMQ as a message broker for the centralized model.
RabbitMQ is provided via Docker Compose. Use this bash command in the root of the project, before running the Java files.
RabbitMQ will be available at "http://localhost:15672"

To start RabbitMQ, run the following command from the project root:

```bash
docker compose up -d
```


## Authors

- Tea Celikik
