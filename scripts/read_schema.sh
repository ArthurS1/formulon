#!/bin/sh
curl -sS -i $ENDPOINT/schema/$@ | awk -f curl_split.awk | jq
