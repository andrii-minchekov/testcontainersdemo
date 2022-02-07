##!/bin/bash
#docker stop cosmosdb || true && docker rm cosmosdb || true
#ipaddr="$(ifconfig | grep "inet " | grep -Fv 127.0.0.1 | awk '{print $2}' | head -n 1)"
#echo "System IP address is $ipaddr"
#docker pull mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator
#docker run --rm -p 8081:8081 -p 10251:10251 -p 10252:10252 -p 10253:10253 -p 10254:10254 \
#  -m 3g --cpus=2.0 --name=cosmosdb \
#  -e AZURE_COSMOS_EMULATOR_PARTITION_COUNT=2 \
#  -e AZURE_COSMOS_EMULATOR_ENABLE_DATA_PERSISTENCE=true \
#  -e AZURE_COSMOS_EMULATOR_IP_ADDRESS_OVERRIDE="$ipaddr" \
#  -d mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator
