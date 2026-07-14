package com.smarthome.security;

import com.smarthome.security.jwt.JwtAuthenticationFilter;
import com.smarthome.security.jwt.JwtService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan(basePackageClasses = {JwtService.class, JwtAuthenticationFilter.class})
public class CommonSecurityAutoConfiguration {
}
