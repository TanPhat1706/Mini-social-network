import React, { useEffect, useState, useMemo } from 'react';
import { useChat } from '../../context/ChatContext';
import './MessengerDropdown.css';
import { getRecentConversations, markMessageAsRead } from '../../api/messageApi';
import axiosClient from '../../api/axiosClient';
import api from '../../api/api'; 

import { differenceInMinutes, differenceInHours, differenceInDays } from 'date-fns';
import { useColorMode } from '../../styles/theme';
import AvatarWithFrame from '../AvatarWithFrame';
import ColoredName from '../ColoredName';
import { showDevelopmentAlert } from '../../utils/swal';

interface Conversation {
  partnerId: number;
  studentCode: string; 
  partnerName: string;
  partnerAvatar: string;
  lastMessage: string;
  timestamp: string;
  isRead: boolean;
  isFriendOnly?: boolean;
  currentAvatarFrame?: string; 
  currentNameColor?: string; 
}

interface Friend {
  id: number;
  studentCode: string; 
  fullName: string;
  avatarUrl: string;
}

interface UserPresence {
  online: boolean;
  lastSeen?: string;
}

interface Props {
  onClose: () => void;
  onMessageRead: () => void;
}

const formatShortTime = (lastSeen?: string) => {
  if (!lastSeen) return '';
  const lastDate = new Date(lastSeen);
  const mins = differenceInMinutes(new Date(), lastDate);

  if (mins < 60) return `${mins <= 0 ? 1 : mins}p`;

  const hours = differenceInHours(new Date(), lastDate);
  if (hours < 24) return `${hours}g`;

  const days = differenceInDays(new Date(), lastDate);
  if (days < 7) return `${days}n`;

  return ''; 
};

