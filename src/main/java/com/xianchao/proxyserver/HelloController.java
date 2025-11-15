package com.xianchao.proxyserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/")
public class HelloController {

    @GetMapping("say")
    public String world() {
        return "Hello, World!";
    }
}
