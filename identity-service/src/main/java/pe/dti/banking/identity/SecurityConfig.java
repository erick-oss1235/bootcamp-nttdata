package pe.dti.banking.identity;

import org.springframework.context.annotation.*;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {
	
	@Bean
	SecurityWebFilterChain chain(ServerHttpSecurity http) { 
		return http.csrf(ServerHttpSecurity.CsrfSpec::disable).authorizeExchange(ex -> ex.anyExchange().permitAll())
				.build();
	}
}
