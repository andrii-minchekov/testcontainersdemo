package com.example.postgresreactive;

import static org.assertj.core.api.Assertions.assertThat;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.example.postgresreactive.local.Asset;
import com.example.postgresreactive.local.AssetRepository;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ContextConfiguration(initializers = PostgresReactiveApplicationTests.Initializer.class)
class PostgresReactiveApplicationTests {
	private static final Logger log = LoggerFactory.getLogger(PostgresReactiveApplicationTests.class);

	@Container
	public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:11.1")
			.withDatabaseName("assets")
			.withUsername("sa")
			.withPassword("sa");

	static class Initializer
			implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
			var url = postgreSQLContainer.getJdbcUrl().replaceAll("jdbc", "r2dbc");
			var username = postgreSQLContainer.getUsername();
			var password = postgreSQLContainer.getPassword();
			log.info("Postgresql container url: {}:{} -> {}", username, password, url);
			TestPropertyValues.of(
					"spring.datasource.driver-class-name=org.postgresql.Driver",
					"spring.r2dbc.name=assets",
					"spring.r2dbc.url=" + url,
					"spring.r2dbc.username=" + username,
					"spring.r2dbc.password=" + password
			).applyTo(configurableApplicationContext.getEnvironment());
		}
	}

	@Autowired
	AssetRepository repository;

	@BeforeAll
	public static void beforeAll() {

	}

	@Test
	public void save_and_find_asset() throws InterruptedException {

		var asset = Asset.builder().assetId("z_1").fileName("z_1.png").build();
		var saved = repository.save(asset).block();

		log.info("Saved asset: {}", saved);

		assertThat(saved).isNotNull();
		assertThat(saved.getId()).isNotNull();

		var assetFound = repository.findById(saved.getId()).block();
		assertThat(assetFound.getAssetId()).isEqualTo(asset.getAssetId());
		assertThat(assetFound.getFileName()).isEqualTo(asset.getFileName());
	}

	@Test
	public void save_and_query_large_number() {
		var n = 1000;
		var assets = Flux.range(1, n)
				.map(i ->
						Asset.builder()
								.assetId("a_" + i)
								.fileName((i % 2 == 0 ? "a_" : "b_") + i + ".png")
								.build()).collectList().block();

		long t0 = System.currentTimeMillis();
		var tot = repository.saveAll(assets).count().block();
		long t1 = System.currentTimeMillis();

		log.info("saved {} assets in {}", tot, (t1 - t0) / 1000.0);

		t0 = System.currentTimeMillis();
		Flux<Asset> findByFileName = repository.findByFileNameStartingWith("a_");
		findByFileName.as(StepVerifier::create)
				.thenConsumeWhile(v -> {
					assertThat(v.getFileName()).startsWith("a_");
					return true;
				})
				.verifyComplete();
		t1 = System.currentTimeMillis();

		var counted = findByFileName.count().block();

		log.info("Loaded {} assets in {}", counted, (t1 - t0) / 1000.0);

		assertThat(counted).isEqualTo(n / 2);
	}

}
