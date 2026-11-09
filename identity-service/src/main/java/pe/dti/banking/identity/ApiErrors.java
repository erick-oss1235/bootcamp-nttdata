package pe.dti.banking.identity;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, Object>> onStatus(ResponseStatusException exception) { 
		String message = exception.getReason() == null ? "No se pudo completar la solicitud." : exception.getReason();
		return ResponseEntity.status(exception.getStatusCode()).body(Map.of("error", message));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> onInvalid(IllegalArgumentException exception) { 
		return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
	}

	@ExceptionHandler(WebExchangeBindException.class)
	public ResponseEntity<Map<String, Object>> onValidation(WebExchangeBindException exception) { 
		return ResponseEntity.badRequest()
				.body(Map.of("error", "Los datos enviados no cumplen las validaciones requeridas."));
	}
}
