package com.example.cosmosdb;


import static org.assertj.core.api.Assertions.assertThat;

import reactor.core.publisher.Flux;

import java.util.List;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;


/**
 * Run this against manually started mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator container
 */
@SpringBootTest
@ActiveProfiles({"integration"})
@Disabled
public class CosmosDBIntegrationIT {
	private static final Logger log = LoggerFactory.getLogger(CosmosDBIntegrationIT.class);

	@Autowired
	AssetRepository repository;

	@Test
	void testRepository() {
		var asset1 = Asset.builder().id("1").fileName("z_1.png").build();
		var savedAsset = repository.save(asset1);

		Asset all = repository.findById(savedAsset.getId()).get();

		assertThat(all).isEqualTo(asset1);
	}

	@BeforeEach
	void init() {
		log.info("Attempting to delete all assets");
		repository.deleteAll();
		log.info("Deleted all assets");
	}

	@Test
	void save_and_query_large_number() {
		var n = 300;
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
