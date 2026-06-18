import React from 'react';
import '../assets/css/AvatarFrames.css'; // Mượn luôn file CSS của viền Avatar để chứa code màu tên
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

    // Nếu user có mua màu, thẻ <span> sẽ nhận class màu. Nếu không, nó là text bình thường.
    if (colorClass) {
        return <span className={colorClass} style={baseStyle} onClick={studentCode ? handleClick : undefined}>{name}</span>;
    }

    return <span style={baseStyle} onClick={studentCode ? handleClick : undefined}>{name}</span>;
};

export default ColoredName;