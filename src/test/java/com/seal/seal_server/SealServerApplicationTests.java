package com.seal.seal_server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class SealServerApplicationTests {
	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoadsAndConnectsToNeon() throws Exception {
		try (var connection = dataSource.getConnection();
			 var statement = connection.createStatement();
			 var result = statement.executeQuery("SELECT 1")) {
			assertTrue(result.next());
			assertEquals(1, result.getInt(1));
		}
	}

}
