import React from 'react';
import { getApiBaseUrl } from '../../config/apiBase';
import '../../styles/cosmetics/index.css';

const DEFAULT_AVATAR_IMAGE =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="160" height="160" viewBox="0 0 160 160"><rect width="160" height="160" fill="%23eef2f7"/><circle cx="80" cy="62" r="30" fill="%2394a3b8"/><path d="M26 144c5-25 28-42 54-42s49 17 54 42" fill="%2394a3b8"/></svg>';

const HOLE_FRAMES = new Set([
  // Original hole frames
  'css-frame-golden-glow',
  'css-frame-magma-fire',
  'css-frame-cosmic-galaxy',
  'css-frame-ice-crystal',
  'css-frame-electric-storm',
  'css-frame-shadow-dragon',
  'css-frame-divine-halo',
  'css-frame-sakura-bloom',
  // New hole frames — VOID / COSMIC / CELESTIAL
  'css-frame-void-abyss',
  'css-frame-eclipse-solar',
  'css-frame-nebula-star',
  'css-frame-aurora-borealis',
  'css-frame-blood-eclipse',
  // New hole frames — HOLY / ROYAL
  'css-frame-angelic-halo',
  'css-frame-holy-radiance',
  'css-frame-platinum-royal',
  'css-frame-emerald-crown',
  // New hole frames — CYBER
  'css-frame-hologram-shift',
  'css-frame-glitch-core',
  // New hole frames — FIRE
  'css-frame-phoenix-flare',
  'css-frame-blazing-feather',
  'css-frame-dragon-flame',
  // New hole frames — ICE / THUNDER / OCEAN
  'css-frame-frost-crystal',
  'css-frame-thunder-storm',
  'css-frame-tidal-wave',
  // New hole frames — SUMMER EVENT (Mythic & Legendary)
  'css-frame-smr-ocean-mythic',
  'css-frame-smr-phoenix-sun',
  'css-frame-smr-sunlight',
  'css-frame-smr-gold-beach',
  'css-frame-smr-sand-party',
  'css-frame-smr-water-art',
  'css-frame-smr-solar-mythic'
]);

export interface AvatarRendererProps {
  src?: string | null;
  effectKey?: string | null;
  size?: number;
  className?: string;
  alt?: string;
  onClick?: (e: React.MouseEvent) => void;
}

function resolveAvatarUrl(src?: string | null): string {
  const normalizedSrc = src?.trim();
  if (!normalizedSrc) return DEFAULT_AVATAR_IMAGE;
  if (/^(https?:|data:|blob:)/i.test(normalizedSrc)) return normalizedSrc;
  const apiBase = getApiBaseUrl();
  if (normalizedSrc.startsWith('/')) return `${apiBase}${normalizedSrc}`;
  return `${apiBase}/${normalizedSrc}`;
}

const AvatarRenderer: React.FC<AvatarRendererProps> = ({
  src,
  effectKey,
  size = 50,
  className,
  alt = 'User Avatar',
  onClick,
}) => {
  const frameClass = effectKey?.trim() || '';
  const hasHoleFrame = HOLE_FRAMES.has(frameClass);
  const shouldShowPointer = Boolean(onClick) || className?.includes('hoverable-avatar');
  const wrapperClass = ['avatar-wrapper', className].filter(Boolean).join(' ');
  return (
    <div
      className={wrapperClass}
      style={{ width: size, height: size, cursor: shouldShowPointer ? 'pointer' : 'default', overflow: 'visible' }}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      onKeyDown={
        onClick
          ? (e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onClick(e as unknown as React.MouseEvent);
              }
            }
          : undefined
      }
    >
      <div className="avatar-frame-base">
        {frameClass && <div className={frameClass} aria-hidden="true" />}
        <img
          src={resolveAvatarUrl(src)}
          alt={alt}
          className="avatar-image"
          style={{ padding: hasHoleFrame ? '4px' : '0px' }}
          onError={(e) => { e.currentTarget.src = DEFAULT_AVATAR_IMAGE; }}
        />
      </div>
    </div>
  );
};

export default AvatarRenderer;