package rebar.graph.neo4j;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.v1.exceptions.ClientException;

import com.google.common.collect.ImmutableList;

public class DriverStackTraceTest extends IntegrationTest {

	@Test
	public void testIt() {

		// tests for regresssion in neo4j driver that was fixed here: https://github.com/neo4j/neo4j-java-driver/commit/7a79d06c921bce8ba51efe61dd4a79b86b8a4198
		try {
			getNeo4jDriver().getDriver().session().run("match (a:Foo INTENTIONALL BROKEN")
					.forEachRemaining(System.out::println);
			Assertions.failBecauseExceptionWasNotThrown(ClientException.class);
		} catch (ClientException e) {

			Assertions.assertThat(e.getStackTrace()).as("calling class should be in stack trace")
					.anyMatch(p -> p.getClassName().equals(getClass().getName()));

		}
	}
}
