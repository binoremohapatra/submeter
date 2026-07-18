package com.submeter.config;

import com.submeter.security.rbac.RbacInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * MVC configuration: registers the RBAC interceptor and CORS rules.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RbacInterceptor rbacInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rbacInterceptor)
                // Apply to all API paths; the interceptor skips endpoints without @RequiresRole
                .addPathPatterns("/**")
                // Exclude auth endpoints (they're public and have no @RequiresRole annotation)
                .excludePathPatterns("/auth/**", "/webhooks/**", "/actuator/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // In prod, restrict to the exact frontend origin.
                // In dev, allow localhost:3000 (Next.js dev server).
              .allowedOriginPatterns(
                 "http://localhost:3000",
               "https://submeter-lac.vercel.app"
)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)  // Required for cookie-based auth
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get("uploads");
        registry.addResourceHandler("/api/uploads/**")
                .addResourceLocations(uploadDir.toUri().toString());
    }
}
