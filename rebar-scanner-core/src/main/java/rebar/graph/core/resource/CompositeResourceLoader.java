package rebar.graph.core.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

public class CompositeResourceLoader implements ResourceLoader {

	List<ResourceLoader> resourceLoaders = Lists.newCopyOnWriteArrayList();

	@Override
	public Stream<LoadableResource> getResources() {

		Stream<LoadableResource> result = new ArrayList<LoadableResource>().stream();

		for (ResourceLoader loader : resourceLoaders) {
			result = Streams.concat(result, loader.getResources());
		}
		return result;
	}

}
