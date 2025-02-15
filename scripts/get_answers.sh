#!/bin/sh
curl -sS -i $ENDPOINT/schema/$1/version/$2/submissions | awk -f curl_split.awk | jq
