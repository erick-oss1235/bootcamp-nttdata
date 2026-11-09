package pe.dti.banking.gateway;

import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;


@Configuration
public class GatewaySecurity {
	@Bean
	SecurityWebFilterChain chain(ServerHttpSecurity http) { // caadena de seguridad
		return http.csrf(ServerHttpSecurity.CsrfSpec::disable).authorizeExchange(
				ex -> ex.pathMatchers("/api/v1/auth/**", "/actuator/health").permitAll().anyExchange().authenticated())
				.oauth2ResourceServer(o -> o.jwt(jwt -> {
				})).build();
	}

	@Bean
	ReactiveJwtDecoder jwtDecoder(@Value("${banking.jwt-secret}") String secret) { // JWT
		return NimbusReactiveJwtDecoder
				.withSecretKey(
						new SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"))
				.macAlgorithm(MacAlgorithm.HS256).build();
	}
}
