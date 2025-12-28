import React, { useEffect, useState } from 'react';
import axiosClient from '../../api/axiosClient';
import { useChat } from '../../context/ChatContext';
import './MessengerDropdown.css';

interface Conversation {
  partnerId: number;
  partnerName: string;
  partnerAvatar: string;
  lastMessage: string;
  timestamp: string;
  isRead: boolean;
}

interface Props {
  onClose: () => void;
  onMessageRead: () => void;
}

const MessengerDropdown: React.FC<Props> = ({ onClose, onMessageRead }) => {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const { openChat } = useChat();

  useEffect(() => {
    // Gọi API lấy danh sách
    fetchConversations();
  }, []);

  const fetchConversations = () => {
    axiosClient.get('/messages/recent')
      .then(res => setConversations(res.data))
      .catch(console.error);
  };

  const handleItemClick = async (conv: Conversation) => {
    // 1. Mở ChatBox ngay lập tức
    openChat({
      id: conv.partnerId,
      fullName: conv.partnerName,
      avatarUrl: conv.partnerAvatar,
      studentCode: '', className: '', email: '', bio: '', role: '', active: true, createdAt: '', lastLogin: ''
    });

    // 2. Cập nhật giao diện NGAY LẬP TỨC (Optimistic UI) - Giống Facebook
    if (!conv.isRead) {
       const updatedList = conversations.map(c => 
          c.partnerId === conv.partnerId ? { ...c, isRead: true } : c
       );
       setConversations(updatedList);

       onMessageRead();
       
       // 3. Gọi API cập nhật ngầm bên dưới
       try {
         await axiosClient.put(`/messages/read/${conv.partnerId}`);
       } catch (e) { console.error(e); }
    }

    // 4. Đóng Dropdown
    onClose();
  };

  const formatTimeFB = (isoString: string) => {
    const date = new Date(isoString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'Vừa xong';
    if (diffMins < 60) return `${diffMins} phút`;
    if (diffHours < 24) return `${diffHours} giờ`;
    if (diffDays < 7) return `${diffDays} ngày`;
    return `${Math.floor(diffDays / 7)} tuần`;
  };

  return (
    <div className="messenger-dropdown" onClick={(e) => e.stopPropagation()}>
      <div className="msg-dd-header">
        <div className="msg-dd-title">Đoạn chat</div>
        <div className="msg-dd-actions">
           <div className="msg-dd-icon">•••</div>
           <div className="msg-dd-icon">⤢</div>
           <div className="msg-dd-icon">📝</div>
        </div>
      </div>

      <div className="msg-dd-search">
         <input className="msg-search-input" placeholder="Tìm kiếm trên Messenger" />
      </div>

      <div className="msg-dd-tabs">
         <div className="msg-pill active">Tất cả</div>
         <div className="msg-pill inactive">Chưa đọc</div>
      </div>

      <div className="msg-dd-list">
        {conversations.length > 0 ? (
          conversations.map(c => (
            <div 
              key={c.partnerId} 
              // Logic class: Chỉ thêm 'unread' nếu isRead === false
              className={`msg-item ${!c.isRead ? 'unread' : ''}`}
              onClick={() => handleItemClick(c)}
            >
               <div className="msg-item-avatar-wrapper">
                 <img src={c.partnerAvatar || `https://ui-avatars.com/api/?name=${c.partnerName}`} className="msg-item-avatar" alt="ava" />
               </div>
               
               <div className="msg-item-info">
                  <div className="msg-item-name">{c.partnerName}</div>
                  <div className="msg-item-preview">
                     {/* Logic hiển thị nội dung: Nếu mình gửi thì hiện "Bạn: " */}
                     <span>{c.lastMessage}</span>
                     <span style={{margin: '0 4px'}}>·</span>
                     <span>{formatTimeFB(c.timestamp)}</span>
                  </div>
               </div>

               {/* Chấm xanh chỉ hiện khi chưa đọc */}
               {!c.isRead && <div className="msg-item-dot"></div>}
            </div>
          ))
        ) : (
          <div style={{padding: '20px', textAlign: 'center', color: '#888'}}>Không có đoạn chat nào.</div>
        )}
      </div>
      
      <div className="msg-dd-footer">
         Xem tất cả trong Messenger
      </div>
    </div>
  );
};

export default MessengerDropdown;