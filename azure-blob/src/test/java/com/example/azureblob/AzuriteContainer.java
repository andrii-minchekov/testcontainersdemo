package com.example.azureblob;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class AzuriteContainer extends GenericContainer<AzuriteContainer> {

	private static final DockerImageName DEFAULT_IMAGE_NAME =
			DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite");
	public AzuriteContainer() {
		this(DEFAULT_IMAGE_NAME);
	}

	// Azurite default configuration
	private static String accountKey = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";
	private static String accountName = "devstoreaccount1";

	public AzuriteContainer(final DockerImageName dockerImageName) {
		super(dockerImageName);

		dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

		withExposedPorts(10000);
	}

	public String getAccountKey() {
		return accountKey;
	}

	public String getAccountName() {
		return accountName;
	}

	public String getBlobEndpoint() {
		return "http://127.0.0.1:" + getMappedPort(10000) + "/" + accountName;
	}

	public String getConnectionString() {
		return String.format("DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s;BlobEndpoint=%s;", accountName, accountKey, getBlobEndpoint());
	}
}
