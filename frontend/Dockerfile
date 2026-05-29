# STAGE 1: Build source code React+Vite
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

# LƯU Ý BẢO MẬT: Lệnh này an toàn KHI VÀ CHỈ KHI có file .dockerignore đi kèm
COPY . .

# Chạy lệnh build của Vite (sẽ tạo ra thư mục /dist)
RUN npm run build -- --mode docker

# STAGE 2: Triển khai lên Nginx (VÁ LỖ HỔNG: Dùng bản non-root)
FROM nginxinc/nginx-unprivileged:stable-alpine

# Chép cấu hình Nginx custom (SPA routing + reverse proxy)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Chép folder /dist từ Stage 1 sang thư mục chạy web của Nginx
COPY --from=build /app/dist /usr/share/nginx/html

# VÁ LỖ HỔNG: Đổi sang port 8080 (Bản unprivileged không thể chạy port 80)
EXPOSE 8080

# Khởi chạy Nginx
CMD ["nginx", "-g", "daemon off;"]