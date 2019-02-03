package rebar.graph.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import rebar.graph.core.BaseConfig;
import rebar.graph.kubernetes.KubeScanner;


@Configuration
@ComponentScan(basePackageClasses= {KubeScanner.class,CoreSpringConfig.class,BaseConfig.class})
public class KubernetesSpringConfig {


}
