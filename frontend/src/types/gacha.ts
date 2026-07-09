import type { CosmeticItem, CosmeticRarity } from './cosmetic';

export type GachaPhase = 'IDLE' | 'FETCHING' | 'ANIMATING' | 'REVEAL';

export interface GachaInfoResponse {
  theme: string;
  totalPoolSize: number;
  ownedCount: number;
  remainingForGuarantee: number;
  currentRates: Record<string, number>;
}

export interface GachaHistoryItem {
  itemId: number;
  name: string;
  type: 'AVATAR_FRAME' | 'NAME_COLOR';
  rarity: CosmeticRarity;
  effectKey: string;
  purchasedAt: string;
}