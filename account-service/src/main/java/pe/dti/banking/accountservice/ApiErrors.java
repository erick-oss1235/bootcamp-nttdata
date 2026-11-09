package pe.dti.banking.accountservice;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import java.util.Map;
import org.springframework.web.bind.support.WebExchangeBindException;


@RestControllerAdvice
public class ApiErrors {
	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, Object>> onStatus(ResponseStatusException e) {
		return ResponseEntity.status(e.getStatusCode())
				.body(Map.of("error", e.getReason() == null ? "No se pudo completar la solicitud." : e.getReason()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> onInvalid(IllegalArgumentException e) { 
		return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
	}

	@ExceptionHandler(WebExchangeBindException.class)
	public ResponseEntity<Map<String, Object>> onValidation(WebExchangeBindException exception) { 
		return ResponseEntity.badRequest()
				.body(Map.of("error", "Los datos enviados no cumplen las validaciones requeridas."));
	}
}
