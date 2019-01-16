package rebar.dashboard;

import javax.servlet.Filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import rebar.graph.neo4j.GraphDriver;

@Configuration
public class DashboardConfig {

	
	@Bean
	public GraphDriver neo4jDriver() {
		// Not really interested in supporting gremlin
		return (GraphDriver) new GraphDriver.Builder().build();
	}
	
	@Bean
	public AuthFilter securityFilter() {
		return new AuthFilter();
	}
	@Bean
	public FilterRegistrationBean<Filter> fizz() {
		
		
		FilterRegistrationBean<Filter> b = new FilterRegistrationBean<>();
		b.setFilter(securityFilter());
		return b;
	}

}
