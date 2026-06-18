import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useParams } from 'react-router-dom';
import { CircularProgress, Box, Typography, Paper, Button, Grid } from '@mui/material';

// Import Context & API
import { useAuth } from '../context/AuthContext';
import api from '../api/api';
import type { User } from '../types';

// Import MẢNH GHÉP COMPONENTS
import PostCard from '../components/post/CardPost';
import type { PostData } from '../components/post/CardPost';
import AvatarWithFrame from '../components/AvatarWithFrame';
import ColoredName from '../components/ColoredName';
import ProfileHeader from '../components/profile/ProfileHeader';
import ProfileIntro from '../components/profile/ProfileIntro';
import { useProfileNavigation } from '../utils/useProfileNavigation'; 

// 🟢 IMPORT HÀM TÍNH THỜI GIAN
import { differenceInMinutes, differenceInHours, differenceInDays } from 'date-fns';

interface ProfileApiResponse {
  userId: number;
  username: string;
  fullName: string;
  email: string;
  avatarUrl?: string;
  coverPhotoUrl?: string;
  bio?: string;
  className?: string;
  role?: string;
  active?: boolean;
  createdAt?: string;
  lastLogin?: string;
  joinedAt?: string;
  relationshipStatus?: string;
  blocked?: boolean;
  isSelfProfile?: boolean;
  currentAvatarFrame?: string | null;
  currentNameColor?: string | null;
}

interface ProfileUser extends User {
  isSelfProfile?: boolean;
  relationshipStatus?: string;
  blocked?: boolean;
}

// 🟢 INTERFACE TRẠNG THÁI & HÀM FORMAT
interface UserPresence {
  online: boolean;
  lastSeen?: string;
}

const formatShortTime = (lastSeen?: string) => {
  if (!lastSeen) return '';
  const lastDate = new Date(lastSeen);
  const mins = differenceInMinutes(new Date(), lastDate);

  if (mins < 60) return `${mins <= 0 ? 1 : mins}p`;

  const hours = differenceInHours(new Date(), lastDate);
  if (hours < 24) return `${hours}g`;

  const days = differenceInDays(new Date(), lastDate);
  if (days < 7) return `${days}n`;

  return ''; 
};

