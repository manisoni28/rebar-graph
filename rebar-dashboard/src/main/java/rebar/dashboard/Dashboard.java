package rebar.dashboard;

import org.neo4j.driver.v1.Driver;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public class Dashboard implements ApplicationContextAware {

	static ApplicationContext applicationContext;


	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	@Override
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		applicationContext = ctx;
		
	}

	
}
