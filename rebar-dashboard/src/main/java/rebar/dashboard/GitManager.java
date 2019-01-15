package rebar.dashboard;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class GitManager {

	Logger logger = LoggerFactory.getLogger(GitManager.class);
	AtomicReference<Git> git = new AtomicReference<>();

	public static final String GIT_URL="GIT_URL";
	public static final String GIT_SSH_KEY="GIT_SSH_KEY";
	
	public synchronized Git getGit() throws IOException, GitAPIException {
		if (git.get()==null) {
			
			File tempDir = Files.createTempDirectory("rebar-git").toFile();
			File sourceRoot = new File(".").getAbsoluteFile().getParentFile().getParentFile();
		
			Git g = Git.cloneRepository().setURI(sourceRoot.toURI().toURL().toExternalForm()).setDirectory(tempDir)
					.setCloneAllBranches(true).call();
			git.set(g);
		}
		Git g = git.get();
		
		return g;
		/*
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

				rsa = "-----BEGIN RSA PRIVATE KEY-----\n"
						+ rsa.replace("\n", "").replace("\r", "").replace(" ", "").replaceAll("((\\-+)(.*?)(\\-+))", "")
						+ "\n-----END RSA PRIVATE_KEY-----\n";
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
		if (git.get() == null) {

			String x = System.getenv(GIT_URL);
			if (Strings.isNullOrEmpty(x)) {
				throw new IllegalStateException(GIT_URL+" not set");
			}
			Path tempDir = Files.createTempDirectory("jgit");
			Git g = Git.cloneRepository().setURI(x).setBare(true).setDirectory(tempDir.toFile())
					.setTransportConfigCallback(cb).call();
			git.set(g);
		} else {
			try {
				git.get().fetch().setTransportConfigCallback(cb).call();
			} catch (Exception e) {
				git.set(null);
				return getGit();
			}
		}

		return git.get();*/
	}


	
	
	


}
