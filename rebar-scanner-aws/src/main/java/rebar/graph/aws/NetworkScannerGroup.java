package rebar.graph.aws;

public class NetworkScannerGroup extends SerialScanner {

	public NetworkScannerGroup() {
		addScanners(InternetGatewayScanner.class);
		addScanners(EgressOnlyInternetGatewayScanner.class);
		addScanners(RouteTableScanner.class);
		addScanners(VpcPeeringConnectionScanner.class);
		addScanners(VpnGatewayScanner.class);
		addScanners(VpcEndpointScanner.class);
		
	}

}
