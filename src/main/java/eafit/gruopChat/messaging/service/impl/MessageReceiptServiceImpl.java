package eafit.gruopChat.messaging.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eafit.gruopChat.grpc.UserGrpcClient;
import eafit.gruopChat.grpc.UserResponse;
import eafit.gruopChat.group.repository.GroupMemberRepository;
import eafit.gruopChat.messaging.dto.ReceiptEvent;
import eafit.gruopChat.messaging.dto.ReceiptResponseDTO;
import eafit.gruopChat.messaging.model.Message;
import eafit.gruopChat.messaging.model.MessageReceipt;
import eafit.gruopChat.messaging.repository.MessageReceiptRepository;
import eafit.gruopChat.messaging.repository.MessageRepository;
import eafit.gruopChat.messaging.service.MessageReceiptService;
import eafit.gruopChat.shared.enums.MessageStatus;
import eafit.gruopChat.user.exception.UserNotFoundException;

import java.util.List;

@Service
@Transactional
public class MessageReceiptServiceImpl implements MessageReceiptService {

    private final MessageReceiptRepository receiptRepository;
    private final MessageRepository        messageRepository;
    private final UserGrpcClient           userGrpcClient;
    private final GroupMemberRepository    memberRepository;

    public MessageReceiptServiceImpl(
            MessageReceiptRepository receiptRepository,
            MessageRepository messageRepository,
            UserGrpcClient userGrpcClient,
            GroupMemberRepository memberRepository) {
        this.receiptRepository = receiptRepository;
        this.messageRepository = messageRepository;
        this.userGrpcClient    = userGrpcClient;
        this.memberRepository  = memberRepository;
    }

    @Override
    public ReceiptResponseDTO markAsRead(Long userId, ReceiptEvent event) {

        UserResponse user = userGrpcClient.getUserById(String.valueOf(userId))
                .orElseThrow(() -> new UserNotFoundException(userId));

        Message message = messageRepository.findById(event.messageId())
                .orElseThrow(() -> new RuntimeException("Mensaje no encontrado: " + event.messageId()));

        // Si el sender se ve a sí mismo, ignorar
        if (message.getSenderId().equals(userId)) {
            return buildResponse(event, userId, user.getUsername(), message.getStatus());
        }

        // Si ya existe el receipt, no duplicar
        boolean yaLeyo = receiptRepository
                .findByMessageMessageIdAndUserId(event.messageId(), userId)
                .isPresent();

        if (!yaLeyo) {
            MessageReceipt receipt = new MessageReceipt();
            receipt.setMessage(message);
            receipt.setUserId(userId);
            receiptRepository.save(receipt);
        }

        MessageStatus nuevoStatus = calcularStatus(message, event.groupId(), event.channelId());
        message.setStatus(nuevoStatus);

        return buildResponse(event, userId, user.getUsername(), nuevoStatus);
    }

    @Override
    public void markPendingAsDelivered(Long userId, Long groupId) {
        List<Message> pendientes = messageRepository
                .findByGroupGroupIdAndStatus(groupId, MessageStatus.SENT);

        for (Message msg : pendientes) {
            if (!msg.getSenderId().equals(userId)) {
                msg.setStatus(MessageStatus.DELIVERED);
            }
        }
    }

    private MessageStatus calcularStatus(Message message, Long groupId, Long channelId) {
        long totalMiembros = memberRepository.countByGroupGroupId(groupId) - 1;
        if (totalMiembros <= 0) return MessageStatus.READ;

        long totalQueHanLeido = receiptRepository
                .countByMessageMessageId(message.getMessageId());

        return (totalQueHanLeido >= totalMiembros)
                ? MessageStatus.READ
                : MessageStatus.DELIVERED;
    }

    private ReceiptResponseDTO buildResponse(
            ReceiptEvent event, Long userId, String userName, MessageStatus status) {
        return new ReceiptResponseDTO(
                event.messageId(),
                event.groupId(),
                event.channelId(),
                userId,
                userName,
                status
        );
    }
}