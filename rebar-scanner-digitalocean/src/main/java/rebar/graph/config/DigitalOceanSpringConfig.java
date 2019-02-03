package rebar.graph.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import rebar.graph.core.BaseConfig;
import rebar.graph.digitalocean.DigitalOceanScannerModule;

@Configuration
@ComponentScan(basePackageClasses= {DigitalOceanScannerModule.class,CoreSpringConfig.class,BaseConfig.class})
public class DigitalOceanSpringConfig {



}
