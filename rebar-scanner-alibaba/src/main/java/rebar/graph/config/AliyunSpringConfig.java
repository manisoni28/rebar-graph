package rebar.graph.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import rebar.graph.alibaba.AliyunScanner;
import rebar.graph.core.BaseConfig;

@Configuration
@ComponentScan(basePackageClasses= {AliyunSpringConfig.class,CoreSpringConfig.class,BaseConfig.class,AliyunScanner.class})

public class AliyunSpringConfig {

	

}
