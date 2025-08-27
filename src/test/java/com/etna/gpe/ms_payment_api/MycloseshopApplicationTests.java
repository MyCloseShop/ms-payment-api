package com.etna.gpe.ms_payment_api;

import com.etna.gpe.ms_payment_api.config.TestRabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = MycloseshopApplication.class)
@ActiveProfiles("test")
@Import(TestRabbitMQConfig.class)
class MycloseshopApplicationTests {

	@Test
	void contextLoads() {
		// This test verifies that the Spring Boot application context loads successfully
		// No additional assertions are needed as the test will fail if context loading fails
	}

}
