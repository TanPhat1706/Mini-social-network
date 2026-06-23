import React, { useState, useEffect, useRef, useMemo } from 'react';
import SockJS from 'sockjs-client';
import { Client, type IMessage } from '@stomp/stompjs';
import { useNavigate } from 'react-router-dom';
import axiosClient from '../../api/axiosClient';
import api from '../../api/api';
import { getApiBaseUrl } from '../../config/apiBase';
import type { User } from '../../types';
import { useChat } from '../../context/ChatContext';
import './ChatBox.css';
import { getMessagesHistory } from '../../api/messageApi';
import { format, isToday, isYesterday, differenceInMinutes, differenceInHours, differenceInDays } from 'date-fns';
import { vi } from 'date-fns/locale';
import { Tooltip, Menu, MenuItem, Popover, Box } from '@mui/material';
import ReplyIcon from '@mui/icons-material/Reply';
import SentimentSatisfiedAltIcon from '@mui/icons-material/SentimentSatisfiedAlt';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import InsertEmoticonIcon from '@mui/icons-material/InsertEmoticon';

import EmojiPicker, { Theme } from 'emoji-picker-react';
import AvatarWithFrame from '../AvatarWithFrame';
import ColoredName from '../ColoredName';
import { useProfileNavigation } from '../../utils/useProfileNavigation';
import { useColorMode } from '../../styles/theme';

interface ChatReaction {
  userId: number;
  reactionType: string;
}

interface Message {
  id?: number;
  senderId: number;
  receiverId: number;
  content: string;
  timestamp?: string;
  messageType?: string;
  gameSessionId?: number;
  isDeletedEveryone?: boolean;
  deletedBySenderId?: number;
  reactions?: ChatReaction[];
  isRead?: boolean;
}

interface Props {
  currentUser: User;
}

interface UserPresence {
  studentCode: string;
  online: boolean; 
  lastSeen?: string;
}

const REACTION_EMOJIS: Record<string, string> = {
  LOVE: '❤️', HAHA: '😆', WOW: '😮', SAD: '😢', ANGRY: '😡', LIKE: '👍'
};

const formatMessageTime = (timestamp?: string) => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  if (isToday(date)) return format(date, 'HH:mm');
  if (isYesterday(date)) return `Hôm qua ${format(date, 'HH:mm')}`;
  const formattedDate = format(date, 'EEEE, dd/MM HH:mm', { locale: vi });
  return formattedDate.charAt(0).toUpperCase() + formattedDate.slice(1);
};

const formatLastSeen = (online: boolean, lastSeen?: string) => {
  if (online) return 'Đang hoạt động';
  if (!lastSeen) return 'Ngoại tuyến';

  const lastDate = new Date(lastSeen);
  const mins = differenceInMinutes(new Date(), lastDate);

  if (mins < 1) return 'Hoạt động vài giây trước';
  if (mins < 60) return `Hoạt động ${mins} phút trước`;

  const hours = differenceInHours(new Date(), lastDate);
  if (hours < 24) return `Hoạt động ${hours} giờ trước`;

  const days = differenceInDays(new Date(), lastDate);
  if (days < 7) return `Hoạt động ${days} ngày trước`;

  return `Hoạt động từ ${format(lastDate, 'dd/MM/yyyy')}`;
};

