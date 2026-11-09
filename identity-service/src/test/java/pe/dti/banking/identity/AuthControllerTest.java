package pe.dti.banking.identity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class AuthControllerTest {

	private final AuthController controller = new AuthController("admin",
			new BCryptPasswordEncoder().encode("local-test-password"), "test-key-with-more-than-32-characters-1234");

	@Test
	void createsSignedJwtForValidCredentials() { // Crear JWT firmado con credenciales válidas
		AuthController.Token token = controller.login(new AuthController.Credentials("admin", "local-test-password"));

		assertEquals("Bearer", token.tokenType());
		assertEquals(3600, token.expiresIn());
		assertEquals(3, token.accessToken().split("\\.").length);
	}

	@Test
	void rejectsWrongPassword() { // Rechazar contraseña incorrecta
		assertThrows(ResponseStatusException.class,
				() -> controller.login(new AuthController.Credentials("admin", "wrong-password")));
	}
}
