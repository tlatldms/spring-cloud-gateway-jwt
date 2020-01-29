package com.quadcore.gw2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping;
import org.springframework.cloud.gateway.route.RouteLocator;

import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.HashMap;

@SpringBootApplication
public class Gw2Application {

    public static void main(String[] args) {
        SpringApplication.run(Gw2Application.class, args);
    }


    @Bean
    public CorsConfiguration corsConfiguration(RoutePredicateHandlerMapping routePredicateHandlerMapping) {
        CorsConfiguration corsConfiguration = new CorsConfiguration().applyPermitDefaultValues();
        Arrays.asList(HttpMethod.OPTIONS, HttpMethod.PUT, HttpMethod.GET, HttpMethod.DELETE, HttpMethod.POST) .forEach(m -> corsConfiguration.addAllowedMethod(m));
        corsConfiguration.addAllowedOrigin("*");
        routePredicateHandlerMapping.setCorsConfigurations(new HashMap<String, CorsConfiguration>() {{ put("/**", corsConfiguration); }});
        return corsConfiguration;
    }



    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        String authserver="http://localhost:8083/";
        return builder.routes()
                .route("path_route",  r-> r.path("/test")
                        .filters(f -> f
                                .addRequestHeader("Hello", "World")
                                .rewritePath("/test", "/"))
                                .uri("http://localhost:8083/"))
                .route("auth",  r-> r.path("/auth/**")
                        .filters(f -> f
                                .rewritePath("/auth/(?<segment>.*)", "/auth/${segment}")
                                .hystrix(config -> config
                                .setName("fallbackpoint")
                                .setFallbackUri("forward:/fallback")))
                        .uri(authserver))
                .build();
    }
}
