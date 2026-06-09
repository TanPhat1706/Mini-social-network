import React, { createContext, useEffect, useState, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { IMessage } from '@stomp/stompjs';
import axios from 'axios';
import { useNavigate } from 'react-router-dom'; // 🔴 MỚI THÊM
import type { GameSession, GameWsEvent } from '../types/game';
import { getApiBaseUrl } from '../config/apiBase';
import { showError } from '../utils/swal';

// ... (Giữ nguyên interface WebSocketContextType)
interface WebSocketContextType {
    notifications: any[];
    unreadCount: number;
    markAllAsRead: () => void;
    refreshNotifications: () => void;
    setNotifications: React.Dispatch<React.SetStateAction<any[]>>;
    currentGame: GameSession | null;
    setCurrentGame: (game: GameSession | null) => void;
    subscribeToRoom: (sessionId: number) => void;
    sendGameAction: (destination: string, body: any) => void;
    isConnected: boolean;
}

export const WebSocketContext = createContext<WebSocketContextType | null>(null);

export const WebSocketProvider = ({ children }: { children: React.ReactNode }) => {
    const navigate = useNavigate();
    const [notifications, setNotifications] = useState<any[]>([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [currentGame, setCurrentGame] = useState<GameSession | null>(null);
    const [isConnected, setIsConnected] = useState(false);

    const clientRef = useRef<Client | null>(null);
    const roomSubscriptionRef = useRef<any>(null);
    const token = localStorage.getItem('token');

    // ... (Giữ nguyên mapNotification và fetchHistory)
    const mapNotification = (data: any) => ({
        id: data.id || Date.now(),
        senderId: data.senderId,
        senderName: data.senderName,
        senderAvatar: data.senderAvatar,
        message: data.message,
        createdAt: data.createdAt || data.timestamp || new Date().toISOString(),
        targetUrl: data.targetUrl || (data.type === 'FRIEND_REQUEST' ? `/profile/${data.senderStudentCode || data.senderId}` : '#'),
        isRead: data.read || false,
        type: data.type
    });

    const fetchHistory = async () => {
        if (!token) return;
        try {
            const response = await axios.get(`${getApiBaseUrl()}/api/notifications`, {
                headers: {
                    Authorization: `Bearer ${token}`,
                    'ngrok-skip-browser-warning': 'true',
                },
            });
            const listMapped = (Array.isArray(response.data) ? response.data : []).map(mapNotification);
            setNotifications(listMapped);
            setUnreadCount(listMapped.filter((n: any) => !n.isRead).length);
        } catch (error) {
            console.error(">>> [API ERROR] Lỗi fetch notification:", error);
        }
    };

    // 🟢 HÀM XỬ LÝ "ĐÃ ĐỌC TẤT CẢ" CHUẨN KIẾN TRÚC SSoT (Single Source of Truth)
    const markAllAsRead = async () => {
        if (unreadCount === 0) return; // Nếu không có thông báo chưa đọc thì không làm gì cả

        // 1. [OPTIMISTIC UI] Update UI ngay lập tức cho mượt
        setUnreadCount(0);
        setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));

        // 2. [BACKGROUND SYNC] Gọi API xuống backend cập nhật SQL Server
        try {
            // Lưu ý em dùng axiosClient hay axios tùy config của em nhé
            await axios.put(`${getApiBaseUrl()}/api/notifications/read-all`, {}, {
                headers: { Authorization: `Bearer ${token}` }
            });
            console.log(">>> [DB SYNC] Đã cập nhật thành công trạng thái đọc xuống Database");
        } catch (err) {
            console.error(">>> [API ERROR] Lỗi đồng bộ trạng thái thông báo:", err);
        }
    };

    const subscribeToRoom = (sessionId: number) => {
        if (!clientRef.current?.connected) {
            console.warn(">>> [WS] Chưa kết nối, không thể subscribe phòng.");
            return;
        }
        if (roomSubscriptionRef.current) {
            roomSubscriptionRef.current.unsubscribe();
        }

        console.log(`>>> [WS] Đang lắng nghe phòng game: ${sessionId}`);
        roomSubscriptionRef.current = clientRef.current.subscribe(`/topic/game/${sessionId}`, (message: IMessage) => {
            const event: GameWsEvent = JSON.parse(message.body);
            console.log(">>> [GAME UPDATE]", event);
            if (event.session) {
                setCurrentGame(event.session);
            }
        });
    };

    const sendGameAction = (destination: string, body: any) => {
        if (clientRef.current?.connected) {
            clientRef.current.publish({
                destination: `/app/${destination}`,
                body: JSON.stringify(body)
            });
        }
    };

    useEffect(() => {
        if (!token) return;
        fetchHistory();

        const client = new Client({
            webSocketFactory: () => new SockJS(`${getApiBaseUrl()}/ws`),
            connectHeaders: { Authorization: `Bearer ${token}` },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,

            onConnect: () => {
                console.log(">>> [WS CONNECTED] ✅");
                setIsConnected(true);

                client.subscribe('/user/queue/notifications', (message) => {
                    const newNoti = mapNotification(JSON.parse(message.body));
                    setNotifications((prev) => [newNoti, ...prev]);
                    setUnreadCount((prev) => prev + 1);
                });

                client.subscribe('/user/queue/game-events', (message) => {
                    const event: GameWsEvent = JSON.parse(message.body);
                    console.log(">>> [PERSONAL GAME EVENT]", event);

                    if (event.type === 'ERROR') {
                        showError(`Lỗi: ${event.message}`);


                        // 🔴 MỚI THÊM: CHUYỂN HƯỚNG CẢ 2 NGƯỜI VÀO PHÒNG CÙNG LÚC
                        navigate(`/games/tic-tac-toe/${event.session.id}`);
                    }
                });
            },
            onDisconnect: () => {
                console.log(">>> [WS DISCONNECTED] ❌");
                setIsConnected(false);
            }
        });

        client.activate();
        clientRef.current = client;

        return () => {
            if (clientRef.current) clientRef.current.deactivate();
        };
    }, [token]);

    // const markAllAsRead = () => {
    //     setUnreadCount(0);
    //     setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
    // };

    return (
        <WebSocketContext.Provider value={{
            notifications, unreadCount, markAllAsRead, refreshNotifications: fetchHistory, setNotifications, currentGame, setCurrentGame, subscribeToRoom, sendGameAction, isConnected
        }}>
            {children}
        </WebSocketContext.Provider>
    );
};