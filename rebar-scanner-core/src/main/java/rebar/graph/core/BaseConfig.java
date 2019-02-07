package rebar.graph.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Preconditions;

import rebar.graph.config.CoreSpringConfig;
import rebar.graph.core.alert.SlackAlertManager;
import rebar.graph.core.resource.CompositeResourceLoader;
import rebar.graph.core.resource.FilesystemResourceLoader;
import rebar.graph.core.resource.GitResourceLoader;
import rebar.graph.neo4j.GraphDriver;
import rebar.util.EnvConfig;

@Configuration
@ComponentScan(basePackageClasses= {CoreSpringConfig.class})
public class BaseConfig {

	@Autowired
	ApplicationContext applicationContext;
	
	@Bean
	EnvConfig envConfig() {
		return new EnvConfig();
	}
	
	@Bean
	ConfigOptions configOptions() {
		return new ConfigOptions(envConfig());
	}
	@Bean
	GraphDriver graphDriver() {
		return new GraphDriver.Builder().withEnv(envConfig()).build();
	}
	@Bean
	GraphBuilder graphDB() {
		return new GraphBuilder(graphDriver());
	}
	@Bean
	RebarGraph rebarGraph() {
		RebarGraph.applicationContext = this.applicationContext;
		Preconditions.checkNotNull(applicationContext);
		RebarGraph g = new RebarGraph.Builder().withEnv(envConfig()).withGraphBuilder(graphDB()).build();
		return g;
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
	
	@Bean
	SlackAlertManager slackAlertManager() {
		return new SlackAlertManager();
	}
}
