package rebar.graph.aws;

public class VpcScannerGroup extends SerialScanner {

	public VpcScannerGroup() {
		super();
		addScanners(AccountScanner.class);
		addScanners(RegionScanner.class);
		addScanners(VpcScanner.class);
		addScanners(AvailabilityZoneScanner.class);
		addScanners(SecurityGroupScanner.class);
		addScanners(SubnetScanner.class);
	}

}
