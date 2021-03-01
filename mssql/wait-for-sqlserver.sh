#!/bin/sh
until /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P $SA_PASSWORD -Q 'SELECT Name from sys.Databases'; do
  >&2 echo "sqlserver is not yet ready - sleeping"
  sleep 1
done