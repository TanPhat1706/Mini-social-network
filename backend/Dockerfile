# STAGE 1: Dùng Maven để build code thành file .jar
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app
# Copy file cấu hình maven và source code vào
COPY pom.xml .
COPY src ./src
# Chạy lệnh build ra file .jar (bỏ qua bước test để build nhanh hơn)
RUN mvn clean package -DskipTests

# STAGE 2: Chạy file .jar bằng JRE siêu nhẹ (Tối ưu tài nguyên Cloud)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Copy file .jar từ Stage 1 sang Stage 2
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
# Mở port mặc định của Spring Boot
EXPOSE 8080
# Lệnh khởi chạy ứng dụng Java
ENTRYPOINT ["java", "-jar", "app.jar"]
