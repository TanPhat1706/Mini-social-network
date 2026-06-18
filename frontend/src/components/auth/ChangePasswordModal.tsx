import { useState } from 'react';
import { Box, Button, Modal, TextField, Typography, Snackbar, Alert, CircularProgress, IconButton, InputAdornment } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';
import api from '../../api/api';

interface ChangePasswordModalProps {
  onClose: () => void;
}

const style = {
  position: 'absolute' as const,
  top: '50%',
  left: '50%',
  transform: 'translate(-50%, -50%)',
  width: 380,
  bgcolor: 'background.paper',
  borderRadius: 3,
  boxShadow: 24,
  p: 4,
};

const ChangePasswordModal: React.FC<ChangePasswordModalProps> = ({ onClose }) => {
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showOld, setShowOld] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [toastOpen, setToastOpen] = useState(false);

  const passwordsMatch = newPassword && confirmPassword && newPassword === confirmPassword;
  const passwordsMismatch = newPassword && confirmPassword && newPassword !== confirmPassword;

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');

    if (!oldPassword.trim() || !newPassword.trim() || !confirmPassword.trim()) {
      setError('Vui lòng điền tất cả các trường.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp');
      return;
    }

    setLoading(true);
    try {
      await api.post('/api/auth/change-password', {
        oldPassword,
        newPassword,
        confirmPassword,
      });
      setToastOpen(true);
      setTimeout(() => {
        setLoading(false);
        onClose();
      }, 1200);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Không thể đổi mật khẩu.');
      setLoading(false);
    }
  };

  return (
    <Modal open onClose={onClose} aria-labelledby="change-password-title" disableEnforceFocus>
      <Box sx={style}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography id="change-password-title" variant="h6" component="h2">
            Đổi mật khẩu
          </Typography>
          <IconButton size="small" onClick={onClose}>
            <CloseIcon fontSize="small" />
          </IconButton>
        </Box>

        <form onSubmit={handleSubmit} noValidate>
          <TextField
            fullWidth
            variant="outlined"
            label="Mật khẩu cũ"
            type={showOld ? 'text' : 'password'}
            value={oldPassword}
            onChange={(e) => setOldPassword(e.target.value)}
            margin="normal"
            disabled={loading}
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton onClick={() => setShowOld((prev) => !prev)} edge="end">
                    {showOld ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />

          <TextField
            fullWidth
            variant="outlined"
            label="Mật khẩu mới"
            type={showNew ? 'text' : 'password'}
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            margin="normal"
            disabled={loading}
            error={Boolean(passwordsMismatch)}
            helperText={passwordsMismatch ? 'Mật khẩu không khớp' : ' '}
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton onClick={() => setShowNew((prev) => !prev)} edge="end">
                    {showNew ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />

          <TextField
            fullWidth
            variant="outlined"
            label="Xác nhận mật khẩu"
            type={showConfirm ? 'text' : 'password'}
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            margin="normal"
            disabled={loading}
            error={Boolean(passwordsMismatch)}
            helperText={passwordsMismatch ? 'Mật khẩu không khớp' : ' '}
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton onClick={() => setShowConfirm((prev) => !prev)} edge="end">
                    {showConfirm ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />

          {error && (
            <Typography color="error" variant="body2" sx={{ mt: 1, mb: 1 }}>
              {error}
            </Typography>
          )}

          <Button
            type="submit"
            fullWidth
            variant="contained"
            color="primary"
            disabled={loading || passwordsMismatch || !oldPassword || !newPassword || !confirmPassword}
            sx={{ mt: 1, py: 1.5 }}
          >
            {loading ? <CircularProgress size={20} color="inherit" /> : 'Lưu mật khẩu mới'}
          </Button>
        </form>

        <Snackbar
          open={toastOpen}
          autoHideDuration={3000}
          onClose={() => setToastOpen(false)}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
        >
          <Alert severity="success" sx={{ width: '100%' }}>
            Đổi mật khẩu thành công
          </Alert>
        </Snackbar>
      </Box>
    </Modal>
  );
};

export default ChangePasswordModal;
