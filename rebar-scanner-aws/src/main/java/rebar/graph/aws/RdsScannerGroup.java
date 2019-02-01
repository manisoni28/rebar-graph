package rebar.graph.aws;

public class RdsScannerGroup extends SerialScanner {

	public RdsScannerGroup() {
		super();
		addScanners(RdsClusterScanner.class);
		addScanners(RdsInstanceScanner.class);
	}

}
