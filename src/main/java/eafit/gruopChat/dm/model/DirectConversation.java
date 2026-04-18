package eafit.gruopChat.dm.model;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
@Entity
@Table(name = "direct_conversations", uniqueConstraints = { @UniqueConstraint(columnNames = {"user_a_id", "user_b_id"}) })
public class DirectConversation {
    public enum Status { PENDING, ACTIVE }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversation_id")
    private Long conversationId;
    @Column(name = "user_a_id", nullable = false)
    private Long userAId;
    @Column(name = "user_b_id", nullable = false)
    private Long userBId;
    @Column(name = "requested_by_id", nullable = false)
    private Long requestedById;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }
    public Long getConversationId()             { return conversationId; }
    public Long getUserAId()                    { return userAId; }
    public void setUserAId(Long userAId)        { this.userAId = userAId; }
    public Long getUserBId()                    { return userBId; }
    public void setUserBId(Long userBId)        { this.userBId = userBId; }
    public Long getRequestedById()              { return requestedById; }
    public void setRequestedById(Long id)       { this.requestedById = id; }
    public Status getStatus()                   { return status; }
    public void setStatus(Status s)             { this.status = s; }
    public LocalDateTime getCreatedAt()         { return createdAt; }
    public LocalDateTime getRespondedAt()       { return respondedAt; }
    public void setRespondedAt(LocalDateTime t) { this.respondedAt = t; }
}
