package rebar.graph.aws;

public class Ec2ScannerGroup extends SerialScanner {

	public Ec2ScannerGroup() {
		super();
		addScanners(Ec2InstanceScanner.class);
		addScanners(AmiScanner.class);
		addScanners(LaunchConfigScanner.class);
		addScanners(LaunchTemplateScanner.class);
		addScanners(ElbClassicScanner.class);
		addScanners(ElbScanner.class);
		addScanners(ElbTargetGroupScanner.class);
		addScanners(AsgScanner.class);
	}

}
