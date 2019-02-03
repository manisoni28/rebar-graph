package rebar.graph.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import rebar.graph.core.GraphDB;
import rebar.graph.core.RebarGraph;
import rebar.graph.core.RebarGraph.Builder;
import rebar.graph.core.resource.CompositeResourceLoader;
import rebar.graph.core.resource.FilesystemResourceLoader;
import rebar.graph.core.resource.GitResourceLoader;
import rebar.graph.neo4j.GraphDriver;
import rebar.util.EnvConfig;

@Configuration
@ComponentScan(basePackageClasses= {})
public class CoreSpringConfig {


}
