FROM mcr.microsoft.com/mssql/server:2019-latest

USER root
COPY ./sql/init.sql /docker-entrypoint-initdb.d/init.sql
COPY entrypoint.sh /usr/src/app/entrypoint.sh
RUN chmod +x /usr/src/app/entrypoint.sh

ENTRYPOINT ["/usr/src/app/entrypoint.sh"]
