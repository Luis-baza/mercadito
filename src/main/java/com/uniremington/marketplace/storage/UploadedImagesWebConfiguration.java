package com.uniremington.marketplace.storage;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadedImagesWebConfiguration implements WebMvcConfigurer {

    private final ProductoImageStorageService imageStorageService;

    public UploadedImagesWebConfiguration(ProductoImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(imageStorageService.resourceLocation());
    }
}
