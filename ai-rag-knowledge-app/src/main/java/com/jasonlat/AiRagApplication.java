package com.jasonlat;


import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@Configurable
@SpringBootApplication
@EnableEncryptableProperties
@ComponentScan(basePackages = {"com.jasonlat", "cc.jq1024.middleware"})
public class AiRagApplication {

    @Value("${app.config.cross-origin}")
    private String crossOrigin;
    @PostConstruct
    public void init() {
        log.info("Cross Origin: {}", crossOrigin);
    }

    public static void main(String[] args) {
        SpringApplication.run(AiRagApplication.class);
        log.info("Ai Rag Application Started...");
    }

}
