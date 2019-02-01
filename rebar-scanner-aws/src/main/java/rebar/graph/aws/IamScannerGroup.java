package rebar.graph.aws;

import com.amazonaws.services.ec2.model.IamInstanceProfile;

public class IamScannerGroup extends SerialScanner {

	public IamScannerGroup() {
		super();
		addScanners(IamUserScanner.class);
		addScanners(IamRoleScanner.class);
		addScanners(IamPolicyScanner.class);
		addScanners(IamInstanceProfileScanner.class);
		
	}

}
