package eafit.gruopChat.messaging.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import eafit.gruopChat.infrastructure.config.RabbitMQConfig;
import eafit.gruopChat.messaging.dto.MessageResponseDTO;

@Component
public class MessageEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public MessageEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(MessageResponseDTO message) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                "",
                message
        );
    }
}