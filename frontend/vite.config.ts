import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react({
      babel: {
        plugins: [['babel-plugin-react-compiler']],
      },
    }),
  ],

  define: {
    global: 'window',
  },

  server: {
    // Cho phép mở dev server qua ngrok (Host header ≠ localhost)
    allowedHosts: true,
  },

  // 🟢 CẤU HÌNH BUILD SỬ DỤNG FUNCTION CHO MANUAL CHUNKS
  build: {
    chunkSizeWarningLimit: 1000, // Nới lỏng cảnh báo lên 1000kB (1MB)
    rollupOptions: {
      output: {
        // Sử dụng Callback Function thay vì Object để TypeScript không báo lỗi
        manualChunks(id) {
          // Nếu file nằm trong thư mục node_modules (thư viện bên thứ 3)
          if (id.includes('node_modules')) {
            // Tách riêng các thư viện của Material UI
            if (id.includes('@mui') || id.includes('@emotion')) {
              return 'mui';
            }
            // Tách riêng thư viện SweetAlert2
            if (id.includes('sweetalert2')) {
              return 'sweetalert';
            }
            // Tách riêng hệ sinh thái React
            if (id.includes('react') || id.includes('react-dom') || id.includes('react-router')) {
              return 'vendor';
            }
            // Các thư viện còn lại sẽ được gom chung vào file 'dependencies'
            return 'dependencies';
          }
        }
      }
    }
  }
})