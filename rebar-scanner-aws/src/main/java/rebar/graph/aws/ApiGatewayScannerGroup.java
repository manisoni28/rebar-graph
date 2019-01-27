package rebar.graph.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;

public class ApiGatewayScannerGroup extends SerialScanner {

	public ApiGatewayScannerGroup() {
		super();
		addScanners(ApiGatewayRestApiScanner.class);
	}

	
}
