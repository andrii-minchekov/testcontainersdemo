# Trying out testcontainers for different databases

Every database was straight forward, except for CosmosDB. If someone can tell me why the 
CosmosDBEmulatorContainer needs the following lines with JUnit5 but not with JUnit4, please leave a comment

```java
(...)
			.withEnv("AZURE_COSMOS_EMULATOR_IP_ADDRESS_OVERRIDE", "127.0.0.1")
			.withCreateContainerCmdModifier(
					e -> e.getHostConfig().withPortBindings(
							new PortBinding(Ports.Binding.bindPort(8081), new ExposedPort(8081))
					));
```
