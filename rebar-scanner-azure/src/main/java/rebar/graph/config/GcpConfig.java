package rebar.graph.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import rebar.graph.azure.AzureScannerModule;
import rebar.graph.core.BaseConfig;
@Configuration
@ComponentScan(basePackageClasses= {CoreSpringConfig.class,BaseConfig.class,GcpConfig.class})
public class GcpConfig {

	@Bean
	public AzureScannerModule gcpScannerModule() {
		return new AzureScannerModule();
	}

}
