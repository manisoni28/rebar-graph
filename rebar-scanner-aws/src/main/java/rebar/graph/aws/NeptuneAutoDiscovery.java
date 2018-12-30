/**
 * Copyright 2018 Rob Schoening
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rebar.graph.aws;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;



import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DescribeDBClustersRequest;
import com.amazonaws.services.rds.model.DescribeDBClustersResult;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class NeptuneAutoDiscovery {

	private List<DBCluster> getNeptuneClusters(AmazonRDS rds) {

		List<DBCluster> clusters = Lists.newArrayList();
		DescribeDBClustersRequest request = new DescribeDBClustersRequest();
		do {
			DescribeDBClustersResult result = rds.describeDBClusters(request);

			clusters.addAll(result.getDBClusters().stream().filter(p -> p.getEngine().equals("neptune"))
					.collect(Collectors.toList()));
			request.setMarker(result.getMarker());
		} while (!Strings.isNullOrEmpty(request.getMarker()));
		return clusters;
	}

	public Optional<String> getEndpoint() {
		DefaultAWSCredentialsProviderChain c = new DefaultAWSCredentialsProviderChain();
		AmazonRDS rds = AmazonRDSClientBuilder.standard().withCredentials(c).build();

		List<DBCluster> clusterList = getNeptuneClusters(rds);
		if (clusterList.isEmpty()) {
			return Optional.empty();
		}
		DBCluster cluster = clusterList.get(0);
		return Optional.ofNullable(cluster.getEndpoint() + ":" + cluster.getPort());

	}
}
