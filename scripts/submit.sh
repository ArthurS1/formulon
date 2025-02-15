#!/bin/sh
curl -sS -i -X POST -d "$(cat answer.json)" $ENDPOINT/schema/$1/version/$2/submit | awk -f curl_split.awk | jq
