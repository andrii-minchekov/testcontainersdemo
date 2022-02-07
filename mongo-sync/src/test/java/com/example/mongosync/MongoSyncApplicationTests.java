package com.example.mongosync;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import reactor.core.publisher.Flux;

import java.util.List;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.example.mongosync.local.Asset;
import com.example.mongosync.local.AssetRepository;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = MongoSyncApplicationTests.Initializer.class)
class MongoSyncApplicationTests {
	private static final Logger log = LoggerFactory.getLogger(MongoSyncApplicationTests.class);

	@Autowired
	AssetRepository repository;

	@Container
	static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:4.4.2");

	static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
					configurableApplicationContext,
					format("spring.data.mongodb.uri=mongodb://%s:%s",mongoContainer.getContainerIpAddress(), mongoContainer.getMappedPort(27017)),
			"spring.data.mongodb.database=Assets");
		}
	}

	@Test
	public void testRepository() {
		repository.deleteAll();
		repository.save(Asset.builder().id("1").fileName("a_1.png").build());
		repository.save(Asset.builder().id("2").fileName("a_2.png").build());

		List<Asset> all = repository.findAll();
		assertThat(all).hasSize(2);
		assertThat(all.get(0).getId()).isEqualTo("1");
		assertThat(all.get(1).getId()).isEqualTo("2");
	}

	@Test
	public void save_and_query_large_number() {
		var n = 1000;
		var assets = Flux.range(1, n)
				.map(i ->
						Asset.builder()
								.id(String.valueOf(i))
								.assetId("a_" + i)
								.fileName((i % 2 == 0 ? "a_" : "b_") + i + ".png")
								.build()).collectList().block();

		long t0 = System.currentTimeMillis();
		repository.saveAll(assets);
		long t1 = System.currentTimeMillis();

		log.info("saved {} assets in {}", n, (t1 - t0) / 1000.0);

		t0 = System.currentTimeMillis();
		List<Asset> jpegs = repository.findByFileNameStartingWith("a_");
		t1 = System.currentTimeMillis();
		log.info("Loaded {} assets in {}", jpegs.size(), (t1 - t0) / 1000.0);

		assertThat(jpegs.size()).isEqualTo(n / 2);

		assertThat(jpegs).filteredOn(new Condition<>(
				a -> a.getFileName().startsWith("b_"),
				"fileName starts with b_")).isEmpty();
	}
}