const ProfilePage: React.FC = () => {
  const { studentCode } = useParams<{ studentCode?: string }>();
  const { user: currentUser, updateUser } = useAuth();
  const navigateToProfile = useProfileNavigation();

  const [profileUser, setProfileUser] = useState<ProfileUser | null>(null);
  const [friends, setFriends] = useState<User[]>([]);
  const [posts, setPosts] = useState<PostData[]>([]);
  const [loadingPosts, setLoadingPosts] = useState(false);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [isBlocked, setIsBlocked] = useState(false);

  // 🟢 STATE QUẢN LÝ TAB & PRESENCES (TRẠNG THÁI HOẠT ĐỘNG)
  const [activeTab, setActiveTab] = useState(0);
  const [presences, setPresences] = useState<Record<string, UserPresence>>({});

  const profileCode = studentCode || currentUser?.studentCode;

  // 1. FETCH PROFILE INFO
  useEffect(() => {
    if (!profileCode) return;
    const fetchProfileData = async () => {
      setLoading(true); setNotFound(false); setIsBlocked(false);
      try {
        const userRes = await api.get(`/api/users/${profileCode}/profile`);
        const data = userRes.data as ProfileApiResponse;
        setProfileUser({
          id: data.userId,
          studentCode: data.username,
          email: data.email,
          fullName: data.fullName,
          className: data.className ?? '',
          role: data.role ?? '',
          avatarUrl: data.avatarUrl,
          coverPhotoUrl: data.coverPhotoUrl,
          bio: data.bio,
          active: data.active ?? true,
          currentAvatarFrame: data.currentAvatarFrame ?? null,
          currentNameColor: data.currentNameColor ?? null,
          vptlPoints: undefined,
          createdAt: data.createdAt ?? data.joinedAt ?? '',
          lastLogin: data.lastLogin ?? '',
          isSelfProfile: data.isSelfProfile,
          relationshipStatus: data.relationshipStatus,
          blocked: data.blocked
        });
        setIsBlocked(data.relationshipStatus === 'BLOCKED' || data.blocked === true);
      } catch (error) {
        if (axios.isAxiosError(error) && error.response?.status === 404) setNotFound(true);
      } finally {
        setLoading(false);
      }
    };
    fetchProfileData();
  }, [profileCode]);

  // 2. FETCH POSTS & FRIENDS
  useEffect(() => {
    if (!profileUser || isBlocked || !profileCode) return;
    const fetchPostsAndFriends = async () => {
      try {
        setLoadingPosts(true);
        const [postsRes, friendsRes] = await Promise.all([
          api.get(`/api/users/${profileCode}/posts`),
          api.get(`/api/users/${profileCode}/friends`)
        ]);
        setPosts(postsRes.data.content || []);
        setFriends(friendsRes.data.content || []);
      } catch (error) {
        console.error('Lỗi tải dữ liệu profile:', error);
      } finally {
        setLoadingPosts(false);
      }
    };
    fetchPostsAndFriends();
  }, [profileUser, profileCode, isBlocked]);

  // 🟢 3. EFFECT POLLING: CHỈ LẤY TRẠNG THÁI NẾU LÀ TRANG CỦA CHÍNH MÌNH (QUYỀN RIÊNG TƯ)
  useEffect(() => {
    if (!profileUser?.isSelfProfile || friends.length === 0) return;

    const fetchAllPresences = async () => {
      try {
        const promises = friends.map(friend => api.get(`/api/users/${friend.studentCode}/presence`));
        const results = await Promise.allSettled(promises);

        const newPresences: Record<string, UserPresence> = {};
        results.forEach((result, index) => {
          if (result.status === 'fulfilled') {
            newPresences[friends[index].studentCode] = result.value.data;
          }
        });
        setPresences(newPresences);
      } catch (error) {
        console.error("Lỗi tải trạng thái bạn bè:", error);
      }
    };

    fetchAllPresences();
    const interval = setInterval(fetchAllPresences, 60000);

    return () => clearInterval(interval);
  }, [friends, profileUser?.isSelfProfile]);


  const handleUpdateProfile = (updatedUser: User) => {
    setProfileUser(prev => prev ? { ...prev, ...updatedUser } : null);

    if (profileUser?.isSelfProfile) {
      updateUser({
        id: updatedUser.id,
        studentCode: updatedUser.studentCode,
        email: updatedUser.email,
        fullName: updatedUser.fullName,
        className: updatedUser.className,
        role: updatedUser.role,
        avatarUrl: updatedUser.avatarUrl,
        coverPhotoUrl: updatedUser.coverPhotoUrl,
        bio: updatedUser.bio,
        active: updatedUser.active,
        createdAt: updatedUser.createdAt,
        lastLogin: updatedUser.lastLogin,
        vptlPoints: updatedUser.vptlPoints,
        currentAvatarFrame: updatedUser.currentAvatarFrame ?? null,
        currentNameColor: updatedUser.currentNameColor ?? null,
      });
    }
  };

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue);
  };

  // 🟢 HÀM RENDER HUY HIỆU (Tái sử dụng cho cả 2 chỗ)
  const renderPresenceBadge = (studentCode: string, avatarSize: number) => {
    // CHỐT CHẶN BẢO MẬT: Nếu không phải trang của mình thì return null (không render gì cả)
    if (!profileUser?.isSelfProfile) return null; 

    const presence = presences[studentCode];
    if (!presence) return null;

    const isOnline = presence.online;
    const offlineText = !isOnline ? formatShortTime(presence.lastSeen) : '';
    const isRecentOffline = offlineText.endsWith('p');

    // Căn chỉnh linh hoạt: Avatar to (80) thì nhích vào trong, Avatar nhỏ (60) thì thụt ra ngoài để không lẹm mặt
    const bottomPos = avatarSize >= 80 ? 4 : 0;
    const rightPos = avatarSize >= 80 ? 4 : -4;

    if (isOnline) {
      return (
        <Box sx={{
          position: 'absolute', bottom: bottomPos, right: bottomPos, 
          width: avatarSize >= 80 ? 16 : 14, height: avatarSize >= 80 ? 16 : 14,
          backgroundColor: '#31a24c', borderRadius: '50%',
          border: '2px solid', borderColor: 'background.paper', zIndex: 10
        }} />
      );
    } else if (offlineText) {
      return (
        <Box sx={{
          position: 'absolute', bottom: bottomPos - 2, right: rightPos,
          backgroundColor: isRecentOffline ? '#e7f3ff' : 'action.selected',
          color: isRecentOffline ? '#1877f2' : 'text.secondary',
          borderRadius: '10px',
          border: '2px solid', borderColor: 'background.paper',
          px: 0.8, minWidth: '20px', height: '18px',
          fontSize: '10px', fontWeight: 800, zIndex: 10,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
        }}>
          {offlineText}
        </Box>
      );
    }
    return null;
  };

  if (loading && !profileUser) return <Box sx={{ display: 'flex', justifyContent: 'center', mt: 10 }}><CircularProgress /></Box>;
  if (notFound) return <Box sx={{ display: 'flex', justifyContent: 'center', mt: 10 }}><Typography variant="h5">Người dùng không tồn tại</Typography></Box>;
  if (!profileUser) return null;

  return (
    <Box sx={{ bgcolor: 'background.default', minHeight: '100vh', pb: 5 }}>

      {/* HEADER */}
      <ProfileHeader
        profileUser={profileUser}
        isSelfProfile={Boolean(profileUser.isSelfProfile)}
        friendCount={friends.length}
        onUpdateProfile={handleUpdateProfile}
        activeTab={activeTab} 
        onTabChange={handleTabChange} 
      />

      <Box sx={{ maxWidth: 1064, mx: 'auto', px: { xs: 2, md: 0 }, mt: 3 }}>
        
        {/* TAB BẠN BÈ (2) */}
        {activeTab === 2 ? (
          <Paper elevation={1} sx={{ p: 3, borderRadius: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
              <Typography variant="h5" fontWeight="bold">Bạn bè</Typography>
            </Box>
            
            {friends.length > 0 ? (
              <Grid container spacing={2}>
                {friends.map((friend) => (
                  <Grid size={{ xs: 12, sm: 6, md: 4 }} key={friend.id}>
                    <Box 
                      sx={{ 
                        display: 'flex', alignItems: 'center', p: 2, 
                        border: '1px solid', borderColor: 'divider', borderRadius: 2,
                        cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' }
                      }}
                      onClick={() => navigateToProfile(friend.studentCode)}
                    >
                      {/* 🟢 RENDER AVATAR KÈM HUY HIỆU (SIZE 60) */}
                      <Box sx={{ position: 'relative', display: 'inline-block' }}>
                        <AvatarWithFrame src={friend.avatarUrl} name={friend.fullName} frameClass={(friend as any).currentAvatarFrame} size={60} />
                        {renderPresenceBadge(friend.studentCode, 60)}
                      </Box>

                      <Box sx={{ ml: 2, overflow: 'hidden' }}>
                        <Typography variant="subtitle1" fontWeight="bold" noWrap>
                          <ColoredName name={friend.fullName} colorClass={(friend as any).currentNameColor} />
                        </Typography>
                        <Typography variant="body2" color="text.secondary" noWrap>{friend.studentCode}</Typography>
                      </Box>
                    </Box>
                  </Grid>
                ))}
              </Grid>
            ) : (
              <Typography color="text.secondary" align="center" sx={{ py: 4 }}>Chưa có bạn bè nào.</Typography>
            )}
          </Paper>
        ) : (
          /* TAB BÀI VIẾT (0) & GIỚI THIỆU (1) */
          <Box sx={{ display: 'flex', flexDirection: { xs: 'column', md: 'row' }, gap: 3, alignItems: 'flex-start' }}>
            
            {/* CỘT TRÁI */}
            <Box sx={{ width: { xs: '100%', md: '360px' }, flexShrink: 0, display: 'flex', flexDirection: 'column', gap: 3 }}>
              <ProfileIntro user={profileUser} isSelfProfile={Boolean(profileUser.isSelfProfile)} onEditClick={() => {}} />

              {/* Box bạn bè nhỏ bên trái (chỉ hiện khi ở tab Bài viết - index 0) */}
              {activeTab === 0 && (
                <Paper elevation={1} sx={{ p: 3, borderRadius: 2 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                    <Typography variant="h6" fontWeight="bold">Bạn bè</Typography>
                    <Typography variant="body2" color="text.secondary">{friends.length} người bạn</Typography>
                  </Box>

                  {friends.length > 0 ? (
                    <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 2 }}>
                      {friends.slice(0, 9).map(f => (
                        <Box key={f.id} sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', cursor: 'pointer', '&:hover': { transform: 'scale(1.05)' }, transition: 'transform 0.2s' }} onClick={() => navigateToProfile(f.studentCode)}>
                          
                          {/* 🟢 RENDER AVATAR KÈM HUY HIỆU (SIZE 80) */}
                          <Box sx={{ position: 'relative', display: 'inline-block' }}>
                            <AvatarWithFrame src={f.avatarUrl} name={f.fullName} frameClass={(f as any).currentAvatarFrame} size={80} />
                            {renderPresenceBadge(f.studentCode, 80)}
                          </Box>

                          <Typography variant="caption" sx={{ mt: 1, fontWeight: 'bold', textAlign: 'center', width: '100%', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                            <ColoredName name={f.fullName} colorClass={(f as any).currentNameColor} />
                          </Typography>
                        </Box>
                      ))}
                    </Box>
                  ) : (
                    <Typography variant="body2" color="text.secondary" fontStyle="italic">Chưa có bạn bè nào.</Typography>
                  )}
                  <Button fullWidth variant="text" onClick={() => setActiveTab(2)} sx={{ mt: 2, bgcolor: 'action.hover', color: 'text.primary', fontWeight: 'bold' }}>
                    Xem tất cả bạn bè
                  </Button>
                </Paper>
              )}
            </Box>

            {/* CỘT PHẢI */}
            {activeTab === 0 && (
               <Box sx={{ flexGrow: 1, width: '100%', maxWidth: '680px' }}>
                 <Paper elevation={1} sx={{ p: 2, mb: 3, borderRadius: 2 }}>
                   <Typography variant="h6" fontWeight="bold">Bài viết</Typography>
                 </Paper>

                 {loadingPosts ? (
                   <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}><CircularProgress /></Box>
                 ) : isBlocked ? (
                   <Paper sx={{ p: 4, textAlign: 'center', borderRadius: 2 }}>
                     <Typography variant="h6" fontWeight="bold">Nội dung đang bị giới hạn</Typography>
                   </Paper>
                 ) : posts.length > 0 ? (
                   posts.map(post => <PostCard key={post.id} post={post} onDeleteSuccess={(id) => setPosts(posts.filter(p => p.id !== id))} />)
                 ) : (
                   <Paper sx={{ p: 4, textAlign: 'center', borderRadius: 2 }}>
                     <Typography color="text.secondary">Chưa có bài viết nào.</Typography>
                   </Paper>
                 )}
               </Box>
            )}
            
            {activeTab === 1 && (
               <Box sx={{ flexGrow: 1, width: '100%', maxWidth: '680px' }}>
                  <Paper elevation={1} sx={{ p: 4, textAlign: 'center', borderRadius: 2 }}>
                     <Typography color="text.secondary">Đang cập nhật phần Giới thiệu chi tiết...</Typography>
                  </Paper>
               </Box>
            )}

          </Box>
        )}
      </Box>
    </Box>
  );
};

export default ProfilePage;