#!/bin/sh
curl -w "%{http_code}" $ENDPOINT/schema/$@ | jq
