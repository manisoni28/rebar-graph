package rebar.graph.core.resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import rebar.util.EnvConfig;
import rebar.util.RebarException;

public class SimpleGit {

	Logger logger = LoggerFactory.getLogger(SimpleGit.class);
	AtomicReference<Git> git = new AtomicReference<>();
	private long fetchDelayMillis = TimeUnit.MINUTES.toMillis(1);
	static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	public static final String GIT_URL = "GIT_URL";
	public static final String GIT_SSH_KEY = "GIT_SSH_KEY";
	EnvConfig env = new EnvConfig();

	TransportConfigCallback transportConfigCallback = null;

	public SimpleGit withEnv(EnvConfig env) {
		Preconditions.checkNotNull(env);
		this.env = env;
		return this;
	}

	public String getGitUrl() {
		return env.get(GIT_URL).orElse(null);
	}

	public SimpleGit withGitUrl(String url) {
		this.env = this.env.withEnv(GIT_URL, url);
		return this;
	}

	public class TreeContext {
		String ref = "master";

		public Stream<String> files() throws IOException, GitAPIException {
			org.eclipse.jgit.lib.Repository repo = getGit().getRepository();

			List<String> files = Lists.newArrayList();
			RevWalk revWalk = null;
			try {
				revWalk = new RevWalk(repo);
				String refName = ref;
				Ref head = repo.findRef(refName);
				logger.info("resolved {} => {}", refName, head);
				Preconditions.checkState(head != null, "could not resolve ref: " + refName);
				RevCommit commit = revWalk.parseCommit(head.getObjectId());

				RevTree tree = commit.getTree();

				try (TreeWalk treeWalk = new TreeWalk(repo)) {
					treeWalk.addTree(tree);

					treeWalk.setRecursive(true);

					while (treeWalk.next()) {

						String path = treeWalk.getPathString();
						logger.info(path);
						files.add(path);

					}
				}

			} finally {
				if (revWalk != null) {
					revWalk.dispose();
					revWalk.close();
				}
			}
			return files.stream();

		}

		public String getFileAsString(String path) throws IOException, GitAPIException {
			return new String(getFile(path), Charsets.UTF_8);
		}

		public byte[] getFile(String path) {
			org.eclipse.jgit.lib.Repository repo = getGit().getRepository();

			List<String> files = Lists.newArrayList();
			RevWalk revWalk = null;
			try {
				revWalk = new RevWalk(repo);
				String refName = ref;
				Ref head = repo.findRef(refName);
				logger.info("resolved {} => {}", refName, head);
				Preconditions.checkState(head != null, "could not resolve ref: " + refName);
				RevCommit commit = revWalk.parseCommit(head.getObjectId());

				RevTree tree = commit.getTree();

				try (TreeWalk treeWalk = new TreeWalk(repo)) {
					treeWalk.addTree(tree);
					treeWalk.setRecursive(true);
					treeWalk.setFilter(PathFilter.create(path));
					ObjectId objectId = treeWalk.getObjectId(0);
					ObjectLoader loader = repo.open(objectId);

					return loader.getCachedBytes(500000);
				}
			} catch (IOException e) {
				throw new RebarException(e);
			} finally {
				if (revWalk != null) {
					revWalk.close();
					revWalk.dispose();
				}
			}
		}
	}

	public TreeContext tree() {
		return tree("master");
	}

	public TreeContext tree(String branch) {
		TreeContext ctx = new TreeContext();
		ctx.ref = Strings.isNullOrEmpty(branch) ? "master" : branch;
		return ctx;
	}

	public void fetch() {
		Stopwatch sw = Stopwatch.createStarted();
		try {
			FetchResult r = applyTransportCallback(getGit().fetch()).call();
		} catch (GitAPIException e) {
			tryRecovery(e);
			throw new RebarException(e);
		} finally {
			logger.warn("fetch took {}ms", sw.elapsed(TimeUnit.MILLISECONDS));
		}

	}

	private void safeFetch() {
		try {
			fetch();
		} catch (Exception e) {
			logger.error("problem fetching from git", e);
		}
	}

	private void tryRecovery(Exception e) {

	}

	public synchronized Git getGit() {
		try {
			if (git.get() == null) {

				File tempDir = Files.createTempDirectory("simple-git").toFile();

				Git g = applyTransportCallback(Git.cloneRepository()).setURI(getGitUrl()).setDirectory(tempDir)
						.setCloneAllBranches(true).call();
				logger.info("{}", g);
				git.set(g);
				executor.scheduleWithFixedDelay(this::safeFetch, fetchDelayMillis, fetchDelayMillis,
						TimeUnit.MILLISECONDS);
			}
			Git g = git.get();

			return g;

		} catch (IOException | GitAPIException e) {
			throw new RebarException(e);
		}

	}

	synchronized Optional<TransportConfigCallback> transportCallback() {

		if (transportConfigCallback != null) {
			return Optional.ofNullable(transportConfigCallback);
		}
		if (env.get(GIT_SSH_KEY).isPresent()) {

			JschConfigSessionFactory sshSessionFactory = new JschConfigSessionFactory() {

				@Override
				protected void configure(Host host, Session session) {
					session.setConfig("StrictHostKeyChecking", "no");
				}

				@Override
				protected JSch createDefaultJSch(FS fs) throws JSchException {
					JSch defaultJSch = super.createDefaultJSch(fs);
					defaultJSch.removeAllIdentity();

					String rsa = Strings.nullToEmpty(System.getenv(GIT_SSH_KEY));

					rsa = "-----BEGIN RSA PRIVATE KEY-----\n" + rsa.replace("\n", "").replace("\r", "").replace(" ", "")
							.replaceAll("((\\-+)(.*?)(\\-+))", "") + "\n-----END RSA PRIVATE_KEY-----\n";
					defaultJSch.addIdentity("test", rsa.getBytes(), null, null);
					return defaultJSch;
				}
			};
			TransportConfigCallback cb = new TransportConfigCallback() {
				public void configure(Transport transport) {
					SshTransport sshTransport = (SshTransport) transport;
					sshTransport.setSshSessionFactory(sshSessionFactory);
				}
			};
			this.transportConfigCallback = cb;
		}
		return Optional.ofNullable(transportConfigCallback);
	}

	private CloneCommand applyTransportCallback(CloneCommand cmd) {
		transportCallback().ifPresent(it -> {
			cmd.setTransportConfigCallback(it);
		});
		return cmd;
	}

	private FetchCommand applyTransportCallback(FetchCommand cmd) {
		transportCallback().ifPresent(it -> {
			cmd.setTransportConfigCallback(it);
		});
		return cmd;
	}
}
