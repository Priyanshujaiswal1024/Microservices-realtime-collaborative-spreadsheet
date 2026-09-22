# WebSocket Protocol & STOMP Specification

## Connection Setup

- **Transport**: Spring WebSocket + STOMP over SockJS
- **Endpoint**: `/ws` (forwarded to `/ws/sheet/{sheetId}` on `collab-service`)
- **Authentication**: JWT passed in the STOMP `CONNECT` frame:
```
CONNECT
Authorization:Bearer <JWT_ACCESS_TOKEN>
accept-version:1.2,1.1,1.0
heart-beat:10000,10000

^@
```

---

## Subscriptions (Incoming Data)

### 1. Cell State Stream
- **Topic**: `/topic/sheet/{sheetId}/cells`
- **Payload**: `CellUpdateMessage`
```json
{
  "type": "CELL_EDIT",
  "sheetId": "sheet-1",
  "row": 3,
  "col": 2,
  "cellState": {
    "value": "$45,000",
    "timestamp": {
      "physicalTime": 1725184800000,
      "logicalCounter": 4,
      "clientId": "client_a7f9"
    },
    "clientId": "client_a7f9",
    "userId": "user-1",
    "dataType": "NUMBER"
  }
}
```

### 2. Presence & Live Cursors
- **Topic**: `/topic/sheet/{sheetId}/presence`
- **Payload**: `CursorMoveMessage` or `PresenceMessage`
```json
{
  "type": "CURSOR_MOVE",
  "sheetId": "sheet-1",
  "row": 3,
  "col": 2,
  "userId": "user-1",
  "userName": "Alex Rivera",
  "color": "#3B82F6"
}
```

---

## Publications (Outgoing Client Events)

### 1. Send Cell Edit
- **Destination**: `/app/sheet/{sheetId}/edit`
```json
{
  "row": 3,
  "col": 2,
  "cellState": {
    "value": "1200",
    "timestamp": {
      "physicalTime": 1725184800000,
      "logicalCounter": 5,
      "clientId": "client_a7f9"
    },
    "clientId": "client_a7f9"
  }
}
```

### 2. Send Cursor Movement
- **Destination**: `/app/sheet/{sheetId}/cursor`
```json
{
  "row": 3,
  "col": 2,
  "userName": "Alex Rivera",
  "color": "#3B82F6"
}
```

### 3. Send Presence Status
- **Destination**: `/app/sheet/{sheetId}/presence`
```json
{
  "action": "JOIN",
  "userName": "Alex Rivera",
  "color": "#3B82F6"
}
```
