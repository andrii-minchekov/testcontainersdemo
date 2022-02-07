package com.example.cosmosdb;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.SneakyThrows;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Properties;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.CosmosDBEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;

@Testcontainers
public class CosmosDBEmulatorContainerJunit5NoSpringTest {
	static final Logger log = LoggerFactory.getLogger(CosmosDBEmulatorContainerJunit5NoSpringTest.class);

	Properties originalSystemProperties;
	CosmosAsyncClient client;

	// The CosmosDBEmulatorContainer has port 8081 hard coded.
	@Container
	public static CosmosDBEmulatorContainer emulator = new CosmosDBEmulatorContainer(
			DockerImageName.parse("mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:latest")
	)
			.withEnv("AZURE_COSMOS_EMULATOR_PARTITION_COUNT", "2")
			.withEnv("AZURE_COSMOS_EMULATOR_ENABLE_DATA_PERSISTENCE", "true")
			// Without this line, it doesn't work
			.withEnv("AZURE_COSMOS_EMULATOR_IP_ADDRESS_OVERRIDE", "127.0.0.1")
			.withCreateContainerCmdModifier(e -> e.getHostConfig().withPortBindings(
					new PortBinding(Ports.Binding.bindPort(8081), new ExposedPort(8081))
			));

	@BeforeEach
	@SneakyThrows
	public void setUp() {
		Path keyStoreFile = File.createTempFile("azure-cosmos-emulator", "keystore").toPath();

		KeyStore keyStore = emulator.buildNewKeyStore();
		keyStore.store(new FileOutputStream(keyStoreFile.toFile()), emulator.getEmulatorKey().toCharArray());

		originalSystemProperties = (Properties) System.getProperties().clone();
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

	@AfterEach
	public void teardown() {
		client.getDatabase("Assets").delete().block();
		System.setProperties(originalSystemProperties);
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
