package eafit.gruopChat.presence.controller;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import eafit.gruopChat.grpc.UserGrpcClient;
import eafit.gruopChat.grpc.UserResponse;
import eafit.gruopChat.group.repository.GroupMemberRepository;
import eafit.gruopChat.presence.dto.PresenceEventDTO;
import eafit.gruopChat.presence.service.PresenceService;
@RestController
public class PresenceController {
    private final PresenceService       presenceService;
    private final GroupMemberRepository memberRepository;
    private final UserGrpcClient        userGrpcClient;
    public PresenceController(PresenceService presenceService,
                              GroupMemberRepository memberRepository,
                              UserGrpcClient userGrpcClient) {
        this.presenceService  = presenceService;
        this.memberRepository = memberRepository;
        this.userGrpcClient   = userGrpcClient;
    }
    @GetMapping("/api/presence/group/{groupId}")
    public ResponseEntity<List<PresenceEventDTO>> getGroupPresence(
            @PathVariable Long groupId) {
        List<PresenceEventDTO> presence = memberRepository
                .findByGroupGroupId(groupId)
                .stream()
                .map(m -> {
                    Long uid = m.getUserId();
                    boolean online = presenceService.isOnline(uid);
                    UserResponse user = userGrpcClient.getUserById(String.valueOf(uid)).orElse(null);
                    String username = user != null ? user.getUsername() : null;
                    return new PresenceEventDTO(
                            uid,
                            username,
                            online,
                            online ? null : presenceService.getLastSeen(uid)
                    );
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(presence);
    }
}