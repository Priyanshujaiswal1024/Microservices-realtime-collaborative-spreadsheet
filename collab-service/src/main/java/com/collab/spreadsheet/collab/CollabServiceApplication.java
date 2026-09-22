package com.collab.spreadsheet.collab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;


@SpringBootApplication
@EnableDiscoveryClient
@EnableKafka
public class CollabServiceApplication {

    public static void main(String[] args) {
        System.out.println("this is Collab Service Application");

        SpringApplication.run(CollabServiceApplication.class,  args);

    }
}

