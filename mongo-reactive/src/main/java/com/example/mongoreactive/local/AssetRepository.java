package com.example.mongoreactive.local;


import reactor.core.publisher.Flux;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepository extends ReactiveMongoRepository<Asset, String> {

	Flux<Asset> findByFileNameStartingWith(String s);
}
