package com.quadcore.gw2.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;



@Order(-1)
@Component
public class JwtRequestFilter implements GlobalFilter {

    final Logger logger =
            LoggerFactory.getLogger(JwtRequestFilter.class);

    @Autowired
    private JwtValidator jwtValidator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        try {
            String token = exchange.getRequest().getHeaders().get("Authorization").get(0).substring(7);
            logger.info("token : " + token);
            if (jwtValidator.isValidate(token)) {
                Map<String, Object> userInfo = jwtValidator.getUserParseInfo(token);
                logger.info("Request user info: " + userInfo);
            } else {
                logger.info("not valid");
            }
        }

        catch (NullPointerException e) {
            logger.warn("no token. Client will be directed to login page");
            exchange.getResponse().getHeaders().set("status", "401");
            //exchange.getResponse().setStatusCode(HttpStatus.valueOf(401));
        }
        return chain.filter(exchange);
    }
}