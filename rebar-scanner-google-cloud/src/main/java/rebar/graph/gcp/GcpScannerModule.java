package rebar.graph.gcp;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rebar.graph.core.Main;
import rebar.graph.core.ScannerModule;
import rebar.graph.neo4j.GraphSchema;

public class GcpScannerModule extends ScannerModule {

	Logger logger = LoggerFactory.getLogger(GcpScannerModule.class);
	GcpScanner scanner;

	void scanAll() {
		try {
			if (scanner == null) {

				scanner = getRebarGraph().newScanner(GcpScanner.class);
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
		GraphSchema schema = getRebarGraph().getGraphBuilder().schema();
		schema.createUniqueConstraint(GcpEntityType.GcpProject.name(), "urn",apply);
		schema.createUniqueConstraint(GcpEntityType.GcpProject.name(), "projectNumber",apply);
		schema.createUniqueConstraint(GcpEntityType.GcpProject.name(), "projectId",apply);
		schema.createUniqueConstraint(GcpEntityType.GcpComputeInstance.name(), "urn",apply);
		schema.createUniqueConstraint(GcpEntityType.GcpZone.name(), "urn",apply);
		schema.createUniqueConstraint(GcpEntityType.GcpRegion.name(), "urn",apply);
		schema.createUniqueConstraint(GcpEntityType.GcpRegion.name(), "regionName",apply);

	}
}
