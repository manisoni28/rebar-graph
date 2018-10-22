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
package rebar.graph.driver.gremlin;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.addE;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.addV;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.inE;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.outV;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.unfold;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty.Cardinality;
import org.apache.tinkerpop.gremlin.structure.util.empty.EmptyGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import rebar.graph.driver.GraphDriver;

public class GremlinDriver extends GraphDriver {

	static Logger logger = LoggerFactory.getLogger(GremlinDriver.class);
	static ObjectMapper mapper = new ObjectMapper();

	GraphTraversalSource graphTraversalSource;

	GremlinDriver(GraphTraversalSource g) {
		this.graphTraversalSource = g;
	}

	public static class Builder extends GraphDriver.Builder{
		GraphTraversalSource graphTraversalSource;
	
		
		public Builder withGraphTraversalSource(GraphTraversalSource g) {
			this.graphTraversalSource = g;
			return this;
		}

		
	

	

	

		Optional<String> getFromEnvironment(String key) {
			String val = System.getenv(key);
			if (val!=null) {
				return Optional.ofNullable(val);
			}
			
			val = System.getProperty(key);
			if (val!=null) {
				return Optional.ofNullable(val);
			}
			
			return Optional.ofNullable(val);
		}
		public GremlinDriver build() {
			if (graphTraversalSource!=null) {
				
				GremlinDriver gd = new GremlinDriver(graphTraversalSource);
				return gd;
			}
			else {
				
				String url = getUrl().orElse("gremlin://localhost:8182");
				Pattern p = Pattern.compile("gremlin://(.+)\\:(\\d+).*");
				Matcher m = p.matcher(url);
				if (m.matches()) {
					String host = m.group(1);
					String port = m.group(2);
					
					
					
					Cluster.Builder builder = Cluster.build();
			        builder.addContactPoint(host);
			        builder.port(Integer.parseInt(port));
			        
			        if (getUsername().isPresent() && getPassword().isPresent()) {
			        	builder.credentials(getUsername().get(),getPassword().get());
			        }
			        
			        Cluster cluster = builder.create();
			        
			      
			        Client c = cluster.connect();
			    
			    
			      
			        GraphTraversalSource g = EmptyGraph.instance()
			                .traversal()
			                .withRemote(
			                    DriverRemoteConnection.using(cluster)
			                );
			        g.V().hasLabel("CheckConnectivity").limit(1).tryNext();
			        GremlinDriver gd = new GremlinDriver(g);
			        return gd;
			        
				}
				else {
					throw new IllegalArgumentException("unknown protocol: "+url);
				}
			}
			
		}
	}

	private static boolean isNullOrEmpty(String in) {
		return in == null || in.isEmpty();
	}

	public GraphTraversalSource traversal() {
		return graphTraversalSource;
	}
	
	

	private static GraphTraversal<?, Vertex> applyProperties(GraphTraversal<Vertex,Vertex> t, Object...kv) {
		for (int i=0; kv!=null && i< kv.length; i+=2) {
			t = t.property(Cardinality.single,(String)kv[i],kv[i+1]);
		}
		return t;
	}
	
	/*
	@SuppressWarnings("unchecked")
	public List<Edge> mergeEdge(Vertex fromVertex, String edgeLabel, String toLabel, String...kv) {
		return GraphUtil.mergeEdge(fromVertex, edgeLabel, toLabel, kv);
	}
	*/
	
	private static GraphTraversal<Vertex,Vertex> id(GraphTraversal<Vertex,Vertex> gt, Object ...kv) {
		for (int i=0; kv!=null && i<kv.length; i+=2) {
			gt = gt.has(kv[i].toString(),kv[i+1]);
		}
		return gt;
	}
	@SuppressWarnings("unchecked")
	public  List<Edge> mergeEdges(Vertex fromVertex, String edgeLabel, String toLabel, Object...kv) {
		GraphTraversal<Vertex,Vertex> gt = traversal().V(fromVertex.id()).as("from").V().hasLabel(toLabel);
		for (int i=0; kv!=null && i<kv.length; i+=2) {
			gt = gt.has(kv[i].toString(),kv[i+1]);
		}
	
		List<Edge> edges = new ArrayList<>();
		gt.as("to").coalesce(__.inE(edgeLabel).where(__.outV().as("from")), __.addE(edgeLabel).from("from")).forEachRemaining(it->{
		
			if (logger.isDebugEnabled()) {
				logger.debug("added edge: {}",it);	
			}
			edges.add(it);
		});
		return edges;
	}

	/**
	 * Merge (upsert) a directed edge between two vertices.
	 * @param fromVertex
	 * @param edgeLabel
	 * @param toVertex
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Optional<Edge> mergeEdge( Vertex fromVertex, String edgeLabel, Vertex toVertex) {
		if (fromVertex.id()==toVertex.id()) {
			throw new IllegalArgumentException("self relationship not allowed");
		}
		return traversal().V(fromVertex.id()).as("from").V(toVertex.id()).as("to")
				.coalesce(inE(edgeLabel).where(outV().as("from")), addE(edgeLabel).from("from")).tryNext();
	}
	@SuppressWarnings("unchecked")
	public Vertex mergeVertex(String label, Object...kv)  {
		
		// hat tip: https://stackoverflow.com/a/49758568
		
		GraphTraversal<Vertex,Vertex> gt = traversal().V().hasLabel(label);
		
		for (int i=0; kv!=null && i< kv.length; i+=2) {
			gt = gt.has(kv[i].toString(),kv[i+1]);
		}
		
		return gt.fold().coalesce(unfold(),applyProperties(addV(label),kv)).tryNext().get();
	}
	
	
	@SuppressWarnings("rawtypes")
	public GremlinTemplate gremlin(Function<GraphTraversalSource, GraphTraversal> f) {
		return new GremlinTemplate(this).gremlin(f);
	}

	public GremlinTemplate newTemplate() {
		return new GremlinTemplate(this);
	}

}
