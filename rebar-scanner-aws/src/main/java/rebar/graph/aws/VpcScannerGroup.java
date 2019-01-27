package rebar.graph.aws;

public class VpcScannerGroup extends SerialScanner {

	public VpcScannerGroup() {
		addScanners(AccountScanner.class);
		addScanners(RegionScanner.class);
		addScanners(VpcScanner.class);
		addScanners(AvailabilityZoneScanner.class);
		addScanners(SecurityGroupScanner.class);
		addScanners(SubnetScanner.class);
	}

}
