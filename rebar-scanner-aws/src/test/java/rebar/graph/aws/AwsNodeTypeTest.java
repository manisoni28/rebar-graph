package rebar.graph.aws;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class AwsNodeTypeTest {

	@Test
	public void testIt() {

		ScanResult result = new ClassGraph().enableClassInfo().whitelistPackages(getClass().getPackage().getName())
				.scan();

		List<Class> exclusions = ImmutableList.of(ElbListenerScanner.class, SerialScanner.class,
				AllEntityScanner.class);

		result.getSubclasses(AbstractEntityScanner.class.getName()).stream().filter(p -> !p.isAbstract())

				.forEach(it -> {
					try {

						Class clazz = Class.forName(it.getName());
						if (!exclusions.contains(clazz)) {
							System.out.println(clazz);
		//					Assertions.assertThat(AbstractEntityScanner.class.cast(clazz.getDeclaredConstructor(AwsScanner.class).newInstance(null)).getEntityType())
		//							.isNotNull();

						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});

	}

}