package com.example.postgressql.local;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * Locally downloaded asset
 */
@ToString @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Entity
@Table(indexes = @Index(columnList = "fileName"))
public class LocalAsset {
	@Id
	String id;

	String productId;
	String assetId;
	String useType;
	String vendorId;
	String swatchId;
	String uri;
	String extension;
	String altText;

	String fileName;
	String contentType;
	byte[] hash;

	LocalDateTime processDate;
	LocalDateTime uploadDate;

}
