package com.example.cosmosdb;

import java.util.List;
import org.springframework.stereotype.Repository;
import com.azure.spring.data.cosmos.repository.CosmosRepository;

@Repository("blockingRepository")
public interface AssetRepository extends CosmosRepository<Asset, String> {

	List<Asset> findByFileNameStartingWith(String s);
}
