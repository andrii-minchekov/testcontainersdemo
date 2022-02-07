package com.example.cosmosdb;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.SneakyThrows;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Properties;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.CosmosDBEmulatorContainer;
import org.testcontainers.utility.DockerImageName;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;

public class CosmosDBEmulatorContainerJunit4NoSpringTest {
	static final Logger log = LoggerFactory.getLogger(CosmosDBEmulatorContainerJunit4NoSpringTest.class);

	static Properties originalSystemProperties;
	CosmosAsyncClient client;

	@BeforeClass
	public static void captureOriginalSystemProperties() {
		originalSystemProperties = (Properties) System.getProperties().clone();
	}

	@AfterClass
	public static void restoreOriginalSystemProperties() {
		System.setProperties(originalSystemProperties);
	}

	@Rule
	public TemporaryFolder tempFolder = TemporaryFolder.builder().assureDeletion().build();

	@Rule
	public CosmosDBEmulatorContainer emulator = new CosmosDBEmulatorContainer(
			DockerImageName.parse("mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:latest")
	).withEnv("AZURE_COSMOS_EMULATOR_PARTITION_COUNT", "2")
			.withEnv("AZURE_COSMOS_EMULATOR_ENABLE_DATA_PERSISTENCE", "true");

	@Before
	@SneakyThrows
	public void setUp() {
		Path keyStoreFile = tempFolder.newFile("azure-cosmos-emulator.keystore").toPath();
		KeyStore keyStore = emulator.buildNewKeyStore();
		keyStore.store(new FileOutputStream(keyStoreFile.toFile()), emulator.getEmulatorKey().toCharArray());

		System.setProperty("javax.net.ssl.trustStore", keyStoreFile.toString());
		System.setProperty("javax.net.ssl.trustStorePassword", emulator.getEmulatorKey());
		System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");

		client = new CosmosClientBuilder()
				.gatewayMode()
				.endpointDiscoveryEnabled(false)
				.endpoint(emulator.getEmulatorEndpoint())
				.key(emulator.getEmulatorKey())
				.buildAsyncClient();

		CosmosDatabaseResponse databaseResponse =
				client.createDatabaseIfNotExists("Assets").block();
		Assertions.assertThat(databaseResponse.getStatusCode()).isEqualTo(201);
		CosmosContainerResponse containerResponse =
				client.getDatabase("Assets").createContainerIfNotExists("Assets", "/productId").block();
		Assertions.assertThat(containerResponse.getStatusCode()).isEqualTo(201);
	}

	@After
	public void teardown() {
		client.getDatabase("Assets").delete().block();
	}

	@Test
	public void testWithCosmosClient() {
		var container = client.getDatabase("Assets").getContainer("Assets");
		int n = 10;
		var assetFlux = Flux.range(1, n)
				.map(i ->
						Asset.builder()
								.id(String.valueOf(i))
								.assetId("a_" + i)
								.fileName((i % 2 == 0 ? "a_" : "b_") + i + ".png")
								.build());
		long t0 = System.currentTimeMillis();
		var count = assetFlux.flatMap(container::createItem)
				.count().block();
		long t1 = System.currentTimeMillis();

		log.info("saved {} assets in {}", count, (t1 - t0) / 1000.0);

		t0 = System.currentTimeMillis();
		Flux<Asset> findByFileName = container.queryItems("SELECT * FROM c WHERE STARTSWITH(c.fileName, \"a\", false)", Asset.class);

		findByFileName.as(StepVerifier::create)
				.thenConsumeWhile(v -> {
					assertThat(v.getFileName()).startsWith("a_");
					return true;
				})
				.verifyComplete();

		t1 = System.currentTimeMillis();
		Long tot = findByFileName.count().block();

		log.info("Loaded {} assets in {}", tot, (t1 - t0) / 1000.0);
		assertThat(tot).isEqualTo(n / 2);
	}
}
