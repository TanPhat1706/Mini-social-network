import React from 'react';
import type { User } from '../../types';
import axiosClient from '../../api/axiosClient';

// 🔴 IMPORT MA THUẬT CSS VÀO ĐÂY
import AvatarWithFrame from '../AvatarWithFrame';
import ColoredName from '../ColoredName'; // (Sửa đường dẫn cho đúng từng file)
interface Props {
  requests: User[];
  onRequestChanged: () => void; // Hàm callback để báo cho Header reload lại list
}

const FriendRequests: React.FC<Props> = ({ requests, onRequestChanged }) => {

  const confirmRequest = async (userId: number) => {
    try {
      await axiosClient.post(`/friends/accept/${userId}`);
      onRequestChanged();
    } catch (err) { console.error(err); }
  };

  const deleteRequest = async (userId: number) => {
    try {
      await axiosClient.delete(`/friends/remove/${userId}`);
      onRequestChanged();
    } catch (err) { console.error(err); }
  };

  if (requests.length === 0) {
    return <div style={{ padding: '10px', textAlign: 'center', color: '#65676b' }}>Không có thông báo mới.</div>;
  }

  return (
    <div style={{ padding: '8px' }}>
      <div style={{ fontSize: '16px', fontWeight: 'bold', padding: '0 8px 8px' }}>Lời mời kết bạn</div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
        {requests.map(u => (
          <div key={u.id} style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '8px', borderRadius: '8px', transition: '0.2s' }}>

            {/* 🔴 AVATAR CÓ VIỀN CHO NGƯỜI GỬI LỜI MỜI */}
            <div style={{ flexShrink: 0 }}>
              <AvatarWithFrame
                src={u.avatarUrl || `https://ui-avatars.com/api/?name=${u.fullName}`}
                frameClass={(u as any).currentAvatarFrame}
                size={48}
              />
            </div>

            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 'bold', fontSize: '14px', color: '#050505' }}>
                <ColoredName name={u.fullName} colorClass={(u as any).currentNameColor} />
              </div>              <div style={{ fontSize: '12px', color: '#65676b', marginBottom: '5px' }}>{u.studentCode}</div>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button
                  onClick={() => confirmRequest(u.id)}
                  style={{ background: '#1877F2', color: 'white', border: 'none', borderRadius: '6px', padding: '6px 12px', fontSize: '13px', cursor: 'pointer', fontWeight: 'bold', flex: 1 }}
                >
                  Chấp nhận
                </button>
                <button
                  onClick={() => deleteRequest(u.id)}
                  style={{ background: '#e4e6eb', color: 'black', border: 'none', borderRadius: '6px', padding: '6px 12px', fontSize: '13px', cursor: 'pointer', fontWeight: 'bold', flex: 1 }}
                >
                  Xóa
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default FriendRequests;