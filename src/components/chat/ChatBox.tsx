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

  // Helper: Chuẩn hóa message từ Backend (Dù trả về sender Object hay senderId đều nhận hết)
  const mapMessage = (data: any): Message => {
    return {
      id: data.id,
      content: data.content,
      timestamp: data.timestamp || data.createdAt,
      // Ưu tiên lấy ID trực tiếp, nếu không có thì lấy từ object sender
      senderId: data.senderId || data.sender?.id,
      receiverId: data.receiverId || data.receiver?.id
    };
  };

  // 1. Fetch tin nhắn cũ (API)
  useEffect(() => {
    if (targetUser) {
        setMessages([]); 
        axiosClient.get(`/messages/${currentUser.id}/${targetUser.id}`)
          .then(res => {
             // Map dữ liệu API luôn cho chắc
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

    // --- FIX: Tận dụng kết nối Socket chung nếu có thể, nhưng ở đây ta fix connection riêng ---
    // Ngắt kết nối cũ nếu có để tránh duplicate tin nhắn
    if (stompClientRef.current && stompClientRef.current.connected) {
        stompClientRef.current.disconnect();
    }

    const socket = new SockJS('https://mini-social-network-ayab.onrender.com/ws');
    const client = Stomp.over(socket);
    client.debug = () => {}; // Tắt log cho gọn

    const headers = { 'Authorization': `Bearer ${token}` };

    client.connect(headers, () => {
      // Subscribe kênh cá nhân của mình
      client.subscribe('/user/queue/messages', (payload: any) => {
        const rawMsg = JSON.parse(payload.body);
        console.log(">>> Raw Socket Msg:", rawMsg); // Debug xem BE trả về gì

        const newMessage = mapMessage(rawMsg); // <--- QUAN TRỌNG: Map lại dữ liệu
        
        // Check xem tin nhắn này có thuộc cuộc hội thoại đang mở không
        const isRelated = 
            (newMessage.senderId === targetUser.id && newMessage.receiverId === currentUser.id) || 
            (newMessage.senderId === currentUser.id && newMessage.receiverId === targetUser.id);

        if (isRelated) {
            setMessages(prev => {
                // Chống duplicate: Kiểm tra xem message ID đã tồn tại chưa
                if (newMessage.id && prev.some(m => m.id === newMessage.id)) return prev;
                return [...prev, newMessage];
            });
        }
      });
    }, (err: any) => console.error("WS Error:", err));

    stompClientRef.current = client;

    // Cleanup khi đóng chat box hoặc đổi người chat
    return () => { 
        if (client && client.connected) {
            client.disconnect();
        }
    };
  }, [currentUser.id, targetUser]); // Dependency chuẩn

  // 3. Auto Scroll
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isMinimized]);

  // 4. Gửi tin nhắn
  const sendMessage = () => {
    if (!input.trim() || !stompClientRef.current || !targetUser) return;
    
    // Tạo message object chuẩn form Backend nhận
    const chatPayload = { 
        senderId: currentUser.id, // Hoặc sender: {id: ...} tuỳ BE
        receiverId: targetUser.id, 
        content: input 
    };

    // Tạo message hiển thị ngay lập tức (Optimistic UI)
    const optimisticMsg: Message = {
        senderId: currentUser.id,
        receiverId: targetUser.id,
        content: input,
        timestamp: new Date().toISOString()
    };

    try {
      stompClientRef.current.send("/app/chat", {}, JSON.stringify(chatPayload));
      
      // Update UI ngay lập tức
      // setMessages(prev => [...prev, optimisticMsg]);
      setInput('');
    } catch (e) { console.error("Send Error:", e); }
  };

  if (!targetUser) return null;

  // ... (Phần render UI giữ nguyên như code cũ của em) ...
  // LƯU Ý: Đảm bảo phần render dùng đúng field (senderId)
  // ...

  // --- MINIMIZED UI ---
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

  // --- FULL UI ---
  return (
    <div className="fb-chat-container">
      {/* Header */}
      <div className="fb-chat-header">
         {/* ... code cũ ... */}
         <div className="fb-chat-actions">
           <i className="fb-icon minimize-icon" onClick={() => setIsMinimized(true)}>─</i>
           <i className="fb-icon close-icon" onClick={closeChat}>✖</i>
         </div>
      </div>

      {/* Body */}
      <div className="fb-chat-body">
        {messages.map((msg, index) => {
          const isMe = msg.senderId === currentUser.id; // Check ID chuẩn
          return (
            <div key={index} className={`fb-message-row ${isMe ? 'fb-my-row' : 'fb-their-row'}`}>
               {/* Nội dung tin nhắn */}
               <div className={`fb-message-bubble ${isMe ? 'fb-my-bubble' : 'fb-their-bubble'}`}>
                  {msg.content}
               </div>
            </div>
          )
        })}
        <div ref={messagesEndRef} />
      </div>

      {/* Footer */}
      <div className="fb-chat-footer">
        <div className="fb-input-container">
          <input 
             value={input} 
             onChange={(e) => setInput(e.target.value)} 
             onKeyDown={(e) => e.key === 'Enter' && sendMessage()}
             placeholder="Aa" className="fb-chat-input" autoFocus
          />
        </div>
        <div className="fb-footer-icons-right" onClick={sendMessage}>
           <i className="fb-send-btn">➤</i>
        </div>
      </div>
    </div>
  );
};

export default ChatBox;