package rebar.graph.digitalocean;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import rebar.graph.core.Main;
import rebar.graph.core.ScannerModule;

@Component
public class DigitalOceanScannerModule extends ScannerModule {

	Logger logger = LoggerFactory.getLogger(DigitalOceanScannerModule.class);
	DigitalOceanScanner scanner;
	private void scanAll() {
		try {
			if (scanner==null) {
				scanner = getRebarGraph().newScanner(DigitalOceanScanner.class);
			}
			scanner.scan();
		} catch (Exception e) {
			logger.error("uncaught exception", e);
		}
	}

	@Override
	protected void doStartModule() {
		// we will use the more sophisticated scheduling system once we get it factored up and out of AWS module
		getExecutor().scheduleWithFixedDelay(this::scanAll, 0, 5, TimeUnit.MINUTES);
	}

	public static void main(String[] args) throws Exception {
		Main.main(args);
	}

	@Override
	public void applyConstraints(boolean apply) {
		getRebarGraph().getGraphDB().schema().createUniqueConstraint("DigitalOceanAccount", "urn",apply);
		getRebarGraph().getGraphDB().schema().createUniqueConstraint("DigitalOceanRegion", "urn",apply);
		getRebarGraph().getGraphDB().schema().createUniqueConstraint("DigitalOceanDroplet", "urn",apply);
	}

}
