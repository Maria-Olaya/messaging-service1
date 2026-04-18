package eafit.gruopChat.presence.service.impl;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eafit.gruopChat.grpc.UserGrpcClient;
import eafit.gruopChat.grpc.UserResponse;
import eafit.gruopChat.group.repository.GroupMemberRepository;
import eafit.gruopChat.messaging.service.MessageReceiptService;
import eafit.gruopChat.presence.dto.PresenceEventDTO;
import eafit.gruopChat.presence.model.UserPresence;
import eafit.gruopChat.presence.repository.UserPresenceRepository;
import eafit.gruopChat.presence.service.PresenceService;

@Service
@Transactional
public class PresenceServiceImpl implements PresenceService {

    private final Set<Long> onlineUsers = ConcurrentHashMap.newKeySet();

    private final UserPresenceRepository presenceRepository;
    private final GroupMemberRepository  memberRepository;
    private final UserGrpcClient         userGrpcClient;
    private final SimpMessagingTemplate  messagingTemplate;
    private final MessageReceiptService  receiptService;

    public PresenceServiceImpl(UserPresenceRepository presenceRepository,
                               GroupMemberRepository memberRepository,
                               UserGrpcClient userGrpcClient,
                               SimpMessagingTemplate messagingTemplate,
                               MessageReceiptService receiptService) {
        this.presenceRepository = presenceRepository;
        this.memberRepository   = memberRepository;
        this.userGrpcClient     = userGrpcClient;
        this.messagingTemplate  = messagingTemplate;
        this.receiptService     = receiptService;
    }

    @Override
    public void userConnected(Long userId) {
        onlineUsers.add(userId);
        memberRepository.findByUserId(userId).forEach(member ->
            receiptService.markPendingAsDelivered(userId, member.getGroup().getGroupId())
        );
        broadcastPresence(userId, true);
    }

    @Override
    public void userDisconnected(Long userId) {
        onlineUsers.remove(userId);

        LocalDateTime now = LocalDateTime.now();
        // Solo guardar presence si el usuario existe en el user-service
        if (userGrpcClient.existsUser(String.valueOf(userId))) {
            UserPresence presence = presenceRepository.findById(userId)
                    .orElseGet(() -> {
                        UserPresence p = new UserPresence();
                        p.setUserId(userId);   // <-- ver nota abajo
                        return p;
                    });
            presence.setLastSeen(now);
            presenceRepository.save(presence);
        }

        broadcastPresence(userId, false);
    }

    @Override
    public boolean isOnline(Long userId) {
        return onlineUsers.contains(userId);
    }

    @Override
    public LocalDateTime getLastSeen(Long userId) {
        return presenceRepository.findById(userId)
                .map(UserPresence::getLastSeen)
                .orElse(null);
    }

    private void broadcastPresence(Long userId, boolean online) {
        UserResponse user = userGrpcClient.getUserById(String.valueOf(userId)).orElse(null);
        if (user == null) return;

        LocalDateTime lastSeen = online ? null : getLastSeen(userId);
        PresenceEventDTO event = new PresenceEventDTO(
                userId, user.getUsername(), online, lastSeen);

        memberRepository.findByUserId(userId).forEach(member ->
            messagingTemplate.convertAndSend(
                "/topic/presence." + member.getGroup().getGroupId(), event)
        );
    }
}