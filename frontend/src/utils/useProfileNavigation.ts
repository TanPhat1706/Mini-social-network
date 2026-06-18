import { useNavigate } from 'react-router-dom';

export const useProfileNavigation = () => {
  const navigate = useNavigate();

  const navigateToProfile = (studentCode?: string) => {
    if (studentCode) {
      // Nếu có truyền studentCode -> Vào trang của người khác
      navigate(`/profile/${studentCode}`);
    } else {
      // Nếu không truyền -> Vào trang của chính mình
      navigate('/profile');
    }
  };

  return navigateToProfile;
};