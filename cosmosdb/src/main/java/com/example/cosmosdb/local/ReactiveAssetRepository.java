package com.example.cosmosdb.local;

import reactor.core.publisher.Flux;

import org.springframework.stereotype.Repository;
import com.azure.spring.data.cosmos.repository.ReactiveCosmosRepository;

@Repository
public interface ReactiveAssetRepository extends ReactiveCosmosRepository<Asset, String> {

	Flux<Asset> findByFileNameStartingWith(String s);
}
