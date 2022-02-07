package com.example.mongosync.local;


import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepository extends MongoRepository<Asset, String> {

	List<Asset> findByFileNameStartingWith(String s);
}
