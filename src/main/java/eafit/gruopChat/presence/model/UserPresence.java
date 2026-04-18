package eafit.gruopChat.presence.model;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
@Entity
@Table(name = "user_presence")
public class UserPresence {
    @Id
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
}
