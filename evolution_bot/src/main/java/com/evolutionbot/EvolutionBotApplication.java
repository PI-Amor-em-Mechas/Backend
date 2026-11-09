package com.evolutionbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EvolutionBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvolutionBotApplication.class, args);
    }
}
