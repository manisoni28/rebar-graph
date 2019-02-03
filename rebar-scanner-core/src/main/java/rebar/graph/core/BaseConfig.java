package rebar.graph.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import rebar.graph.config.CoreSpringConfig;
import rebar.graph.core.resource.CompositeResourceLoader;
import rebar.graph.core.resource.FilesystemResourceLoader;
import rebar.graph.core.resource.GitResourceLoader;
import rebar.graph.neo4j.GraphDriver;
import rebar.util.EnvConfig;

@Configuration
@ComponentScan(basePackageClasses= {CoreSpringConfig.class},basePackages= {"rebar.graph.github"})
public class BaseConfig {

	@Bean
	EnvConfig envConfig() {
		return new EnvConfig();
	}
	
	@Bean
	GraphDriver graphDriver() {
		return new GraphDriver.Builder().withEnv(envConfig()).build();
	}
	@Bean
	GraphDB graphDB() {
		return new GraphDB(graphDriver());
	}
	@Bean
	RebarGraph rebarGraph() {
		return new RebarGraph.Builder().withEnv(envConfig()).withGraphDB(graphDB()).build();
	}
	
	@Bean
	RebarScheduler rebarScheduler() {
		return new RebarScheduler();
	}
	@Bean
	FilesystemResourceLoader filesystemResourceLoader() {
		return new FilesystemResourceLoader(envConfig());
	}
	@Bean
	GitResourceLoader gitResourceLoader() {
		return new GitResourceLoader(envConfig());
	}
	
	@Bean
	CompositeResourceLoader compositeResourceLoader() {
		return new CompositeResourceLoader();
	}
}
