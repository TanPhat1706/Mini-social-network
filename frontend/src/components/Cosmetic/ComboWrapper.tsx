import React from 'react';
import { detectNationalCombo } from '../../utils/comboDetector';

interface ComboWrapperProps {
  /** effectKey của Avatar Frame (css-frame-wc-X) */
  frameClass?: string | null;
  /** effectKey của Name Color (css-color-wc-X) */
  colorClass?: string | null;
  /** Nội dung bên trong (Avatar + Name) */
  children: React.ReactNode;
  /** Class tuỳ chọn thêm vào wrapper */
  className?: string;
  /** Style tuỳ chọn thêm vào wrapper */
  style?: React.CSSProperties;
}

/**
 * 🥚 ComboWrapper — Easter Egg Combo Container
 *
 * Bọc Avatar + Name lại trong một div duy nhất.
 * Khi phát hiện National Combo hợp lệ, tự động đặt `data-combo="<nation>"`
 * để CSS [data-combo="vietnam"] có thể áp hiệu ứng hào quang.
 *
 * @example
 * <ComboWrapper frameClass={author.currentAvatarFrame} colorClass={author.currentNameColor}>
 *   <AvatarWithFrame ... />
 *   <ColoredName ... />
 * </ComboWrapper>
 */
const ComboWrapper: React.FC<ComboWrapperProps> = ({
  frameClass,
  colorClass,
  children,
  className,
  style,
}) => {
  const combo = detectNationalCombo(frameClass, colorClass);

  return (
    <div
      className={['wc-combo-wrapper', className].filter(Boolean).join(' ')}
      data-combo={combo ?? undefined}
      style={style}
    >
      {children}
    </div>
  );
};

export default ComboWrapper;
