package rebar.graph.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import rebar.util.EnvConfig;

public class SimpleGit {

	Logger logger = LoggerFactory.getLogger(SimpleGit.class);
	AtomicReference<Git> git = new AtomicReference<>();

	public static final String GIT_URL = "GIT_URL";
	public static final String GIT_SSH_KEY = "GIT_SSH_KEY";
	EnvConfig env = new EnvConfig();

	public String getGitUrl() {
		return env.get(GIT_URL).orElse(null);
	}

	public SimpleGit withGitUrl(String url) {
		this.env = this.env.withEnv(GIT_URL, url);
		return this;
	}

	public class TreeContext {
		String branch = "master";
	
		public Stream<String> files() throws IOException, GitAPIException {
			org.eclipse.jgit.lib.Repository repo = getGit().getRepository();

			List<String> files = Lists.newArrayList();
			RevWalk revWalk = null;
			try  {
				revWalk = new RevWalk(repo);
				String refName = branch;
				Ref head = repo.findRef(refName);
				logger.info("resolved {} => {}",refName,head);
				Preconditions.checkState(head!=null, "could not resolve ref: "+refName);
				RevCommit commit = revWalk.parseCommit(head.getObjectId());
				
				RevTree tree = commit.getTree();

				try (TreeWalk treeWalk = new TreeWalk(repo)) {
					treeWalk.addTree(tree);

					treeWalk.setRecursive(true);
				
					while (treeWalk.next()) {

						String path = treeWalk.getPathString();
						logger.info(path);
						files.add(path);
					/*	ObjectId objectId = treeWalk.getObjectId(0);
						ObjectLoader loader = repo.open(objectId);
						byte b[] = loader.getBytes();*/

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
		
		public String getFileAsString(String path) throws IOException,GitAPIException {
			return new String(getFile(path),Charsets.UTF_8);
		}
		public byte [] getFile(String path) throws IOException, GitAPIException {
			org.eclipse.jgit.lib.Repository repo = getGit().getRepository();

			List<String> files = Lists.newArrayList();
			RevWalk revWalk = null;
			try  {
				revWalk = new RevWalk(repo);
				String refName = branch;
				Ref head = repo.findRef(refName);
				logger.info("resolved {} => {}",refName,head);
				Preconditions.checkState(head!=null, "could not resolve ref: "+refName);
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
			}
			finally {
				if (revWalk!=null) {
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
		ctx.branch = Strings.isNullOrEmpty(branch) ? "master" : branch;
		return ctx;
	}
	public synchronized Git getGit() throws IOException, GitAPIException {
		if (git.get() == null) {

			File tempDir = Files.createTempDirectory("simple-git").toFile();
			File sourceRoot = new File(".").getAbsoluteFile().getParentFile().getParentFile();

			Git g = Git.cloneRepository().setURI(getGitUrl()).setDirectory(tempDir).setCloneAllBranches(true).call();
			logger.info("{}", g);
			git.set(g);
		}
		Git g = git.get();

		return g;
		/*
		 * JschConfigSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
		 * 
		 * @Override protected void configure(Host host, Session session) {
		 * session.setConfig("StrictHostKeyChecking", "no"); }
		 * 
		 * @Override protected JSch createDefaultJSch(FS fs) throws JSchException { JSch
		 * defaultJSch = super.createDefaultJSch(fs); defaultJSch.removeAllIdentity();
		 * 
		 * String rsa = Strings.nullToEmpty(System.getenv(GIT_SSH_KEY));
		 * 
		 * rsa = "-----BEGIN RSA PRIVATE KEY-----\n" + rsa.replace("\n",
		 * "").replace("\r", "").replace(" ", "").replaceAll("((\\-+)(.*?)(\\-+))", "")
		 * + "\n-----END RSA PRIVATE_KEY-----\n"; defaultJSch.addIdentity("test",
		 * rsa.getBytes(), null, null); return defaultJSch; } }; TransportConfigCallback
		 * cb = new TransportConfigCallback() { public void configure(Transport
		 * transport) { SshTransport sshTransport = (SshTransport) transport;
		 * sshTransport.setSshSessionFactory(sshSessionFactory); } }; if (git.get() ==
		 * null) {
		 * 
		 * String x = System.getenv(GIT_URL); if (Strings.isNullOrEmpty(x)) { throw new
		 * IllegalStateException(GIT_URL+" not set"); } Path tempDir =
		 * Files.createTempDirectory("jgit"); Git g =
		 * Git.cloneRepository().setURI(x).setBare(true).setDirectory(tempDir.toFile())
		 * .setTransportConfigCallback(cb).call(); git.set(g); } else { try {
		 * git.get().fetch().setTransportConfigCallback(cb).call(); } catch (Exception
		 * e) { git.set(null); return getGit(); } }
		 * 
		 * return git.get();
		 */
	}

	

}
