package com.example.mongoreactive;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
import com.example.mongoreactive.local.Asset;
import com.example.mongoreactive.local.AssetRepository;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ContextConfiguration(initializers = MongoReactiveApplicationTests.Initializer.class)
class MongoReactiveApplicationTests {

	private static final Logger log = LoggerFactory.getLogger(MongoReactiveApplicationTests.class);

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
		repository.save(Asset.builder().id("1").fileName("z_1.png").build()).block();

		Mono<Asset> all = repository.findById("1");

		StepVerifier.create(all)
						.expectNextMatches(a -> a.getFileName().equals("z_1.png"))
								.verifyComplete();
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
		var count = repository.saveAll(assets).count().block();
		long t1 = System.currentTimeMillis();

		log.info("saved {} assets in {}", count, (t1 - t0) / 1000.0);

		t0 = System.currentTimeMillis();
		Flux<Asset> findByFileName = repository.findByFileNameStartingWith("a_");

		findByFileName.as(StepVerifier::create)
		        .thenConsumeWhile(v -> {
			assertThat(v.getFileName()).startsWith("a_");
			return true;
		})
		.verifyComplete();

		t1 = System.currentTimeMillis();
		long tot = findByFileName.count().block();

		log.info("Loaded {} assets in {}", tot, (t1 - t0) / 1000.0);

		assertThat(tot).isEqualTo(n / 2);
	}
}
