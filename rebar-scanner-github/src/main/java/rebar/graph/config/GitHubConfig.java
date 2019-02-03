package rebar.graph.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import rebar.graph.core.BaseConfig;
import rebar.graph.github.GitHubScannerModule;
@Configuration
@ComponentScan(basePackageClasses= {CoreSpringConfig.class,BaseConfig.class,GitHubConfig.class})
public class GitHubConfig {

	@Bean
	public GitHubScannerModule gitHubScannerModule() {
		return new GitHubScannerModule();
	}

}
