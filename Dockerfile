# Sử dụng một ảnh nền Java 17 chính thức từ Maven
FROM maven:3.8.5-openjdk-17 AS build

# Đặt thư mục làm việc bên trong container
WORKDIR /app

# Sao chép tệp pom.xml và tải các dependency về trước để tận dụng cache
COPY pom.xml .
RUN mvn dependency:go-offline

# Sao chép toàn bộ mã nguồn còn lại
COPY src ./src

# Build ứng dụng
RUN mvn clean install -DskipTests

# Giai đoạn 2: Tạo một ảnh nhỏ hơn để chạy ứng dụng
FROM openjdk:17-jdk-slim

# Đặt thư mục làm việc
WORKDIR /app

# Sao chép tệp .jar đã được build từ giai đoạn trước
COPY --from=build /app/target/SpringMongoProject-0.0.1-SNAPSHOT.jar app.jar

# Cổng mà ứng dụng Spring Boot sẽ chạy (Render sẽ tự động dùng cổng này)
EXPOSE 4000

# Lệnh để khởi động ứng dụng
ENTRYPOINT ["java","-jar","app.jar"]