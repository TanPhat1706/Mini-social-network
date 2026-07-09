import React, { useState, useEffect, useRef } from 'react';
import { Box, TextField, IconButton, CircularProgress, Typography, InputAdornment, Switch, FormControlLabel, Snackbar, Alert } from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import CloseIcon from '@mui/icons-material/Close';
import api from '../../api/api';
import CommentItem from './CommentItem';
import type { CommentData } from '../../types/types';

// 🟢 MỚI: Import 2 Component VIP của chúng ta vào
import AvatarWithFrame from '../AvatarWithFrame';
import ColoredName from '../ColoredName';

// 🟢 MỚI: Cập nhật Props để nhận toàn bộ thông tin User thay vì chỉ mỗi cái link Avatar
interface CommentSectionProps {
    postId: number;
    currentUser?: {
        fullName: string;
        studentCode: string;
        avatarUrl?: string;
        currentAvatarFrame?: string | null;
        currentNameColor?: string | null;
    };
}

export default function CommentSection({ postId, currentUser }: CommentSectionProps) {
    const [comments, setComments] = useState<CommentData[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    
    const [content, setContent] = useState('');
    const [replyTo, setReplyTo] = useState<{ name: string; parentId: number } | null>(null);
    const [submitting, setSubmitting] = useState(false);
    
    const [isAnonymous, setIsAnonymous] = useState(false);
    // Snackbar để thông báo sau khi reply thành công
    const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'info' }>(
        { open: false, message: '', severity: 'success' }
    );
    
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
                parentCommentId: replyTo?.parentId || null,
                isAnonymous: isAnonymous
            };
    
            const res = await api.post('/api/comments', payload);
            const savedComment = res.data;
    
            // 🟢 TỐI ƯU UI: Enrichment dữ liệu
            // Chúng ta tạo một bản sao đã làm giàu thông tin để cập nhật State ngay lập tức
            const enrichedComment = {
                ...savedComment,
                author: {
                    ...savedComment.author,
                    // Nếu bình luận ẩn danh, thông tin author thường được backend trả về là null hoặc thông tin ẩn danh
                    // Nếu không ẩn danh, ta ghi đè thông tin frame/color từ currentUser để đảm bảo UI khớp 100%
                    ...(isAnonymous ? {} : {
                        currentAvatarFrame: currentUser?.currentAvatarFrame || savedComment.author.currentAvatarFrame,
                        currentNameColor: currentUser?.currentNameColor || savedComment.author.currentNameColor
                    })
                }
            };
    
            if (replyTo) {
                // Cập nhật replies ngay trong state của comment cha
                setComments(prev => prev.map(c => {
                    if (c.id === replyTo.parentId) {
                        return {
                            ...c,
                            replyCount: c.replyCount + 1,
                            replies: [...(c.replies || []), enrichedComment]
                        };
                    }
                    return c;
                }));
                handleCancelReply();
                // Hiện Snackbar thông báo nhẹ nhàng thay vì alert() chặn UI
                setSnackbar({ open: true, message: 'Đã gửi phản hồi thành công! 💬', severity: 'success' });
            } else {
                // Cập nhật State để React tự render lại UI ngay lập tức
                setComments(prev => [enrichedComment, ...prev]);
            }
    
            setContent('');
        } catch (error) {
            console.error("Failed to post comment", error);
            // Có thể thêm showError("Không thể gửi bình luận") ở đây
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
            <Box sx={{ maxHeight: '400px', overflowY: 'auto', mb: 2, p: 1 }}>
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
            <Box sx={{ display: 'flex', alignItems: 'flex-start', pt: 1.5, borderTop: 1, borderColor: 'divider' }}>
                
                {/* 🟢 ĐÃ SỬA: Dùng AvatarWithFrame thay cho Avatar thường */}
                <Box sx={{ mr: 1, mt: 0.5 }}>
                    <AvatarWithFrame 
                        src={isAnonymous ? "https://ui-avatars.com/api/?name=Anonymous&background=808080&color=fff" : currentUser?.avatarUrl} 
                        frameClass={isAnonymous ? null : currentUser?.currentAvatarFrame} 
                        size={36} 
                    />
                </Box>
                
                <Box sx={{ flexGrow: 1 }}>
                    
                    {/* 🟢 HIỂN THỊ TÊN NGƯỜI ĐANG BÌNH LUẬN Ở TRÊN KHUNG NHẬP */}
                    {currentUser && !isAnonymous && (
                        <Typography variant="caption" sx={{ ml: 1, mb: 0.5, display: 'block' }}>
                            Bình luận dưới tên: <ColoredName name={currentUser.fullName} colorClass={currentUser.currentNameColor} studentCode={currentUser.studentCode} />
                        </Typography>
                    )}
                    {isAnonymous && (
                        <Typography variant="caption" sx={{ ml: 1, mb: 0.5, display: 'block', color: 'text.secondary', fontStyle: 'italic' }}>
                            Đang bình luận với tư cách: <b>Một người dùng ẩn danh</b> 🕵️
                        </Typography>
                    )}

                    {replyTo && (
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
                        data-testid="comment-input"
                        fullWidth multiline maxRows={4}
                        placeholder={replyTo ? "Viết câu trả lời..." : "Viết bình luận..."}
                        variant="outlined" size="small"
                        value={content} inputRef={inputRef}
                        onChange={(e) => setContent(e.target.value)}
                        onKeyDown={handleKeyDown} disabled={submitting}
                        sx={{ 
                            '& .MuiOutlinedInput-root': { borderRadius: '20px', bgcolor: 'background.default' },
                            '& fieldset': { border: 'none' } 
                        }}
                        InputProps={{
                            endAdornment: (
                                <InputAdornment position="end">
                                    <IconButton
                                        data-testid="comment-submit"
                                        onClick={handleSubmit}
                                        disabled={!content.trim() || submitting}
                                        color={isAnonymous ? "warning" : "primary"}
                                    >
                                        {submitting ? <CircularProgress size={20} /> : <SendIcon />}
                                    </IconButton>
                                </InputAdornment>
                            )
                        }}
                    />
                    
                    {/* KHU VỰC TOOLBAR DƯỚI INPUT */}
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 0.5, px: 1 }}>
                        <FormControlLabel
                            data-testid="comment-anonymous-toggle"
                            control={
                                <Switch
                                    data-testid="comment-anonymous-switch"
                                    size="small"
                                    checked={isAnonymous}
                                    onChange={(e) => setIsAnonymous(e.target.checked)}
                                    color="warning"
                                />
                            }
                            label={
                                <Typography 
                                    variant="caption" 
                                    sx={{ 
                                        color: isAnonymous ? 'warning.main' : 'text.secondary', 
                                        fontWeight: isAnonymous ? 'bold' : 'normal',
                                        transition: 'all 0.3s ease'
                                    }}
                                >
                                    Bình luận ẩn danh 🕵️
                                </Typography>
                            }
                        />
                        
                        <Typography variant="caption" color="text.secondary">
                            Nhấn Enter để gửi
                        </Typography>
                    </Box>
                    
                </Box>
            </Box>

            {/* SNACKBAR THÔNG BÁO REPLY THÀNH CÔNG */}
            <Snackbar
                open={snackbar.open}
                autoHideDuration={3000}
                onClose={() => setSnackbar(prev => ({ ...prev, open: false }))}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
            >
                <Alert
                    onClose={() => setSnackbar(prev => ({ ...prev, open: false }))}
                    severity={snackbar.severity}
                    variant="filled"
                    sx={{ width: '100%', borderRadius: 2 }}
                >
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </Box>
    );
}