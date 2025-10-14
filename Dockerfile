## Sử dụng image chính thức của MySQL
#FROM mysql:8.0
#
## Thiết lập thư mục làm việc
#WORKDIR /usr/src/app
#
## Copy file SQL và script khởi động (nếu có)
#COPY ./sql/init.sql /docker-entrypoint-initdb.d/init.sql
#COPY entrypoint.sh /usr/src/app/entrypoint.sh
#
## Chuyển quyền và xử lý newline
#RUN chmod +x /usr/src/app/entrypoint.sh && \
#    apt-get update && apt-get install -y dos2unix && dos2unix /usr/src/app/entrypoint.sh
#
## Cổng MySQL mặc định
#EXPOSE 3306
#
## Dùng entrypoint riêng (nếu bạn muốn xử lý thêm)
#ENTRYPOINT ["/usr/src/app/entrypoint.sh"]

#FROM mysql:8.0
#
#WORKDIR /usr/src/app
#
#COPY ./sql/init.sql /docker-entrypoint-initdb.d/init.sql
#COPY entrypoint.sh /usr/src/app/entrypoint.sh
#
## Chỉ cần chmod, bỏ apt-get
#RUN chmod +x /usr/src/app/entrypoint.sh
#
#EXPOSE 3306
#
#ENTRYPOINT ["/usr/src/app/entrypoint.sh"]


FROM mysql:8.0
COPY entrypoint.sh /usr/src/app/entrypoint.sh
RUN chmod +x /usr/src/app/entrypoint.sh
ENTRYPOINT ["/usr/src/app/entrypoint.sh"]