const MessengerDropdown: React.FC<Props> = ({ onClose, onMessageRead }) => {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [friends, setFriends] = useState<Friend[]>([]); 
  const [searchQuery, setSearchQuery] = useState(''); 
  
  const [presences, setPresences] = useState<Record<string, UserPresence>>({});

  const { openChat } = useChat();
  
  const { mode } = useColorMode();
  const isDark = mode === 'dark';

  useEffect(() => {
    fetchConversations();
    fetchFriends();
  }, []);

  const fetchConversations = () => {
    getRecentConversations()
      .then(res => setConversations(res.data))
      .catch(console.error);
  };

  const fetchFriends = () => {
    axiosClient.get('/friends/list')
      .then(res => setFriends(res.data))
      .catch(console.error);
  };

  useEffect(() => {
    const codes = new Set([
      ...conversations.map(c => c.studentCode),
      ...friends.map(f => f.studentCode)
    ].filter(Boolean));

    if (codes.size === 0) return;

    const fetchAllPresences = async () => {
      try {
        const promises = Array.from(codes).map(code => api.get(`/api/users/${code}/presence`));
        const results = await Promise.allSettled(promises);

        const newPresences: Record<string, UserPresence> = {};
        results.forEach((res, index) => {
          if (res.status === 'fulfilled') {
            newPresences[Array.from(codes)[index]] = res.value.data;
          }
        });
        setPresences(newPresences);
      } catch (error) {
        console.error("Lỗi tải trạng thái dropdown:", error);
      }
    };

    fetchAllPresences();
    const interval = setInterval(fetchAllPresences, 60000);

    return () => clearInterval(interval);
  }, [conversations, friends]);

  const handleItemClick = async (conv: Conversation) => {
    openChat({
      id: conv.partnerId,
      studentCode: conv.studentCode, 
      fullName: conv.partnerName,
      avatarUrl: conv.partnerAvatar,
      className: '', email: '', bio: '', role: '', active: true, createdAt: '', lastLogin: '',
      currentAvatarFrame: conv.currentAvatarFrame, 
      currentNameColor: conv.currentNameColor
    });

    if (!conv.isRead && !conv.isFriendOnly) {
       const updatedList = conversations.map(c => 
          c.partnerId === conv.partnerId ? { ...c, isRead: true } : c
       );
       setConversations(updatedList);
       onMessageRead();
       
       try {
        await markMessageAsRead(conv.partnerId); 
      } catch (e) { console.error(e); }
    }

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

  const displayList = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    
    if (!query) return conversations;

    const matchedConversations = conversations.filter(c => 
      c.partnerName.toLowerCase().includes(query)
    );

    const existingIds = new Set(matchedConversations.map(c => c.partnerId));
    
    const matchedFriends: Conversation[] = friends
      .filter(f => f.fullName.toLowerCase().includes(query) && !existingIds.has(f.id))
      .map(f => ({
        partnerId: f.id,
        studentCode: f.studentCode,
        partnerName: f.fullName,
        partnerAvatar: f.avatarUrl,
        lastMessage: 'Bạn bè trên hệ thống', 
        timestamp: '',
        isRead: true,
        isFriendOnly: true 
      }));

    return [...matchedConversations, ...matchedFriends];
  }, [searchQuery, conversations, friends]);

  const handleAlertKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      showDevelopmentAlert();
    }
  };

  // 🟢 Đã gỡ bỏ các comment sai cú pháp JSX gây vỡ giao diện
  return (
    <div 
      className={`messenger-dropdown ${isDark ? 'msg-dark-mode' : ''}`} 
      onClick={(e) => e.stopPropagation()}
      tabIndex={-1}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.stopPropagation();
        }
      }}
    >
      <div className="msg-dd-header">
        <div className="msg-dd-title">Đoạn chat</div>
        <div className="msg-dd-actions">
           <div className="msg-dd-icon" role="button" tabIndex={0} onClick={showDevelopmentAlert} onKeyDown={handleAlertKeyDown}>•••</div>
           <div className="msg-dd-icon" role="button" tabIndex={0} onClick={showDevelopmentAlert} onKeyDown={handleAlertKeyDown}>⤢</div>
           <div className="msg-dd-icon" role="button" tabIndex={0} onClick={showDevelopmentAlert} onKeyDown={handleAlertKeyDown}>📝</div>
        </div>
      </div>

      <div className="msg-dd-search">
         <input 
            className="msg-search-input" 
            placeholder="Tìm kiếm trên Messenger" 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)} 
         />
      </div>

      {!searchQuery && (
        <div className="msg-dd-tabs">
           <div className="msg-pill active">Tất cả</div>
           <div 
              className="msg-pill inactive" 
              role="button" 
              tabIndex={0} 
              onClick={showDevelopmentAlert} 
              onKeyDown={handleAlertKeyDown}
           >
              Chưa đọc
           </div>
        </div>
      )}

      <div className="msg-dd-list">
        {displayList.length > 0 ? (
          displayList.map(c => {
            const presence = presences[c.studentCode];
            const isOnline = presence?.online;
            const offlineText = !isOnline ? formatShortTime(presence?.lastSeen) : '';
            const isRecentOffline = offlineText.endsWith('p');
            const borderColor = isDark ? '#242526' : '#ffffff'; 

            return (
              <div 
                key={c.partnerId} 
                className={`msg-item ${!c.isRead ? 'unread' : ''}`}
                role="button"
                tabIndex={0}
                onClick={() => handleItemClick(c)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    handleItemClick(c);
                  }
                }}
              >
                 <div className="msg-item-avatar-wrapper">
                 <AvatarWithFrame 
                     src={c.partnerAvatar || `https://ui-avatars.com/api/?name=${c.partnerName}`} 
                     name={c.partnerName}
                     frameClass={c.currentAvatarFrame} 
                     size={56} 
                   />
                   
                   {isOnline ? (
                     <div style={{
                       position: 'absolute', bottom: 0, right: 0, width: 14, height: 14,
                       backgroundColor: '#31a24c', borderRadius: '50%',
                       border: `2px solid ${borderColor}`, zIndex: 10
                     }} />
                   ) : offlineText ? (
                     <div style={{
                       position: 'absolute', bottom: -2, right: -4,
                       backgroundColor: isRecentOffline ? '#e7f3ff' : (isDark ? '#3A3B3C' : '#f0f2f5'),
                       color: isRecentOffline ? '#1877f2' : (isDark ? '#B0B3B8' : '#65676b'),
                       borderRadius: '10px',
                       border: `2px solid ${borderColor}`,
                       padding: '0 4px', minWidth: '20px', height: '16px',
                       fontSize: '10px', fontWeight: 800, zIndex: 10,
                       display: 'flex', alignItems: 'center', justifyContent: 'center',
                       boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
                     }}>
                       {offlineText}
                     </div>
                   ) : null}
                 </div>
                 
                 <div className="msg-item-info">
                 <div className="msg-item-name">
                      <ColoredName name={c.partnerName} colorClass={c.currentNameColor} />
                    </div>
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
            );
          })
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