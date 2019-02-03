package rebar.graph.catalog;

import org.assertj.core.api.IntegerAssert;
import org.junit.jupiter.api.Test;

import rebar.graph.test.AbstractIntegrationTest;

public class FilesystemCatalogLoaderTest extends AbstractIntegrationTest {

	@Test
	public void testIt() {
		getRebarGraph().newScanner(CatalogScanner.class).scan();
	}

}
