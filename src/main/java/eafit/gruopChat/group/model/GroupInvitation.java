package eafit.gruopChat.group.model;
import java.time.LocalDateTime;
import eafit.gruopChat.shared.enums.InvitationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
@Entity
@Table(name = "group_invitations")
public class GroupInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_id")
    private Long invitationId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Group group;
    @Column(name = "invited_by", nullable = false)
    private Long invitedBy;
    @Column(name = "invited_user_id", nullable = false)
    private Long invitedUserId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status = InvitationStatus.PENDING;
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
    @PrePersist
    protected void onCreate() { this.sentAt = LocalDateTime.now(); }
    public Long getInvitationId() { return invitationId; }
    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
    public Long getInvitedBy() { return invitedBy; }
    public void setInvitedBy(Long invitedBy) { this.invitedBy = invitedBy; }
    public Long getInvitedUserId() { return invitedUserId; }
    public void setInvitedUserId(Long invitedUserId) { this.invitedUserId = invitedUserId; }
    public InvitationStatus getStatus() { return status; }
    public void setStatus(InvitationStatus status) { this.status = status; }
    public LocalDateTime getSentAt() { return sentAt; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
}
