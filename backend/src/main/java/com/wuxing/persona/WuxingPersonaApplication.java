package com.wuxing.persona;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.wuxing.persona.mapper")
@SpringBootApplication
public class WuxingPersonaApplication {

    public static void main(String[] args) {
        SpringApplication.run(WuxingPersonaApplication.class, args);
    }
}
