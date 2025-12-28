import React, { createContext, useContext, useState, type ReactNode } from 'react';
import type { User } from '../types';

interface ChatContextType {
  chatTarget: User | null;
  openChat: (user: User) => void;
  closeChat: () => void;
  isMinimized: boolean;
  setIsMinimized: (val: boolean) => void;
}

const ChatContext = createContext<ChatContextType | undefined>(undefined);

export const ChatProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [chatTarget, setChatTarget] = useState<User | null>(null);
  const [isMinimized, setIsMinimized] = useState(false);

  const openChat = (user: User) => {
    // Nếu đang chat với người khác thì chuyển sang người mới và mở lên
    setChatTarget(user);
    setIsMinimized(false); 
  };

  const closeChat = () => {
    setChatTarget(null);
    setIsMinimized(false);
  };

  return (
    <ChatContext.Provider value={{ chatTarget, openChat, closeChat, isMinimized, setIsMinimized }}>
      {children}
    </ChatContext.Provider>
  );
};

// eslint-disable-next-line react-refresh/only-export-components
export const useChat = () => {
  const context = useContext(ChatContext);
  if (!context) throw new Error("useChat must be used within a ChatProvider");
  return context;
};