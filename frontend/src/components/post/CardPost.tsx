import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Card, CardHeader, CardContent, CardActions,
  IconButton, Typography, Box, Divider, Button, Link,
  Menu, MenuItem, ListItemIcon,
  Collapse, Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  Tooltip
} from '@mui/material';
import { showSuccess, showError, showDevelopmentAlert } from '../../utils/swal';
import { Link as RouterLink } from 'react-router-dom';

// Import Icons
import MoreVertIcon from '@mui/icons-material/MoreVert';
import ThumbUpOutlinedIcon from '@mui/icons-material/ThumbUpOutlined';
import ChatBubbleOutlineOutlinedIcon from '@mui/icons-material/ChatBubbleOutlineOutlined';
import ShareOutlinedIcon from '@mui/icons-material/ShareOutlined';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import PublicIcon from '@mui/icons-material/Public';
import LockIcon from '@mui/icons-material/Lock';
import PendingIcon from '@mui/icons-material/Pending';
import GroupIcon from '@mui/icons-material/Group';
import icons from 'react-reactions/src/helpers/icons'; // 🔴 IMPORT BỘ ICON FACEBOOK

// Import API & Components
import api from '../../api/api';
import EditPost from './EditPost';
import PostMediaGrid from './PostMediaGrid';
import CommentSection from '../comment/CommentSection';

import AvatarWithFrame from '../AvatarWithFrame';
import ColoredName from '../ColoredName';
import ReactionListDialog from './ReactionListDialog';
import ReactionTooltip from './ReactionTooltip';

// 🟢 IMPORT CONTEXT
import { useAuth } from '../../context/AuthContext';

// --- Types ---
export interface PostMedia {
  id: number;
  url: string;
  type: string;
}

export interface PostAuthor {
  id: number;
  studentCode: string;
  username: string;
  fullName: string;
  avatarUrl: string;
  currentAvatarFrame?: string;
  currentNameColor?: string;
}

export type ReactionType = 'LIKE' | 'LOVE' | 'HAHA' | 'WOW' | 'SAD' | 'ANGRY';

// 🔴 ĐỔI TỪ EMOJI SANG ICON KEY
const REACTION_CONFIG: Record<ReactionType, { iconKey: string; label: string; color: string }> = {
  LIKE: { iconKey: 'like', label: 'Thích', color: 'primary.main' },
  LOVE: { iconKey: 'love', label: 'Yêu thích', color: 'error.main' },
  HAHA: { iconKey: 'haha', label: 'Haha', color: 'warning.dark' },
  WOW: { iconKey: 'wow', label: 'Wow', color: 'warning.main' },
  SAD: { iconKey: 'sad', label: 'Buồn', color: 'info.main' },
  ANGRY: { iconKey: 'angry', label: 'Giận', color: 'error.dark' }
};

const getReactionImg = (type: ReactionType) => icons.find('facebook', REACTION_CONFIG[type].iconKey);

const REACTION_ORDER: ReactionType[] = ['LIKE', 'LOVE', 'HAHA', 'WOW', 'SAD', 'ANGRY'];

const formatCount = (count: number) => {
  if (count < 1000) return String(count);
  if (count < 1000000) return `${(count / 1000).toFixed(count % 1000 === 0 ? 0 : 1)}K`;
  return `${(count / 1000000).toFixed(count % 1000000 === 0 ? 0 : 1)}M`;
};

export interface PostData {
  id: number;
  content: string;
  createdAt: string;
  author: PostAuthor;
  media: PostMedia[];
  likeCount: number;
  commentCount: number;
  shareCount: number;
  originalPost?: PostData;
  likedByCurrentUser: boolean;
  visibility?: string;
  selfPost?: boolean;
  reactionCounts?: Record<string, number>;
  currentUserReaction?: string | null;
}

interface PostCardProps {
  post: PostData;
  onDeleteSuccess: (id: number) => void;
}

const formatDate = (dateString: string) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return new Intl.DateTimeFormat('vi-VN', {
    day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit'
  }).format(date);
};

