import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useParams } from 'react-router-dom';
import { CircularProgress, Box, Typography, Paper, Button } from '@mui/material';

// Import Context & API
import { useAuth } from '../context/AuthContext';
import api from '../api/api';
import type { User } from '../types';

// Import MẢNH GHÉP COMPONENTS
import PostCard from '../components/post/CardPost';
import type { PostData } from '../components/post/CardPost';
import AvatarWithFrame from '../components/AvatarWithFrame';
import { useProfileNavigation } from '../hooks/useProfileNavigation';
import ColoredName from '../components/ColoredName';
import ProfileHeader from '../components/profile/ProfileHeader';
import ProfileIntro from '../components/profile/ProfileIntro';

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

const ProfilePage: React.FC = () => {
  const { studentCode } = useParams<{ studentCode?: string }>();
  const { user: currentUser, updateUser } = useAuth();

  const [profileUser, setProfileUser] = useState<ProfileUser | null>(null);
  const [friends, setFriends] = useState<User[]>([]);
  const [posts, setPosts] = useState<PostData[]>([]);
  const [loadingPosts, setLoadingPosts] = useState(false);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [isBlocked, setIsBlocked] = useState(false);

  const profileCode = studentCode || currentUser?.studentCode;

  const navigateToProfile = useProfileNavigation();

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

  if (loading && !profileUser) return <Box sx={{ display: 'flex', justifyContent: 'center', mt: 10 }}><CircularProgress /></Box>;
  if (notFound) return <Box sx={{ display: 'flex', justifyContent: 'center', mt: 10 }}><Typography variant="h5">Người dùng không tồn tại</Typography></Box>;
  if (!profileUser) return null;

  return (
    <Box sx={{ bgcolor: 'background.default', minHeight: '100vh', pb: 5 }}>

      {/* 🔴 HEADER CHUẨN FACEBOOK */}
      <ProfileHeader
        profileUser={profileUser}
        isSelfProfile={Boolean(profileUser.isSelfProfile)}
        friendCount={friends.length}
        onUpdateProfile={handleUpdateProfile}
      />

      {/* 🔴 BỘ KHUNG FLEXBOX - CỞI TRÓI KHUNG BÀI VIẾT */}
      <Box sx={{
        maxWidth: 1064,
        mx: 'auto',
        px: { xs: 2, md: 0 },
        display: 'flex',
        flexDirection: { xs: 'column', md: 'row' },
        gap: 3,
        alignItems: 'flex-start',
        mt: 3
      }}>

        {/* CỘT TRÁI: INTRO & FRIENDS (360px cố định) */}
        <Box sx={{ width: { xs: '100%', md: '360px' }, flexShrink: 0, display: 'flex', flexDirection: 'column', gap: 3 }}>
          <ProfileIntro
            user={profileUser}
            isSelfProfile={Boolean(profileUser.isSelfProfile)}
            onEditClick={() => { /* Xử lý ở Header */ }}
          />

          <Paper elevation={1} sx={{ p: 3, borderRadius: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6" fontWeight="bold">Bạn bè</Typography>
              <Typography variant="body2" color="text.secondary">{friends.length} người bạn</Typography>
            </Box>

            {friends.length > 0 ? (
              <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 2 }}>
                {friends.slice(0, 9).map(f => (
                  <Box key={f.id} sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                    <AvatarWithFrame src={f.avatarUrl} name={f.fullName} frameClass={(f as any).currentAvatarFrame} size={80} onClick={(e) => { e.stopPropagation(); navigateToProfile(f.studentCode); }} />
                    <Typography variant="caption" sx={{ mt: 1, fontWeight: 'bold', textAlign: 'center', width: '100%', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      <ColoredName name={f.fullName} colorClass={(f as any).currentNameColor} studentCode={f.studentCode} />
                    </Typography>
                  </Box>
                ))}
              </Box>
            ) : (
              <Typography variant="body2" color="text.secondary" fontStyle="italic">Chưa có bạn bè nào.</Typography>
            )}
            <Button fullWidth variant="text" sx={{ mt: 2, bgcolor: 'action.hover', color: 'text.primary', fontWeight: 'bold' }}>
              Xem tất cả bạn bè
            </Button>
          </Paper>
        </Box>

        {/* CỘT PHẢI: BÀI VIẾT (Tự động giãn ra tối đa 680px) */}
        <Box sx={{
          flexGrow: 1,
          width: '100%',
          maxWidth: '680px'
        }}>
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
      </Box>
    </Box>
  );
};

export default ProfilePage;
