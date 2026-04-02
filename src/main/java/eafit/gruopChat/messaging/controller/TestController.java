package eafit.gruopChat.messaging.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eafit.gruopChat.messaging.dto.MessageResponseDTO;
import eafit.gruopChat.messaging.event.MessageEventPublisher;


@RestController
@RequestMapping("/test")
public class TestController {

    private final MessageEventPublisher publisher;

    public TestController(MessageEventPublisher publisher) {
        this.publisher = publisher;
    }

    @GetMapping("/send")
    public String send() {

        publisher.publish(new MessageResponseDTO(
                1L,
                1L,
                null,
                1L,
                "mari",
                null,
                "hola rabbit",
                null,
                null,
                java.time.LocalDateTime.now(),
                null,
                false,
                null
        ));

        return "mensaje enviado";
    }
}