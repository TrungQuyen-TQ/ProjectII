#!/bin/bash
/opt/mssql/bin/sqlservr &

# Chờ SQL Server start
sleep 20s

# Import script
/opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P "Sa123456!" -i /docker-entrypoint-initdb.d/init.sql

wait
