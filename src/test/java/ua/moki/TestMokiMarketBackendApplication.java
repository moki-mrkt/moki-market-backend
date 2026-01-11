package ua.moki;

import org.springframework.boot.SpringApplication;

public class TestMokiMarketBackendApplication {

    public static void main(String[] args) {
        SpringApplication.from(MokiMarketBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
