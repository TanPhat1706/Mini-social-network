// src/main.tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.tsx'; // Đảm bảo App có đuôi .tsx
import './styles/Auth.css'; // Import file CSS

// Lấy element gốc của ứng dụng (thường là <div id="root">)
const rootElement = document.getElementById('root');

if (rootElement) {
  // Tạo root React 18
  ReactDOM.createRoot(rootElement).render(
    // Sử dụng React.StrictMode để kiểm tra các vấn đề tiềm ẩn
    <React.StrictMode>
      {/* Component chính của ứng dụng */}
      <App />
    </React.StrictMode>,
  );
} else {
  // Xử lý trường hợp không tìm thấy phần tử root
  console.error("Không tìm thấy phần tử có ID 'root' trong tài liệu.");
}