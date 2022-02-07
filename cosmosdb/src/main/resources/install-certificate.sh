#!/usr/bin/env bash

echo "Fetching CosmosDb certificate..."
rm -Rf ./emulatorcert.crt
curl -k https://localhost:8081/_explorer/emulator.pem > emulatorcert.crt
cat emulatorcert.crt

echo "Removing precious certificate from $JAVA_HOME/lib/security/cacerts"
sudo keytool -delete -alias "cosmosdb" -keystore "$JAVA_HOME"/lib/security/cacerts -storepass changeit -noprompt

echo "Adding certificate to $JAVA_HOME/lib/security/cacerts"
sudo keytool -importcert -file ./emulatorcert.crt -keystore "$JAVA_HOME"/lib/security/cacerts -alias "cosmosdb" --storepass changeit -noprompt

echo "Result"
rm -Rf ./emulatorcert.crt
sudo keytool -list -keystore "$JAVA_HOME"/lib/security/cacerts -alias "cosmosdb" --storepass changeit
