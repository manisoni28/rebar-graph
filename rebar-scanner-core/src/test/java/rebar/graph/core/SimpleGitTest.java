package rebar.graph.core;

import java.nio.file.Files;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class SimpleGitTest {

	@Test
	public void testIt() throws Exception {
		SimpleGit sgit = new SimpleGit().withGitUrl("https://github.com/rebar-cloud/git-test.git");
		
		Assertions.assertThat(sgit.tree().files().anyMatch(p->p.equals("README.md"))).isTrue();
	
		String contents = sgit.tree().getFileAsString("README.md");
		Assertions.assertThat(contents).contains("# git-test");
	}
}
