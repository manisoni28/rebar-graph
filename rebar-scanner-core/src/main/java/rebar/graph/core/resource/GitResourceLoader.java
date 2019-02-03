package rebar.graph.core.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import rebar.graph.core.resource.ResourceLoader.LoadableResource;
import rebar.util.EnvConfig;
import rebar.util.RebarException;

public class GitResourceLoader implements ResourceLoader {

	static Logger logger = LoggerFactory.getLogger(GitResourceLoader.class);
	SimpleGit simpleGit;
	EnvConfig env;

	public GitResourceLoader(EnvConfig env) {
		Preconditions.checkNotNull(env);
		this.env = env;
		logger.info("GIT_URL: {}", this.env.get("GIT_URL").orElse(""));
		if (this.env.get("GIT_URL").isPresent()) {
			simpleGit = new SimpleGit().withEnv(this.env);
		}
	}

	@Override
	public Stream<LoadableResource> getResources() {
	
		List<LoadableResource> resources = Lists.newArrayList();
		if (simpleGit==null) {
			return resources.stream();
		}
		try {
			String url = simpleGit.getGitUrl();

			if (!Strings.isNullOrEmpty(url)) {
				simpleGit.tree().files().forEach(it -> {

					LoadableResource r = new LoadableResource() {

						@Override
						public String getPath() {
							return it;
						}

						@Override
						public Supplier<InputStream> getInputStreamSupplier() {
							Supplier<InputStream> supplier = new Supplier<InputStream>() {

								@Override
								public InputStream get() {

									byte[] data = simpleGit.tree().getFile(it);
									return new ByteArrayInputStream(data);

								}
							};
							return supplier;
						}
					};
					resources.add(r);
				});
			}
		} catch (Exception e) {
			logger.warn("exception", e);
		}

		return resources.stream();
	}

}
