import React from 'react';
import AvatarRenderer from './Cosmetic/AvatarRenderer';

interface AvatarProps {
  src?: string | null;
  name?: string | null;
  frameClass?: string | null;
  size?: number;
  className?: string;
  onClick?: (e: React.MouseEvent) => void;
}

const AvatarWithFrame: React.FC<AvatarProps> = ({
  src,
  name: _name,
  frameClass,
  size = 50,
  className,
  onClick,
}) => (
  <AvatarRenderer
    src={src}
    effectKey={frameClass}
    size={size}
    className={className}
    onClick={onClick}
  />
);

export default AvatarWithFrame;
