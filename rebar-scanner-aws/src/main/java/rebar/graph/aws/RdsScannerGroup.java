package rebar.graph.aws;

public class RdsScannerGroup extends SerialScanner {

	public RdsScannerGroup() {
		addScanners(RdsClusterScanner.class);
		addScanners(RdsInstanceScanner.class);
	}

}
