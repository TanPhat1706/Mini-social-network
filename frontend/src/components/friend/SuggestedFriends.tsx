import React, { useEffect, useState } from 'react';
import axiosClient from '../../api/axiosClient';
import type { User } from '../../types';
import FriendButton from './FriendButton';
import AvatarWithFrame from '../AvatarWithFrame';
import ColoredName from '../ColoredName'; 
import { useTheme } from '@mui/material/styles';
// 🟢 1. IMPORT HOOK ĐIỀU HƯỚNG
import { useProfileNavigation } from '../../utils/useProfileNavigation';

interface Props {
  currentUserId: number;
}

const SuggestedFriends: React.FC<Props> = ({ currentUserId }) => {
  const [users, setUsers] = useState<User[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  
  const theme = useTheme();
  
  // 🟢 2. KHỞI TẠO HOOK
  const navigateToProfile = useProfileNavigation();

  const fetchSuggestions = (page: number) => {
    axiosClient.get(`/friends/suggested?page=${page}&size=5`)
      .then(res => {
        const data = res.data;
        const content = data?.content || data?.data?.content || (Array.isArray(data) ? data : []);
        setUsers(content);
        setTotalPages(data?.totalPages || data?.data?.totalPages || 1);
        setCurrentPage(page);
      })
      .catch(err => console.error(err));
  };

  useEffect(() => { fetchSuggestions(0); }, []);

  const handleNextPage = () => {
    const nextPage = (currentPage + 1) % totalPages;
    fetchSuggestions(nextPage);
  };

  return (
    <div style={{ background: theme.palette.background.paper, borderRadius: '8px', padding: '16px', boxShadow: '0 1px 2px rgba(0,0,0,0.1)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <h3 style={{ color: theme.palette.text.secondary, fontSize: '16px', fontWeight: 'bold', margin: 0 }}>Gợi ý kết bạn</h3>
        {totalPages > 1 && (
          <button
            onClick={handleNextPage}
            style={{ background: 'none', border: 'none', color: theme.palette.primary.main, fontSize: '13px', fontWeight: '600', cursor: 'pointer', padding: 0 }}
          >
            Đổi gợi ý
          </button>
        )}
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {users.map(u => (
          <div key={u.id} style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            
            {/* 🟢 3. GẮN SỰ KIỆN CLICK VÀ CON TRỎ CHUỘT VÀO KHỐI THÔNG TIN NÀY */}
            <div 
              style={{ display: 'flex', alignItems: 'center', gap: '10px', flex: 1, minWidth: 0, cursor: 'pointer' }}
              onClick={() => navigateToProfile(u.studentCode)}
            >
              <div style={{ flexShrink: 0 }}>
                <AvatarWithFrame
                  src={u.avatarUrl || `https://ui-avatars.com/api/?name=${u.fullName}`}
                  frameClass={(u as any).currentAvatarFrame}
                  size={40}
                />
              </div>

              <div style={{ overflow: 'hidden' }}>
                <div style={{
                  fontWeight: '600', fontSize: '14px',
                  color: theme.palette.text.primary,
                  whiteSpace: 'nowrap', textOverflow: 'ellipsis', overflow: 'hidden'
                }}>
                  <ColoredName name={u.fullName} colorClass={(u as any).currentNameColor} />
                </div>
                <div style={{ fontSize: '12px', color: theme.palette.text.secondary }}>{u.studentCode}</div>
              </div>
            </div>

            {/* Khối nút kết bạn đứng độc lập, không bị ảnh hưởng bởi sự kiện onClick ở trên */}
            <div style={{ flexShrink: 0, width: '120px' }}>
              <FriendButton targetUserId={u.id} currentUserId={currentUserId} />
            </div>
          </div>
        ))}
        {users.length === 0 && <div style={{ fontSize: '13px', color: theme.palette.text.disabled, textAlign: 'center' }}>Không có gợi ý mới.</div>}
      </div>
    </div>
  );
};

export default SuggestedFriends;