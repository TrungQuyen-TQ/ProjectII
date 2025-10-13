FROM mcr.microsoft.com/mssql/server:2019-latest

USER root
WORKDIR /usr/src/app

# Cài đặt sqlcmd (mssql-tools)
RUN apt-get update && \
    apt-get install -y curl apt-transport-https gnupg2 && \
    curl https://packages.microsoft.com/keys/microsoft.asc | apt-key add - && \
    curl https://packages.microsoft.com/config/ubuntu/20.04/prod.list > /etc/apt/sources.list.d/mssql-release.list && \
    apt-get update && ACCEPT_EULA=Y apt-get install -y mssql-tools unixodbc-dev && \
    echo 'export PATH="$PATH:/opt/mssql-tools/bin"' >> ~/.bashrc

# Copy script và file init
COPY ./sql/init.sql /docker-entrypoint-initdb.d/init.sql
COPY entrypoint.sh /usr/src/app/entrypoint.sh

# Chuyển quyền & convert newline để chạy trên Linux
RUN chmod +x /usr/src/app/entrypoint.sh && \
    apt-get install -y dos2unix && dos2unix /usr/src/app/entrypoint.sh

# Biến môi trường bắt buộc
ENV ACCEPT_EULA=Y

EXPOSE 1433
ENTRYPOINT ["/usr/src/app/entrypoint.sh"]
