package rebar.graph.catalog;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rebar.graph.core.Main;
import rebar.graph.core.ScannerModule;

public class CatalogScannerModule extends ScannerModule {

	Logger logger = LoggerFactory.getLogger(CatalogScannerModule.class);
	CatalogScanner scanner;
	private void scanAll() {
		try {
			if (scanner==null) {
				scanner = getRebarGraph().newScanner(CatalogScanner.class);
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
		getRebarGraph().getGraphDB().schema().createUniqueConstraint("ServiceCatalogEntry", "urn",apply);
		getRebarGraph().getGraphDB().schema().createUniqueConstraint("ServiceCatalogEntry", "name",apply);
		getRebarGraph().getGraphDB().schema().createUniqueConstraint("DatabaseCatalogEntry", "urn",apply);
		getRebarGraph().getGraphDB().schema().createUniqueConstraint("DatabaseCatalogEntry", "name",apply);
		getRebarGraph().getGraphDB().schema().createUniqueConstraint("QueueCatalogEntry", "urn",apply);
		getRebarGraph().getGraphDB().schema().createUniqueConstraint("QueueCatalogEntry", "name",apply);
		getRebarGraph().getGraphDB().schema().createUniqueConstraint("StreamCatalogEntry", "urn",apply);
		getRebarGraph().getGraphDB().schema().createUniqueConstraint("StreamCatalogEntry", "name",apply);
	}
}
