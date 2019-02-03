package rebar.graph.digitialocean;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import rebar.graph.digitalocean.DigitalOceanScanner;
import rebar.graph.test.AbstractIntegrationTest;

public abstract class DigitalOceanIntegrationTest extends AbstractIntegrationTest {

	static DigitalOceanScanner scanner;
	static boolean skipAll=false;
	@Override
	protected void beforeAll() {
		
		
		super.beforeAll();
		
		
		getGraphDriver().cypher("match (a) where labels(a)[0]=~'DigitalOcean.*' detach delete a").exec();
		checkAccess();
		getScanner().scan();
	}

	Logger logger = LoggerFactory.getLogger(DigitalOceanIntegrationTest.class);
	
	@BeforeEach
	private void checkAccess() {
		try {
			if (scanner==null) {
				scanner = getRebarGraph().newScanner(DigitalOceanScanner.class);
				scanner.getAccountScanner().scan();
				skipAll=false;
			}
		
		}
		catch (Exception e) {
			skipAll = true;
		}
		Assumptions.assumeTrue(scanner!=null && (!skipAll));
	}
	public DigitalOceanScanner getScanner() {
		Preconditions.checkState(scanner!=null,"scanner not initialized");
		return scanner;
	}
	

}
