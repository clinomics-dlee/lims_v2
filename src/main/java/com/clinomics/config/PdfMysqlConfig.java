package com.clinomics.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.clinomics.repository.pdf"
		, entityManagerFactoryRef = "pdfEntityManagerFactory"
		, transactionManagerRef = "pdfTransactionManager")
public class PdfMysqlConfig {

	public static final String UNITNAME = "pdf";

	@Bean(name = "pdfDataSource")
	@Qualifier("pdfDataSource")
	@ConfigurationProperties(prefix = "datasource.pdf")
	public DataSource pdfDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean(name = "pdfEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean pdfEntityManagerFactory(EntityManagerFactoryBuilder builder) {
		Map<String, Object> properties = new HashMap<String, Object>();
	    properties.put("hibernate.hbm2ddl.auto", "none");
	    
		return builder.dataSource(pdfDataSource())
				.packages("com.clinomics.entity.pdf")
				.persistenceUnit(UNITNAME)
				.properties(properties)
				.build();
	}

	@Bean(name = "pdfTransactionManager")
	public PlatformTransactionManager pdfTransactionManager() {
		JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
		jpaTransactionManager.setDataSource(pdfDataSource());
		jpaTransactionManager.setPersistenceUnitName(UNITNAME);
		return jpaTransactionManager;
	}
}
