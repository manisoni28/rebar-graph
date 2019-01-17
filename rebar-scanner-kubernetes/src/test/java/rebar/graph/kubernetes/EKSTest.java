package rebar.graph.kubernetes;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClient;

public class EKSTest {

	static Logger logger = LoggerFactory.getLogger(EKSTest.class);

	@Test
	public void testConnectByName() {

		// This is very environment dependent

		try {
			KubernetesClient client = EKS.newClientBuilder().withClusterName("test").build();

			client.nodes().list().getItems().forEach(it -> {
				System.out.println(it);
			});

			
			// Now that we have the client, we can do the reverse lookup
			String url = client.getMasterUrl().toExternalForm();
			client.close();
			client = EKS.newClientBuilder().withUrl(url).build();
			client.nodes().list().getItems().forEach(it -> {
				System.out.println(it);
			});
		} catch (Exception e) {
			logger.info("this may be ignored", e);
		}

	}

}
