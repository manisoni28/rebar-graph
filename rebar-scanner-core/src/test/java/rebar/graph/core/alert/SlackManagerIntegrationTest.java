package rebar.graph.core.alert;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import rebar.graph.core.CoreIntegrationTest;

public class SlackManagerIntegrationTest extends CoreIntegrationTest {

	@Autowired 
	SlackAlertManager slack;
	
	@Test
	public void testIt() {
		Assertions.assertThat(slack).isNotNull();
		slack.alert().text("hello!").send();
	}
}
