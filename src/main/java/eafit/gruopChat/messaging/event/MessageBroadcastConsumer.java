package eafit.gruopChat.messaging.event;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import eafit.gruopChat.infrastructure.config.RabbitMQConfig;
import eafit.gruopChat.messaging.dto.MessageResponseDTO;

@Component
public class MessageBroadcastConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    public MessageBroadcastConsumer(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void receive(MessageResponseDTO msg) {

        if (msg.channelId() != null) {
            messagingTemplate.convertAndSend(
                "/topic/channel." + msg.channelId(),
                msg
            );
        } else {
            messagingTemplate.convertAndSend(
                "/topic/group." + msg.groupId(),
                msg
            );
        }
    }
}