package rebar.graph.aws;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import rebar.util.RebarException;

public class AwsEntityTypeTest {
	Logger logger = LoggerFactory.getLogger(AwsEntityTypeTest.class);

	@Test
	public void testIt() {

		ScanResult result = new ClassGraph().enableClassInfo().whitelistPackages(getClass().getPackage().getName())
				.scan();

		List<Class> exclusions = ImmutableList.of(ElbListenerScanner.class, SerialScanner.class,
				AllEntityScannerGroup.class);

		result.getSubclasses(AwsEntityScanner.class.getName()).stream().filter(p -> !p.isAbstract())

				.forEach(it -> {
					try {

						Class clazz = Class.forName(it.getName());
						if (!exclusions.contains(clazz)) {
							logger.info("instantiating {}", clazz);
							AwsEntityScanner x = (AwsEntityScanner) clazz.newInstance();

							Assertions.assertThat(x.getEntityTypeName()).isNotEmpty();
							AwsEntityType et = AwsEntityType.valueOf(x.getEntityTypeName());

							Assertions.assertThat(x.getEntityTypeName()).isEqualTo(et.name());

						}
					} catch (RuntimeException e) {
						throw e;
					} catch (Exception e) {

						throw new RebarException(e);
					}
				});

	}

	@Test
	public void testValues() {
		for (AwsEntityType t : AwsEntityType.values()) {
			if (t != AwsEntityType.UNKNOWN) {

				Assertions.assertThat(t.name()).startsWith("Aws").isEqualTo(t.toString());

				Assertions.assertThat(t).isSameAs(AwsEntityType.valueOf(t.name()));
			}
		}
	}

}
