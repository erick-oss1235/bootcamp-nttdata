package pe.dti.banking.accountservice;

import org.springframework.boot.SpringApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableKafka
@SpringBootApplication
public class Application {
	public static void main(String[] args) { 
		SpringApplication.run(Application.class, args);
	}
}
