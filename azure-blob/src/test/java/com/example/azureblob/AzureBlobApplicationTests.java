package com.example.azureblob;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlockBlobItem;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(initializers = AzureBlobApplicationTests.Initializer.class)
@Testcontainers
class AzureBlobApplicationTests {
	static final Logger log = LoggerFactory.getLogger(AzureBlobApplicationTests.class);

	@Container
	static AzuriteContainer container = new AzuriteContainer().withExposedPorts(10000);
	static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		@Override
		public void initialize(ConfigurableApplicationContext configurableApplicationContext) {

			TestPropertyValues.of(
					"AZURE_STORAGE_CONNECTION_STRING=" + container.getConnectionString(),
					"azure.storage.accountName=" + container.getAccountName(),
					"azure.storage.connection-string=" + container.getConnectionString(),
					"azure.storage.account-key=" + container.getAccountKey(),
					"containerName=" + "container-" + UUID.randomUUID(),
					"azure.storage.blob-endpoint=" + container.getBlobEndpoint()
					).applyTo(configurableApplicationContext.getEnvironment());
		}
	}

	@Autowired
	BlobContainerAsyncClient asyncClient;

	@Test
	void upload_and_download_file_with_client() {
		var containerName = "container-" + UUID.randomUUID();
		log.info("Creating container {}", containerName);
		var containerClient = new BlobContainerClientBuilder()
				.connectionString(container.getConnectionString())
				.containerName(containerName)
				.buildAsyncClient();
		containerClient.create().block();
		log.info("Created container {}", containerName);


		// create blob and upload text
		var blobName = "blob-" + UUID.randomUUID();
		var blobClient = containerClient.getBlobAsyncClient(blobName);
		var content = "hello world".getBytes();

		log.info("Uploading blob {} to container {}", blobName, containerName);

		BlockBlobItem uploadItem = blobClient.upload(BinaryData.fromBytes(content), true).block();
		var md5 = HexFormat.of().formatHex(uploadItem.getContentMd5());
		assertThat(md5).isEqualTo("5eb63bbbe01eeed093cb22bb8f5acdc3");
		log.info("Uploaded blob {} to container {}: {}", blobName, containerName, uploadItem);

		log.info("Downloading blob {} from container {}", blobName, containerName);
		var downloaded = blobClient.downloadContent().block();
		var text = new String(downloaded.toBytes());
		assertThat(text).isEqualTo("hello world");

		log.info("Downloaded blob {} from container {} : {}", blobName, containerName, text);
	}

	@Test
	void save_and_load_blob_with_spring_client() {
		var blobName = "blob-" + UUID.randomUUID();
		var content = "hello world".getBytes();
		var data = BinaryData.fromBytes(content);

		log.info("Uploading blob {}", blobName);
		var uploaded = asyncClient.getBlobAsyncClient(blobName)
				.upload(data, true)
				.block();
		var md5 = HexFormat.of().formatHex(uploaded.getContentMd5());
		log.info("Uploaded blob with hash {}", md5);

		assertThat(md5).isEqualTo("5eb63bbbe01eeed093cb22bb8f5acdc3");

		log.info("Downloading blob with name {}", blobName);
		var downloaded = asyncClient.getBlobAsyncClient(blobName).downloadContent().block();
		var str = new String(downloaded.toBytes());
		log.info("Downloaded blob with name {}:{}", blobName, str);

		assertThat(str).isEqualTo("hello world");
	}

}
