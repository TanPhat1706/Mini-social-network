import React, { useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Box, Button, TextField, Typography, Snackbar, Alert, CircularProgress } from '@mui/material';
import api from '../../api/api';
import '../../assets/css/login.css';

const ResetPassword: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token') || '';

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [toastOpen, setToastOpen] = useState(false);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');

    if (!token) {
      setError('Token không hợp lệ.');
      return;
    }

    if (!newPassword || !confirmPassword) {
      setError('Vui lòng điền đủ thông tin.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.');
      return;
    }

    setLoading(true);
    try {
      await api.post('/api/auth/reset-password', {
        token,
        newPassword,
        confirmPassword,
      });
      setToastOpen(true);
      setTimeout(() => {
        setLoading(false);
        navigate('/login');
      }, 1400);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Đặt lại mật khẩu thất bại.');
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-card" style={{ maxWidth: '420px' }}>
        <h2 style={{ fontSize: '32px', marginBottom: '8px' }}>Đặt lại mật khẩu</h2>
        <p style={{ color: '#606770', marginBottom: '18px' }}>Nhập mật khẩu mới để hoàn tất.</p>

        <form onSubmit={handleSubmit}>
          <input
            name="newPassword"
            type="password"
            className="form-input"
            placeholder="Mật khẩu mới"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            required
          />
          <input
            name="confirmPassword"
            type="password"
            className="form-input"
            placeholder="Xác nhận mật khẩu"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
          />

          {error && (
            <Typography color="error" variant="body2" sx={{ marginTop: '8px' }}>
              {error}
            </Typography>
          )}

          <button type="submit" className="btn-register-new" style={{ width: '100%', marginTop: '18px' }} disabled={loading}>
            {loading ? 'Đang cập nhật...' : 'Cập nhật mật khẩu'}
          </button>
        </form>

        <Snackbar
          open={toastOpen}
          autoHideDuration={3000}
          onClose={() => setToastOpen(false)}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
        >
          <Alert severity="success" sx={{ width: '100%' }}>
            Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại.
          </Alert>
        </Snackbar>
      </div>
    </div>
  );
};

export default ResetPassword;
