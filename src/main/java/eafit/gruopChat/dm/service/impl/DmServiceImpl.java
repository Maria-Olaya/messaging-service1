package eafit.gruopChat.dm.service.impl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import eafit.gruopChat.dm.dto.ConversationResponseDTO;
import eafit.gruopChat.dm.dto.DmMessageRequestDTO;
import eafit.gruopChat.dm.dto.DmMessageResponseDTO;
import eafit.gruopChat.dm.model.DirectConversation;
import eafit.gruopChat.dm.model.DirectConversation.Status;
import eafit.gruopChat.dm.model.DmMessage;
import eafit.gruopChat.dm.repository.DirectConversationRepository;
import eafit.gruopChat.dm.repository.DmMessageRepository;
import eafit.gruopChat.dm.service.DmService;
import eafit.gruopChat.grpc.UserGrpcClient;
import eafit.gruopChat.grpc.UserResponse;
import eafit.gruopChat.group.repository.GroupMemberRepository;
import eafit.gruopChat.shared.enums.MessageType;
import eafit.gruopChat.user.exception.UserNotFoundException;
@Service
@Transactional
public class DmServiceImpl implements DmService {
    private final DirectConversationRepository convRepo;
    private final DmMessageRepository          msgRepo;
    private final UserGrpcClient               userGrpcClient;
    private final GroupMemberRepository        memberRepo;
    public DmServiceImpl(DirectConversationRepository convRepo, DmMessageRepository msgRepo,
                         UserGrpcClient userGrpcClient, GroupMemberRepository memberRepo) {
        this.convRepo       = convRepo;
        this.msgRepo        = msgRepo;
        this.userGrpcClient = userGrpcClient;
        this.memberRepo     = memberRepo;
    }
    @Override
    public ConversationResponseDTO startConversation(Long requestingUserId, String targetEmail) {
        if (!userGrpcClient.existsUser(String.valueOf(requestingUserId))) throw new UserNotFoundException(requestingUserId);
        UserResponse target = userGrpcClient.getUserByEmail(targetEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + targetEmail));
        Long targetId = Long.parseLong(target.getId());
        if (requestingUserId.equals(targetId)) throw new IllegalArgumentException("No puedes iniciar una conversacion contigo mismo");
        return convRepo.findBetween(requestingUserId, targetId)
                .map(dc -> toDTO(dc, requestingUserId))
                .orElseGet(() -> createNew(requestingUserId, targetId));
    }
    private ConversationResponseDTO createNew(Long meId, Long targetId) {
        boolean shareGroup = shareAnyGroup(meId, targetId);
        DirectConversation dc = new DirectConversation();
        if (meId < targetId) { dc.setUserAId(meId); dc.setUserBId(targetId); }
        else                 { dc.setUserAId(targetId); dc.setUserBId(meId); }
        dc.setRequestedById(meId);
        dc.setStatus(shareGroup ? Status.ACTIVE : Status.PENDING);
        return toDTO(convRepo.save(dc), meId);
    }
    private boolean shareAnyGroup(Long uid1, Long uid2) {
        Set<Long> groupsA = memberRepo.findByUserId(uid1).stream().map(m -> m.getGroup().getGroupId()).collect(Collectors.toSet());
        return memberRepo.findByUserId(uid2).stream().anyMatch(m -> groupsA.contains(m.getGroup().getGroupId()));
    }
    @Override @Transactional(readOnly = true)
    public List<ConversationResponseDTO> listConversations(Long userId) {
        return convRepo.findAllByUserId(userId).stream().map(dc -> toDTO(dc, userId)).collect(Collectors.toList());
    }
    @Override @Transactional(readOnly = true)
    public List<ConversationResponseDTO> listPendingRequests(Long userId) {
        return convRepo.findPendingIncoming(userId, Status.PENDING).stream().map(dc -> toDTO(dc, userId)).collect(Collectors.toList());
    }
    @Override
    public ConversationResponseDTO acceptRequest(Long conversationId, Long userId) {
        DirectConversation dc = findConv(conversationId);
        assertIsRecipient(dc, userId);
        assertStatus(dc, Status.PENDING);
        dc.setStatus(Status.ACTIVE);
        dc.setRespondedAt(LocalDateTime.now());
        return toDTO(dc, userId);
    }
    @Override
    public void declineRequest(Long conversationId, Long userId) {
        DirectConversation dc = findConv(conversationId);
        assertIsRecipient(dc, userId);
        assertStatus(dc, Status.PENDING);
        convRepo.delete(dc);
    }
    @Override @Transactional(readOnly = true)
    public List<DmMessageResponseDTO> getMessages(Long conversationId, Long requestingUserId, int page, int size) {
        DirectConversation dc = findConv(conversationId);
        assertParticipant(dc, requestingUserId);
        if (dc.getStatus() == Status.PENDING && !dc.getRequestedById().equals(requestingUserId))
            throw new IllegalArgumentException("Acepta la solicitud para ver los mensajes");
        return msgRepo.findByConversationId(conversationId, PageRequest.of(page, size)).stream().map(this::toMsgDTO).collect(Collectors.toList());
    }
    @Override
    public DmMessageResponseDTO sendMessage(Long senderId, DmMessageRequestDTO req) {
        DirectConversation dc = findConv(req.conversationId());
        assertParticipant(dc, senderId);
        if (dc.getStatus() == Status.PENDING && !dc.getRequestedById().equals(senderId))
            throw new IllegalArgumentException("Acepta la solicitud para poder responder");
        UserResponse sender = userGrpcClient.getUserById(String.valueOf(senderId)).orElseThrow(() -> new UserNotFoundException(senderId));
        if (req.type() == MessageType.TEXT && (req.content() == null || req.content().isBlank()))
            throw new IllegalArgumentException("El contenido no puede estar vacio");
        DmMessage msg = new DmMessage();
        msg.setConversation(dc);
        msg.setSenderId(senderId);
        msg.setSenderName(sender.getUsername());
        msg.setType(req.type() != null ? req.type() : MessageType.TEXT);
        msg.setContent(req.content());
        msg.setFileUrl(req.fileUrl());
        msg.setFileName(req.fileName());
        return toMsgDTO(msgRepo.save(msg));
    }
    @Override
    public DmMessageResponseDTO deleteMessage(Long messageId, Long requestingUserId) {
        DmMessage msg = msgRepo.findById(messageId).orElseThrow(() -> new RuntimeException("Mensaje no encontrado: " + messageId));
        if (!msg.getSenderId().equals(requestingUserId)) throw new IllegalArgumentException("Solo el autor puede eliminar el mensaje");
        msg.setDeleted(true);
        return toMsgDTO(msg);
    }
    private DirectConversation findConv(Long id) {
        return convRepo.findById(id).orElseThrow(() -> new RuntimeException("Conversacion no encontrada: " + id));
    }
    private void assertParticipant(DirectConversation dc, Long userId) {
        if (!dc.getUserAId().equals(userId) && !dc.getUserBId().equals(userId))
            throw new IllegalArgumentException("No eres participante de esta conversacion");
    }
    private void assertIsRecipient(DirectConversation dc, Long userId) {
        if (dc.getRequestedById().equals(userId)) throw new IllegalArgumentException("No puedes aceptar tu propia solicitud");
        assertParticipant(dc, userId);
    }
    private void assertStatus(DirectConversation dc, Status expected) {
        if (dc.getStatus() != expected) throw new IllegalArgumentException("Estado invalido: " + dc.getStatus());
    }
    private ConversationResponseDTO toDTO(DirectConversation dc, Long requestingUserId) {
        Long otherId = dc.getUserAId().equals(requestingUserId) ? dc.getUserBId() : dc.getUserAId();
        UserResponse other = userGrpcClient.getUserById(String.valueOf(otherId)).orElse(null);
        boolean isIncoming = dc.getStatus() == Status.PENDING && !dc.getRequestedById().equals(requestingUserId);
        return new ConversationResponseDTO(dc.getConversationId(), otherId,
                other != null ? other.getUsername() : null,
                other != null ? other.getEmail() : null,
                dc.getStatus(), isIncoming, dc.getCreatedAt());
    }
    private DmMessageResponseDTO toMsgDTO(DmMessage m) {
        return new DmMessageResponseDTO(m.getMessageId(), m.getConversation().getConversationId(),
                m.getSenderId(), m.getSenderName(), m.getType(),
                m.isDeleted() ? null : m.getContent(),
                m.isDeleted() ? null : m.getFileUrl(),
                m.isDeleted() ? null : m.getFileName(),
                m.getSentAt(), m.isDeleted());
    }
}
