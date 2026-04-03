package eafit.gruopChat;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRabbit
public class MessagingServiceApplication  {

	public static void main(String[] args) {
		SpringApplication.run(MessagingServiceApplication .class, args);
	}

}
