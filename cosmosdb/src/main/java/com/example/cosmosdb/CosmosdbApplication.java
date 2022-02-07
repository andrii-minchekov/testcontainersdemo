package com.example.cosmosdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.azure.spring.data.cosmos.repository.config.EnableReactiveCosmosRepositories;

@SpringBootApplication
@EnableReactiveCosmosRepositories
public class CosmosdbApplication {

	public static void main(String[] args) {
		SpringApplication.run(CosmosdbApplication.class, args);
	}

}
