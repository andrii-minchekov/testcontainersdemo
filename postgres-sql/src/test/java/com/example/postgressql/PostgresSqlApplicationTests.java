package com.example.postgressql;

import static org.assertj.core.api.Assertions.assertThat;

import reactor.core.publisher.Flux;

import java.util.List;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.example.postgressql.local.AssetRepository;
import com.example.postgressql.local.LocalAsset;


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgresSqlApplicationTests.Initializer.class)
class PostgresSqlApplicationTests {
	private static final Logger log = LoggerFactory.getLogger(PostgresSqlApplicationTests.class);

	@Container
	public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:11.1")
			.withDatabaseName("integration-tests-db")
			.withUsername("sa")
			.withPassword("sa");

	static class Initializer
			implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
			var url = postgreSQLContainer.getJdbcUrl();
			var username = postgreSQLContainer.getUsername();
			var password = postgreSQLContainer.getPassword();
			TestPropertyValues.of(
					"spring.datasource.driver-class-name=org.postgresql.Driver",
					"spring.jpa.hibernate.ddl-auto: create-drop",
					"spring.datasource.url=" + url,
					"spring.datasource.username=" + username,
					"spring.datasource.password=" + password
			).applyTo(configurableApplicationContext.getEnvironment());
		}
	}

	@Autowired
	AssetRepository repository;

	@Test
	public void save_and_find_asset() throws InterruptedException {

		var asset = LocalAsset.builder().id("1").assetId("a_1").fileName("a_1.png").build();
		repository.save(asset);

		var assetFound = repository.findById("1").orElseGet(() -> LocalAsset.builder().build());
		assertThat(assetFound).isEqualTo(asset);
	}

	@Test
	public void save_and_query_large_number() {
		var n = 1000;
		var assets = Flux.range(1, n)
			.map(i ->
					LocalAsset.builder()
							.id(String.valueOf(i))
							.assetId("a_" + i)
							.fileName((i % 2 == 0 ? "a_" : "b_") + i + ".png")
							.build()).collectList().block();

		long t0 = System.currentTimeMillis();
		repository.saveAll(assets);
		long t1 = System.currentTimeMillis();

		log.info("saved {} assets in {}", n, (t1 - t0) / 1000.0);

		t0 = System.currentTimeMillis();
		List<LocalAsset> jpegs = repository.findByFileNameStartingWith("a_");
		t1 = System.currentTimeMillis();
		log.info("Loaded {} assets in {}", jpegs.size(), (t1 - t0) / 1000.0);

		assertThat(jpegs.size()).isEqualTo(n / 2);

		assertThat(jpegs).filteredOn(new Condition<>(
				a -> a.getFileName().startsWith("b_"),
				"name John"
		)).hasSize(0);
	}

}