const renderVisibilityIcon = (visibility?: string) => {
  const iconStyle = { fontSize: 14, ml: 0.7, color: 'text.secondary' };
  switch (visibility) {
    case 'PUBLIC':
      return <Tooltip title="Công khai"><PublicIcon sx={iconStyle} /></Tooltip>;
    case 'PRIVATE':
      return <Tooltip title="Chỉ mình tôi"><LockIcon sx={iconStyle} /></Tooltip>;
    case 'PENDING':
      return <Tooltip title="Đang chờ duyệt"><PendingIcon sx={{ ...iconStyle, color: 'warning.main' }} /></Tooltip>;
    case 'CLASS':
      return <Tooltip title="Lớp học"><GroupIcon sx={iconStyle} /></Tooltip>;
    default:
      return <Tooltip title="Công khai"><PublicIcon sx={iconStyle} /></Tooltip>;
  }
}

const SharedPostContent = ({ originalPost }: { originalPost: PostData }) => {
  if (!originalPost) {
    return (
      <Box sx={{ p: 2, bgcolor: 'background.default', border: 1, borderColor: 'divider', borderRadius: 2 }}>
        <Typography variant="body2" color="text.secondary" fontStyle="italic">
          Bài viết này không còn khả dụng.
        </Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ mt: 2, border: 1, borderColor: 'divider', borderRadius: 2, overflow: 'hidden' }}>
      <Box sx={{ p: 1.5, display: 'flex', alignItems: 'center', bgcolor: 'background.default', borderBottom: 1, borderColor: 'divider' }}>
        <Box sx={{ mr: 1.5 }}>
          <Link component={RouterLink} to={`/profile/${originalPost.author.studentCode}`} underline="none" sx={{ display: 'inline-flex' }}>
            <AvatarWithFrame
              src={originalPost.author.avatarUrl}
              name={originalPost.author.fullName}
              frameClass={originalPost.author.currentAvatarFrame}
              size={32}
              className="hoverable-avatar"
            />
          </Link>
        </Box>
        <Box>
          <Link component={RouterLink} to={`/profile/${originalPost.author.studentCode}`} underline="hover" color="text.primary">
            <Typography variant="subtitle2" fontWeight="bold">
              <ColoredName
                name={originalPost.author.fullName}
                colorClass={originalPost.author.currentNameColor}
              />
            </Typography>
          </Link>
          <Link component={RouterLink} to={`/posts/${originalPost.id}`} underline="hover" color="text.secondary">
            <Typography variant="caption">
              {formatDate(originalPost.createdAt)}
            </Typography>
          </Link>
        </Box>
      </Box>

      <Box sx={{ px: 2, py: 1 }}>
        <Typography variant="body2" style={{ whiteSpace: 'pre-line' }}>{originalPost.content}</Typography>
      </Box>

      {originalPost.media && originalPost.media.length > 0 && (
        <PostMediaGrid media={originalPost.media} />
      )}
    </Box>
  );
};

