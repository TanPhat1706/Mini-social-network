import React, { useEffect, useState } from 'react';
import axiosClient from '../../api/axiosClient';
import type { User } from '../../types';
import FriendButton from './FriendButton';

// 🔴 IMPORT MA THUẬT CSS VÀO ĐÂY
import AvatarWithFrame from '../AvatarWithFrame';
import ColoredName from '../ColoredName'; // (Sửa đường dẫn cho đúng từng file)
interface Props {
  currentUserId: number;
}

const SuggestedFriends: React.FC<Props> = ({ currentUserId }) => {
  const [users, setUsers] = useState<User[]>([]);

  // 🟢 STATE QUẢN LÝ PHÂN TRANG
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // 🟢 HÀM FETCH CÓ THAM SỐ PAGE
  const fetchSuggestions = (page: number) => {
    // Gọi API với size=5, page bắt đầu từ 0
    axiosClient.get(`/friends/suggested?page=${page}&size=5`)
      .then(res => {
        // API Spring Data phân trang sẽ trả về mảng dữ liệu trong res.data.content
        const data = res.data;

        // normalize dữ liệu
        const content =
          data?.content ||
          data?.data?.content ||
          (Array.isArray(data) ? data : []);

        setUsers(content);

        setTotalPages(
          data?.totalPages ||
          data?.data?.totalPages ||
          1
        );
        setCurrentPage(page);
      })
      .catch(err => console.error(err));
  };

  useEffect(() => {
    fetchSuggestions(0); // Load trang đầu tiên khi mount
  }, []);

  // 🟢 LOGIC NÚT ĐỔI GỢI Ý (Xoay vòng trang)
  const handleNextPage = () => {
    // Nếu trang tiếp theo vượt quá tổng số trang thì quay lại trang 0
    const nextPage = (currentPage + 1) % totalPages;
    fetchSuggestions(nextPage);
  };

  return (
    <div style={{ background: 'white', borderRadius: '8px', padding: '16px', boxShadow: '0 1px 2px rgba(0,0,0,0.1)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <h3 style={{ color: '#65676b', fontSize: '16px', fontWeight: 'bold', margin: 0 }}>Gợi ý kết bạn</h3>

        {/* NÚT ĐỔI GỢI Ý (Chỉ hiện khi có nhiều hơn 1 trang) */}
        {totalPages > 1 && (
          <button
            onClick={handleNextPage}
            style={{ background: 'none', border: 'none', color: '#1877F2', fontSize: '13px', fontWeight: '600', cursor: 'pointer', padding: 0 }}
          >
            Đổi gợi ý
          </button>
        )}
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {users.map(u => (
          <div key={u.id} style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            {/* VÙNG 1: AVATAR CÓ VIỀN & INFO */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flex: 1, minWidth: 0 }}>

              {/* 🔴 AVATAR CÓ VIỀN */}
              <div style={{ flexShrink: 0 }}>
                <AvatarWithFrame
                  src={u.avatarUrl || `https://ui-avatars.com/api/?name=${u.fullName}`}
                  frameClass={(u as any).currentAvatarFrame}
                  size={40}
                />
              </div>

              <div style={{ overflow: 'hidden' }}>
                <div style={{
                  fontWeight: '600',
                  fontSize: '14px',
                  color: '#050505',
                  whiteSpace: 'nowrap',
                  textOverflow: 'ellipsis',
                  overflow: 'hidden'
                }}>
                  <div style={{ fontWeight: '600', fontSize: '14px', color: '#050505', whiteSpace: 'nowrap', textOverflow: 'ellipsis', overflow: 'hidden' }}>
                    <ColoredName name={u.fullName} colorClass={(u as any).currentNameColor} />
                  </div>                </div>
                <div style={{ fontSize: '12px', color: '#65676b' }}>{u.studentCode}</div>
              </div>
            </div>

            {/* VÙNG 2: BUTTON */}
            <div style={{ flexShrink: 0, width: '120px' }}>
              <FriendButton targetUserId={u.id} currentUserId={currentUserId} />
            </div>
          </div>
        ))}
        {users.length === 0 && <div style={{ fontSize: '13px', color: '#888', textAlign: 'center' }}>Không có gợi ý mới.</div>}
      </div>
    </div>
  );
};

export default SuggestedFriends;