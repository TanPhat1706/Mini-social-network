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

export const showDevelopmentAlert = () => {
  Swal.fire({
    icon: 'info',
    title: 'Tính năng đang phát triển',
    text: 'Chức năng này đang trong quá trình hoàn thiện và sẽ sớm ra mắt. Cảm ơn bạn đã kiên nhẫn!',
    confirmButtonText: 'Đã hiểu',
    confirmButtonColor: '#1877F2',
    showClass: {
      popup: 'animate__animated animate__fadeInDown animate__faster'
    },
    hideClass: {
      popup: 'animate__animated animate__fadeOutUp animate__faster'
    }
  });
};
