package rebar.graph.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import rebar.graph.core.BaseConfig;
import rebar.graph.gcp.GcpScannerModule;
@Configuration
@ComponentScan(basePackageClasses= {CoreSpringConfig.class,BaseConfig.class,GcpConfig.class})
public class GcpConfig {

	@Bean
	public GcpScannerModule gcpScannerModule() {
		return new GcpScannerModule();
	}

}
