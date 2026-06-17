import React, { useEffect, useState, useMemo } from 'react';
import { useChat } from '../../context/ChatContext';
import './MessengerDropdown.css';
import { getRecentConversations, markMessageAsRead } from '../../api/messageApi';
import axiosClient from '../../api/axiosClient'; // 🟢 Import axiosClient để gọi API lấy bạn bè

interface Conversation {
  partnerId: number;
  partnerName: string;
  partnerAvatar: string;
  lastMessage: string;
  timestamp: string;
  isRead: boolean;
  isFriendOnly?: boolean; // 🟢 Cờ để phân biệt người này chỉ là bạn bè (chưa từng chat)
}

interface Friend {
  id: number;
  fullName: string;
  avatarUrl: string;
}

interface Props {
  onClose: () => void;
  onMessageRead: () => void;
}

const MessengerDropdown: React.FC<Props> = ({ onClose, onMessageRead }) => {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [friends, setFriends] = useState<Friend[]>([]); // 🟢 State lưu danh sách bạn bè
  const [searchQuery, setSearchQuery] = useState(''); // 🟢 State lưu nội dung tìm kiếm

  const { openChat } = useChat();

  useEffect(() => {
    // Gọi API lấy danh sách nhắn tin gần đây
    fetchConversations();
    // 🟢 Gọi ngầm API lấy danh sách bạn bè để phục vụ tìm kiếm
    fetchFriends();
  }, []);

  const fetchConversations = () => {
    getRecentConversations()
      .then(res => setConversations(res.data))
      .catch(console.error);
  };

  const fetchFriends = () => {
    // Dựa theo API Docs: GET /api/auth/friends/list
    axiosClient.get('/friends/list')
      .then(res => setFriends(res.data))
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

    // 2. Cập nhật giao diện NGAY LẬP TỨC (Optimistic UI)
    if (!conv.isRead && !conv.isFriendOnly) {
       const updatedList = conversations.map(c => 
          c.partnerId === conv.partnerId ? { ...c, isRead: true } : c
       );
       setConversations(updatedList);
       onMessageRead();
       
       // 3. Gọi API cập nhật ngầm bên dưới
       try {
        await markMessageAsRead(conv.partnerId); 
      } catch (e) { console.error(e); }
    }

    // 4. Đóng Dropdown
    onClose();
  };

  const formatTimeFB = (isoString: string) => {
    if (!isoString) return '';
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

  // 🟢 LOGIC TÌM KIẾM THEO THỜI GIAN THỰC (0ms delay)
  const displayList = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    
    // Nếu không gõ tìm kiếm, chỉ hiển thị danh sách đã chat
    if (!query) return conversations;

    // 1. Lọc những người đã nhắn tin có tên khớp từ khóa
    const matchedConversations = conversations.filter(c => 
      c.partnerName.toLowerCase().includes(query)
    );

    // 2. Lọc những bạn bè có tên khớp từ khóa (Loại bỏ những ai đã có mặt ở mảng matchedConversations)
    const existingIds = new Set(matchedConversations.map(c => c.partnerId));
    
    const matchedFriends: Conversation[] = friends
      .filter(f => f.fullName.toLowerCase().includes(query) && !existingIds.has(f.id))
      .map(f => ({
        partnerId: f.id,
        partnerName: f.fullName,
        partnerAvatar: f.avatarUrl,
        lastMessage: 'Bạn bè trên Mini Social', // Hiển thị nội dung ảo thay cho tin nhắn cuối
        timestamp: '',
        isRead: true,
        isFriendOnly: true // Đánh dấu đây chỉ là kết quả tìm kiếm bạn bè
      }));

    // Gộp chung kết quả: Đã chat lên trước, Bạn bè (chưa chat) xếp sau
    return [...matchedConversations, ...matchedFriends];
  }, [searchQuery, conversations, friends]);

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
         <input 
            className="msg-search-input" 
            placeholder="Tìm kiếm trên Messenger" 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)} // 🟢 Bind giá trị vào ô input
         />
      </div>

      {/* 🟢 Nếu đang gõ tìm kiếm thì ẩn 2 cái nút Tabs đi cho giống Messenger */}
      {!searchQuery && (
        <div className="msg-dd-tabs">
           <div className="msg-pill active">Tất cả</div>
           <div className="msg-pill inactive">Chưa đọc</div>
        </div>
      )}

      <div className="msg-dd-list">
        {displayList.length > 0 ? (
          displayList.map(c => (
            <div 
              key={c.partnerId} 
              className={`msg-item ${!c.isRead ? 'unread' : ''}`}
              onClick={() => handleItemClick(c)}
            >
               <div className="msg-item-avatar-wrapper">
                 <img src={c.partnerAvatar || `https://ui-avatars.com/api/?name=${c.partnerName}`} className="msg-item-avatar" alt="ava" />
               </div>
               
               <div className="msg-item-info">
                  <div className="msg-item-name">{c.partnerName}</div>
                  <div className="msg-item-preview">
                     <span>{c.lastMessage}</span>
                     {!c.isFriendOnly && c.timestamp && (
                        <>
                          <span style={{margin: '0 4px'}}>·</span>
                          <span>{formatTimeFB(c.timestamp)}</span>
                        </>
                     )}
                  </div>
               </div>

               {!c.isRead && <div className="msg-item-dot"></div>}
            </div>
          ))
        ) : (
          <div style={{padding: '20px', textAlign: 'center', color: '#888'}}>
             {searchQuery ? 'Không tìm thấy kết quả nào.' : 'Không có đoạn chat nào.'}
          </div>
        )}
      </div>
    </div>
  );
};

export default MessengerDropdown;