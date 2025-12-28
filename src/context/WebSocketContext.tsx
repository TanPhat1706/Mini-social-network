// WebSocketContext.tsx
import React, { createContext, useContext, useEffect, useState, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from 'axios';

interface WebSocketContextType {
    notifications: any[];
    unreadCount: number;
    setUnreadCount: (count: number) => void;
    markAllAsRead: () => void;
    refreshNotifications: () => void; // Thêm hàm này để các component khác có thể ép load lại
    setNotifications: React.Dispatch<React.SetStateAction<any[]>>;
}

const WebSocketContext = createContext<WebSocketContextType | null>(null);

export const WebSocketProvider = ({ children }: { children: React.ReactNode }) => {
    const [notifications, setNotifications] = useState<any[]>([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const clientRef = useRef<Client | null>(null);
    const token = localStorage.getItem('token'); 

    // Hàm chuẩn hóa dữ liệu từ Backend (Dù là Like/Comment hay FriendRequest đều dùng được)
    const mapNotification = (data: any) => {
        return {
            id: data.id || Date.now(), // Nếu không có ID thì tạo tạm
            senderId: data.senderId,
            senderName: data.senderName,
            senderAvatar: data.senderAvatar,
            message: data.message,
            // Xử lý sự khác biệt giữa createdAt (Date) và timestamp (String)
            createdAt: data.createdAt || data.timestamp || new Date().toISOString(),
            // Nếu FriendshipService không gửi targetUrl, ta tự tạo
            targetUrl: data.targetUrl || (data.type === 'FRIEND_REQUEST' ? `/profile/${data.senderId}` : '#'),
            isRead: data.isRead || false,
            type: data.type
        };
    };

    const fetchHistory = async () => {
        if (!token) return;
        try {
            // Đảm bảo URL đúng với Controller của em
            const response = await axios.get('https://mini-social-network-ayab.onrender.com/api/notifications', {
                headers: { Authorization: `Bearer ${token}` }
            });
            
            console.log(">>> [API HISTORY]", response.data);
            
            // Map lại dữ liệu cũ từ API cho đồng bộ luôn
            const listRaw = Array.isArray(response.data) ? response.data : [];
            const listMapped = listRaw.map(mapNotification);

            setNotifications(listMapped);
            
            const unread = listMapped.filter((n: any) => !n.isRead).length;
            setUnreadCount(unread);
        } catch (error) {
            console.error("Lỗi fetch notification:", error);
        }
    };

    // Gọi API khi mới vào trang
    useEffect(() => {
        fetchHistory();
    }, [token]);

    // Kết nối WebSocket
    useEffect(() => {
        if (!token) return;
        
        // Ngắt kết nối cũ nếu có để tránh duplicate
        if (clientRef.current?.active) {
            clientRef.current.deactivate();
        }

        console.log(">>> [WS INIT] Connecting...");

        const client = new Client({
            webSocketFactory: () => new SockJS('https://mini-social-network-ayab.onrender.com/ws'), 
            connectHeaders: { Authorization: `Bearer ${token}` },
            reconnectDelay: 5000,
            
            onConnect: () => {
                console.log(">>> [WS CONNECTED] Subscribing...");
                
                client.subscribe('/user/queue/notifications', (message) => {
                    if (message.body) {
                        try {
                            const rawData = JSON.parse(message.body);
                            console.log(">>> [WS RECV] Raw:", rawData);

                            // 1. Chuẩn hóa dữ liệu trước khi lưu vào State
                            const newNoti = mapNotification(rawData);

                            // 2. Cập nhật State
                            setNotifications((prev) => [newNoti, ...prev]);
                            setUnreadCount((prev) => prev + 1);
                        } catch (e) {
                            console.error("Lỗi parse notification:", e);
                        }
                    }
                });
            },
            onStompError: (frame) => {
                console.error('>>> [WS ERROR]:', frame.headers['message']);
            }
        });

        client.activate();
        clientRef.current = client;

        return () => { 
            if (clientRef.current) {
                console.log(">>> [WS CLEANUP] Deactivating...");
                clientRef.current.deactivate();
            }
        };
    }, [token]);

    const markAllAsRead = () => {
        setUnreadCount(0);
        setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
        // TODO: Gọi API mark-all-read về Backend nếu cần
    };

    return (
        <WebSocketContext.Provider value={{ 
            notifications, 
            unreadCount, 
            setUnreadCount, 
            markAllAsRead,
            refreshNotifications: fetchHistory, // Expose hàm này ra
            setNotifications
        }}>
            {children}
        </WebSocketContext.Provider>
    );
};

export const useWebSocket = () => {
    const context = useContext(WebSocketContext);
    if (!context) throw new Error("useWebSocket must be used within WebSocketProvider");
    return context;
};