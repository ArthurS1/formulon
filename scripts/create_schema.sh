#!/bin/sh
curl -sS -i -d "$(cat create_schema_data.json)" $ENDPOINT/schema | awk -f curl_split.awk | jq
