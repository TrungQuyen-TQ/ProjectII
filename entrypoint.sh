#!/bin/bash
set -e

echo "⏳ Đang khởi động MySQL..."
docker-entrypoint.sh mysqld &

# Đợi MySQL sẵn sàng
until mysqladmin ping -h "localhost" --silent; do
  echo "⏳ Chờ MySQL khởi động..."
  sleep 2
done

echo "✅ MySQL đã khởi động, tiến hành import dữ liệu..."
mysql -u root -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < /docker-entrypoint-initdb.d/init.sql || true

wait

