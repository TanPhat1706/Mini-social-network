import React from 'react';
import NameRenderer from './Cosmetic/NameRenderer';
import { useProfileNavigation } from '../hooks/useProfileNavigation';

interface ColoredNameProps {
  name?: string | null;
  colorClass?: string | null;
  studentCode?: string;
}

const ColoredName: React.FC<ColoredNameProps> = ({ name, colorClass, studentCode }) => {
  const navigateToProfile = useProfileNavigation();

  const handleClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    navigateToProfile(studentCode);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleClick(e as unknown as React.MouseEvent);
    }
  };

  const interactiveStyle: React.CSSProperties | undefined = studentCode
    ? { cursor: 'pointer', textDecoration: 'none' }
    : undefined;

  return (
    <NameRenderer
      name={name}
      effectKey={colorClass}
      style={interactiveStyle}
      onClick={studentCode ? handleClick : undefined}
      onKeyDown={studentCode ? handleKeyDown : undefined}
      role={studentCode ? 'button' : undefined}
      tabIndex={studentCode ? 0 : undefined}
    />
  );
};

export default ColoredName;
