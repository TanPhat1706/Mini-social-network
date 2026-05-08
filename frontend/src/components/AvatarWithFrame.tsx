import React from 'react';
import '../assets/css/AvatarFrames.css';
import { getApiBaseUrl } from '../config/apiBase';

interface AvatarProps {
    src?: string | null;
    name?: string | null;
    frameClass?: string | null;
    size?: number; 
}

const AvatarWithFrame: React.FC<AvatarProps> = ({ 
    src, 
    name: _name,
    frameClass, 
    size = 50 
}) => {
    const API_BASE = getApiBaseUrl();
    const DEFAULT_AVATAR_IMAGE =
        'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="160" height="160" viewBox="0 0 160 160"><rect width="160" height="160" fill="%23eef2f7"/><circle cx="80" cy="62" r="30" fill="%2394a3b8"/><path d="M26 144c5-25 28-42 54-42s49 17 54 42" fill="%2394a3b8"/></svg>';

    const resolveAvatarUrl = () => {
        const normalizedSrc = src?.trim();
        if (!normalizedSrc) return DEFAULT_AVATAR_IMAGE;
        if (/^(https?:|data:|blob:)/i.test(normalizedSrc)) return normalizedSrc;
        if (normalizedSrc.startsWith('/')) return `${API_BASE}${normalizedSrc}`;
        return `${API_BASE}/${normalizedSrc}`;
    };

    return (
        <div 
            className="avatar-wrapper" 
            style={{ width: size, height: size }}
        >
            {frameClass && (
                <div className={frameClass}></div>
            )}

            <img 
                src={resolveAvatarUrl()}
                alt="User Avatar"
                className="avatar-image"
                style={{ 
                    width: '100%', 
                    height: '100%',
                    padding: frameClass === 'css-frame-golden-glow' ? '4px' : '0px'
                }}
                onError={(e) => {
                    e.currentTarget.src = DEFAULT_AVATAR_IMAGE;
                }}
            />
        </div>
    );
};

export default AvatarWithFrame;