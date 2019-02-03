package rebar.graph.catalog;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import rebar.graph.test.AbstractIntegrationTest;

public abstract class CatalogIntegrationTest extends AbstractIntegrationTest {

	static CatalogScanner scanner;
	static boolean skipAll=false;
	@Override
	protected void beforeAll() {
		
		
		super.beforeAll();
		

		getGraphDriver().cypher("match (a) where labels(a)[0]=~'.*CatalogEntry' detach delete a").exec();
		checkAccess();
		getScanner().scan();
	}

	Logger logger = LoggerFactory.getLogger(CatalogIntegrationTest.class);
	
	@BeforeEach
	private void checkAccess() {
		try {
			if (scanner==null) {
				scanner = getRebarGraph().newScanner(CatalogScanner.class);
				scanner.scan();
				skipAll=false;
			}
		
		}
		catch (Exception e) {
			skipAll = true;
		}
		Assumptions.assumeTrue(scanner!=null && (!skipAll));
	}
	public CatalogScanner getScanner() {
		Preconditions.checkState(scanner!=null,"scanner not initialized");
		return scanner;
	}
	

}
