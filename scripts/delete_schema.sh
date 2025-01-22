#!/bin/sh
curl -w "%{http_code}" -X DELETE $ENDPOINT/schema/$@
