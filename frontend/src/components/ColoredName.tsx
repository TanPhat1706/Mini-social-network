import React from 'react';
import '../assets/css/AvatarFrames.css'; 
import { useProfileNavigation } from '../hooks/useProfileNavigation';

interface ColoredNameProps {
    name?: string | null;
    colorClass?: string | null;
    studentCode?: string; // optional mã sinh viên để điều hướng
}

const ColoredName: React.FC<ColoredNameProps> = ({ name, colorClass, studentCode }) => {
    const navigateToProfile = useProfileNavigation();

    if (!name) return null;

    const handleClick = (e: React.MouseEvent) => {
        e.stopPropagation();
        navigateToProfile(studentCode);
    };

    const baseStyle: React.CSSProperties = studentCode ? { cursor: 'pointer', textDecoration: 'none' } : {};

    // 🟢 THÊM XỬ LÝ SỰ KIỆN BÀN PHÍM VÀ ROLE BUTTON CHO SONARCLOUD
    if (colorClass) {
        return (
            <span 
                className={colorClass} 
                style={baseStyle} 
                onClick={studentCode ? handleClick : undefined}
                role={studentCode ? 'button' : undefined}
                tabIndex={studentCode ? 0 : undefined}
                onKeyDown={studentCode ? (e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        handleClick(e as any);
                    }
                } : undefined}
            >
                {name}
            </span>
        );
    }

    return (
        <span 
            style={baseStyle} 
            onClick={studentCode ? handleClick : undefined}
            role={studentCode ? 'button' : undefined}
            tabIndex={studentCode ? 0 : undefined}
            onKeyDown={studentCode ? (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    handleClick(e as any);
                }
            } : undefined}
        >
            {name}
        </span>
    );
};

export default ColoredName;