package eafit.gruopChat.group.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import eafit.gruopChat.group.model.GroupInvitation;
import eafit.gruopChat.shared.enums.InvitationStatus;
public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {
    List<GroupInvitation> findByInvitedUserIdAndStatus(Long userId, InvitationStatus status);
    List<GroupInvitation> findByGroupGroupId(Long groupId);
    Optional<GroupInvitation> findByGroupGroupIdAndInvitedUserIdAndStatus(Long groupId, Long userId, InvitationStatus status);
}
