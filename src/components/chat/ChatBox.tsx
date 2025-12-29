import React, { useState, useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import axiosClient from '../../api/axiosClient';
import type { User } from '../../types';
import { useChat } from '../../context/ChatContext';
import './ChatBox.css';

interface Message {
  id?: number;
  senderId: number;
  receiverId: number;
  content: string;
  timestamp?: string;
}

interface Props {
  currentUser: User;
}

const ChatBox: React.FC<Props> = ({ currentUser }) => {
  const { chatTarget, closeChat, isMinimized, setIsMinimized } = useChat();
  const targetUser = chatTarget;

  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const stompClientRef = useRef<any>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const mapMessage = (data: any): Message => {
    return {
      id: data.id,
      content: data.content,
      timestamp: data.timestamp || data.createdAt,
      senderId: data.senderId || data.sender?.id,
      receiverId: data.receiverId || data.receiver?.id
    };
  };

  // 1. Fetch tin nhắn cũ
  useEffect(() => {
    if (targetUser) {
        setMessages([]); 
        axiosClient.get(`/messages/${currentUser.id}/${targetUser.id}`)
          .then(res => {
             const mapped = res.data.map((m: any) => mapMessage(m));
             setMessages(mapped);
          })
          .catch(console.error);
    }
  }, [currentUser.id, targetUser]);

  // 2. Kết nối WebSocket
  useEffect(() => {
    const token = localStorage.getItem('token'); 
    if (!targetUser || !token) return;

    // Ngắt kết nối cũ nếu có (dùng ref để check chắc chắn)
    if (stompClientRef.current && stompClientRef.current.connected) {
        stompClientRef.current.disconnect(() => {
            console.log("Disconnected old connection");
        });
    }

    const socket = new SockJS('https://mini-social-network-ayab.onrender.com/ws');
    const client = Stomp.over(socket);
    client.debug = () => {}; 

    const headers = { 'Authorization': `Bearer ${token}` };

    client.connect(headers, () => {
      client.subscribe('/user/queue/messages', (payload: any) => {
        const rawMsg = JSON.parse(payload.body);
        const newMessage = mapMessage(rawMsg);
        
        const isRelated = 
            (newMessage.senderId === targetUser.id && newMessage.receiverId === currentUser.id) || 
            (newMessage.senderId === currentUser.id && newMessage.receiverId === targetUser.id);

        if (isRelated) {
            setMessages(prev => {
                if (newMessage.id && prev.some(m => m.id === newMessage.id)) return prev;
                return [...prev, newMessage];
            });
        }
      });
    }, (err: any) => console.error("WS Error:", err));

    stompClientRef.current = client;

    // --- KHÔI PHỤC ĐOẠN DISCONNECT CỦA BẠN ---
    return () => { 
        if (client && client.connected) {
            client.disconnect(() => {
                console.log("Disconnected from WebSocket");
            });
        }
    };
  }, [currentUser.id, targetUser]);

  // 3. Auto Scroll
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isMinimized]);

  // 4. Send Message
  const sendMessage = () => {
    if (!input.trim() || !stompClientRef.current || !targetUser) return;
    
    const chatPayload = { 
        senderId: currentUser.id, 
        receiverId: targetUser.id, 
        content: input 
    };

    try {
      stompClientRef.current.send("/app/chat", {}, JSON.stringify(chatPayload));
      setInput('');
    } catch (e) { console.error("Send Error:", e); }
  };

  if (!targetUser) return null;

  if (isMinimized) {
    return (
      <div className="chat-bubble-container" onClick={() => setIsMinimized(false)}>
         <img 
           src={targetUser.avatarUrl || `https://ui-avatars.com/api/?name=${targetUser.fullName}`} 
           className="chat-bubble-avatar" alt="chat"
         />
         <div className="chat-bubble-close" onClick={(e) => { e.stopPropagation(); closeChat(); }}>✖</div>
      </div>
    );
  }

  return (
    <div className="fb-chat-container">
      {/* Header */}
      <div className="fb-chat-header">
         <div className="fb-chat-user-info">
            <div style={{position: 'relative'}}>
                <img 
                    src={targetUser.avatarUrl || `https://ui-avatars.com/api/?name=${targetUser.fullName}`} 
                    className="fb-chat-avatar-header" 
                    alt="avatar" 
                />
                <span className="fb-active-status-dot"></span>
            </div>
            <div className="fb-chat-names">
                <span className="fb-target-name">{targetUser.fullName}</span>
                <span className="fb-active-status-text">Đang hoạt động</span>
            </div>
         </div>

         <div className="fb-chat-actions">
           <i className="fb-icon minimize-icon" onClick={() => setIsMinimized(true)}>─</i>
           <i className="fb-icon close-icon" onClick={closeChat}>✖</i>
         </div>
      </div>

      {/* Body */}
      <div className="fb-chat-body">
        {messages.map((msg, index) => {
          const isMe = msg.senderId === currentUser.id;
          const nextMsg = messages[index + 1];
          const isLastInGroup = !nextMsg || nextMsg.senderId !== msg.senderId;

          return (
            <div key={index} className={`fb-message-row ${isMe ? 'fb-my-row' : 'fb-their-row'}`}>
               {!isMe && (
                   <div className="fb-avatar-spacer">
                       {isLastInGroup && (
                           <img 
                               src={targetUser.avatarUrl || `https://ui-avatars.com/api/?name=${targetUser.fullName}`} 
                               className="fb-msg-avatar-tiny" 
                               alt="avt" 
                           />
                       )}
                   </div>
               )}
               <div className={`fb-message-bubble ${isMe ? 'fb-my-bubble' : 'fb-their-bubble'} ${isLastInGroup ? 'last-in-group' : ''}`}>
                  {msg.content}
               </div>
            </div>
          )
        })}
        <div ref={messagesEndRef} />
      </div>

      {/* Footer: ĐÃ TỐI GIẢN */}
      <div className="fb-chat-footer">
        <div className="fb-input-container">
          <input 
             value={input} 
             onChange={(e) => setInput(e.target.value)} 
             onKeyDown={(e) => e.key === 'Enter' && sendMessage()}
             placeholder="Aa" 
             className="fb-chat-input" 
             autoFocus
          />
        </div>
        
        {/* Chỉ hiện nút Gửi (Mũi tên) */}
        <div className="fb-footer-icons-right" onClick={sendMessage}>
           <i className={`fb-send-btn ${input.trim() ? 'active' : ''}`}>➤</i>
        </div>
      </div>
    </div>
  );
};

export default ChatBox;