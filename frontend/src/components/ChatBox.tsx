import React, { useState, useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import axiosClient from '../api/axiosClient';
import type { User } from '../types';
import { useChat } from '../context/ChatContext'; // Dùng Context
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
  // Lấy dữ liệu từ Context thay vì props
  const { chatTarget, closeChat, isMinimized, setIsMinimized } = useChat();
  const targetUser = chatTarget;

  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const stompClientRef = useRef<any>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 1. Reset messages khi đổi người chat
  useEffect(() => {
    if (targetUser) {
        setMessages([]); // Clear cũ
        axiosClient.get(`/messages/${currentUser.id}/${targetUser.id}`)
          .then(res => setMessages(res.data))
          .catch(console.error);
    }
  }, [currentUser.id, targetUser]);

  // 2. Kết nối WebSocket
  useEffect(() => {
    if (!targetUser) return;

    let client: any = null;
    const socket = new SockJS('http://localhost:8080/ws');
    client = Stomp.over(socket);
    client.debug = () => {};

    client.connect({}, () => {
      client.subscribe(`/user/${currentUser.id}/queue/messages`, (payload: any) => {
        const newMessage = JSON.parse(payload.body);
        if (newMessage.senderId === targetUser.id || newMessage.senderId === currentUser.id) {
           setMessages(prev => [...prev, newMessage]);
        }
      });
    }, (err: any) => console.error(err));

    stompClientRef.current = client;
    return () => { if (client && client.connected) client.disconnect(); };
  }, [currentUser.id, targetUser]);

  // 3. Auto Scroll
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isMinimized]); // Scroll khi có tin mới hoặc khi mở lại từ bubble

  // 4. Gửi tin
  const sendMessage = () => {
    if (!input.trim() || !stompClientRef.current || !targetUser) return;
    const chatMessage = { senderId: currentUser.id, receiverId: targetUser.id, content: input };
    try {
      stompClientRef.current.send("/app/chat", {}, JSON.stringify(chatMessage));
      setMessages(prev => [...prev, { ...chatMessage, timestamp: new Date().toISOString() }]);
      setInput('');
    } catch (e) { console.error(e); }
  };

  if (!targetUser) return null;

  // --- GIAO DIỆN BONG BÓNG (MINIMIZED) ---
  if (isMinimized) {
    return (
      <div className="chat-bubble-container" onClick={() => setIsMinimized(false)} title={targetUser.fullName}>
         <img 
           src={targetUser.avatarUrl || `https://ui-avatars.com/api/?name=${targetUser.fullName}`} 
           className="chat-bubble-avatar" alt="chat"
         />
         <div className="chat-bubble-close" onClick={(e) => { e.stopPropagation(); closeChat(); }}>✖</div>
      </div>
    );
  }

  // --- GIAO DIỆN FULL ---
  return (
    <div className="fb-chat-container">
      <div className="fb-chat-header">
        <div className="fb-chat-user-info">
           <img src={targetUser.avatarUrl || `https://ui-avatars.com/api/?name=${targetUser.fullName}`} className="fb-chat-avatar-header" alt="ava" />
           <div className="fb-chat-names">
             <span className="fb-target-name">{targetUser.fullName}</span>
             <span className="fb-active-status-text">Đang hoạt động</span>
           </div>
        </div>
        <div className="fb-chat-actions">
          <i className="fb-icon minimize-icon" onClick={() => setIsMinimized(true)} title="Thu nhỏ">─</i>
          <i className="fb-icon close-icon" onClick={closeChat} title="Đóng">✖</i>
        </div>
      </div>

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
                     <img src={targetUser.avatarUrl || `https://ui-avatars.com/api/?name=${targetUser.fullName}`} className="fb-msg-avatar-tiny" alt="ava"/>
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