package eafit.gruopChat.dm.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import eafit.gruopChat.dm.model.DirectConversation;
import eafit.gruopChat.dm.model.DirectConversation.Status;
public interface DirectConversationRepository extends JpaRepository<DirectConversation, Long> {
    @Query("SELECT dc FROM DirectConversation dc WHERE (dc.userAId = :uid1 AND dc.userBId = :uid2) OR (dc.userAId = :uid2 AND dc.userBId = :uid1)")
    Optional<DirectConversation> findBetween(@Param("uid1") Long uid1, @Param("uid2") Long uid2);
    @Query("SELECT dc FROM DirectConversation dc WHERE dc.userAId = :userId OR dc.userBId = :userId ORDER BY dc.createdAt DESC")
    List<DirectConversation> findAllByUserId(@Param("userId") Long userId);
    @Query("SELECT dc FROM DirectConversation dc WHERE dc.status = :status AND dc.requestedById != :userId AND (dc.userAId = :userId OR dc.userBId = :userId)")
    List<DirectConversation> findPendingIncoming(@Param("userId") Long userId, @Param("status") Status status);
}
