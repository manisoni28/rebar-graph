package rebar.dashboard.report;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import rebar.dashboard.GitManager;
import rebar.dashboard.report.Report.Parameter;
import rebar.graph.neo4j.Neo4jDriver;
import rebar.util.RebarException;

@Component
public class ReportManager {

	AtomicReference<Map<String, Report>> reportMapRef = new AtomicReference(null);

	private static String REPORT_REGEX = ".*\\/reports\\/((.*?)\\.(yaml|yml|json))";

	static class ParameterImpl implements Parameter {
		String name;

		ParameterImpl(String n) {
			this.name = n;
		}

		public String getName() {
			return name;
		}
	}

	static class ReportImpl implements Report {

		JsonNode data;

		@Override
		public String getDescription() {
			return data.path("description").asText();
		}

		@Override
		public String getName() {
			return data.path("name").asText();
		}

		@Override
		public String getQuery() {
			return data.path("query").asText();
		}

		public String toString() {
			return MoreObjects.toStringHelper(ReportImpl.class).add("name", getName()).toString();
		}

		@Override
		public Map<String, Parameter> getParameters() {
			return Maps.newHashMap();
		}
	}

	@Autowired
	Neo4jDriver driver;

	@Autowired
	GitManager gitManager;

	public ReportManager() {
		// TODO Auto-generated constructor stub
	}

	protected Map<String, Report> loadReportsFromNeo4j() {
		
		Map<String,Report> reports = Maps.newConcurrentMap();
		driver.cypher("match (r:Report) return r").forEach(it -> {
			String name = it.path("name").asText().trim();
			
			if (!Strings.isNullOrEmpty(name)) {
				ReportImpl ri = new ReportImpl();
				ri.data = it;
				reports.put(name, ri);
			}
			
		});

		return reports;
	}

	public synchronized Map<String, Report> getReportMap() {
		/*
		 * try { Map<String, Report> map = reportMapRef.get(); if (map == null) { Git
		 * git = gitManager.getGit(); map = refresh(git); reportMapRef.set(map); }
		 * return map; } catch (IOException | GitAPIException e) { throw new
		 * RebarException(e); }
		 */
		return loadReportsFromNeo4j();
	
	}

	protected static Map<String, Report> refresh(Git git) throws IOException {
		String ref = "master";
		Repository repo = git.getRepository();
		Map<String, Report> reportMap = Maps.newHashMap();
		try (RevWalk revWalk = new RevWalk(repo)) {

			Ref head = repo.findRef(ref);

			RevCommit commit = revWalk.parseCommit(head.getObjectId());
			RevTree tree = commit.getTree();
		//	Yaml y;

			ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
			ObjectMapper jsonMapper = new ObjectMapper();
			try (TreeWalk treeWalk = new TreeWalk(git.getRepository())) {
				treeWalk.addTree(tree);
				// not walk the tree recursively so we only get the elements in the top-level
				// directory
				treeWalk.setRecursive(true);

				Pattern p = Pattern.compile(REPORT_REGEX);
				while (treeWalk.next()) {

					Matcher m = p.matcher(treeWalk.getPathString());
					if (m.matches()) {
						String type = m.group(3).toLowerCase();
						String name = m.group(2);
						
						ObjectId objectId = treeWalk.getObjectId(0);
						ObjectLoader loader = repo.open(objectId);
						byte[] b = loader.getBytes(64000);

						JsonNode data = null;
						if (type.equals("yaml") || type.equals("yml")) {
							data = yamlMapper.readTree(b);
						} else if (type.equals("json")) {
							data = jsonMapper.readTree(b);
						}
						if (data instanceof ObjectNode) {
							ObjectNode on = (ObjectNode) data;
							on.put("name", name);

							ReportImpl ri = new ReportImpl();
							ri.data = data;
							reportMap.put(name, ri);
						}

					}

				}

			}
			revWalk.dispose();
		}
		return reportMap;
	}

	public void refresh() {

	}
}
