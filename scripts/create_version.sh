#!/bin/sh
curl -sS -i -d "$(cat schema.json)" $ENDPOINT/schema/$@/version/add | awk -f curl_split.awk | jq
