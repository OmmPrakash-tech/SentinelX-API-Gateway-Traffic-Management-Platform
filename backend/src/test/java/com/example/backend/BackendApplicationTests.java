package com.example.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties={"spring.datasource.url=${TEST_DATABASE_URL:jdbc:h2:mem:context;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE}","sentinel.seed=false"})
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
