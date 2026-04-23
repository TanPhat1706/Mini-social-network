# STAGE 1: Build source code React+Vite
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
# Chạy lệnh build của Vite (sẽ tạo ra thư mục /dist)
RUN npm run build -- --mode docker

# STAGE 2: Triển khai lên Nginx
FROM nginx:stable-alpine
# Chép cấu hình Nginx custom (SPA routing + reverse proxy)
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Chép folder /dist từ Stage 1 sang thư mục chạy web của Nginx
COPY --from=build /app/dist /usr/share/nginx/html
# Mở port 80 cho web server
EXPOSE 80
# Khởi chạy Nginx
CMD ["nginx", "-g", "daemon off;"]
