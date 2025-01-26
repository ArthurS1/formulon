#!/bin/sh
curl -sS -i -X PUT -d "$(cat update_schema_data.json)" $ENDPOINT/schema/$@ | awk -f curl_split.awk | jq
