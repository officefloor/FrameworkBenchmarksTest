package net.officefloor.benchmark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
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
 * Tests cached queries.
 */
public class CachedTest {

	public static final String URL = "http://localhost:8181/cached-worlds?count=";

	public static final SystemPropertiesRule systemProperties = BenchmarkEnvironment.createSystemProperties();

	public static final PostgreSqlRule dataSource = BenchmarkEnvironment.createPostgreSqlRule();

	public static final SetupWorldTableRule setupWorldTable = new SetupWorldTableRule(dataSource);

	public static final OfficeFloorRule server = new OfficeFloorRule();

	public static final HttpClientRule client = new HttpClientRule();

	@ClassRule
	public static final RuleChain order = RuleChain.outerRule(systemProperties).around(dataSource)
			.around(setupWorldTable).around(server).around(client);

	protected String getServerName() {
		return "O";
	}

	@Test
	public void ensureHandleZero() throws Exception {
		this.doTest("0", 1);
	}

	@Test
	public void ensureHandleBlank() throws Exception {
		this.doTest("", 1);
	}

	@Test
	public void ensureHandleFoo() throws Exception {
		this.doTest("foo", 1);
	}

	@Test
	public void ensureHandleLarge() throws Exception {
		this.doTest("501", 500);
	}

	@Test
	public void ensureMultiple() throws Exception {
		this.doTest("20", 20);
	}

	private void doTest(String queriesValue, int expectedRows) throws Exception {
		HttpResponse response = client.execute(new HttpGet(URL + queriesValue));
		String entity = EntityUtils.toString(response.getEntity());
		assertEquals("Should be successful:\n\n" + entity, 200, response.getStatusLine().getStatusCode());
		assertEquals("Incorrect content-type", "application/json", response.getFirstHeader("content-type").getValue());
		assertEquals("Incorrect server", this.getServerName(), response.getFirstHeader("Server").getValue());
		assertNotNull("Should have date", response.getFirstHeader("date"));
		WorldResponse[] worlds = new ObjectMapper().readValue(entity, WorldResponse[].class);
		assertEquals("Incorrect number of worlds", expectedRows, worlds.length);
		for (WorldResponse world : worlds) {
			assertTrue("Invalid id: " + world.id, (world.id >= 1) && (world.id <= 10000));
		}
	}

	@Test
	public void validate() throws Exception {
		BenchmarkEnvironment.doValidateTest(URL + "20");
	}

	@Test
	public void stress() throws Exception {
		BenchmarkEnvironment.doRequestResponseStressTest(URL + "20");
	}

	@Data
	public static class WorldResponse {
		private int id;
		private int randomNumber;
	}

}
