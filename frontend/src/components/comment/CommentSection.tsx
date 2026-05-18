import React, { useState, useEffect, useRef } from 'react';
import { Box, TextField, IconButton, CircularProgress, Typography, Avatar, InputAdornment, Button } from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import CloseIcon from '@mui/icons-material/Close';
import api from '../../api/api';
import CommentItem from './CommentItem';
import type { CommentData } from '../../types/types';

interface CommentSectionProps {
    postId: number;
    currentUserAvatar?: string;
}

export default function CommentSection({ postId, currentUserAvatar }: CommentSectionProps) {
    const [comments, setComments] = useState<CommentData[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    
    const [content, setContent] = useState('');
    const [replyTo, setReplyTo] = useState<{ name: string; parentId: number } | null>(null);
    const [submitting, setSubmitting] = useState(false);
    
    const inputRef = useRef<HTMLInputElement>(null);

    useEffect(() => {
        fetchComments(0);
    }, [postId]);

    const fetchComments = async (pageNum: number) => {
        try {
            const res = await api.get(`/api/comments/post/${postId}?page=${pageNum}&size=5`);
            const newComments = res.data.content;
            
            if (pageNum === 0) {
                setComments(newComments);
            } else {
                setComments(prev => [...prev, ...newComments]);
            }
            
            setHasMore(!res.data.last);
            setLoading(false);
        } catch (error) {
            console.error("Failed to load comments", error);
            setLoading(false);
        }
    };

    const handleLoadMore = () => {
        const nextPage = page + 1;
        setPage(nextPage);
        fetchComments(nextPage);
    };

    const handleReplyClick = (name: string, parentId: number) => {
        setReplyTo({ name, parentId });
        if (inputRef.current) inputRef.current.focus();
    };

    const handleCancelReply = () => setReplyTo(null);

    const handleSubmit = async () => {
        if (!content.trim()) return;
        setSubmitting(true);

        try {
            const payload = {
                content: content,
                postId: postId,
                parentCommentId: replyTo?.parentId || null
            };

            const res = await api.post('/api/comments', payload);
            const newComment = res.data;

            if (replyTo) {
                alert("Đã gửi câu trả lời! Hãy mở mục phản hồi để xem.");
                handleCancelReply();
            } else {
                setComments(prev => [newComment, ...prev]);
            }

            setContent('');
        } catch (error) {
            console.error("Failed to post comment", error);
        } finally {
            setSubmitting(false);
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSubmit();
        }
    };

    return (
        <Box sx={{ mt: 2 }}>
            {/* DANH SÁCH COMMENT */}
            <Box sx={{ maxHeight: '400px', overflowY: 'auto', mb: 2 }}>
                {loading && page === 0 ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center' }}><CircularProgress size={24} /></Box>
                ) : (
                    <>
                        {comments.length === 0 ? (
                            <Typography variant="body2" color="text.secondary" align="center">Chưa có bình luận nào.</Typography>
                        ) : (
                            comments.map(c => (
                                <CommentItem key={c.id} comment={c} onReply={handleReplyClick} />
                            ))
                        )}
                        
                        {hasMore && (
                            <Typography 
                                variant="body2" color="primary" align="center" 
                                sx={{ cursor: 'pointer', mt: 1, fontWeight: 'bold' }}
                                onClick={handleLoadMore}
                            >
                                Xem thêm bình luận
                            </Typography>
                        )}
                    </>
                )}
            </Box>

            {/* KHUNG INPUT */}
            {/* 🟢 ĐÃ SỬA: Thay viền #eee bằng biến divider */}
            <Box sx={{ display: 'flex', alignItems: 'flex-start', pt: 1, borderTop: 1, borderColor: 'divider' }}>
                <Avatar src={currentUserAvatar} sx={{ mr: 1, width: 32, height: 32 }} />
                
                <Box sx={{ flexGrow: 1 }}>
                    {replyTo && (
                        /* 🟢 ĐÃ SỬA: Thay nền #e3f2fd bằng action.hover để nó chìm nhẹ vào nền tối */
                        <Box sx={{ display: 'flex', alignItems: 'center', mb: 0.5, bgcolor: 'action.hover', p: 0.5, borderRadius: 1 }}>
                            <Typography variant="caption" sx={{ mr: 1, color: 'text.primary' }}>
                                Đang trả lời <b>{replyTo.name}</b>
                            </Typography>
                            <IconButton size="small" onClick={handleCancelReply}>
                                <CloseIcon fontSize="small" sx={{ color: 'text.primary' }} />
                            </IconButton>
                        </Box>
                    )}

                    <TextField
                        fullWidth multiline maxRows={4}
                        placeholder={replyTo ? "Viết câu trả lời..." : "Viết bình luận..."}
                        variant="outlined" size="small"
                        value={content} inputRef={inputRef}
                        onChange={(e) => setContent(e.target.value)}
                        onKeyDown={handleKeyDown} disabled={submitting}
                        sx={{ 
                            /* 🟢 ĐÃ SỬA: Đổi màu nền input thành background.default */
                            '& .MuiOutlinedInput-root': { borderRadius: '20px', bgcolor: 'background.default' },
                            '& fieldset': { border: 'none' } 
                        }}
                        InputProps={{
                            endAdornment: (
                                <InputAdornment position="end">
                                    <IconButton 
                                        onClick={handleSubmit} 
                                        disabled={!content.trim() || submitting}
                                        color="primary"
                                    >
                                        {submitting ? <CircularProgress size={20} /> : <SendIcon />}
                                    </IconButton>
                                </InputAdornment>
                            )
                        }}
                    />
                    <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>
                        Nhấn Enter để gửi
                    </Typography>
                </Box>
            </Box>
        </Box>
    );
}