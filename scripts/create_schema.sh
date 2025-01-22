#!/bin/sh
curl -w "%{http_code}" -d "$(cat create_schema_data.json)" $ENDPOINT/schema | jq
