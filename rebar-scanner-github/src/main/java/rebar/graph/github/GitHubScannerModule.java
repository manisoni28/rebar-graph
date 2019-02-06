package rebar.graph.github;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rebar.graph.core.Main;
import rebar.graph.core.ScannerModule;

public class GitHubScannerModule extends ScannerModule {

	Logger logger = LoggerFactory.getLogger(GitHubScannerModule.class);
	GitHubScanner scanner;

	void scanAll() {
		try {
			if (scanner == null) {

				scanner = getRebarGraph().newScanner(GitHubScanner.class);
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
	public void applyConstraints(boolean b) {
		
		
	}
}
