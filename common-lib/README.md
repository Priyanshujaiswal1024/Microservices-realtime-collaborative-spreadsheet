# Common Library Module

Shared library containing DTOs, events, CRDT implementation, and utilities used across all microservices.

## 📦 Contents

### CRDT Implementation

#### `HybridLogicalClock` (HLC)
Custom implementation of Hybrid Logical Clock for distributed timestamp ordering:

```java
// Create new timestamp for local event
HybridLogicalClock hlc = HybridLogicalClock.now("client-123", lastHlc);

// Merge with remote timestamp (received from another client)
HybridLogicalClock merged = HybridLogicalClock.merge("client-123", lastHlc, remoteHlc);

// Compare timestamps
if (hlc1.happenedAfter(hlc2)) {
    // hlc1 is newer
}
```

**Features:**
- Combines physical time with logical counters
- Solves clock skew across distributed pods
- Maintains causal ordering
- Deterministic tie-breaking with client IDs

#### `CellState`
LWW-Element-Register for cell values:

```java
// Create cell state
CellState state = new CellState(
    "value", 
    timestamp, 
    "client-123", 
    "user-456", 
    "TEXT", 
    "{\"bold\":true}"
);

// Merge two states (LWW)
CellState merged = CellState.merge(currentState, incomingState);
```

### Kafka Events

All events extend `BaseEvent` with common fields (eventId, timestamp, actorId, version).

#### `CellEditEvent`
Real-time cell updates:
```java
CellEditEvent event = CellEditEvent.builder()
    .actorId(userId)
    .workbookId(workbookId)
    .sheetId(sheetId)
    .row(0)
    .col(0)
    .cellState(cellState)
    .previousValue("old")
    .build();
```

**Topic:** `cell-edits`  
**Partition Key:** `sheetId`

#### `SheetLifecycleEvent`
Workbook/sheet create/update/delete:
```java
SheetLifecycleEvent event = SheetLifecycleEvent.builder()
    .actorId(userId)
    .action(Action.WORKBOOK_CREATED)
    .workbookId(workbookId)
    .name("My Workbook")
    .build();
```

**Topic:** `sheet-lifecycle`  
**Partition Key:** `workbookId`

#### `CommentEvent`
Comment operations:
```java
CommentEvent event = CommentEvent.builder()
    .actorId(userId)
    .action(Action.COMMENT_CREATED)
    .commentId(commentId)
    .cellId(cellId)
    .text("Great work @john!")
    .mentionedUserIds(List.of("user-john"))
    .build();
```

**Topic:** `comments`  
**Partition Key:** `workbookId`

#### `NotificationEvent`
User notifications:
```java
NotificationEvent event = NotificationEvent.builder()
    .actorId(actorId)
    .type(Type.COMMENT_MENTION)
    .userId(targetUserId)
    .title("You were mentioned")
    .message("John mentioned you in a comment")
    .link("/workbook/123/sheet/456")
    .build();
```

**Topic:** `notifications`  
**Partition Key:** `userId`

### WebSocket Messages

All messages extend `WebSocketMessage` with timestamp and userId.

#### `CellUpdateMessage`
```java
CellUpdateMessage msg = new CellUpdateMessage();
msg.setSheetId(sheetId);
msg.setRow(0);
msg.setCol(0);
msg.setCellState(cellState);
```

#### `CursorMoveMessage`
```java
CursorMoveMessage msg = new CursorMoveMessage();
msg.setSheetId(sheetId);
msg.setRow(5);
msg.setCol(3);
msg.setUserName("John Doe");
msg.setColor("#FF5733");
```

#### `PresenceMessage`
```java
PresenceMessage msg = new PresenceMessage();
msg.setSheetId(sheetId);
msg.setAction(Action.JOIN);
msg.setUserName("Jane Smith");
msg.setColor("#33FF57");
```

#### `CommentMessage`
```java
CommentMessage msg = new CommentMessage();
msg.setSheetId(sheetId);
msg.setCellId(cellId);
msg.setAction(Action.CREATED);
msg.setText("Review this cell");
```

### DTOs and Enums

#### Permission & Role Enums
- `UserRole`: USER, ADMIN, SERVICE
- `PermissionRole`: OWNER, EDITOR, COMMENTER, VIEWER
- `WorkbookVisibility`: PRIVATE, LINK_SHARED, PUBLIC
- `CellDataType`: TEXT, NUMBER, BOOLEAN, DATE, DATETIME, FORMULA, LINK

#### Standard Response DTOs
- `ErrorResponse`: Standardized error format
- `PageResponse<T>`: Paginated response wrapper

### Exceptions

Custom exceptions extending `BaseException`:
- `ResourceNotFoundException` (404)
- `UnauthorizedException` (401)
- `ForbiddenException` (403)
- `ValidationException` (400)

### Utilities

#### `IdGenerator`
```java
String id = IdGenerator.generateId();                    // UUID
String shortId = IdGenerator.generateShortId();          // 8-char UUID
String workbookId = IdGenerator.generateWorkbookId();    // "wb-abc12345"
String sheetId = IdGenerator.generateSheetId();          // "sh-xyz67890"
String cellId = IdGenerator.generateCellId();            // "cell-def34567"
String commentId = IdGenerator.generateCommentId();      // "cmt-ghi89012"
String clientId = IdGenerator.generateClientId();        // "client-jkl45678"
```

#### `KafkaTopics`
```java
String topic = KafkaTopics.CELL_EDITS;        // "cell-edits"
String topic = KafkaTopics.SHEET_LIFECYCLE;   // "sheet-lifecycle"
String topic = KafkaTopics.COMMENTS;          // "comments"
String topic = KafkaTopics.NOTIFICATIONS;     // "notifications"
```

## 🧪 Testing

Run tests:
```bash
cd common-lib
mvn test
```

Tests included:
- `HybridLogicalClockTest` - HLC creation, comparison, merging
- `CellStateTest` - CRDT merge logic, LWW validation

## 📚 Usage in Other Services

Add dependency in service `pom.xml`:
```xml
<dependency>
    <groupId>com.collab.spreadsheet</groupId>
    <artifactId>common-lib</artifactId>
</dependency>
```

Import and use:
```java
import com.collab.spreadsheet.common.crdt.*;
import com.collab.spreadsheet.common.events.*;
import com.collab.spreadsheet.common.dto.*;
import com.collab.spreadsheet.common.websocket.*;
import com.collab.spreadsheet.common.util.*;
```

## 🏗️ Architecture Notes

### Why HLC?
Pure physical timestamps fail in distributed systems because:
- Server clocks can drift
- NTP corrections can cause time to go backwards
- Can't determine causality from timestamps alone

HLC solves this by:
- Using physical time as the primary component
- Adding logical counters for same-time events
- Merging remote timestamps to maintain causality
- Providing client ID tie-breaking for determinism

### Why LWW-Register?
For spreadsheet cells, Last-Write-Wins is appropriate because:
- Cells have simple overwrite semantics (no character-level editing)
- Users expect the "latest" edit to win
- Implementation is simple and performant
- Easy to reason about for debugging

If you need character-level collaborative editing within cells (like Google Docs), you'd need to upgrade to a Sequence CRDT (YATA, RGA, etc.).

### Event Idempotency
All Kafka events include:
- `eventId`: Unique UUID for deduplication
- `timestamp`: Event creation time
- `version`: Schema version for evolution

Consumers MUST check for duplicate `eventId` and skip processing if already seen.

## 📄 License

Part of the Real-Time Collaborative Spreadsheet Platform.
