package com.etna.gpe.ms_payment_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {
        "com.etna.gpe.mycloseshop.security_api",
        "com.etna.gpe.mycloseshop.common_api",

})
public class MycloseshopApplication {

    public static void main(String[] args) {
        SpringApplication.run(MycloseshopApplication.class, args);
    }

}
