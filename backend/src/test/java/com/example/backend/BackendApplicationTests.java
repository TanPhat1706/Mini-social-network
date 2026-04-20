package com.example.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:testdb;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=pht",
		"spring.datasource.password=141204",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.show-sql=false"
})
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
