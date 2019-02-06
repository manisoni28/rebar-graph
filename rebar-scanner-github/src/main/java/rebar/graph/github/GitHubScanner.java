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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kohsuke.github.GHException;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import rebar.graph.core.RebarGraph;
import rebar.graph.core.Scanner;
import rebar.util.EnvConfig;
import rebar.util.Json;
import rebar.util.RebarException;

public class GitHubScanner extends Scanner {

	@Override
	protected void init(RebarGraph g, Map<String, String> config) throws IOException {

		EnvConfig cfg = g.getEnvConfig();

		GitHubBuilder b = new GitHubBuilder();
		if (cfg.get("GITHUB_TOKEN").isPresent()) {
			if (cfg.get("GITHUB_USERNAME").isPresent()) {
				b.withOAuthToken(cfg.get("GITHUB_TOKEN").get(), cfg.get("GITHUB_USERNAME").get());
			} else {
				b.withOAuthToken(cfg.get("GITHUB_TOKEN").get());
			}
		} else {
			if (cfg.get("GITHUB_PASSWORD").isPresent()) {
				b.withPassword(cfg.get("GITHUB_USERNAME").get(), cfg.get("GITHUB_PASSWORD").get());
			}
		}

		cfg.get("GITHUB_URL").ifPresent(it -> {
			b.withEndpoint(it);
		});

		this.github = b.build();
	}

	Logger logger = LoggerFactory.getLogger(GitHubScanner.class);
	GitHub github;

	@Override
	public void doScan() {

		Set<String> orgs = Sets.newHashSet();
		EnvConfig cfg = new EnvConfig();
		orgs.addAll(Splitter.on(CharMatcher.anyOf(" ,;")).omitEmptyStrings().trimResults()
				.splitToList(cfg.get("GITHUB_ORGS").orElse("")));
		orgs.forEach(name -> {
			try {
				scanOrg(name);
			} catch (RuntimeException e) {
				maybeThrow(e);
			}
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

			n.put("createTs", repo.getCreatedAt().getTime());
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

			getGraphBuilder().nodes("GitHubRepo").idKey("orgName", "name").properties(n).merge();

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

			getGraphBuilder().nodes("GitHubOrg").idKey("name").properties(n).merge();

		} catch (Exception e) {
			maybeThrow(e);
		}

	}

	public void scanRepo(String name) {
		try {
			List<String> parts = Splitter.on("/").trimResults().splitToList(name);
			GHRepository r = github.getRepository(name);
			projectRepo(parts.get(0), parts.get(1), r);
		} catch (IOException e) {
			throw new RebarException(e);
		}
	}

	public void scanOrg(String name) {
		try {

			logger.info("scanning GitHub org: {}", name);
			GHOrganization org = github.getOrganization(name);

			project(name, org);
			org.getRepositories().forEach((repoName, repo) -> {
				try {
					projectRepo(name, repoName, repo);
				} catch (RuntimeException e) {
					logger.warn("unexpected exception", e);
				}
			});

			getGraphBuilder().nodes("GitHubOrg").relationship("HAS").on("name", "orgName").to("GitHubRepo").merge();
		} catch (IOException e) {
			throw new RebarException(e);
		}
	}

	@Override
	public void scan(String scannerType, String a, String b, String c, String id) {
		throw new UnsupportedOperationException();
	}



}
