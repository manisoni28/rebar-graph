package rebar.graph.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import rebar.graph.aws.AwsScanner;
import rebar.graph.core.BaseConfig;

@Configuration
@ComponentScan(basePackageClasses = { AwsScanner.class, CoreSpringConfig.class, BaseConfig.class })
public class AwsSpringConfig {

}
