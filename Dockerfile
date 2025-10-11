FROM mcr.microsoft.com/mssql/server:2019-latest

USER root
WORKDIR /usr/src/app

# Copy script và init file
COPY ./sql/init.sql /docker-entrypoint-initdb.d/init.sql
COPY entrypoint.sh /usr/src/app/entrypoint.sh

# Chuyển quyền & convert newline để chạy trên Linux
RUN chmod +x /usr/src/app/entrypoint.sh && \
    apt-get update && apt-get install -y dos2unix && \
    dos2unix /usr/src/app/entrypoint.sh

# Biến môi trường bắt buộc
ENV ACCEPT_EULA=Y
ENV SA_PASSWORD=Sa123456!

EXPOSE 1433

ENTRYPOINT ["/usr/src/app/entrypoint.sh"]