package rebar.graph.aws;

public class ElasticMapReduceScannerGroup extends SerialScanner {

	public ElasticMapReduceScannerGroup() {
		addScanners(ElasticMapReduceClusterScanner.class);
	}

}
