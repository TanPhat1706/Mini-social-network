import React from 'react';
import '../../styles/cosmetics/index.css';

export interface NameRendererProps {
  name?: string | null;
  effectKey?: string | null;
  className?: string;
  style?: React.CSSProperties;
  onClick?: (e: React.MouseEvent) => void;
  onKeyDown?: (e: React.KeyboardEvent) => void;
  role?: React.AriaRole;
  tabIndex?: number;
}

const NameRenderer: React.FC<NameRendererProps> = ({
  name,
  effectKey,
  className,
  style,
  onClick,
  onKeyDown,
  role,
  tabIndex,
}) => {
  if (!name) return null;

  const colorClass = effectKey?.trim() || '';
  const displayClass = ['name-color-base', colorClass, className].filter(Boolean).join(' ');

  return (
    <span
      className={displayClass}
      style={style}
      onClick={onClick}
      onKeyDown={onKeyDown}
      role={role}
      tabIndex={tabIndex}
    >
      {name}
    </span>
  );
};

export default NameRenderer;
