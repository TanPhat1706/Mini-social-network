import React, { useState, useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import { useNavigate } from 'react-router-dom'; // MỚI THÊM: Để chuyển hướng vào phòng game
import axiosClient from '../../api/axiosClient';
import { getApiBaseUrl } from '../../config/apiBase';
import type { User } from '../../types';
import { useChat } from '../../context/ChatContext';
import './ChatBox.css';
import { getMessagesHistory } from '../../api/messageApi';

// IMPORT COMPONENT AVATAR
import AvatarWithFrame from '../AvatarWithFrame';
import ColoredName from '../ColoredName';

interface Message {
  id?: number;
  senderId: number;
  receiverId: number;
  content: string;
  timestamp?: string;
  messageType?: string;
  gameSessionId?: number;
}

interface Props {
  currentUser: User;
}

const ChatBox: React.FC<Props> = ({ currentUser }) => {
  const { chatTarget, closeChat, isMinimized, setIsMinimized } = useChat();
  const targetUser = chatTarget;
  const navigate = useNavigate(); // MỚI THÊM

  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');

  const stompClientRef = useRef<any>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  /* ================= MAP MESSAGE ================= */
  const mapMessage = (data: any): Message => ({
    id: data.id,
    content: data.content,
    timestamp: data.timestamp || data.createdAt,
    senderId: data.senderId || data.sender?.id,
    receiverId: data.receiverId || data.receiver?.id,
    messageType: data.messageType,
    gameSessionId: data.gameSessionId
  });

/* ================= LOAD HISTORY ================= */
useEffect(() => {
  if (!targetUser) return;

  setMessages([]);
  
  // 2. Sử dụng hàm từ messageApi thay vì gọi trực tiếp axiosClient
  getMessagesHistory(currentUser.id, targetUser.id)
    .then(res => setMessages(res.data.map(mapMessage)))
    .catch(console.error);
}, [currentUser.id, targetUser]);

  /* ================= WEBSOCKET ================= */
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!targetUser || !token) return;

    const socket = new SockJS(`${getApiBaseUrl()}/ws`);
    const client = Stomp.over(socket);
    client.debug = () => {};

    client.connect(
      { Authorization: `Bearer ${token}` },
      () => {
        // 1. Lắng nghe tin nhắn Chat thông thường
        client.subscribe('/user/queue/messages', payload => {
          const newMsg = mapMessage(JSON.parse(payload.body));
          setMessages(prev => [...prev, newMsg]);
        });

        // 2. MỚI THÊM: Lắng nghe tín hiệu Game (Khi đối thủ ấn Chấp nhận)
        client.subscribe('/user/queue/game-events', payload => {
          const event = JSON.parse(payload.body);
          if (event.type === 'GAME_INVITE_ACCEPTED' && event.session?.id) {
            // Đóng khung chat và chuyển hướng cả 2 người vào phòng
            closeChat();
            navigate(`/games/tic-tac-toe/${event.session.id}`);
          }
        });
      }
    );

    stompClientRef.current = client;
    return () => {
      client.disconnect(() => {});
    };
  }, [currentUser.id, targetUser, navigate, closeChat]);

  /* ================= AUTO SCROLL ================= */
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isMinimized]);

  /* ================= SEND MESSAGE ================= */
  const sendMessage = () => {
    if (!input.trim() || !stompClientRef.current || !targetUser) return;

    stompClientRef.current.send(
      '/app/chat',
      {},
      JSON.stringify({
        senderId: currentUser.id,
        receiverId: targetUser.id,
        content: input,
        messageType: 'TEXT'
      })
    );
    setInput('');
  };

  /* ================= SEND GAME INVITE ================= */
  const sendGameInvite = () => {
    if (!stompClientRef.current || !targetUser) return;

    stompClientRef.current.send(
      '/app/chat',
      {},
      JSON.stringify({
        senderId: currentUser.id,
        receiverId: targetUser.id,
        content: 'GameInvite', // Gửi không dấu để tránh lỗi SQL
        messageType: 'GAME_INVITE'
      })
    );
  };

  /* ================= HANDLE ACCEPT INVITE (MỚI THÊM) ================= */
  const handleAcceptInvite = (msgId?: number) => {
    if (!stompClientRef.current || !msgId) return;

    // Bắn request lên Controller của Backend
    stompClientRef.current.send(
      '/app/game.invite.accept',
      {},
      JSON.stringify({ inviteMessageId: msgId })
    );
  };

  /* ================= GUARD ================= */
  if (!targetUser) return null;

  /* ================= MINIMIZED ================= */
  if (isMinimized) {
    return (
      <div className="chat-bubble-container" onClick={() => setIsMinimized(false)}>
        <AvatarWithFrame 
          src={targetUser.avatarUrl || `https://ui-avatars.com/api/?name=${targetUser.fullName}`}
          frameClass={(targetUser as any).currentAvatarFrame}
          size={56} 
        />
        <div className="chat-bubble-close" onClick={(e) => { e.stopPropagation(); closeChat(); }}>✖</div>
      </div>
    );
  }

  /* ================= FULL CHAT ================= */
  return (
    <div className="fb-chat-container">
      {/* ===== HEADER ===== */}
      <div className="fb-chat-header">
        <div className="fb-chat-user">
          <div style={{ position: 'relative' }}>
            <AvatarWithFrame 
              src={targetUser.avatarUrl || `https://ui-avatars.com/api/?name=${targetUser.fullName}`}
              frameClass={(targetUser as any).currentAvatarFrame}
              size={36} 
            />
            <span className="fb-online-dot" />
          </div>
          <div>
            <div className="fb-chat-name">
                <ColoredName name={targetUser.fullName} colorClass={(targetUser as any).currentNameColor} />
            </div>            
            <div className="fb-chat-status">Đang hoạt động</div>
          </div>
        </div>

        <div className="fb-chat-actions">
          <i className="fb-icon" onClick={() => setIsMinimized(true)}>─</i>
          <i className="fb-icon" onClick={closeChat}>✖</i>
        </div>
      </div>

      {/* ===== BODY ===== */}
      <div className="fb-chat-body">
        {messages.map((msg, index) => {
          const isMe = msg.senderId === currentUser.id;
          return (
            <div key={index} className={`fb-message-row ${isMe ? 'fb-my-row' : 'fb-their-row'}`}>
              <div className={`fb-message-bubble ${isMe ? 'fb-my-bubble' : 'fb-their-bubble'}`}>
                
                {/* 🔴 MỚI THÊM: GIAO DIỆN LỜI MỜI GAME 🔴 */}
                {msg.messageType === 'GAME_INVITE' ? (
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
                    <div style={{ fontWeight: 'bold' }}>🎮 Cùng chơi Tic Tac Toe!</div>
                    {isMe ? (
                      <span style={{ fontSize: '13px', fontStyle: 'italic', opacity: 0.9 }}>
                        Đang chờ đối thủ...
                      </span>
                    ) : (
                      <button
                        onClick={() => handleAcceptInvite(msg.id)}
                        style={{
                          backgroundColor: '#31a24c',
                          color: 'white',
                          border: 'none',
                          padding: '6px 16px',
                          borderRadius: '6px',
                          fontWeight: 'bold',
                          cursor: 'pointer'
                        }}
                      >
                        Vào chơi ngay
                      </button>
                    )}
                  </div>
                ) : (
                  msg.content
                )}
                
                <div className="fb-message-time">
                  {msg.timestamp && new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </div>
              </div>
            </div>
          );
        })}
        <div ref={messagesEndRef} />
      </div>

      {/* ===== FOOTER ===== */}
      <div className="fb-chat-footer">
        <div 
          className="fb-icon" 
          onClick={sendGameInvite} 
          title="Mời chơi Game"
          style={{ width: '36px', height: '36px', fontSize: '20px' }}
        >
          🎮
        </div>

        <div className="fb-input-container">
          <input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && sendMessage()}
            placeholder="Aa"
            className="fb-chat-input"
          />
        </div>

        <div className={`fb-footer-icons-right ${stompClientRef.current?.connected ? '' : 'fb-send-disabled'}`} onClick={sendMessage}>
          <i className="fb-send-btn">➤</i>
        </div>
      </div>
    </div>
  );
};

export default ChatBox;