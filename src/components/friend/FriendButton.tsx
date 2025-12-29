import React, { useState, useEffect } from 'react';
import axiosClient from '../../api/axiosClient';

interface Props { 
  targetUserId: number; 
  currentUserId: number; 
  className?: string; 
}

const FriendButton: React.FC<Props> = ({ targetUserId, currentUserId, className }) => {
  const [status, setStatus] = useState<string>('NONE');
  const [actionUserId, setActionUserId] = useState<number | null>(null);
  const [isHovering, setIsHovering] = useState(false);

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

  const baseStyle = { 
    padding: '8px 12px', borderRadius: '8px', border: 'none', 
    fontWeight: '600', cursor: 'pointer', fontSize: '13px', 
    transition: 'all 0.2s', 
    // ⭐️ CỐ ĐỊNH KÍCH THƯỚC: Thay width: '100%' bằng giá trị cụ thể
    width: '120px', 
    whiteSpace: 'nowrap' as const, // Ngăn chữ bị xuống dòng
    display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '5px'
  };
  
  if (status === 'ACCEPTED') {
    return (
      <button 
        onClick={() => handleAction('remove')}
        onMouseEnter={() => setIsHovering(true)}
        onMouseLeave={() => setIsHovering(false)}
        style={{
          ...baseStyle, 
          background: isHovering ? '#ffebee' : '#e4e6eb',
          color: isHovering ? '#d32f2f' : 'black'
        }}
        className={className}
      >
        {isHovering ? '❌ Hủy kết bạn' : '✔ Bạn bè'}
      </button>
    );
  }

  if (status === 'PENDING') {
    if (actionUserId === currentUserId) {
      return (
        <button onClick={() => handleAction('remove')} style={{...baseStyle, background: '#e4e6eb', color: '#65676b'}} className={className}>
           Hủy lời mời
        </button>
      );
    } 
    return (
      // ⭐️ BỎ width: '100%' ở div bao quanh để các nút giữ size cố định
      <div style={{display:'flex', gap:'8px'}}>
        <button onClick={() => handleAction('accept')} style={{...baseStyle, width: '100px', background: '#1877F2', color: 'white'}}>Chấp nhận</button>
        <button onClick={() => handleAction('remove')} style={{...baseStyle, width: '70px', background: '#e4e6eb', color: 'black'}}>Xóa</button>
      </div>
    );
  }

  return (
    <button onClick={() => handleAction('add')} style={{...baseStyle, background: '#e7f3ff', color: '#1877F2'}} className={className}>
      + Thêm bạn bè
    </button>
  );
};
export default FriendButton;