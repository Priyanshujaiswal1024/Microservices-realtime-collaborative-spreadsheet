package com.collab.spreadsheet.common.events;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;

/**
 * Event published for user identity lifecycle events
 * 
 * Topic: user-events
 * Partition Key: email
 * Consumers: notification-service, audit-service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("USER_EVENT")
public class UserEvent extends BaseEvent {

    private static final long serialVersionUID = 1L;

    public enum Type {
        USER_REGISTERED,
        OTP_GENERATED,
        USER_VERIFIED,
        PASSWORD_RESET_REQUESTED,
        USER_LOGGED_IN
    }

    private Type type;
    private String userId;
    private String email;
    private String username;
    private String otp;
    private String purpose;

    @Builder
    public UserEvent(String actorId, Type type, String userId, String email, 
                     String username, String otp, String purpose) {
        super();
        initializeBaseFields(actorId);
        this.type = type;
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.otp = otp;
        this.purpose = purpose;
    }

    @Override
    public String getEventType() {
        return "USER_EVENT";
    }
}
