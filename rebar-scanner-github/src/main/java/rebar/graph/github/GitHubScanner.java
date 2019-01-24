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
package rebar.graph.github;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import org.kohsuke.github.GHException;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Splitter;

import rebar.graph.core.Scanner;
import rebar.graph.core.ScannerBuilder;
import rebar.util.EnvConfig;
import rebar.util.Json;

public class GitHubScanner extends Scanner {

	Logger logger = LoggerFactory.getLogger(GitHubScanner.class);
	GitHub github;

	public GitHubScanner(ScannerBuilder<? extends Scanner> builder) {
		super(builder);

	}

	@Override
	public void doScan() {

		Set<String> orgs = getNeo4jDriver().cypher("match (a:GitHubOrg) return a.name as name").stream().map(n -> {
			return n.path("name").asText();
		}).collect(Collectors.toSet());
		EnvConfig cfg = new EnvConfig();
		orgs.addAll(Splitter.on(" ,;").omitEmptyStrings().trimResults().splitToList(cfg.get("GITHUB_ORGS").orElse("")));
		orgs.forEach(name -> {
			scanOrganization(name);
		});

	}

	protected void projectRepo(String orgName, String repoName, GHRepository repo) {
		logger.info("projecting repo {}/{}", orgName, repoName);
		try {
			ObjectNode n = Json.objectNode();
			n.put("name", repoName);
			n.put("orgName", orgName);
			n.put("defaultBranch", repo.getDefaultBranch());
			n.put("description", repo.getDescription());

			n.put("fullName", repo.getFullName());
			n.put("gitTransportUrl", repo.getGitTransportUrl());
			n.put("sshUrl", repo.getSshUrl());
			n.put("httpTransportUrl", repo.getHttpTransportUrl());
			n.put("htmlUrl", repo.getHtmlUrl().toExternalForm());

			n.put("createdAt", repo.getCreatedAt().getTime());
			n.put("language", repo.getLanguage());
			n.put("mirrorUrl", repo.getMirrorUrl());
			n.put("owner", repo.getOwnerName());
			try {
				// repo.getCollaborators();
			} catch (GHException e) {
				// e.printStackTrace();
				// this will happen becuase of permissions
			}

			GHRepository parent = repo.getParent();
			if (parent != null) {
				// this is broken
			}

			getGraphDB().nodes("GitHubRepo").idKey("orgName", "name").properties(n).merge();

		} catch (IOException e) {
			maybeThrow(e);
		}

	}

	protected void project(String name, GHOrganization org) {
		try {
			ObjectNode n = Json.objectNode();

			n.put("name", name);
			n.put("htmlUrl", org.getHtmlUrl().toExternalForm());
			n.put("email", org.getEmail());
			n.put("company", org.getCompany());
			n.put("blog", org.getBlog());

			getGraphDB().nodes("GitHubOrg").idKey("name").properties(n).merge();

		} catch (Exception e) {
			maybeThrow(e);
		}

	}

	public void scanOrganization(String name) {
		try {
			logger.info("scanning GitHub org: {}",name);
			GHOrganization org = github.getOrganization(name);
			
			project(name, org);
			org.getRepositories().forEach((repoName, repo) -> {
				projectRepo(name, repoName, repo);

			});
		} catch (IOException e) {
			maybeThrow(e);
		}

		getGraphDB().nodes("GitHubOrg").relationship("HAS").on("name", "orgName").to("GitHubRepo").merge();
	}

	@Override
	public void scan(String scannerType, String a, String b, String c, String id) {
		throw new UnsupportedOperationException();
	}

}
