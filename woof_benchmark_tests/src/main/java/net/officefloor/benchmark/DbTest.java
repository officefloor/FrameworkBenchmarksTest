package net.officefloor.benchmark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import net.officefloor.jdbc.postgresql.test.PostgreSqlRule;
import net.officefloor.server.http.HttpClientRule;
import net.officefloor.test.OfficeFloorRule;
import net.officefloor.test.system.SystemPropertiesRule;

/**
 * Tests single query.
 */
public class DbTest {

	public static final String URL = "http://localhost:8181/db";

	public static final SystemPropertiesRule systemProperties = BenchmarkEnvironment.createSystemProperties();

	public static final PostgreSqlRule dataSource = BenchmarkEnvironment.createPostgreSqlRule();

	public static final OfficeFloorRule server = new OfficeFloorRule();

	public static final HttpClientRule client = new HttpClientRule();

	@ClassRule
	public static final RuleChain order = RuleChain.outerRule(systemProperties).around(dataSource).around(server)
			.around(client);

	public static void setupDatabase(Connection connection) throws Exception {
		try {
			connection.createStatement().executeQuery("SELECT * FROM World");
		} catch (SQLException ex) {
			connection.createStatement().executeUpdate("CREATE TABLE World ( id INT PRIMARY KEY, randomNumber INT)");
			PreparedStatement insert = connection
					.prepareStatement("INSERT INTO World (id, randomNumber) VALUES (?, ?)");
			for (int i = 0; i < 10000; i++) {
				insert.setInt(1, i + 1);
				insert.setInt(2, ThreadLocalRandom.current().nextInt(1, 10000));
				insert.executeUpdate();
			}
		}
	}

	@BeforeClass
	public static void setupDatabase() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			setupDatabase(connection);
		}
	}

	protected String getServerName() {
		return "O";
	}

	@Test
	public void validRequest() throws Exception {
		HttpResponse response = client.execute(new HttpGet(URL));
		String entity = EntityUtils.toString(response.getEntity());
		assertEquals("Should be successful:\n\n" + entity, 200, response.getStatusLine().getStatusCode());
		assertEquals("Incorrect content-type", "application/json", response.getFirstHeader("content-type").getValue());
		assertEquals("Incorrect server", this.getServerName(), response.getFirstHeader("Server").getValue());
		assertNotNull("Should have date", response.getFirstHeader("date"));
		WorldResponse world = new ObjectMapper().readValue(entity, WorldResponse.class);
		assertTrue("Invalid id: " + world.id, (world.id >= 1) && (world.id <= 10000));
		assertTrue("Invalid randomNumber: " + world.randomNumber,
				(world.randomNumber >= 1) && (world.randomNumber <= 10000));
	}

	@Test
	public void validate() throws Exception {
		BenchmarkEnvironment.doValidateTest(URL);
	}

	@Test
	public void stress() throws Exception {
		BenchmarkEnvironment.doRequestResponseStressTest(URL);
	}

	@Data
	public static class WorldResponse {
		private int id;
		private int randomNumber;
	}

}
