package rebar.graph.catalog;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Lists;

import rebar.util.Json;
import rebar.util.RebarException;

public class FilesystemCatalogLoader {

	ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
	static Logger logger = LoggerFactory.getLogger(FilesystemCatalogLoader.class);
	Pattern CATALOG_PATTERN = Pattern
			.compile(".*(queue|queues|stream|streams|database|databases|service|services)\\/(.*?)\\.(json|yaml|yml)");

	File dir = new File(".");

	public FilesystemCatalogLoader() {
		withFile(new File("."));
	}

	public FilesystemCatalogLoader withFile(File dir) {
		this.dir = dir;
		return this;
	}

	public Collection<CatalogEntry> scan() {
		List<CatalogEntry> services = Lists.newArrayList();
		SimpleFileVisitor<Path> v = new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Matcher m = CATALOG_PATTERN.matcher(file.toString());
				if (m.matches()) {
					String name = m.group(2);
					String dx = m.group(1);
					CatalogEntityType type = null;
					if (dx.toLowerCase().startsWith("service")) {
						type = CatalogEntityType.ServiceCatalogEntry;
					} else if (dx.startsWith("database")) {
						type = CatalogEntityType.DatabaseCatalogEntry;
					} else if (dx.startsWith("queue")) {
						type = CatalogEntityType.QueueCatalogEntry;
					} else if (dx.startsWith("stream")) {
						type = CatalogEntityType.StreamCatalogEntry;
					} else {
						logger.warn("unknwon catalog type: {}", dx);
					}

					if (type != null) {
						CatalogEntry entry = new FileCatalogEntry(type, name, file);
						services.add(entry);
					}

				}
				return FileVisitResult.CONTINUE;
			}

		};
		try {
			java.nio.file.Files.walkFileTree(dir.toPath(), v);
		} catch (IOException e) {
			throw new RebarException(e);
		}

		return services;
	}

	public class FileCatalogEntry implements CatalogEntry {

		FileCatalogEntry(CatalogEntityType type, String name, Path p) {
			this.type = type;
			this.name = name;
			this.path = p;
		}

		CatalogEntityType type;
		String name;
		Path path;

		@Override
		public String getName() {
			return name;
		}

		public CatalogEntityType getType() {
			return type;
		}

		@Override
		public JsonNode getData() {
			try {
				String fileName = path.toFile().getName();
				if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
					ObjectNode n = (ObjectNode) yaml.readTree(path.toFile());
					return n;
				}
				else if (fileName.endsWith(".json")) {
					ObjectNode n = (ObjectNode) Json.objectMapper().readTree(path.toFile());
					n.put("name", getName());
					return n;
				}
				else {
					throw new RebarException("unknown file type: "+path);
				}
				
			} catch (IOException e) {
				throw new RebarException(e);
			}
		}

	}
}
