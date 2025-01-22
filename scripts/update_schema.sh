#!/bin/sh
curl -w "%{http_code}" -X PUT -d "$(cat update_schema_data.json)" $ENDPOINT/schema/$@ | jq
