#!/bin/bash

docker-compose run service /bin/sh -c "./wait-for-buckets.sh && ./wait-for-it.sh mssql:1433 -s -t 90 -- mvn clean test -Dtests=integration"; docker-compose down -v;
