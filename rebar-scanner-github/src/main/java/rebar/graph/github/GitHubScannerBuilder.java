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

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import rebar.graph.core.ScannerBuilder;
import rebar.util.EnvConfig;
import rebar.util.RebarException;

public class GitHubScannerBuilder extends ScannerBuilder<GitHubScanner> {

	EnvConfig cfg = new EnvConfig();
	@Override
	public GitHubScanner build() {
		try {

			GitHubBuilder b = new GitHubBuilder();
			
			
			if (cfg.get("GITHUB_TOKEN").isPresent()) {
				if (cfg.get("GITHUB_USERNAME").isPresent()) {
					b.withOAuthToken(cfg.get("GITHUB_TOKEN").get(), cfg.get("GITHUB_USERNAME").get());
				}
				else {
					b.withOAuthToken(cfg.get("GITHUB_TOKEN").get());
				}
			}
			else {
				if (cfg.get("GITHUB_PASSWORD").isPresent()) {
					b.withPassword(cfg.get("GITHUB_USERNAME").get(), cfg.get("GITHUB_PASSWORD").get());
				}
			}
			
			cfg.get("GITHUB_URL").ifPresent(it->{
				b.withEndpoint(it);
			});
			
	
			GitHub gh = b.build();
			GitHubScanner scanner = new GitHubScanner(this);
			scanner.github = gh;

			return scanner;
		} catch (IOException e) {
			throw new RebarException(e);
		}
	}

}
