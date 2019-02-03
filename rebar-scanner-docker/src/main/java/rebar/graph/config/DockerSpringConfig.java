package rebar.graph.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import rebar.graph.core.BaseConfig;
import rebar.graph.docker.DockerScanner;

@Configuration
@ComponentScan(basePackageClasses= {DockerScanner.class,CoreSpringConfig.class,BaseConfig.class})
public class DockerSpringConfig {



}
