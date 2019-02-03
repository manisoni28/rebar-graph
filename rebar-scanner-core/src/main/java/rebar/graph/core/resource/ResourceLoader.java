package rebar.graph.core.resource;

import java.io.InputStream;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface ResourceLoader {

	public static interface LoadableResource {
		public String getPath();
		public Supplier<InputStream> getInputStreamSupplier();
	}
	
	public Stream<LoadableResource> getResources();
}
