package eafit.gruopChat.group.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import eafit.gruopChat.group.model.GroupMember;
import eafit.gruopChat.shared.enums.GroupRole;
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    Optional<GroupMember> findByGroupGroupIdAndUserId(Long groupId, Long userId);
    boolean existsByGroupGroupIdAndUserId(Long groupId, Long userId);
    List<GroupMember> findByGroupGroupId(Long groupId);
    List<GroupMember> findByGroupGroupIdAndRole(Long groupId, GroupRole role);
    void deleteByGroupGroupIdAndUserId(Long groupId, Long userId);
    List<GroupMember> findByUserId(Long userId);
    long countByGroupGroupId(Long groupId);
}
