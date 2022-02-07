package com.example.sqlserverjpa.local;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@ToString @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@Table(indexes = @Index(columnList = "fileName"))
public class Asset {
	@Id
	String id;

	String productId;
	String assetId;

	String fileName;
}
