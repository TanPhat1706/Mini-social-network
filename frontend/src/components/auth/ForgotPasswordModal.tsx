import { useState } from 'react';
import { Box, Button, Modal, TextField, Typography, Snackbar, Alert, CircularProgress, IconButton } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import api from '../../api/api';

interface ForgotPasswordModalProps {
  onClose: () => void;
}

const style = {
  position: 'absolute' as const,
  top: '50%',
  left: '50%',
  transform: 'translate(-50%, -50%)',
  width: 360,
  bgcolor: 'background.paper',
  borderRadius: 3,
  boxShadow: 24,
  p: 4,
};

const ForgotPasswordModal: React.FC<ForgotPasswordModalProps> = ({ onClose }) => {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [toastOpen, setToastOpen] = useState(false);

  const isValidEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');

    if (!email.trim()) {
      setError('Email không được để trống');
      return;
    }

    if (!isValidEmail) {
      setError('Vui lòng nhập email hợp lệ');
      return;
    }

    setLoading(true);
    try {
      await api.post('/api/auth/forgot-password', { email });
      setToastOpen(true);
      setTimeout(() => {
        setLoading(false);
        onClose();
      }, 1200);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Đã có lỗi xảy ra, vui lòng thử lại');
      setLoading(false);
    }
  };

  return (
    <Modal open onClose={onClose} aria-labelledby="forgot-password-title" disableEnforceFocus>
      <Box sx={style}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography id="forgot-password-title" variant="h6" component="h2">
            Quên mật khẩu
          </Typography>
          <IconButton size="small" onClick={onClose}>
            <CloseIcon fontSize="small" />
          </IconButton>
        </Box>

        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Nhập email của bạn để nhận link xác nhận đổi mật khẩu.
        </Typography>

        <form onSubmit={handleSubmit} noValidate>
          <TextField
            fullWidth
            variant="outlined"
            label="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={Boolean(email && !isValidEmail)}
            helperText={email && !isValidEmail ? 'Email không hợp lệ' : ' '}
            margin="normal"
            disabled={loading}
          />

          {error && (
            <Typography color="error" variant="body2" sx={{ mb: 1 }}>
              {error}
            </Typography>
          )}

          <Button
            type="submit"
            fullWidth
            variant="contained"
            color="primary"
            disabled={loading}
            sx={{ mt: 1, py: 1.5 }}
          >
            {loading ? <CircularProgress size={20} color="inherit" /> : 'Gửi link xác nhận'}
          </Button>
        </form>

        <Snackbar
          open={toastOpen}
          autoHideDuration={3000}
          onClose={() => setToastOpen(false)}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
        >
          <Alert severity="success" sx={{ width: '100%' }}>
            Đã gửi link xác nhận về email của bạn
          </Alert>
        </Snackbar>
      </Box>
    </Modal>
  );
};

export default ForgotPasswordModal;