const ChatBox: React.FC<Props> = ({ currentUser }) => {
  const { chatTarget, closeChat, isMinimized, setIsMinimized } = useChat();
  const targetUser = chatTarget;
  const navigate = useNavigate();
  const navigateToProfile = useProfileNavigation();

  const { mode } = useColorMode();
  const isDark = mode === 'dark';

  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isConnected, setIsConnected] = useState(false);
  const [presence, setPresence] = useState<UserPresence | null>(null);

  const [isPartnerTyping, setIsPartnerTyping] = useState(false);
  const myTypingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const partnerTypingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const [anchorElMore, setAnchorElMore] = useState<null | HTMLElement>(null);
  const [selectedMsgForMore, setSelectedMsgForMore] = useState<Message | null>(null);
  const [anchorElReact, setAnchorElReact] = useState<null | HTMLElement>(null);
  const [selectedMsgForReact, setSelectedMsgForReact] = useState<Message | null>(null);
  
  const [anchorElEmoji, setAnchorElEmoji] = useState<null | HTMLElement>(null);

  const stompClientRef = useRef<Client | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const mapMessage = (data: any): Message => ({
    id: data.id,
    content: data.content,
    timestamp: data.timestamp || data.createdAt,
    senderId: data.senderId || data.sender?.id,
    receiverId: data.receiverId || data.receiver?.id,
    messageType: data.messageType,
    gameSessionId: data.gameSessionId,
    isDeletedEveryone: data.isDeletedEveryone || data.deletedEveryone,
    deletedBySenderId: data.deletedBySenderId,
    isRead: data.isRead !== undefined ? data.isRead : (data.read || false),
    reactions: data.reactions || []
  });

  useEffect(() => {
    if (!targetUser?.studentCode) return;
    const fetchPresence = async () => {
      try {
        const res = await api.get(`/api/users/${targetUser.studentCode}/presence`);
        setPresence(res.data);
      } catch (error) {
        console.error("Lỗi lấy trạng thái hoạt động", error);
      }
    };
    fetchPresence();
    const interval = setInterval(fetchPresence, 60000);
    return () => clearInterval(interval);
  }, [targetUser?.studentCode]);

  useEffect(() => {
    if (!targetUser?.id || !currentUser?.id) return;
    setMessages([]);
    getMessagesHistory(currentUser.id, targetUser.id)
      .then(res => {
        if (res.data && Array.isArray(res.data)) {
          setMessages(res.data.map(mapMessage));
        }
      })
      .catch(console.error);
  }, [currentUser?.id, targetUser?.id]);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!targetUser?.id || !currentUser?.id || !token) return;

    const socket = new SockJS(`${getApiBaseUrl()}/ws`);
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: { Authorization: `Bearer ${token}` },
      debug: () => { },
      onConnect: () => {
        setIsConnected(true);

        client.subscribe('/user/queue/messages', (payload: IMessage) => {
          const newMsg = mapMessage(JSON.parse(payload.body));
          setMessages(prev => {
            const existingIndex = prev.findIndex(m => m.id === newMsg.id);
            if (existingIndex !== -1) {
              const updatedMessages = [...prev];
              updatedMessages[existingIndex] = newMsg;
              return updatedMessages;
            }
            return [...prev, newMsg];
          });
          setIsPartnerTyping(false);
        });

        client.subscribe('/user/queue/game-events', (payload: IMessage) => {
          const event = JSON.parse(payload.body);
          if (event.type === 'GAME_INVITE_ACCEPTED' && event.session?.id) {
            closeChat();
            navigate(`/games/tic-tac-toe/${event.session.id}`);
          }
        });

        client.subscribe('/user/queue/typing', (payload: IMessage) => {
          const event = JSON.parse(payload.body);
          const isPartnerTypingNow = event.isTyping !== undefined ? event.isTyping : event.typing;

          if (event.senderId === targetUser.id) {
            if (isPartnerTypingNow) {
              setIsPartnerTyping(true);
              if (partnerTypingTimeoutRef.current) clearTimeout(partnerTypingTimeoutRef.current);
              partnerTypingTimeoutRef.current = setTimeout(() => setIsPartnerTyping(false), 3000);
            } else {
              setIsPartnerTyping(false);
              if (partnerTypingTimeoutRef.current) clearTimeout(partnerTypingTimeoutRef.current);
            }
          }
        });

        client.subscribe('/user/queue/read', (payload: IMessage) => {
          const event = JSON.parse(payload.body);
          if (event.senderId === targetUser.id) {
            setMessages(prev => prev.map(msg =>
              msg.senderId === currentUser.id ? { ...msg, isRead: true } : msg
            ));
          }
        });
      },
      onWebSocketClose: () => setIsConnected(false)
    });

    client.activate();
    stompClientRef.current = client;

    return () => { client.deactivate(); };
  }, [currentUser?.id, targetUser?.id, navigate, closeChat]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isMinimized, isPartnerTyping]);

  const handleMarkAsRead = async () => {
    if (!targetUser) return;
    try {
      await axiosClient.put(`/messages/read/${targetUser.id}`);
      window.dispatchEvent(new CustomEvent('syncUnreadBadge', {
        detail: { partnerId: targetUser.id }
      }));
      stompClientRef.current?.publish({
        destination: '/app/chat.read',
        body: JSON.stringify({ senderId: currentUser.id, receiverId: targetUser.id })
      });
    } catch (err) {
      console.error("Lỗi đánh dấu đã đọc", err);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setInput(e.target.value);

    if (!stompClientRef.current || !isConnected || !targetUser) return;

    stompClientRef.current.publish({
      destination: '/app/chat.typing',
      body: JSON.stringify({ senderId: currentUser.id, receiverId: targetUser.id, typing: true })
    });

    if (myTypingTimeoutRef.current) clearTimeout(myTypingTimeoutRef.current);
    myTypingTimeoutRef.current = setTimeout(() => {
      stompClientRef.current?.publish({
        destination: '/app/chat.typing',
        body: JSON.stringify({ senderId: currentUser.id, receiverId: targetUser.id, isTyping: false })
      });
    }, 2000);
  };

  const sendMessage = () => {
    if (!input.trim() || !stompClientRef.current || !targetUser || !isConnected) return;
    stompClientRef.current.publish({
      destination: '/app/chat',
      body: JSON.stringify({
        senderId: currentUser.id,
        receiverId: targetUser.id,
        content: input,
        messageType: 'TEXT'
      })
    });
    setInput('');
    setAnchorElEmoji(null); 

    stompClientRef.current?.publish({
      destination: '/app/chat.typing',
      body: JSON.stringify({ senderId: currentUser.id, receiverId: targetUser.id, typing: false })
    });
    if (myTypingTimeoutRef.current) clearTimeout(myTypingTimeoutRef.current);
  };

  const sendGameInvite = () => {
    if (!stompClientRef.current || !targetUser || !isConnected) return;
    stompClientRef.current.publish({
      destination: '/app/chat',
      body: JSON.stringify({ senderId: currentUser.id, receiverId: targetUser.id, content: 'GameInvite', messageType: 'GAME_INVITE' })
    });
  };

  const handleAcceptInvite = (msgId?: number) => {
    if (!stompClientRef.current || !msgId || !isConnected) return;
    stompClientRef.current.publish({
      destination: '/app/game.invite.accept',
      body: JSON.stringify({ inviteMessageId: msgId })
    });
  };

  const handleRevoke = (revokeType: 'EVERYONE' | 'SELF') => {
    if (!stompClientRef.current || !selectedMsgForMore?.id || !isConnected) return;
    stompClientRef.current.publish({
      destination: '/app/chat.revoke',
      body: JSON.stringify({ messageId: selectedMsgForMore.id, requesterId: currentUser.id, revokeType: revokeType })
    });
    setAnchorElMore(null);
  };

  const handleReact = (reactionType: string) => {
    if (!stompClientRef.current || !selectedMsgForReact?.id || !isConnected) return;
    stompClientRef.current.publish({
      destination: '/app/chat.react',
      body: JSON.stringify({ messageId: selectedMsgForReact.id, userId: currentUser.id, reactionType: reactionType })
    });
    setAnchorElReact(null);
  };

  const renderedMessages = useMemo(() => {
    const visibleMessages = messages.filter(
      msg => msg.deletedBySenderId !== currentUser.id && msg.messageType !== 'SYSTEM'
    );
    
    let lastReadMyMsgIndex = -1;
    visibleMessages.forEach((msg, idx) => {
      if (msg.senderId === currentUser.id && msg.isRead) {
        lastReadMyMsgIndex = idx;
      }
    });

    return visibleMessages.map((msg, index) => {
      const isMe = msg.senderId === currentUser.id;
      const reactionTypes = Array.from(new Set(msg.reactions?.map(r => r.reactionType) || []));
      const reactionCount = msg.reactions?.length || 0;

      return (
        <React.Fragment key={index}>
          <div className={`fb-message-row ${isMe ? 'fb-my-row' : 'fb-their-row'}`}>
            <Tooltip
              title={formatMessageTime(msg.timestamp)}
              placement={isMe ? "left" : "right"}
              arrow={false}
              enterDelay={400}
              leaveDelay={0}
              slotProps={{
                popper: { sx: { zIndex: 3000 } },
                tooltip: {
                  sx: {
                    bgcolor: isDark ? '#4E4F50' : '#E4E6EB',
                    color: isDark ? '#E4E6EB' : '#050505',
                    fontSize: '12px', fontWeight: 500,
                    padding: '6px 10px', borderRadius: '12px', boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
                    whiteSpace: 'nowrap', margin: isMe ? '0 12px 0 0 !important' : '0 0 0 12px !important'
                  }
                }
              }}
            >
              <div className={`fb-message-wrapper ${isMe ? 'my-wrapper' : 'their-wrapper'}`}>
              {!isMe && (
                <div style={{ marginRight: '8px', alignSelf: 'flex-end' }}>
                  <AvatarWithFrame
                    src={targetUser?.avatarUrl || `https://ui-avatars.com/api/?name=${targetUser?.fullName}`}
                    frameClass={(targetUser as any).currentAvatarFrame}
                    size={28}
                  />
                </div>
              )}
                <div className={`fb-message-bubble-container ${(!msg.isDeletedEveryone && reactionCount > 0) ? 'has-reaction' : ''}`}>
                  <div className={`fb-message-bubble ${msg.isDeletedEveryone ? 'fb-revoked-bubble' : (isMe ? 'fb-my-bubble' : 'fb-their-bubble')}`}>
                    {msg.isDeletedEveryone ? (
                      <span style={{ fontStyle: 'italic', color: '#65676b' }}>Tin nhắn đã thu hồi</span>
                    ) : msg.messageType === 'GAME_INVITE' ? (
                      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
                        <div style={{ fontWeight: 'bold' }}>🎮 Cùng chơi Tic Tac Toe!</div>
                        {isMe ? (
                          <span style={{ fontSize: '13px', fontStyle: 'italic', opacity: 0.9 }}>Đang chờ đối thủ...</span>
                        ) : (
                          <button onClick={() => handleAcceptInvite(msg.id)} className="fb-game-btn">Vào chơi ngay</button>
                        )}
                      </div>
                    ) : (
                      msg.content
                    )}
                  </div>

                  {!msg.isDeletedEveryone && reactionCount > 0 && (
                    <div className={`fb-reaction-badge ${isMe ? 'badge-me' : 'badge-their'}`}>
                      {reactionTypes.map((type, i) => (
                        <span key={i} className="fb-reaction-icon-small">
                          {REACTION_EMOJIS[type]}
                        </span>
                      ))}
                      {reactionCount > 1 && <span className="fb-reaction-count">{reactionCount}</span>}
                    </div>
                  )}
                </div>

                {!msg.isDeletedEveryone && (
                  <div className={`fb-message-actions ${isMe ? 'actions-me' : 'actions-their'}`}>
                    <Tooltip title="Thêm biểu tượng cảm xúc" placement="top" arrow slotProps={{ popper: { sx: { zIndex: 3000 } } }}>
                      <SentimentSatisfiedAltIcon
                        sx={{ fontSize: 24 }}
                        className="action-icon"
                        onClick={(e) => { setAnchorElReact(e.currentTarget as any); setSelectedMsgForReact(msg); }}
                      />
                    </Tooltip>
                    <Tooltip title="Trả lời" placement="top" arrow slotProps={{ popper: { sx: { zIndex: 3000 } } }}>
                      <ReplyIcon sx={{ fontSize: 24 }} className="action-icon" />
                    </Tooltip>
                    <Tooltip title="Xem thêm" placement="top" arrow slotProps={{ popper: { sx: { zIndex: 3000 } } }}>
                      <MoreVertIcon
                        sx={{ fontSize: 24 }}
                        className="action-icon"
                        onClick={(e) => { setAnchorElMore(e.currentTarget as any); setSelectedMsgForMore(msg); }}
                      />
                    </Tooltip>
                  </div>
                )}
              </div>
            </Tooltip>
          </div>

          {isMe && index === lastReadMyMsgIndex && (
            <div className="fb-read-receipt">
              <AvatarWithFrame
                src={targetUser?.avatarUrl || `https://ui-avatars.com/api/?name=${targetUser?.fullName}`}
                size={14}
              />
            </div>
          )}
        </React.Fragment>
      );
    });
  }, [messages, currentUser.id, isDark]);

  if (!targetUser) return null;

  if (isMinimized) {
    return (
      <div className="chat-bubble-container" onClick={() => setIsMinimized(false)}>
        <AvatarWithFrame src={targetUser.avatarUrl || `https://ui-avatars.com/api/?name=${targetUser.fullName}`} frameClass={(targetUser as any).currentAvatarFrame} size={56} />
        {presence?.online && <span className="fb-online-dot-minimized" style={{ position: 'absolute', bottom: 4, right: 4, width: 12, height: 12, backgroundColor: '#31a24c', borderRadius: '50%', border: `2px solid ${isDark ? '#242526' : 'white'}` }} />}
        <div className="chat-bubble-close" onClick={(e) => { e.stopPropagation(); closeChat(); }}>✖</div>
      </div>
    );
  }

  return (
    <div className={`fb-chat-container ${isDark ? 'fb-dark-mode' : ''}`}>
      <div className="fb-chat-header">
        {/* 🟢 SỬA VỊ TRÍ 1: Header User */}
        <div 
          className="fb-chat-user" 
          style={{ cursor: 'pointer' }}
          role="button"
          tabIndex={0}
          onClick={() => { navigateToProfile(targetUser.studentCode); closeChat(); }} 
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              navigateToProfile(targetUser.studentCode);
              closeChat();
            }
          }}
        >
          <div style={{ position: 'relative' }}>
            <AvatarWithFrame src={targetUser.avatarUrl || `https://ui-avatars.com/api/?name=${targetUser.fullName}`} frameClass={(targetUser as any).currentAvatarFrame} size={36} />
            {presence?.online && <span className="fb-online-dot" style={{ border: `2px solid ${isDark ? '#242526' : 'white'}` }} />}
          </div>
          <div>
            <div className="fb-chat-name">
              <ColoredName name={targetUser.fullName} colorClass={(targetUser as any).currentNameColor} />
            </div>
            <div className="fb-chat-status">
              {presence ? formatLastSeen(presence.online, presence.lastSeen) : 'Đang tải...'}
            </div>
          </div>
        </div>

        <div className="fb-chat-actions">
          <i className="fb-icon" onClick={() => setIsMinimized(true)}>─</i>
          <i className="fb-icon" onClick={closeChat}>✖</i>
        </div>
      </div>

      <div className="fb-chat-body">
        {renderedMessages}

        {isPartnerTyping && (
          <div className="fb-message-row fb-their-row">
            <div className="fb-message-wrapper their-wrapper">
              <AvatarWithFrame src={targetUser.avatarUrl || `https://ui-avatars.com/api/?name=${targetUser.fullName}`} size={28} />
              <div className="fb-typing-indicator">
                <span className="fb-typing-dot"></span>
                <span className="fb-typing-dot"></span>
                <span className="fb-typing-dot"></span>
              </div>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* FOOTER: INPUT & EMOJI */}
      <div className="fb-chat-footer">
        {/* 🟢 SỬA VỊ TRÍ 2: Nút mời chơi Game */}
        <div 
          className="fb-icon" 
          title="Mời chơi Game" 
          style={{ width: '36px', height: '36px', fontSize: '20px' }}
          role="button"
          tabIndex={0}
          onClick={sendGameInvite} 
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              sendGameInvite();
            }
          }}
        >
          🎮
        </div>

        <div className="fb-input-container">
          <input
            value={input}
            onChange={handleInputChange}
            onKeyDown={(e) => e.key === 'Enter' && sendMessage()}
            onFocus={handleMarkAsRead}
            placeholder="Aa"
            className="fb-chat-input"
          />
          
          {/* 🔴 NÚT GỌI BẢNG EMOJI ĐÃ FIX TYPESCRIPT & BỔ SUNG SONARCLOUD */}
          <InsertEmoticonIcon
            sx={{ color: '#0084ff', cursor: 'pointer', mx: 0.5 }}
            role="button"
            tabIndex={0}
            onClick={(e) => setAnchorElEmoji(e.currentTarget as any)} // Ép kiểu để qua mặt TypeScript
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                setAnchorElEmoji(e.currentTarget as any);
              }
            }}
          />
        </div>

        {/* 🟢 SỬA VỊ TRÍ 3: Nút Send Message */}
        <div 
          className={`fb-footer-icons-right ${isConnected ? '' : 'fb-send-disabled'}`} 
          role="button"
          tabIndex={isConnected ? 0 : -1}
          onClick={sendMessage}
          onKeyDown={(e) => {
            if (isConnected && (e.key === 'Enter' || e.key === ' ')) {
              e.preventDefault();
              sendMessage();
            }
          }}
        >
          <i className="fb-send-btn">➤</i>
        </div>
      </div>

      <Menu 
        anchorEl={anchorElMore} 
        open={Boolean(anchorElMore)} 
        onClose={() => setAnchorElMore(null)} 
        sx={{ zIndex: 3500 }}
        disableScrollLock={true}
      >
        <MenuItem onClick={() => handleRevoke('SELF')}>Thu hồi phía bạn</MenuItem>
        {selectedMsgForMore?.senderId === currentUser.id && (
          <MenuItem onClick={() => handleRevoke('EVERYONE')} sx={{ color: '#e53935' }}>Thu hồi với mọi người</MenuItem>
        )}
      </Menu>

      <Popover 
        anchorEl={anchorElReact} 
        open={Boolean(anchorElReact)} 
        onClose={() => setAnchorElReact(null)} 
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }} 
        transformOrigin={{ vertical: 'bottom', horizontal: 'center' }} 
        sx={{ zIndex: 3500 }}
        disableScrollLock={true}
      >
        <div className="fb-reaction-bar-popup">
          {Object.entries(REACTION_EMOJIS).map(([type, emoji]) => (
            /* 🟢 SỬA VỊ TRÍ 4: Nút thả Reaction */
            <span 
              key={type} 
              className="fb-reaction-emoji-btn" 
              role="button"
              tabIndex={0}
              onClick={() => handleReact(type)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  handleReact(type);
                }
              }}
            >
              {emoji}
            </span>
          ))}
        </div>
      </Popover>

      <Popover
        anchorEl={anchorElEmoji}
        open={Boolean(anchorElEmoji)}
        onClose={() => setAnchorElEmoji(null)}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
        transformOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        sx={{ zIndex: 3500 }}
        disableScrollLock={true}
        PaperProps={{
          sx: { borderRadius: '12px', mb: 1, overflow: 'hidden', boxShadow: '0 4px 12px rgba(0,0,0,0.15)' }
        }}
      >
        <EmojiPicker
          onEmojiClick={(emojiData) => {
            setInput(prev => prev + emojiData.emoji);
          }}
          theme={isDark ? Theme.DARK : Theme.LIGHT} 
          searchPlaceHolder="Tìm kiếm emoji..."
          width={300}
          height={350}
        />
      </Popover>
    </div>
  );
};

export default ChatBox;