package ua.moki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class MokiMarketBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MokiMarketBackendApplication.class, args);
    }

}
