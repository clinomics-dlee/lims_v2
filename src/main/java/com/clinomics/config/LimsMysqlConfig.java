package com.clinomics.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.clinomics.repository.lims"
		, entityManagerFactoryRef = "limsEntityManagerFactory"
		, transactionManagerRef = "limsTransactionManager")
public class LimsMysqlConfig {

	public static final String UNITNAME = "lims";

	@Bean(name = "limsDataSource")
	@Qualifier("limsDataSource")
	@Primary
	@ConfigurationProperties(prefix = "datasource.lims")
	public DataSource limsDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean(name = "limsEntityManagerFactory")
	@Qualifier("limsEntityManagerFactory")
	@Primary
	public LocalContainerEntityManagerFactoryBean limsEntityManagerFactory(EntityManagerFactoryBuilder builder) {
		Map<String, Object> properties = new HashMap<String, Object>();
	    properties.put("hibernate.hbm2ddl.auto", "update");
	    properties.put("hibernate.hbm2ddl.import_files", "data.sql");
	    properties.put("hibernate.physical_naming_strategy", SpringPhysicalNamingStrategy.class.getName());
	    DataSource ds = limsDataSource();
		return builder.dataSource(ds)
				.packages("com.clinomics.entity.lims")
				.persistenceUnit(UNITNAME)
				.properties(properties)
				.build();
	}

	@Bean(name = "limsDataSourceInitializer")
	public DataSourceInitializer limsDataSourceInitializer() {
		ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
		resourceDatabasePopulator.addScript(new ClassPathResource("/data.sql"));

		DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
		dataSourceInitializer.setDataSource(limsDataSource());
		dataSourceInitializer.setDatabasePopulator(resourceDatabasePopulator);
		return dataSourceInitializer;
	}

	@Bean(name = "limsTransactionManager")
	@Primary
	public PlatformTransactionManager limsTransactionManager() {
		JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
		jpaTransactionManager.setDataSource(limsDataSource());
		jpaTransactionManager.setPersistenceUnitName(UNITNAME);
		return jpaTransactionManager;
	}
}
