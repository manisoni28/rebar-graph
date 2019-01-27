package rebar.graph.aws;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class AwsEntityTypeTest {


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
							AwsEntityScanner x = (AwsEntityScanner) clazz.newInstance();

							Assertions.assertThat(x.getEntityTypeName()).isNotEmpty();
							AwsEntityType et = AwsEntityType.valueOf(x.getEntityTypeName());

							Assertions.assertThat(x.getEntityTypeName()).isEqualTo(et.name());

						}
					} catch (Exception e) {
						throw new RuntimeException(e);
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
