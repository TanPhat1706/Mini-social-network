import Swal from 'sweetalert2';
import 'sweetalert2/dist/sweetalert2.min.css';

export const showSuccess = (message: string, title = 'Thành công') =>
  Swal.fire({
    icon: 'success',
    title,
    text: message,
    timer: 2500,
    showConfirmButton: false,
  });

export const showError = (message: string, title = 'Lỗi') =>
  Swal.fire({
    icon: 'error',
    title,
    text: message,
    confirmButtonText: 'Đóng',
  });

export const showWarning = (message: string, title = 'Chú ý') =>
  Swal.fire({
    icon: 'warning',
    title,
    text: message,
    confirmButtonText: 'Đóng',
  });

export const showInfo = (message: string, title = 'Thông tin') =>
  Swal.fire({
    icon: 'info',
    title,
    text: message,
    timer: 2500,
    showConfirmButton: false,
  });
