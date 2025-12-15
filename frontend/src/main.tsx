import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App'; // 1. Bỏ đuôi .tsx (Vite tự hiểu, code sẽ gọn hơn)
import './index.css';    // 2. QUAN TRỌNG: Phải import file CSS toàn cục vào đây

const rootElement = document.getElementById('root');

if (rootElement) {
  ReactDOM.createRoot(rootElement).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>,
  );
} else {
  console.error("Không tìm thấy phần tử root");
}