import React, { useState } from 'react';
import { Box, Avatar, Typography, IconButton, Link, Button, Collapse } from '@mui/material';
import ThumbUpIcon from '@mui/icons-material/ThumbUp'; 
import ThumbUpOutlinedIcon from '@mui/icons-material/ThumbUpOutlined'; 
import { formatDistanceToNow } from 'date-fns';
import { vi } from 'date-fns/locale';
import api from '../../api/api';
import type { CommentData } from '../../types/types'; 

interface CommentItemProps {
    comment: CommentData;
    onReply: (authorName: string, parentId: number) => void; 
}

export default function CommentItem({ comment: initialComment, onReply }: CommentItemProps) {
    const [comment, setComment] = useState(initialComment);
    const [showReplies, setShowReplies] = useState(false);
    const [replies, setReplies] = useState<CommentData[]>([]);
    const [loadingReplies, setLoadingReplies] = useState(false);

    const handleLike = async () => {
        const previousState = { ...comment };
        const isLiked = comment.likedByCurrentUser;
        setComment(prev => ({
            ...prev,
            likedByCurrentUser: !isLiked,
            likeCount: isLiked ? prev.likeCount - 1 : prev.likeCount + 1
        }));

        try {
            await api.post(`/api/comments/${comment.id}/like`);
        } catch (error) {
            setComment(previousState); 
        }
    };

    const handleLoadReplies = async () => {
        if (!showReplies && replies.length === 0) {
            setLoadingReplies(true);
            try {
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
                    /* 🟢 ĐÃ SỬA: Đổi màu nền bong bóng thành background.default */
                    bgcolor: 'background.default', 
                    borderRadius: '18px', 
                    p: 1.5, 
                    display: 'inline-block',
                    maxWidth: '100%' 
                }}>
                    {/* 🟢 ĐÃ SỬA: Ép màu chữ thành text.primary */}
                    <Typography variant="body2" fontWeight="bold" component="span" color="text.primary">
                        {comment.author.fullName}
                    </Typography>
                    <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', color: 'text.primary' }}>
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
                        /* 🟢 ĐÃ SỬA: Đổi màu nền nhỏ thành background.paper để hòa nhập với card */
                        <Box sx={{ display: 'flex', alignItems: 'center', bgcolor: 'background.paper', borderRadius: 10, px: 0.5, boxShadow: 1 }}>
                             <ThumbUpIcon sx={{ width: 12, height: 12, color: 'primary.main' }} />
                             <Typography variant="caption" sx={{ ml: 0.5, color: 'text.primary' }}>{comment.likeCount}</Typography>
                        </Box>
                    )}
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
        </Box>
    );
}