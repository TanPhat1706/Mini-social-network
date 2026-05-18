import React, { useState, useEffect } from 'react';
import axiosClient from '../../api/axiosClient';
import { useTheme } from '@mui/material/styles'; // 🟢 IMPORT THEME

interface Props { 
  targetUserId: number; 
  currentUserId: number; 
  className?: string; 
}

const FriendButton: React.FC<Props> = ({ targetUserId, currentUserId, className }) => {
  const [status, setStatus] = useState<string>('NONE');
  const [actionUserId, setActionUserId] = useState<number | null>(null);
  const [isHovering, setIsHovering] = useState(false);
  
  // 🟢 CÀI ĐẶT BỘ MÀU THEO CHẾ ĐỘ SÁNG TỐI
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  const greyBg = isDark ? '#3A3B3C' : '#e4e6eb';
  const greyText = isDark ? '#E4E6EB' : '#050505';

  useEffect(() => {
    const checkStatus = async () => {
      try {
        const res = await axiosClient.get(`/friends/status/${targetUserId}`);
        setStatus(res.data.status);
        setActionUserId(res.data.actionUserId);
      } catch (err) { console.error(err); }
    };
    checkStatus();
  }, [targetUserId]);

  const handleAction = async (action: 'add' | 'accept' | 'remove') => {
    if (action === 'remove' && status === 'ACCEPTED') {
       if (!window.confirm("Bạn có chắc chắn muốn hủy kết bạn không?")) return;
    }
    try {
      if (action === 'add') {
        await axiosClient.post(`/friends/add/${targetUserId}`);
        setStatus('PENDING'); setActionUserId(currentUserId);
      } else if (action === 'accept') {
        await axiosClient.post(`/friends/accept/${targetUserId}`);
        setStatus('ACCEPTED');
      } else {
        await axiosClient.delete(`/friends/remove/${targetUserId}`);
        setStatus('NONE'); setActionUserId(null);
      }
    } catch(e) { console.error(e); }
  };

  const baseStyle: React.CSSProperties = { 
    padding: '8px 4px', 
    borderRadius: '6px', 
    border: 'none', 
    fontWeight: '600', 
    cursor: 'pointer', 
    fontSize: '13px', 
    transition: 'all 0.2s', 
    width: '100%', 
    height: '36px', 
    display: 'flex', 
    alignItems: 'center', 
    justifyContent: 'center', 
    gap: '4px'
  };
  
  if (status === 'ACCEPTED') {
    return (
      <button 
        onClick={() => handleAction('remove')}
        onMouseEnter={() => setIsHovering(true)}
        onMouseLeave={() => setIsHovering(false)}
        style={{
          ...baseStyle, 
          // 🟢 ĐÃ SỬA: Áp dụng bộ màu xám động
          background: isHovering ? (isDark ? '#4A1920' : '#ffebee') : greyBg, 
          color: isHovering ? (isDark ? '#EF5350' : '#d32f2f') : greyText
        }}
        className={className}
      >
        {isHovering ? 'Hủy' : '✔ Bạn bè'}
      </button>
    );
  }

  if (status === 'PENDING') {
    if (actionUserId === currentUserId) {
      return (
        <button onClick={() => handleAction('remove')} style={{...baseStyle, background: greyBg, color: greyText}} className={className}>
           Hủy lời mời
        </button>
      );
    } 
    return (
      <div style={{display:'flex', gap:'4px', width: '100%'}}>
        <button onClick={() => handleAction('accept')} style={{...baseStyle, background: theme.palette.primary.main, color: 'white', flex: 1}}>Nhận</button>
        <button onClick={() => handleAction('remove')} style={{...baseStyle, background: greyBg, color: greyText, flex: 1}}>Xóa</button>
      </div>
    );
  }

  return (
    <button onClick={() => handleAction('add')} style={{...baseStyle, background: isDark ? 'rgba(24, 119, 242, 0.2)' : '#e7f3ff', color: theme.palette.primary.main}} className={className}>
      + Thêm bạn
    </button>
  );
};
export default FriendButton;