import React, { useEffect, useState } from 'react';
import { Box, Typography, Collapse } from '@mui/material';
import AvatarWithFrame from '../AvatarWithFrame';
import ComboWrapper from '../Cosmetic/ComboWrapper';
import { useProfileNavigation } from '../../hooks/useProfileNavigation';
import ColoredName from '../ColoredName';
import { formatDistanceToNow } from 'date-fns';
import { vi } from 'date-fns/locale';
import api from '../../api/api';
import CommentReactionButton from './CommentReactionButton';
import type { CommentData } from '../../types/types';

interface CommentItemProps {
    comment: CommentData;
    onReply: (authorName: string, parentId: number) => void;
}

export default function CommentItem({ comment: initialComment, onReply }: CommentItemProps) {
    const [comment, setComment] = useState(initialComment);
    const [showReplies, setShowReplies] = useState(false);
    const [replies, setReplies] = useState<CommentData[]>(initialComment.replies || []);
    const [loadingReplies, setLoadingReplies] = useState(false);

    // Sync khi replies được push từ parent (optimistic update sau khi reply thành công)
    React.useEffect(() => {
        if (initialComment.replies && initialComment.replies.length > replies.length) {
            setReplies(initialComment.replies);
            setShowReplies(true); // Tự động mở danh sách replies để user thấy reply vừa gửi
        }
        // Đồng bộ replyCount
        setComment(prev => ({ ...prev, replyCount: initialComment.replyCount }));
    }, [initialComment.replies, initialComment.replyCount]);

    const handleLoadReplies = async () => {
        if (!showReplies && replies.length === 0) {
            setLoadingReplies(true);
            try {
                const res = await api.get(`/api/comments/${comment.id}/replies?page=0&size=10`);
                // Merge: giữ replies đã có từ optimistic update, bổ sung từ API
                setReplies(prev => {
                    const existingIds = new Set(prev.map(r => r.id));
                    const newFromApi = res.data.content.filter((r: CommentData) => !existingIds.has(r.id));
                    return [...prev, ...newFromApi];
                });
            } catch (error) {
                console.error("Lỗi load replies", error);
            } finally {
                setLoadingReplies(false);
            }
        }
        setShowReplies(!showReplies);
    };

    const navigateToProfile = useProfileNavigation();

    return (
        <Box sx={{ display: 'flex', mb: 2 }}>
            <ComboWrapper
                frameClass={(comment.author as any).currentAvatarFrame}
                colorClass={(comment.author as any).currentNameColor}
                style={{ alignItems: 'flex-start' }}
            >
            <Box sx={{ mr: 1, ml: '8px', mt: '8px', flexShrink: 0 }}>
                <AvatarWithFrame
                    src={comment.author.avatarUrl}
                    frameClass={(comment.author as any).currentAvatarFrame}
                    size={32}
                    onClick={(e) => { e.stopPropagation(); navigateToProfile(comment.author.studentCode); }}
                />
            </Box>
            <Box sx={{ flexGrow: 1 }}>
                {/* BUBBLE COMMENT */}
                <Box sx={{
                    /* 🟢 ĐÃ SỬA: Đổi màu nền bong bóng thành background.default */
                    bgcolor: 'background.default',
                    borderRadius: '18px',
                    p: 1.5,
                    display: 'inline-block',
                    maxWidth: '100%'
                }}>
                    {/* 🟢 ĐÃ SỬA: Ép màu chữ thành text.primary */}
                    <Typography variant="body2" fontWeight="bold" component="span" color="text.primary">
                        {/* Make author name clickable to their profile */}
                        {/* comment.author has type UserSummary with studentCode available */}
                        <span style={{ display: 'inline-block' }}>
                            {/* eslint-disable-next-line react/jsx-no-bind */}
                            <ColoredName name={comment.author.fullName} colorClass={(comment.author as any).currentNameColor} studentCode={comment.author.studentCode} />
                        </span>
                    </Typography>
                    <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', color: 'text.primary' }}>
                        {comment.content}
                    </Typography>
                </Box>

                {/* ACTION BUTTONS (Reaction, Reply, Time) */}
                <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', ml: 1.5, mt: 0.5, gap: 2 }}>
                    <CommentReactionButton
                        commentId={comment.id}
                        reactionCounts={comment.reactionCounts}
                        reactionCount={comment.reactionCount}
                        currentUserReaction={comment.currentUserReaction || null}
                        onReactionChange={(nextReactionCounts, nextCurrentUserReaction) => {
                            const nextReactionCount = Object.values(nextReactionCounts).reduce((sum, value) => sum + value, 0);
                            setComment(prev => ({
                                ...prev,
                                reactionCounts: nextReactionCounts,
                                reactionCount: nextReactionCount,
                                currentUserReaction: nextCurrentUserReaction,
                            }));
                        }}
                    />

                    <Typography
                        variant="caption"
                        sx={{ cursor: 'pointer', color: 'text.secondary' }}
                        onClick={() => onReply(comment.author.fullName, comment.id)}
                    >
                        Phản hồi
                    </Typography>

                    <Typography variant="caption" color="text.secondary">
                        {formatDistanceToNow(new Date(comment.createdAt), { addSuffix: true, locale: vi })}
                    </Typography>
                </Box>

                {/* XEM CÁC CÂU TRẢ LỜI */}
                {comment.replyCount > 0 && (
                    <Box sx={{ mt: 1, ml: 1 }}>
                        <Typography
                            variant="caption"
                            color="text.secondary"
                            sx={{ fontWeight: 'bold', cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }}
                            onClick={handleLoadReplies}
                        >
                            {showReplies ? "Ẩn phản hồi" : `Xem ${comment.replyCount} câu trả lời`}
                        </Typography>
                    </Box>
                )}

                {/* DANH SÁCH REPLIES (Recursive) */}
                <Collapse in={showReplies}>
                    <Box sx={{ mt: 1 }}>
                        {replies.map(reply => (
                            <CommentItem key={reply.id} comment={reply} onReply={(name) => onReply(name, comment.id)} />
                        ))}
                        {loadingReplies && <Typography variant="caption" color="text.secondary">Đang tải...</Typography>}
                    </Box>
                </Collapse>
            </Box>
            </ComboWrapper>
        </Box>
    );
}