package com.mtj.DouyinBackend.star;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class StarUserDataInitializer implements CommandLineRunner {

    private final StarUserService service;

    public StarUserDataInitializer(StarUserService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        service.initDataIfNeeded();
    }
}
