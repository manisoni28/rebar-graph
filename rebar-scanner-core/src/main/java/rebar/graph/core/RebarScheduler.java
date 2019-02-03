package rebar.graph.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;

import groovy.lang.GroovyShell;
import it.sauronsoftware.cron4j.Scheduler;
import it.sauronsoftware.cron4j.SchedulerListener;
import it.sauronsoftware.cron4j.SchedulingPattern;
import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskCollector;
import it.sauronsoftware.cron4j.TaskExecutionContext;
import it.sauronsoftware.cron4j.TaskExecutor;
import it.sauronsoftware.cron4j.TaskTable;
import rebar.graph.core.resource.ResourceLoader;
import rebar.util.Json;
import rebar.util.RebarException;

public class RebarScheduler {

	AtomicLong cron4jTheadCounter = new AtomicLong();
	Logger logger = LoggerFactory.getLogger(RebarScheduler.class);
	Scheduler cron4j;
	ScannerModule module;

	public RebarScheduler(ScannerModule module) {
		this.module = module;

	}

	

	public ResourceLoader getResourceLoader() {
		return this.module.resourceLoader;
	}
	private void renameThread() {
		// cron4j has very annoying thread names
		String name = Thread.currentThread().getName();
		if (name.startsWith("cron4j")&& name.length()>20) {
			Thread.currentThread().setName("cron4j-"+cron4jTheadCounter.getAndIncrement());
		}
	}
	class InternalListener implements SchedulerListener {

		@Override
		public void taskLaunching(TaskExecutor executor) {
			renameThread();
			logger.info("taskLaunching startTime={} task={}", executor.getTask());

		}

		@Override
		public void taskSucceeded(TaskExecutor executor) {
			renameThread();
			logger.info("taskSucceeded task={}", executor.getTask());

		}

		@Override
		public void taskFailed(TaskExecutor executor, Throwable exception) {
			renameThread();
			logger.warn("taskFailed task=" + executor.getTask(), exception);
		}

	}

	class InternalTaskCollector implements TaskCollector {

		@Override
		public TaskTable getTasks() {
			renameThread();
			TaskTable tt = new TaskTable();

			getResourceLoader().getResources().forEach(it -> {
				
				if (it.getPath().contains("scripts/") && it.getPath().endsWith(".groovy")) {
					try (InputStream in = it.getInputStreamSupplier().get()) {
						JsonNode n = extractFrontMatter(in);
						System.out.println(CharStreams.toString(new InputStreamReader(it.getInputStreamSupplier().get())));
						logger.info("front matter: {}", n);
						String cron = n.path("cron").asText().trim();
						String scannerType = n.path("scanner").asText().trim();
						if (Strings.isNullOrEmpty(scannerType)
								|| scannerType.equalsIgnoreCase(module.getScannerType())) {
							if ((!Strings.isNullOrEmpty(cron)) && n.path("enabled").asBoolean(true)) {

								final Supplier<InputStream> supplier = it.getInputStreamSupplier();
								Task t = new Task() {

									@Override
									public void execute(TaskExecutionContext context) throws RuntimeException {

										try {
											renameThread();
											new GroovyShell().evaluate(
													CharStreams.toString(new InputStreamReader(supplier.get())));
										} catch (IOException e) {
											throw new RebarException(e);
										}

									}

								};
								tt.add(new SchedulingPattern(cron), t);
							}
						}
					} catch (IOException e) {
						logger.warn("could not read", e);
					}
				}
			});
			logger.info("collected {} tasks", tt.size());
			return tt;
		}

	}

	protected static JsonNode extractFrontMatter(InputStream in) throws IOException {
		String line = null;
	
		Pattern p = Pattern.compile("\\s*(\\/+|#+)(\\s*)(.*)\\:(.*)");
		ObjectNode n = Json.objectNode();
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		for (int i = 0; i < 10 && ((line = br.readLine()) != null); i++) {
			Matcher m = p.matcher(line);
			if (m.matches()) {
				n.put(m.group(3).trim(), m.group(4).trim());
			}
		}
		if (n.size() == 0) {
			return MissingNode.getInstance();
		}
		return n;
	}

	protected void start() {
		cron4j = new Scheduler();
		cron4j.start();

		cron4j.addSchedulerListener(new InternalListener());
		cron4j.addTaskCollector(new InternalTaskCollector());
		logger.info("scheduling heartbeat");
		cron4j.schedule("* * * * *", this::heartbeat);
	}

	private void heartbeat() {
		logger.info("heartbeat");
	}

	public <T extends ScannerModule> T getScannerModule() {
		return (T) module;
	}

	public Scheduler getScheduler() {
		return cron4j;
	}

}
