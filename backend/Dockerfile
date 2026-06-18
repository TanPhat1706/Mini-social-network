# STAGE 1: Build code (GIỮ NGUYÊN HOÀN TOÀN ĐỂ JENKINS BUILD ỔN ĐỊNH)
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

# 🟢 TỐI ƯU 2: Thiết lập múi giờ Việt Nam (Giữ nguyên)
ENV TZ=Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 🔴 VÁ LỖ HỔNG: Tạo một user và group không có đặc quyền tối cao (springuser)
RUN groupadd -g 1001 springgroup && \
    useradd -u 1001 -g springgroup -s /bin/sh -m springuser

# Copy file jar từ stage 1 và GÁN QUYỀN SỞ HỮU cho springuser
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
RUN chown springuser:springgroup app.jar

# 🔴 BẮT BUỘC: Ra lệnh cho Docker chuyển sang dùng user thường trước khi chạy app
USER springuser

EXPOSE 8080

# Chạy ứng dụng Java
ENTRYPOINT ["java", "-jar", "app.jar"]