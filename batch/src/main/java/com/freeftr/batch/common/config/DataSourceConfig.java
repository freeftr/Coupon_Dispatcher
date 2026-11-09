package com.freeftr.batch.common.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {

	// MySQL 드라이버가 JDBC 배치를 여러 행 INSERT 하나로 묶어 보내게 한다
	@Bean
	public static BeanPostProcessor rewriteBatchedStatements() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) {
				if (bean instanceof HikariDataSource dataSource) {
					dataSource.addDataSourceProperty("rewriteBatchedStatements", "true");
				}
				return bean;
			}
		};
	}
}
