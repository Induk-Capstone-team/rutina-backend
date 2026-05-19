package com.rutina.rutinabackend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OfficialPageConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/official")
                .setViewName("forward:/official/index.html");

        registry.addViewController("/official/")
                .setViewName("forward:/official/index.html");
    }
}