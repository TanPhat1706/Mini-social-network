# STAGE 1: Build code
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app

# 🟢 TỐI ƯU 1: Copy pom.xml trước và tải thư viện (để Docker cache lại layer này)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Sau đó mới copy source code vào và build
COPY src ./src
RUN mvn clean package -DskipTests

# STAGE 2: Run app
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 🟢 TỐI ƯU 2: Thiết lập múi giờ Việt Nam để không bị lệch giờ đăng bài, thông báo
ENV TZ=Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080

# Chạy ứng dụng Java
ENTRYPOINT ["java", "-jar", "app.jar"]