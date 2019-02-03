package rebar.graph.core.resource;

import java.util.stream.Stream;

import com.google.common.collect.Streams;

import rebar.graph.core.resource.ResourceLoader.LoadableResource;
import rebar.util.EnvConfig;

public class CompositeResourceLoader implements ResourceLoader {

	FilesystemResourceLoader filesystemLoader;
	GitResourceLoader gitResourceLoader;

	public CompositeResourceLoader(EnvConfig env) {
		filesystemLoader = new FilesystemResourceLoader(env);
		gitResourceLoader = new GitResourceLoader(env);

	}

	@Override
	public Stream<LoadableResource> getResources() {

		return Streams.concat(filesystemLoader.getResources(),gitResourceLoader.getResources());
	}

}
