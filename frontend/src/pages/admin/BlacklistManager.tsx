import { useEffect, useState } from 'react';
import api from '../../api/api';
import { showError, showSuccess } from '../../utils/swal'; 

import {
  Card, CardContent, Typography, Box, CircularProgress,
  TextField, Button, Chip, Divider
} from '@mui/material';

import GppBadIcon from '@mui/icons-material/GppBad'; 
import AddTaskIcon from '@mui/icons-material/AddTask';

interface BlacklistedWord {
  id: number;
  word: string;
}

export const BlacklistManager = () => {
  const [badWords, setBadWords] = useState<BlacklistedWord[]>([]);
  const [newWord, setNewWord] = useState('');
  const [loadingWords, setLoadingWords] = useState(false);

  useEffect(() => {
    fetchBadWords();
  }, []);

  const fetchBadWords = async () => {
    setLoadingWords(true);
    try {
      const res = await api.get<BlacklistedWord[]>('/api/admin/moderation/blacklist');
      setBadWords(res.data);
    } catch (error) {
      console.error("Lỗi lấy danh sách từ cấm", error);
    } finally {
      setLoadingWords(false);
    }
  };

  const handleAddBadWord = async () => {
    if (!newWord.trim()) return;
    try {
      // 🟢 Gửi dữ liệu dưới dạng JSON object chuẩn
      await api.post('/api/admin/moderation/blacklist', { word: newWord.trim() });
      
      showSuccess('Đã thêm từ khóa vào bộ lọc!');
      setNewWord('');
      fetchBadWords(); 
    } catch (error: any) {
      showError(error.response?.data || 'Lỗi khi thêm từ khóa');
    }
  };

  const handleDeleteBadWord = async (id: number) => {
    try {
      await api.delete(`/api/admin/moderation/blacklist/${id}`);
      showSuccess('Đã xóa từ khóa khỏi bộ lọc!');
      fetchBadWords(); 
    } catch (error) {
      showError('Lỗi khi xóa từ khóa');
    }
  };

  return (
    <Box>
      <Typography variant="h4" sx={{ mb: 4, fontWeight: 'bold' }}>Quản lý Bộ lọc (Blacklist)</Typography>

      <Card sx={{ borderRadius: 2, boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}>
        <CardContent sx={{ p: 4 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
            <GppBadIcon color="error" fontSize="large" />
            <Typography variant="h6" fontWeight="bold">
              Bộ lọc Duyệt bài tự động (Auto-Moderation)
            </Typography>
          </Box>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 4 }}>
            Bot AI sẽ tự động quét mọi bài viết mới. Nếu phát hiện chứa các từ khóa dưới đây, bài viết sẽ bị khóa ở trạng thái <b>Chờ duyệt</b>.
          </Typography>

          <Box sx={{ display: 'flex', gap: 2, mb: 4, maxWidth: 500 }}>
            <TextField
              size="small"
              fullWidth
              variant="outlined"
              placeholder="Nhập từ khóa cấm (VD: lùa gà, cá độ...)"
              value={newWord}
              onChange={(e) => setNewWord(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleAddBadWord()}
            />
            <Button 
              variant="contained" color="error" disableElevation 
              startIcon={<AddTaskIcon />} onClick={handleAddBadWord}
            >
              Thêm
            </Button>
          </Box>

          <Divider sx={{ mb: 4 }} />

          <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 2, color: 'text.secondary' }}>
            DANH SÁCH TỪ KHÓA ĐANG BỊ CẤM ({badWords.length})
          </Typography>

          {loadingWords ? (
            <CircularProgress size={24} />
          ) : badWords.length > 0 ? (
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5 }}>
              {badWords.map((item) => (
                <Chip
                  key={item.id}
                  label={item.word}
                  onDelete={() => handleDeleteBadWord(item.id)}
                  color="error"
                  variant="outlined"
                  sx={{ fontWeight: 500, bgcolor: '#ffebee', '&:hover': { bgcolor: '#ffcdd2' } }}
                />
              ))}
            </Box>
          ) : (
            <Typography variant="body2" color="text.secondary" fontStyle="italic">
              Bộ lọc đang trống. Bot hiện đang cho phép đăng mọi bài viết.
            </Typography>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};