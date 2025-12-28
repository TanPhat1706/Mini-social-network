import React, { useState, useEffect } from 'react';
import axiosClient from '../../api/axiosClient';


interface Props { 
  targetUserId: number; 
  currentUserId: number; 
  className?: string; // Để có thể custom style từ bên ngoài nếu cần
}

const FriendButton: React.FC<Props> = ({ targetUserId, currentUserId, className }) => {
  const [status, setStatus] = useState<string>('NONE');
  const [actionUserId, setActionUserId] = useState<number | null>(null);
  const [isHovering, setIsHovering] = useState(false); // State để xử lý hover

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
    // Thêm confirm để tránh bấm nhầm khi hủy kết bạn
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
    transition: 'all 0.2s', width: '100%',
    display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '5px'
  };
  
  // 1. TRƯỜNG HỢP ĐÃ LÀ BẠN BÈ (Sửa lại logic hiển thị)
  if (status === 'ACCEPTED') {
    return (
      <button 
        onClick={() => handleAction('remove')}
        onMouseEnter={() => setIsHovering(true)}
        onMouseLeave={() => setIsHovering(false)}
        style={{
          ...baseStyle, 
          background: isHovering ? '#ffebee' : '#e4e6eb', // Hover nền đỏ nhạt
          color: isHovering ? '#d32f2f' : 'black'        // Hover chữ đỏ đậm
        }}
        className={className}
      >
        {isHovering ? '❌ Hủy kết bạn' : '✔ Bạn bè'}
      </button>
    );
  }

  // 2. TRƯỜNG HỢP ĐANG CHỜ (PENDING)
  if (status === 'PENDING') {
    if (actionUserId === currentUserId) {
      // Mình gửi đi -> Nút Hủy lời mời
      return (
        <button onClick={() => handleAction('remove')} style={{...baseStyle, background: '#e4e6eb', color: '#65676b'}} className={className}>
           Hủy lời mời
        </button>
      );
    } 
    // Người ta gửi đến -> Nút Chấp nhận
    return (
      <div style={{display:'flex', gap:'8px', width: '100%'}}>
        <button onClick={() => handleAction('accept')} style={{...baseStyle, background: '#1877F2', color: 'white', flex: 1}}>Chấp nhận</button>
        <button onClick={() => handleAction('remove')} style={{...baseStyle, background: '#e4e6eb', color: 'black', flex: 1}}>Xóa</button>
      </div>
    );
  }

  // 3. TRƯỜNG HỢP CHƯA KẾT BẠN
  return (
    <button onClick={() => handleAction('add')} style={{...baseStyle, background: '#e7f3ff', color: '#1877F2'}} className={className}>
      + Thêm bạn bè
    </button>
  );
};
export default FriendButton;