import React, { useState } from 'react';
import { Box, Avatar, Typography, IconButton, Link, Button, Collapse } from '@mui/material';
import ThumbUpIcon from '@mui/icons-material/ThumbUp'; // Like đặc
import ThumbUpOutlinedIcon from '@mui/icons-material/ThumbUpOutlined'; // Like rỗng
import { formatDistanceToNow } from 'date-fns';
import { vi } from 'date-fns/locale';
import api from '../../api/api';
import type { CommentData } from '../../types/types'; // Nhớ import đúng file type
 // Nhớ import đúng file type

interface CommentItemProps {
    comment: CommentData;
    onReply: (authorName: string, parentId: number) => void; // Callback khi user bấm "Phản hồi"
}

export default function CommentItem({ comment: initialComment, onReply }: CommentItemProps) {
    const [comment, setComment] = useState(initialComment);
    const [showReplies, setShowReplies] = useState(false);
    const [replies, setReplies] = useState<CommentData[]>([]);
    const [loadingReplies, setLoadingReplies] = useState(false);

    // Xử lý Like Comment
    const handleLike = async () => {
        const previousState = { ...comment };
        // Optimistic Update
        const isLiked = comment.likedByCurrentUser;
        setComment(prev => ({
            ...prev,
            likedByCurrentUser: !isLiked,
            likeCount: isLiked ? prev.likeCount - 1 : prev.likeCount + 1
        }));

        try {
            await api.post(`/api/comments/${comment.id}/like`);
        } catch (error) {
            setComment(previousState); // Revert nếu lỗi
        }
    };

    // Xử lý Load Replies (Lazy Load)
    const handleLoadReplies = async () => {
        if (!showReplies && replies.length === 0) {
            setLoadingReplies(true);
            try {
                // Load trang 0, size 5 (tùy chỉnh)
                const res = await api.get(`/api/comments/${comment.id}/replies?page=0&size=10`);
                setReplies(res.data.content);
            } catch (error) {
                console.error("Lỗi load replies", error);
            } finally {
                setLoadingReplies(false);
            }
        }
        setShowReplies(!showReplies);
    };

    return (
        <Box sx={{ display: 'flex', mb: 2 }}>
            <Avatar src={comment.author.avatarUrl} sx={{ width: 32, height: 32, mr: 1 }} />
            
            <Box sx={{ flexGrow: 1 }}>
                {/* BUBBLE COMMENT */}
                <Box sx={{ 
                    bgcolor: '#f0f2f5', 
                    borderRadius: '18px', 
                    p: 1.5, 
                    display: 'inline-block',
                    maxWidth: '100%' 
                }}>
                    <Typography variant="body2" fontWeight="bold" component="span">
                        {comment.author.fullName}
                    </Typography>
                    <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                        {comment.content}
                    </Typography>
                </Box>

                {/* ACTION BUTTONS (Like, Reply, Time) */}
                <Box sx={{ display: 'flex', alignItems: 'center', ml: 1.5, mt: 0.5, gap: 2 }}>
                    <Typography 
                        variant="caption" 
                        sx={{ cursor: 'pointer', fontWeight: comment.likedByCurrentUser ? 'bold' : 'normal', color: comment.likedByCurrentUser ? 'primary.main' : 'text.secondary' }}
                        onClick={handleLike}
                    >
                        Thích
                    </Typography>
                    
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
                    
                    {/* Số lượng like nhỏ */}
                    {comment.likeCount > 0 && (
                        <Box sx={{ display: 'flex', alignItems: 'center', bgcolor: 'white', borderRadius: 10, px: 0.5, boxShadow: 1 }}>
                             <ThumbUpIcon sx={{ width: 12, height: 12, color: 'primary.main' }} />
                             <Typography variant="caption" sx={{ ml: 0.5 }}>{comment.likeCount}</Typography>
                        </Box>
                    )}
                </Box>

                {/* XEM CÁC CÂU TRẢ LỜI */}
                {comment.replyCount > 0 && (
                    <Box sx={{ mt: 1, ml: 1 }}>
                        <Typography 
                            variant="caption" 
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
                        {loadingReplies && <Typography variant="caption">Đang tải...</Typography>}
                    </Box>
                </Collapse>
            </Box>
        </Box>
    );
}