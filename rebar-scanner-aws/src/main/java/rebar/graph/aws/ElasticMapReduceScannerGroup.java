package rebar.graph.aws;

public class ElasticMapReduceScannerGroup extends SerialScanner {

	public ElasticMapReduceScannerGroup() {
		super();
		addScanners(ElasticMapReduceClusterScanner.class);
	}

}
