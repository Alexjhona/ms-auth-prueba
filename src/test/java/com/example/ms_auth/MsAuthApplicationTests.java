package com.example.ms_auth;

import com.example.ms_auth.repository.AuthUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
				"org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
				"org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
})
class MsAuthApplicationTests {

	@MockBean
	private AuthUserRepository authUserRepository;

	@Test
	void contextLoads() {
	}

}
