package net.officefloor.benchmark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import net.officefloor.jdbc.postgresql.test.PostgreSqlRule;
import net.officefloor.server.http.HttpClientRule;
import net.officefloor.test.OfficeFloorRule;
import net.officefloor.test.system.SystemPropertiesRule;

/**
 * Tests the plain text.
 */
public class PlaintextTest {

	public static final String URL = "http://localhost:8181/plaintext";

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
	public void validRequest() throws Exception {
		HttpResponse response = client.execute(new HttpGet(URL));
		String entity = EntityUtils.toString(response.getEntity());
		assertEquals("Should be successful:\n\n" + entity, 200, response.getStatusLine().getStatusCode());
		assertEquals("Incorrect content-type", "text/plain", response.getFirstHeader("content-type").getValue());
		assertEquals("Incorrect server", this.getServerName(), response.getFirstHeader("Server").getValue());
		assertNotNull("Should have date", response.getFirstHeader("date"));
		assertEquals("Incorrect content", "Hello, World!", entity);
	}

	@Test
	public void validate() throws Exception {
		WoofBenchmarkShared.runWithoutValidation(() -> {
			BenchmarkEnvironment.doValidateTest(URL);
			return null;
		});
	}

	@Test
	public void stress() throws Exception {
		WoofBenchmarkShared.runWithoutValidation(() -> {
			BenchmarkEnvironment.doPipelineStressTest(URL);
			return null;
		});
	}

}