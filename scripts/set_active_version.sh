#!/bin/sh
curl -X PUT -sS -i $ENDPOINT/schema/$1/version/active/$2 | awk -f curl_split.awk | jq
