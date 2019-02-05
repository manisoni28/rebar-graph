package rebar.graph.gcp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.slf4j.Logger;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import rebar.util.RebarException;

/**
 * Obtains credentials. It seems kind of bizarre that I had to write this so
 * that it would look in $HOME/.config/gcloud/ Maybe I am missing something in
 * the SDK.
 * 
 * @author rob
 *
 */
public class GoogleCredentialSupplier implements com.google.common.base.Supplier<GoogleCredential> {

	volatile boolean searchOk = true;
	static Logger logger = org.slf4j.LoggerFactory.getLogger(GoogleCredentialSupplier.class);
	volatile File credentialFile = null;

	File locateCredentialFile() {
		try {
			File dir = new File(System.getProperty("user.home"), ".config/gcloud");

			List<File> foundFiles = Lists.newArrayList();

			if (dir.exists()) {
				Files.walkFileTree(dir.toPath(), new SimplleFileVisitor<Path>() {

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						if (file.toFile().getName().endsWith("application_default_credentials.json")) {
							foundFiles.add(file.toFile());
							return FileVisitResult.TERMINATE;

						}
						return FileVisitResult.CONTINUE;
					}

				});
				Files.walkFileTree(dir.toPath(), new SimplleFileVisitor<Path>() {

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						if (file.toFile().getName().endsWith("adc.json")) {
							foundFiles.add(file.toFile());
							return FileVisitResult.TERMINATE;

						}
						return FileVisitResult.CONTINUE;
					}

				});
			}

			logger.info("found: {}", foundFiles);
			if (!foundFiles.isEmpty()) {
				return foundFiles.get(0);
			}
			return null;
		} catch (IOException e) {
			logger.warn("", e);
		}

		return null;

	}

	@Override
	public GoogleCredential get() {
		// Users/rob/.config/gcloud/legacy_credentials/robschoening@gmail.com/adc.json;

		String gacEnv = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
		File f = null;
		synchronized (this) {
			f = this.credentialFile;
		}

		if (f != null) {
			try {
				GoogleCredential gc = GoogleCredential.getApplicationDefault();
				synchronized (this) {
					searchOk = false;
				}
				return gc;
			} catch (IOException e) {
				logger.info("could not obtain");
			}
		}

		searchOk = searchOk && Strings.isNullOrEmpty(gacEnv);
		if (searchOk == true && credentialFile == null) {
			f = locateCredentialFile();
			searchOk = false;
		}
		if (f == null) {
			throw new RebarException("could not locate credential file");
		}
		try (InputStream is = new FileInputStream(f)) {

			GoogleCredential gc = GoogleCredential.fromStream(is);
			synchronized (this) {
				this.credentialFile = f;
			}
			return gc;

		} catch (IOException e) {
			throw new RebarException(e);
		}

	}

}
