package com.earacg.earaconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EaraconnectApplication {

	public static void main(String[] args) {
		SpringApplication.run(EaraconnectApplication.class, args);
	}

}
