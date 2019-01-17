/**
 * Copyright 2018-2019 Rob Schoening
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
package rebar.graph.kubernetes;

import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import rebar.graph.core.GraphOperation;
import rebar.graph.core.Scanner;
import rebar.graph.neo4j.GraphDriver;

public class MergePodParentRelationshipsOperation implements GraphOperation {

	Logger logger = LoggerFactory.getLogger(MergePodParentRelationshipsOperation.class);

	@Override
	public Stream<JsonNode> exec(Scanner ctx, JsonNode n, GraphDriver neo4j) {
		
		
		KubeScanner scanner = (KubeScanner) ctx;
		String replicaSetUid = n.path("replicaSetUid").asText();
		String clusterId = n.path("clusterId").asText();
		String podName = n.path("name").asText();
		String podUid = n.path("uid").asText(null);
			
		if (!Strings.isNullOrEmpty(replicaSetUid)) {
		//	logger.info("Pod {} has replicaset parent {}",podName,replicaSetUid);

		//	scanner.getRebarGraph().getGraphDB().nodes("KubeReplicaSet").id("clusterId",scanner.getClusterId()).id("uid",replicaSetUid).relationship("HAS")
		//	.to("KubePod").id("clusterId",scanner.getClusterId()).id("uid",podUid).merge();
		}
		scanner.ensureOwnerReferences(Deployment.class, ReplicaSet.class);
		return Stream.of();
	}


}
