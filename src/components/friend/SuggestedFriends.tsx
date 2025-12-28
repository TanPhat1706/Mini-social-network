import React, { useEffect, useState } from 'react';
import axiosClient from '../../api/axiosClient';
import type { User } from '../../types';
import FriendButton from './FriendButton';

interface Props {
    currentUserId: number;
}

const SuggestedFriends: React.FC<Props> = ({ currentUserId }) => {
  const [users, setUsers] = useState<User[]>([]);

  useEffect(() => {
    // Axios tự thêm /auth -> Gọi /auth/friends/suggested
    axiosClient.get('/friends/suggested')
      .then(res => setUsers(res.data))
      .catch(err => console.error(err));
  }, []);

  return (
    <div style={{ background: 'white', borderRadius: '15px', padding: '15px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
      <h3 style={{ marginBottom: '15px', color: '#65676b', fontSize: '16px', fontWeight: 'bold' }}>Gợi ý kết bạn</h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
        {users.map(u => (
          <div key={u.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <img 
                src={u.avatarUrl || `https://ui-avatars.com/api/?name=${u.fullName}`} 
                style={{ width: '40px', height: '40px', borderRadius: '50%', objectFit: 'cover' }} 
                alt="ava"
              />
              <div style={{overflow: 'hidden'}}>
                <div style={{ fontWeight: 'bold', fontSize: '14px', whiteSpace:'nowrap', textOverflow:'ellipsis', maxWidth:'120px' }}>{u.fullName}</div>
                <div style={{ fontSize: '12px', color: '#65676b' }}>{u.studentCode}</div>
              </div>
            </div>
            <FriendButton targetUserId={u.id} currentUserId={currentUserId} />
          </div>
        ))}
        {users.length === 0 && <div style={{fontSize: '13px', color:'#888'}}>Không có gợi ý mới.</div>}
      </div>
    </div>
  );
};

export default SuggestedFriends;