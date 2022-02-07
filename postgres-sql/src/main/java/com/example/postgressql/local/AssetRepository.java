package com.example.postgressql.local;


import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepository extends JpaRepository<LocalAsset, String> {

	List<LocalAsset> findByFileNameStartingWith(String s);
}
