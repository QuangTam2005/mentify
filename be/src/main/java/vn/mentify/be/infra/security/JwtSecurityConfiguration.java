package vn.mentify.be.infra.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import vn.mentify.be.modules.auth.security.JwtProperties;
import vn.mentify.be.modules.auth.security.JwtTokenService;

@Configuration
@ConditionalOnProperty(name = "app.security.jwt.enabled", havingValue = "true")
@EnableConfigurationProperties(JwtProperties.class)
public class JwtSecurityConfiguration {

    @Bean
    JwtDecoder jwtDecoder(JwtTokenService jwtTokenService) {
        return jwtTokenService.decoder();
    }
}
