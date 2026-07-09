export type CosmeticType = 'AVATAR_FRAME' | 'NAME_COLOR';

export type CosmeticTheme =
  | 'NATURE'
  | 'FIRE'
  | 'OCEAN'
  | 'THUNDER'
  | 'SHADOW'
  | 'HOLY'
  | 'COSMIC'
  | 'CYBER'
  | 'ROYAL'
  | 'ICE'
  | 'VOID'
  | 'SUMMER'
  | 'WORLDCUP'; 

export type CosmeticRarity = 'COMMON' | 'RARE' | 'EPIC' | 'LEGENDARY' | 'MYTHIC';

export interface CosmeticItem {
  id: number;
  code: string;
  name: string;
  type: CosmeticType;
  theme: CosmeticTheme;
  rarity: CosmeticRarity;
  effectKey: string;
  price: number;
  description?: string;
  displayOrder?: number;
  active?: boolean;
}

export const COSMETIC_THEMES: CosmeticTheme[] = [
  'NATURE',
  'FIRE',
  'OCEAN',
  'THUNDER',
  'SHADOW',
  'HOLY',
  'COSMIC',
  'CYBER',
  'ROYAL',
  'ICE',
  'VOID',
  'SUMMER', // <-- Bổ sung vào mảng để dùng trong Filter
  'WORLDCUP',
];

export const COSMETIC_RARITIES: CosmeticRarity[] = [
  'COMMON',
  'RARE',
  'EPIC',
  'LEGENDARY',
  'MYTHIC',
];

export const RARITY_LABELS: Record<CosmeticRarity, string> = {
  COMMON: 'Thường',
  RARE: 'Hiếm',
  EPIC: 'Sử thi',
  LEGENDARY: 'Huyền thoại',
  MYTHIC: 'Thần thoại',
};

export const THEME_LABELS: Record<CosmeticTheme, string> = {
  NATURE: 'Thiên nhiên',
  FIRE: 'Lửa',
  OCEAN: 'Đại dương',
  THUNDER: 'Sấm sét',
  SHADOW: 'Bóng tối',
  HOLY: 'Thánh quang',
  COSMIC: 'Vũ trụ',
  CYBER: 'Cyber',
  ROYAL: 'Hoàng gia',
  ICE: 'Băng giá',
  VOID: 'Hư không',
  SUMMER: 'Mùa Hè',
  WORLDCUP: 'WORLD CUP 2026',
};