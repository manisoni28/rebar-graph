package rebar.graph.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import rebar.graph.core.BaseConfig;

@Configuration
@ComponentScan(basePackageClasses= {AlibabaSpringConfig.class,CoreSpringConfig.class,BaseConfig.class})

public class AlibabaSpringConfig {

	

}
