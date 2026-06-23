import React, { useMemo, useRef, useState } from 'react';
import { Box, Typography, Tooltip } from '@mui/material';
import icons from 'react-reactions/src/helpers/icons'; // 🔴 IMPORT BỘ ICON MỚI
import api from '../../api/api';
import CommentReactionListDialog from './CommentReactionListDialog';

type ReactionType = 'LIKE' | 'LOVE' | 'HAHA' | 'WOW' | 'SAD' | 'ANGRY';

// 🔴 CẬP NHẬT LẠI CONFIG SỬ DỤNG ICON KEY TỪ REACT-REACTIONS
const REACTION_CONFIG: Record<ReactionType, { iconKey: string; color: string; label: string }> = {
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

interface CommentReactionButtonProps {
  commentId: number;
  reactionCounts?: Record<string, number>;
  reactionCount?: number;
  currentUserReaction?: string | null;
  onReactionChange: (reactionCounts: Record<string, number>, currentUserReaction: ReactionType | null) => void;
}

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

export default function CommentReactionButton({
  commentId,
  reactionCounts = {},
  reactionCount = 0,
  currentUserReaction,
  onReactionChange,
}: CommentReactionButtonProps) {
  const [showReactionPopup, setShowReactionPopup] = useState(false);
  const [showReactionList, setShowReactionList] = useState(false);
  const [isReacting, setIsReacting] = useState(false);

  const reactionOpenTimeoutRef = useRef<number | null>(null);
  const reactionCloseTimeoutRef = useRef<number | null>(null);
  const touchHoldTimeoutRef = useRef<number | null>(null);
  const skipClickRef = useRef(false);

  const currentReaction = normalizeReactionType(currentUserReaction || null) || null;
  const reactionEntries = Object.entries(reactionCounts)
    .filter(([, count]) => count > 0)
    .sort((a, b) => b[1] - a[1]);

  const sortedReactions = useMemo(() => reactionEntries, [reactionEntries]);
  const totalReactions = sortedReactions.reduce((sum, [, count]) => sum + count, 0) || reactionCount;
  const topReactions = sortedReactions.slice(0, 3) as [ReactionType, number][];
  const activeReaction = currentReaction ? REACTION_CONFIG[currentReaction] : undefined;

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

  const submitReaction = async (requestedReaction: ReactionType, nextReaction: ReactionType | null) => {
    if (isReacting) return;

    const previousCounts = { ...reactionCounts };
    const previousReaction = currentReaction;
    const nextCounts = updateReactionCounts(reactionCounts, currentReaction, nextReaction);

    onReactionChange(nextCounts, nextReaction);
    setIsReacting(true);

    try {
      await api.post(`/api/comments/${commentId}/react`, { reactionType: requestedReaction });
    } catch (error) {
      console.error('Lỗi reaction comment:', error);
      onReactionChange(previousCounts, previousReaction);
    } finally {
      setIsReacting(false);
    }
  };

  const handleReactionButtonClick = () => {
    if (currentReaction) {
      submitReaction(currentReaction, null);
    } else {
      submitReaction('LIKE', 'LIKE');
    }
  };

  const handleSelectReaction = (reactionType: ReactionType) => {
    const nextReaction = currentReaction === reactionType ? null : reactionType;
    submitReaction(reactionType, nextReaction);
    closeReactionPopup();
    skipClickRef.current = true;
  };

  const handleReactionMouseEnter = () => {
    clearReactionCloseTimer();
    if (showReactionPopup) return;

    reactionOpenTimeoutRef.current = window.setTimeout(() => {
      openReactionPopup();
    }, 400);
  };

  const handleReactionMouseLeave = () => {
    clearReactionOpenTimer();
    reactionCloseTimeoutRef.current = window.setTimeout(() => {
      closeReactionPopup();
    }, 200);
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

  return (
    <>
      <Box
        sx={{ display: 'flex', alignItems: 'center', gap: 2 }}
        onMouseEnter={handleReactionMouseEnter}
        onMouseLeave={handleReactionMouseLeave}
        onTouchStart={handleReactionTouchStart}
        onTouchEnd={handleReactionTouchEnd}
        onTouchCancel={handleReactionTouchEnd}
      >
        <Box sx={{ position: 'relative' }}>
          
          <Typography
            variant="caption"
            sx={{
              cursor: 'pointer',
              fontWeight: 'bold',
              color: activeReaction ? activeReaction.color : 'text.secondary',
              '&:hover': { textDecoration: 'underline' },
              userSelect: 'none'
            }}
            onClick={() => {
              if (skipClickRef.current) {
                skipClickRef.current = false;
                return;
              }
              handleReactionButtonClick();
            }}
          >
            {activeReaction ? activeReaction.label : 'Thích'}
          </Typography>

          {/* 🔴 POPUP CHỌN CẢM XÚC ĐÃ FIX LỖI XÊ DỊCH */}
          <Box
            sx={{
              position: 'absolute',
              bottom: '100%',
              left: '50%',
              transform: showReactionPopup ? 'translate(-50%, 0)' : 'translate(-50%, 8px)',
              mb: 1.5, 
              px: 1,
              py: 0.5, // Giảm padding dọc để gọn gàng hơn
              bgcolor: 'background.paper',
              borderRadius: 50,
              boxShadow: '0 2px 10px rgba(0,0,0,0.15)',
              opacity: showReactionPopup ? 1 : 0,
              pointerEvents: showReactionPopup ? 'auto' : 'none',
              transition: 'opacity 180ms ease, transform 180ms ease',
              transformOrigin: 'bottom center',
              display: 'flex',
              alignItems: 'center',
              gap: 0.5,
              zIndex: 20,
            }}
            onMouseEnter={clearReactionCloseTimer}
            onMouseLeave={handleReactionMouseLeave}
          >
            {REACTION_ORDER.map((type) => {
              const config = REACTION_CONFIG[type];
              return (
                <Tooltip key={type} title={config.label} arrow placement="top">
                  {/* 1. Box Wrapper cố định kích thước, ngăn xê dịch layout */}
                  <Box
                    sx={{
                      width: 40,
                      height: 40,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      cursor: 'pointer',
                      borderRadius: '50%',
                      bgcolor: currentReaction === type ? 'action.selected' : 'transparent',
                    }}
                    onClick={() => handleSelectReaction(type)}
                  >
                    {/* 2. Ảnh icon sẽ phóng to khi Box Wrapper được hover */}
                    <Box
                      sx={{
                        width: 32,
                        height: 32,
                        backgroundImage: `url(${getReactionImg(type)})`,
                        backgroundSize: 'contain',
                        backgroundPosition: 'center',
                        backgroundRepeat: 'no-repeat',
                        transition: 'transform 150ms cubic-bezier(0.4, 0, 0.2, 1)',
                        // Phóng to nhẹ và nẩy lên một xíu
                        '.MuiBox-root:hover > &': { transform: 'scale(1.25) translateY(-4px)' },
                      }}
                    />
                  </Box>
                </Tooltip>
              );
            })}
          </Box>
        </Box>

        {/* HIỂN THỊ TỔNG LƯỢT */}
        {totalReactions > 0 && (
          <Box
            sx={{ display: 'flex', alignItems: 'center', cursor: 'pointer', '&:hover': { opacity: 0.8 } }}
            onClick={() => setShowReactionList(true)}
          >
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              {topReactions.length > 0 &&
                topReactions.map(([type], index) => (
                  <Box
                    key={type}
                    sx={{
                      width: 16,
                      height: 16,
                      borderRadius: '50%',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      bgcolor: 'background.paper',
                      ml: index === 0 ? 0 : -0.5,
                      zIndex: topReactions.length - index,
                      boxShadow: '0 0 0 2px #fff',
                      backgroundImage: `url(${getReactionImg(type)})`,
                      backgroundSize: 'contain',
                      backgroundPosition: 'center',
                      backgroundRepeat: 'no-repeat',
                    }}
                  />
                ))}
            </Box>
            <Typography variant="caption" sx={{ ml: 0.5, color: 'text.secondary', userSelect: 'none' }}>
              {formatCount(totalReactions)}
            </Typography>
          </Box>
        )}
      </Box>

      {/* DIALOG DANH SÁCH TƯƠNG TÁC */}
      <CommentReactionListDialog
        open={showReactionList}
        onClose={() => setShowReactionList(false)}
        commentId={commentId}
        reactionCounts={reactionCounts}
        totalReactions={totalReactions}
      />
    </>
  );
}