package eafit.gruopChat.group.model;
import java.time.LocalDateTime;
import eafit.gruopChat.shared.enums.GroupRole;
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
import jakarta.persistence.UniqueConstraint;
@Entity
@Table(name = "group_members", uniqueConstraints = { @UniqueConstraint(columnNames = {"group_id", "user_id"}) })
public class GroupMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Group group;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupRole role = GroupRole.MEMBER;
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;
    @PrePersist
    protected void onCreate() { this.joinedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public GroupRole getRole() { return role; }
    public void setRole(GroupRole role) { this.role = role; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}
