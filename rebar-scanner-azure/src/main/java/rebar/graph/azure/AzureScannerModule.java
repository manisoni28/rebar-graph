package rebar.graph.azure;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rebar.graph.core.Main;
import rebar.graph.core.ScannerModule;

public class AzureScannerModule extends ScannerModule {

	Logger logger = LoggerFactory.getLogger(AzureScannerModule.class);
	AzureScanner scanner;

	void scanAll() {
		try {
			if (scanner == null) {

				scanner = getRebarGraph().newScanner(AzureScanner.class);
			}

			scanner.scan();
		} catch (Exception e) {
			logger.warn("unexpected exception", e);
		}
	}

	@Override
	protected void doStartModule() {

		getExecutor().scheduleWithFixedDelay(this::scanAll, 0, 15, TimeUnit.MINUTES);
	}

	public static void main(String[] args) throws Exception {
		Main.main(args);
	}

	@Override
	public void applyConstraints(boolean apply) {
		// TODO Auto-generated method stub
		
	}
}
