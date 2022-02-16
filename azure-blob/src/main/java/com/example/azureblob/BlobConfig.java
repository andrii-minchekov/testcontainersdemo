package com.example.azureblob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

@Configuration
public class BlobConfig {
	static final Logger log = LoggerFactory.getLogger(BlobConfig.class);

	@Bean("asyncClient")
	BlobContainerAsyncClient asyncClient(BlobServiceClientBuilder blobServiceClientBuilder, @Value("${containerName}") String containerName) {
		return blobServiceClientBuilder
				.buildAsyncClient()
				.getBlobContainerAsyncClient(containerName);
	}

	@Bean
	InitializingBean initializingBeanBlob(BlobContainerAsyncClient asyncClient) {
		return () -> {
			asyncClient.create().block();
			log.info("Container {} created", asyncClient.getBlobContainerName());
		};
	}
}
