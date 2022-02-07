package com.example.postgresreactive;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;

@Configuration
public class DBConfig {

	@Bean
	InitializingBean createTablesInitializer(R2dbcEntityTemplate template) {
		return () -> {
			Integer foo = template.getDatabaseClient()
					.sql("""
							CREATE TABLE IF NOT EXISTS local_asset(
								id SERIAL PRIMARY KEY, 
                                product_id VARCHAR(255), 
                                asset_id VARCHAR(255), 
                                file_name VARCHAR(255) NOT NULL
)
						"""
					)
					.fetch()
					.rowsUpdated()
					.block();
			System.out.println("Created table assets with " + foo + " rows");
		};
	}
}
