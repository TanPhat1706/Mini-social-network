import React, { useEffect, useState } from 'react';
import axiosClient from '../api/axiosClient';
import { useChat } from '../context/ChatContext';
import './MessengerDropdown.css'; // <--- Import file CSS mới

interface Conversation {
  partnerId: number;
  partnerName: string;
  partnerAvatar: string;
  lastMessage: string;
  timestamp: string;
  isRead?: boolean; // Thêm trường này ở DTO backend nếu có, tạm thời giả lập
}

const MessengerDropdown: React.FC<{ onClose: () => void }> = ({ onClose }) => {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const { openChat } = useChat();

  useEffect(() => {
    // API lấy danh sách chat
    axiosClient.get('/messages/recent')
      .then(res => setConversations(res.data))
      .catch(console.error);
  }, []);

  const handleItemClick = (conv: Conversation) => {
    openChat({
      id: conv.partnerId,
      fullName: conv.partnerName,
      avatarUrl: conv.partnerAvatar,
      studentCode: '', className: '', email: '', bio: '', role: '', active: true, createdAt: '', lastLogin: ''
    });
    onClose();
  };

  // Helper format thời gian kiểu FB (17 tuần, 5 phút...)
  const formatTimeFB = (isoString: string) => {
    const date = new Date(isoString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 60) return `${diffMins} phút`;
    if (diffHours < 24) return `${diffHours} giờ`;
    if (diffDays < 7) return `${diffDays} ngày`;
    return `${Math.floor(diffDays / 7)} tuần`; // Giống hình ảnh bạn gửi
  };

  return (
    <div className="messenger-dropdown" onClick={(e) => e.stopPropagation()}>
      
      {/* HEADER: Đoạn chat + Icons */}
      <div className="msg-dd-header">
        <div className="msg-dd-title">Đoạn chat</div>
        <div className="msg-dd-actions">
           <div className="msg-dd-icon" title="Tùy chọn">•••</div>
           <div className="msg-dd-icon" title="Phóng to">⤢</div>
           <div className="msg-dd-icon" title="Soạn tin mới">📝</div>
        </div>
      </div>

      {/* SEARCH BAR */}
      <div className="msg-dd-search">
         <input className="msg-search-input" placeholder="Tìm kiếm trên Messenger" />
      </div>

      {/* TABS: Tất cả / Chưa đọc */}
      <div className="msg-dd-tabs">
         <div className="msg-pill active">Tất cả</div>
         <div className="msg-pill inactive">Chưa đọc</div>
         <div className="msg-pill inactive">Nhóm</div>
      </div>

      {/* DANH SÁCH TIN NHẮN */}
      <div className="msg-dd-list">
        {conversations.length > 0 ? (
          conversations.map(c => {
             // Giả lập trạng thái chưa đọc (ví dụ tin nhắn mới nhất là của người kia)
             // Bạn có thể update DTO Backend để trả về chính xác trường isRead
             const isUnread = false; 

             return (
               <div 
                 key={c.partnerId} 
                 className={`msg-item ${isUnread ? 'unread' : ''}`}
                 onClick={() => handleItemClick(c)}
               >
                  <div className="msg-item-avatar-wrapper">
                    <img 
                      src={c.partnerAvatar || `https://ui-avatars.com/api/?name=${c.partnerName}`} 
                      className="msg-item-avatar" alt="ava" 
                    />
                  </div>
                  
                  <div className="msg-item-info">
                     <div className="msg-item-name">{c.partnerName}</div>
                     <div className="msg-item-preview">
                        <span>{c.lastMessage}</span>
                        <span style={{margin: '0 4px'}}>·</span>
                        <span>{formatTimeFB(c.timestamp)}</span>
                     </div>
                  </div>

                  {isUnread && <div className="msg-item-dot"></div>}
               </div>
             );
          })
        ) : (
          <div style={{padding: '20px', textAlign: 'center', color: '#65676b'}}>
            Không có đoạn chat nào.
          </div>
        )}
      </div>

      <div style={{
          padding: '12px', textAlign: 'center', borderTop:'1px solid #dddfe2', 
          color: '#1877f2', fontWeight: 600, fontSize: '15px', cursor: 'pointer'
      }}>
         Xem tất cả trong Messenger
      </div>

    </div>
  );
};

export default MessengerDropdown;