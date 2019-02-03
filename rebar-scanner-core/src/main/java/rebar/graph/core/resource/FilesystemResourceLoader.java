package rebar.graph.core.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import rebar.graph.core.resource.ResourceLoader.LoadableResource;
import rebar.util.EnvConfig;
import rebar.util.RebarException;

public class FilesystemResourceLoader implements ResourceLoader {

	static Logger logger = LoggerFactory.getLogger(FilesystemResourceLoader.class);
	
	
	class FilesystemLoadableResource implements LoadableResource {

		File file;
		String path;

		public FilesystemLoadableResource(String path, File file) {
			this.path = path;
			this.file = file;
		}
		@Override
		public String getPath() {
			return path;
		}

		@Override
		public Supplier<InputStream> getInputStreamSupplier() {
			Supplier<InputStream> s = new Supplier<InputStream>() {

				@Override
				public InputStream get() {
					try {
						return new FileInputStream(file);
					} catch (IOException e) {
						throw new RebarException(e);
					}

				}
			};
			return s;
		}
		
		public String toString() {
			return MoreObjects.toStringHelper(FilesystemLoadableResource.class).add("path",path).add("file", file).toString();
		}

	}

	EnvConfig env;

	public FilesystemResourceLoader(EnvConfig env) {
		this.env = env;
	}

	
	String normalizePath(Path p, File root) {
		String s = p.toFile().getPath();
		while (s.startsWith(".") || s.startsWith("/")) {
			s = s.substring(1);
		}
		return s;
	}
	@Override
	public Stream<LoadableResource> getResources() {

		List<LoadableResource> resources = Lists.newArrayList();
		File dir = new File(env.get("REBAR_HOME").orElse("."));
		logger.info("Searching {}",dir.getAbsolutePath());
		
		
		SimpleFileVisitor<Path> v = new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

				FilesystemLoadableResource flr = new FilesystemLoadableResource(normalizePath(file, dir),file.toFile());
				resources.add(flr);
				return FileVisitResult.CONTINUE;
			}

		};
		try {
			java.nio.file.Files.walkFileTree(dir.toPath(), v);
		} catch (IOException e) {
			throw new RebarException(e);
		}
		
		return resources.stream();
	}

}
