package com.xianchao.proxyserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class ProxyServerApplication {

    public static void main(String[] args) throws IOException {
        SpringApplication.run(ProxyServerApplication.class, args);
    }

}