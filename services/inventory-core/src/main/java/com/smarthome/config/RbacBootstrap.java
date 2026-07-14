package com.smarthome.config;

import com.smarthome.service.OrganizationMigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
@RequiredArgsConstructor
public class RbacBootstrap implements ApplicationRunner {

    private final OrganizationMigrationService organizationMigrationService;

    @Override
    public void run(ApplicationArguments args) {
        organizationMigrationService.migrateIfNeeded();
    }
}
