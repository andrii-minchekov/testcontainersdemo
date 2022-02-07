package com.example.postgresreactive.local;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@ToString @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Table("local_asset")
public class Asset {
	@Id
	Long id;

	String productId;
	String assetId;

	String fileName;
}
