import React from 'react';
import './Footer.css';

const Footer: React.FC = () => {
  return (
    <div className="footer">
      <div>MiniSocial © 2025</div>
      <div className="footer-links">
        <span>Quyền riêng tư</span> · 
        <span>Điều khoản</span> · 
        <span>Quảng cáo</span> · 
        <span>Cookie</span> · 
        <span>Xem thêm</span>
      </div>
    </div>
  );
};

export default Footer;