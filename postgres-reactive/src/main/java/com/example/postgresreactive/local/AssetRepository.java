package com.example.postgresreactive.local;


import reactor.core.publisher.Flux;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepository extends ReactiveCrudRepository<Asset, Long> {

	Flux<Asset> findByFileNameStartingWith(String s);
}