export default function PostCard({ post: initialPost, onDeleteSuccess }: PostCardProps) {
  const { user } = useAuth();
  const [post, setPost] = useState<PostData>(initialPost);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [shareOpen, setShareOpen] = useState(false);
  const [shareCaption, setShareCaption] = useState('');
  const [shareLoading, setShareLoading] = useState(false);
  const [showComments, setShowComments] = useState(false);
  const [showReactionPopup, setShowReactionPopup] = useState(false);
  const [isReacting, setIsReacting] = useState(false);
  const [showReactionList, setShowReactionList] = useState(false);

  const reactionOpenTimeoutRef = useRef<number | null>(null);
  const reactionCloseTimeoutRef = useRef<number | null>(null);
  const touchHoldTimeoutRef = useRef<number | null>(null);
  const skipClickRef = useRef(false);

  const isMenuOpen = Boolean(anchorEl);

  const handleMenuClick = (event: React.MouseEvent<HTMLElement>) => setAnchorEl(event.currentTarget);
  const handleMenuClose = () => setAnchorEl(null);

  const handleEditClick = () => {
    setIsEditDialogOpen(true);
    handleMenuClose();
  };

  const clearReactionOpenTimer = () => {
    if (reactionOpenTimeoutRef.current) {
      window.clearTimeout(reactionOpenTimeoutRef.current);
      reactionOpenTimeoutRef.current = null;
    }
  };

  const clearReactionCloseTimer = () => {
    if (reactionCloseTimeoutRef.current) {
      window.clearTimeout(reactionCloseTimeoutRef.current);
      reactionCloseTimeoutRef.current = null;
    }
  };

  const clearTouchHoldTimer = () => {
    if (touchHoldTimeoutRef.current) {
      window.clearTimeout(touchHoldTimeoutRef.current);
      touchHoldTimeoutRef.current = null;
    }
  };

  const closeReactionPopup = () => {
    clearReactionOpenTimer();
    clearReactionCloseTimer();
    setShowReactionPopup(false);
  };

  const openReactionPopup = () => {
    clearReactionCloseTimer();
    if (!showReactionPopup) {
      setShowReactionPopup(true);
    }
  };

  const normalizeReactionType = (type?: string | null): ReactionType | undefined => {
    if (!type) return undefined;
    return REACTION_ORDER.includes(type as ReactionType) ? (type as ReactionType) : undefined;
  };

  const updateReactionCounts = (
    counts: Record<string, number>,
    currentReaction: ReactionType | null,
    nextReaction: ReactionType | null
  ) => {
    const nextCounts = { ...counts };

    if (currentReaction) {
      nextCounts[currentReaction] = Math.max((nextCounts[currentReaction] || 1) - 1, 0);
      if (nextCounts[currentReaction] === 0) {
        delete nextCounts[currentReaction];
      }
    }

    if (nextReaction) {
      nextCounts[nextReaction] = (nextCounts[nextReaction] || 0) + 1;
    }

    return nextCounts;
  };

  const submitReaction = async (requestedReaction: ReactionType, nextReaction: ReactionType | null) => {
    if (isReacting) return;

    const previousPostState = { ...post, reactionCounts: { ...(post.reactionCounts || {}) } };
    const currentReaction = normalizeReactionType(post.currentUserReaction || null) || null;
    const nextCounts = updateReactionCounts(post.reactionCounts || {}, currentReaction, nextReaction);

    setPost(prev => ({
      ...prev,
      reactionCounts: nextCounts,
      currentUserReaction: nextReaction
    }));
    setIsReacting(true);

    try {
      await api.post(`/api/posts/${post.id}/react`, { reactionType: requestedReaction });
    } catch (error) {
      console.error('Lỗi reaction:', error);
      setPost(previousPostState);
    } finally {
      setIsReacting(false);
    }
  };

  const handleReactionButtonClick = () => {
    const currentReaction = normalizeReactionType(post.currentUserReaction || null);
    if (currentReaction) {
      submitReaction(currentReaction, null);
    } else {
      submitReaction('LIKE', 'LIKE');
    }
  };

  const handleSelectReaction = (reactionType: ReactionType) => {
    const currentReaction = normalizeReactionType(post.currentUserReaction || null);
    const nextReaction = currentReaction === reactionType ? null : reactionType;

    submitReaction(reactionType, nextReaction);
    closeReactionPopup();
    skipClickRef.current = true;
  };

  const handleReactionMouseEnter = () => {
    clearReactionCloseTimer();
    if (showReactionPopup) return;
    reactionOpenTimeoutRef.current = window.setTimeout(() => { openReactionPopup(); }, 400);
  };

  const handleReactionMouseLeave = () => {
    clearReactionOpenTimer();
    reactionCloseTimeoutRef.current = window.setTimeout(() => { closeReactionPopup(); }, 200);
  };

  const handleReactionTouchStart = () => {
    clearReactionCloseTimer();
    touchHoldTimeoutRef.current = window.setTimeout(() => {
      openReactionPopup();
      skipClickRef.current = true;
    }, 400);
  };

  const handleReactionTouchEnd = () => {
    if (touchHoldTimeoutRef.current) {
      clearTouchHoldTimer();
      return;
    }
  };

  const reactionCounts = post.reactionCounts || {};
  const reactionEntries = Object.entries(reactionCounts)
    .filter(([, count]) => count > 0)
    .sort((a, b) => b[1] - a[1]);

  const sortedReactions = useMemo(() => reactionEntries, [reactionEntries]);
  const totalReactions = sortedReactions.reduce((sum, [, count]) => sum + count, 0);
  const topReactions = sortedReactions.slice(0, 3) as [ReactionType, number][];
  const activeReactionType = normalizeReactionType(post.currentUserReaction || null);
  const activeReaction = activeReactionType ? REACTION_CONFIG[activeReactionType] : undefined;

  const handleDeleteClick = async () => {
    if (window.confirm("Bạn có chắc chắn muốn xóa bài viết này không? Hành động này không thể hoàn tác.")) {
      try {
        const response = await api.delete(`/api/posts/${post.id}`);
        if (response.status === 204 || response.status === 200) {
          onDeleteSuccess(post.id);
        }
      } catch (error: any) {
        console.error("Error deleting post:", error);
        showError(error?.response?.data?.message || "Lỗi mạng hoặc máy chủ khi xóa bài viết!");
      }
    }
    handleMenuClose();
  };

  const handleUpdateSuccess = (updatedPost: PostData) => setPost(updatedPost);
  const handleCommentClick = () => setShowComments(!showComments);

  const handleShareClick = () => {
    setShareCaption('');
    setShareOpen(true);
  };

  const handleShareSubmit = async () => {
    const caption = shareCaption.trim();
    if (!caption) return;
    setShareLoading(true);
    try {
      await api.post(`/api/posts/${post.id}/share`, { content: caption });
      setShareOpen(false);
      setShareCaption('');
      showSuccess('Chia sẻ thành công!');
      setPost(prev => ({ ...prev, shareCount: (prev.shareCount || 0) + 1 }));
    } catch (error) {
      console.error('Lỗi share:', error);
      showError('Không thể chia sẻ bài viết này.');
    } finally {
      setShareLoading(false);
    }
  };

  useEffect(() => {
    return () => {
      clearReactionOpenTimer();
      clearReactionCloseTimer();
      clearTouchHoldTimer();
    };
  }, []);

  useEffect(() => { setPost(initialPost); }, [initialPost]);

  return (
    <>
      <Card
        data-testid="post-card"
        data-self-post={post.selfPost ? 'true' : 'false'}
        data-author-code={post.author.studentCode}
        sx={{ maxWidth: '100%', margin: 'auto', mb: 3, boxShadow: 3, borderRadius: 2 }}
      >
        {/* HEADER */}
        <CardHeader
          avatar={
            <Link component={RouterLink} to={`/profile/${post.author.studentCode}`} style={{ textDecoration: 'none' }}>
              <AvatarWithFrame src={post.author.avatarUrl} name={post.author.fullName} frameClass={post.author.currentAvatarFrame} size={42} className="hoverable-avatar" />
            </Link>
          }
          action={
            <IconButton aria-label="settings" onClick={handleMenuClick}><MoreVertIcon /></IconButton>
          }
          title={
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <Link component={RouterLink} to={`/profile/${post.author.studentCode}`} variant="h6" sx={{ fontWeight: 'bold', textDecoration: 'none', color: 'text.primary', '&:hover': { textDecoration: 'underline' } }}>
                <ColoredName name={post.author.fullName} colorClass={post.author.currentNameColor} />
              </Link>
              {post.originalPost && <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 'normal' }}>đã chia sẻ một bài viết</Typography>}
            </Box>
          }
          subheader={
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              {renderVisibilityIcon(post.visibility)}
              <span style={{ margin: '0 4px', fontSize: '10px', color: '#65676B' }}>•</span>
              <Link component={RouterLink} to={`/posts/${post.id}`} color="inherit" underline="hover" sx={{ color: 'text.secondary', fontSize: '0.875rem' }}>
                {formatDate(post.createdAt)}
              </Link>
            </Box>
          }
        />

        <Menu anchorEl={anchorEl} open={isMenuOpen} onClose={handleMenuClose}>
          {post.selfPost && <MenuItem onClick={handleEditClick}><ListItemIcon><EditIcon fontSize="small" /></ListItemIcon>Chỉnh sửa bài viết</MenuItem>}
          <MenuItem onClick={() => { showDevelopmentAlert(); handleMenuClose(); }}><ListItemIcon><VisibilityOffIcon fontSize="small" /></ListItemIcon>Ẩn bài viết</MenuItem>
          {post.selfPost && <MenuItem onClick={handleDeleteClick} sx={{ color: 'error.main' }}><ListItemIcon><DeleteIcon fontSize="small" color="error" /></ListItemIcon>Xóa bài viết</MenuItem>}
        </Menu>

        <CardContent sx={{ pt: 0 }}>
          {post.content && (
            <Typography variant="body1" color="text.primary" style={{ whiteSpace: 'pre-line', marginBottom: post.originalPost ? 16 : 0 }}>
              {post.content}
            </Typography>
          )}

          {post.originalPost ? (
            <SharedPostContent originalPost={post.originalPost} />
          ) : (
            post.media && post.media.length > 0 && (
              <Box sx={{ mt: 1 }}>
                <PostMediaGrid media={post.media} />
              </Box>
            )
          )}
        </CardContent>

        {/* REACTION SUMMARY */}
        {totalReactions > 0 && (
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', px: 2, py: 1 }}>
            <ReactionTooltip postId={post.id} totalReactions={totalReactions}>
              <Box sx={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }} onClick={() => setShowReactionList(true)}>
                <Box sx={{ display: 'flex', alignItems: 'center', ml: -0.6 }}>
                  {topReactions.map(([type], index) => (
                    <Box
                      key={type}
                      sx={{
                        width: 24,
                        height: 24,
                        borderRadius: '50%',
                        bgcolor: 'background.paper',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        boxShadow: '0 0 0 2px #fff',
                        ml: index === 0 ? 0 : -1,
                        zIndex: topReactions.length - index,
                        backgroundImage: `url(${getReactionImg(type)})`,
                        backgroundSize: 'contain', backgroundPosition: 'center', backgroundRepeat: 'no-repeat',
                      }}
                    />
                  ))}
                </Box>
                <Typography variant="body2" color="text.secondary" sx={{ ml: 1 }}>
                  {formatCount(totalReactions)}
                </Typography>
              </Box>
            </ReactionTooltip>

            <Box sx={{ display: 'flex', gap: 2 }}>
              <Typography variant="body2" color="text.secondary" sx={{ cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }} onClick={handleCommentClick}>
                {post.commentCount} bình luận
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {post.shareCount || 0} chia sẻ
              </Typography>
            </Box>
          </Box>
        )}

        {totalReactions === 0 && (
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', px: 2, py: 1 }}>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <Typography variant="body2" color="text.secondary" sx={{ cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }} onClick={handleCommentClick}>
                {post.commentCount} bình luận
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {post.shareCount || 0} chia sẻ
              </Typography>
            </Box>
          </Box>
        )}

        <Divider variant="middle" sx={{ my: 0 }} />

        {/* POST ACTIONS (LIKE, COMMENT, SHARE) */}
        <CardActions sx={{ justifyContent: 'space-around', p: 1, position: 'relative' }}>
          <Box
            sx={{ position: 'relative', width: '100%', display: 'flex', justifyContent: 'center' }}
            onMouseEnter={handleReactionMouseEnter}
            onMouseLeave={handleReactionMouseLeave}
            onTouchStart={handleReactionTouchStart}
            onTouchEnd={handleReactionTouchEnd}
            onTouchCancel={handleReactionTouchEnd}
          >
            <Button
              fullWidth
              onClick={(event) => {
                if (skipClickRef.current) { skipClickRef.current = false; return; }
                handleReactionButtonClick();
              }}
              startIcon={
                activeReactionType ? (
                  <Box
                    sx={{
                      width: 20, height: 20,
                      backgroundImage: `url(${getReactionImg(activeReactionType)})`,
                      backgroundSize: 'contain', backgroundPosition: 'center', backgroundRepeat: 'no-repeat'
                    }}
                  />
                ) : (
                  <ThumbUpOutlinedIcon />
                )
              }
              sx={{
                color: activeReaction ? activeReaction.color : 'text.secondary',
                fontWeight: activeReaction ? 'bold' : 'normal',
                textTransform: 'none', justifyContent: 'flex-start', width: '100%'
              }}
            >
              {activeReaction ? activeReaction.label : 'Thích'}
            </Button>

            {/* 🔴 HOVER POPUP CẢM XÚC - ĐÃ FIX XÊ DỊCH */}
            <Box
              sx={{
                position: 'absolute',
                bottom: '100%', left: '50%',
                mb: 1, px: 1, py: 0.5,
                bgcolor: 'background.paper', borderRadius: 50,
                boxShadow: '0 2px 10px rgba(0,0,0,0.15)',
                opacity: showReactionPopup ? 1 : 0,
                pointerEvents: showReactionPopup ? 'auto' : 'none',
                transition: 'opacity 180ms ease, transform 180ms ease',
                transformOrigin: 'bottom center',
                transform: showReactionPopup ? 'translate(-50%, 0)' : 'translate(-50%, 8px)',
                display: 'flex', alignItems: 'center', gap: 0.5,
                zIndex: 20,
              }}
              onMouseEnter={clearReactionCloseTimer}
              onMouseLeave={handleReactionMouseLeave}
            >
              {REACTION_ORDER.map((type) => {
                const config = REACTION_CONFIG[type];
                return (
                  <Tooltip key={type} title={config.label} arrow placement="top">
                    {/* WRAPPER CỐ ĐỊNH KÍCH THƯỚC */}
                    <Box
                      sx={{
                        width: 48, height: 48,
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        cursor: 'pointer', borderRadius: '50%',
                        bgcolor: activeReactionType === type ? 'action.selected' : 'transparent',
                      }}
                      onClick={() => handleSelectReaction(type as ReactionType)}
                    >
                      {/* ẢNH ĐƯỢC PHÓNG TO KHI HOVER */}
                      <Box
                        sx={{
                          width: 40, height: 40,
                          backgroundImage: `url(${getReactionImg(type)})`,
                          backgroundSize: 'contain', backgroundPosition: 'center', backgroundRepeat: 'no-repeat',
                          transition: 'transform 150ms cubic-bezier(0.4, 0, 0.2, 1)',
                          '.MuiBox-root:hover > &': { transform: 'scale(1.25) translateY(-4px)' }
                        }}
                      />
                    </Box>
                  </Tooltip>
                );
              })}
            </Box>
          </Box>

          <Button
            fullWidth onClick={handleCommentClick}
            startIcon={<ChatBubbleOutlineOutlinedIcon />}
            sx={{ color: 'text.secondary', textTransform: 'none' }}
          >
            Bình luận
          </Button>

          <Button
            data-testid="post-share-button"
            fullWidth onClick={handleShareClick}
            startIcon={<ShareOutlinedIcon />}
            sx={{ color: 'text.secondary', textTransform: 'none' }}
          >
            Chia sẻ
          </Button>
        </CardActions>

        <Collapse in={showComments} timeout="auto" unmountOnExit>
          <Divider />
          <Box sx={{ p: 2 }}>
            <CommentSection postId={post.id} currentUser={user || undefined} />
          </Box>
        </Collapse>
      </Card>

      {/* SHARE DIALOG */}
      <Dialog open={shareOpen} onClose={() => !shareLoading && setShareOpen(false)} fullWidth maxWidth="sm" data-testid="share-post-dialog">
        <DialogTitle>Chia sẻ bài viết</DialogTitle>
        <DialogContent>
          <TextField
            data-testid="share-post-content"
            autoFocus fullWidth multiline minRows={3}
            placeholder="Nhập nội dung bạn muốn chia sẻ..."
            value={shareCaption} onChange={(e) => setShareCaption(e.target.value)}
            disabled={shareLoading} sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button data-testid="share-post-cancel" onClick={() => setShareOpen(false)} disabled={shareLoading}>Hủy</Button>
          <Button data-testid="share-post-submit" variant="contained" onClick={handleShareSubmit} disabled={!shareCaption.trim() || shareLoading}>
            {shareLoading ? 'Đang chia sẻ...' : 'Chia sẻ'}
          </Button>
        </DialogActions>
      </Dialog>

      {isEditDialogOpen && (
        <EditPost open={isEditDialogOpen} onClose={() => setIsEditDialogOpen(false)} post={post} onUpdateSuccess={handleUpdateSuccess} />
      )}

      {showReactionList && (
        <ReactionListDialog open={showReactionList} onClose={() => setShowReactionList(false)} postId={post.id} reactionCounts={reactionCounts} totalReactions={totalReactions} />
      )}
    </>
  );
}