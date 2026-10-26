package com.collab.spreadsheet.collab.websocket;

import com.collab.spreadsheet.collab.service.CollabOrchestrator;
import com.collab.spreadsheet.collab.service.PresenceTracker;
import com.collab.spreadsheet.common.websocket.CellBatchUpdateMessage;
import com.collab.spreadsheet.common.websocket.CellUpdateMessage;
import com.collab.spreadsheet.common.websocket.CursorMoveMessage;
import com.collab.spreadsheet.common.websocket.PresenceMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Thin WebSocket routing controller. Delegates all business logic to dedicated orchestrator/services.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class CollabWebSocketController {

    private final CollabOrchestrator collabOrchestrator;
    private final PresenceTracker presenceTracker;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.collab.spreadsheet.collab.redis.RedisPubSubBroadcaster redisPubSubBroadcaster;

    @MessageMapping("/sheet/{sheetId}/edit")
    public void handleCellEdit(
            @DestinationVariable String sheetId,
            @Payload CellUpdateMessage message,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        String userId = principal != null ? principal.getName() : 
                (message.getUserId() != null ? message.getUserId() : 
                (message.getCellState() != null && message.getCellState().getUserId() != null ? message.getCellState().getUserId() : "guest"));
        if (headerAccessor != null && headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("sheetId", sheetId);
            headerAccessor.getSessionAttributes().put("userId", userId);
        }
        log.debug("Received cell edit on sheet {} from user {}", sheetId, userId);
        collabOrchestrator.processCellEdit(sheetId, message, userId);
    }

    @MessageMapping("/sheet/{sheetId}/edit/batch")
    public void handleCellBatchEdit(
            @DestinationVariable String sheetId,
            @Payload CellBatchUpdateMessage message,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        String userId = principal != null ? principal.getName() : 
                (message.getUserId() != null ? message.getUserId() : "guest");
        if (headerAccessor != null && headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("sheetId", sheetId);
            headerAccessor.getSessionAttributes().put("userId", userId);
        }
        log.debug("Received cell batch edit on sheet {} with {} updates from user {}",
                sheetId, message.getUpdates() != null ? message.getUpdates().size() : 0, userId);
        collabOrchestrator.processCellBatchEdit(sheetId, message, userId);
        messagingTemplate.convertAndSend("/topic/sheet/" + sheetId + "/cells", message);
    }

    @MessageMapping("/sheet/{sheetId}/cursor")
    public void handleCursorMove(
            @DestinationVariable String sheetId,
            @Payload CursorMoveMessage message,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        String userId = principal != null ? principal.getName() : (message.getUserId() != null ? message.getUserId() : "anonymous");
        if (headerAccessor != null && headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("sheetId", sheetId);
            headerAccessor.getSessionAttributes().put("userId", userId);
            if (message.getUserName() != null) {
                headerAccessor.getSessionAttributes().put("userName", message.getUserName());
            }
        }
        presenceTracker.updateCursor(sheetId, message, userId);
    }

    @MessageMapping("/sheet/{sheetId}/presence")
    public void handlePresence(
            @DestinationVariable String sheetId,
            @Payload PresenceMessage message,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        String userId = principal != null ? principal.getName() : (message.getUserId() != null ? message.getUserId() : "anonymous");
        
        if (headerAccessor != null && headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("sheetId", sheetId);
            headerAccessor.getSessionAttributes().put("userId", userId);
            if (message.getUserName() != null) {
                headerAccessor.getSessionAttributes().put("userName", message.getUserName());
            }
        }

        if (message.getAction() == PresenceMessage.Action.JOIN) {
            presenceTracker.handleJoin(sheetId, userId, message.getUserName(), message.getColor());
        } else if (message.getAction() == PresenceMessage.Action.LEAVE) {
            presenceTracker.handleLeave(sheetId, userId, message.getUserName());
        }
    }

    @MessageMapping("/sheet/{sheetId}/comment")
    public void handleComment(
            @DestinationVariable String sheetId,
            @Payload Object commentPayload) {
        log.info("Broadcasting comment on sheet {}", sheetId);
        messagingTemplate.convertAndSend("/topic/sheet/" + sheetId + "/comments", commentPayload);
    }

    @MessageMapping("/workbook/{workbookId}/update")
    public void handleWorkbookUpdate(
            @DestinationVariable String workbookId,
            @Payload Object workbookPayload) {
        log.info("Broadcasting workbook update for workbook {}", workbookId);
        messagingTemplate.convertAndSend("/topic/workbook/" + workbookId + "/update", workbookPayload);
        messagingTemplate.convertAndSend("/topic/workbook/" + workbookId + "/sheets", workbookPayload);
        messagingTemplate.convertAndSend("/topic/workbook/global/update", workbookPayload);
    }

    @MessageMapping("/sheet/{sheetId}/clear")
    public void handleClearSheet(
            @DestinationVariable String sheetId,
            @Payload(required = false) Object clearPayload,
            Principal principal) {
        String userId = principal != null ? principal.getName() : "guest";
        log.info("Received clear sheet command on sheet {} from user {}", sheetId, userId);
        collabOrchestrator.clearSheet(sheetId);

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("type", "CLEAR_SHEET");
        response.put("action", "CLEAR_SHEET");
        response.put("sheetId", sheetId);
        response.put("userId", userId);
        response.put("timestamp", java.time.Instant.now().toString());

        CellUpdateMessage clearMsg = new CellUpdateMessage();
        clearMsg.setSheetId(sheetId);
        clearMsg.setRow(-1);
        clearMsg.setCol(-1);
        clearMsg.setUserId(userId);
        clearMsg.setTimestamp(java.time.Instant.now());
        clearMsg.setCellState(new com.collab.spreadsheet.common.crdt.CellState(
                "CLEAR_SHEET",
                new com.collab.spreadsheet.common.crdt.HybridLogicalClock(System.currentTimeMillis(), 0, "server"),
                "server",
                userId,
                "ACTION",
                "{}"
        ));
        redisPubSubBroadcaster.broadcast(sheetId, clearMsg);

        messagingTemplate.convertAndSend("/topic/sheet/" + sheetId + "/cells", response);
        messagingTemplate.convertAndSend("/topic/sheet/" + sheetId + "/comments", response);
        messagingTemplate.convertAndSend("/topic/sheet/" + sheetId + "/updates", response);
    }

    @MessageMapping("/workbook/global/update")
    public void handleGlobalWorkbookUpdate(@Payload Object workbookPayload) {
        log.info("Broadcasting global workbook update");
        messagingTemplate.convertAndSend("/topic/workbook/global/update", workbookPayload);
    }

    @MessageMapping("/sheet/{sheetId}/protect")
    public void handleProtectRange(
            @DestinationVariable String sheetId,
            @Payload Object protectPayload) {
        log.info("Broadcasting protected range on sheet {}", sheetId);
        messagingTemplate.convertAndSend("/topic/sheet/" + sheetId + "/protected-ranges", protectPayload);
    }
}
