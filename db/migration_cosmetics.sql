USE MiniSocialDB;
GO

-- 1. Thêm các cột mới vào bảng items (Cho phép NULL tạm thời để update)
ALTER TABLE items ADD 
    code VARCHAR(50),
    theme VARCHAR(50),
    rarity VARCHAR(50),
    collection_code VARCHAR(50),
    effect_key VARCHAR(100),
    display_order INT DEFAULT 0 NOT NULL,
    event_tag VARCHAR(50);
GO

-- 2. Cập nhật dữ liệu cũ (Map image_url thành effect_key và gán hệ)
-- Viền Rắn Lục
UPDATE items SET code = 'frame_snake_green', theme = 'NATURE', rarity = 'COMMON', effect_key = 'css-frame-snake-green', type = 'AVATAR_FRAME', display_order = 10 
WHERE name = N'Viền Rắn Lục';

-- Viền Rắn Hoàng Kim
UPDATE items SET code = 'frame_golden_snake', theme = 'HOLY', rarity = 'LEGENDARY', effect_key = 'css-frame-golden-snake', type = 'AVATAR_FRAME', display_order = 50 
WHERE name = N'Viền Rắn Hoàng Kim';

-- Tên Tím Neon
UPDATE items SET code = 'name_neon_purple', theme = 'CYBER', rarity = 'RARE', effect_key = 'css-color-neon-purple', type = 'NAME_COLOR', display_order = 20 
WHERE name = N'Tên màu Tím Neon';

-- Tên Hoàng Kim
UPDATE items SET code = 'name_golden', theme = 'HOLY', rarity = 'RARE', effect_key = 'css-color-golden', type = 'NAME_COLOR', display_order = 21 
WHERE name = N'Màu tên Hoàng Kim';

-- Viền Dung Nham
UPDATE items SET code = 'frame_magma_fire', theme = 'FIRE', rarity = 'EPIC', effect_key = 'css-frame-magma-fire', type = 'AVATAR_FRAME', display_order = 30 
WHERE name = N'Viền Dung Nham';

-- Viền Dải Ngân Hà
UPDATE items SET code = 'frame_cosmic_galaxy', theme = 'COSMIC', rarity = 'LEGENDARY', collection_code = 'celestial_set', effect_key = 'css-frame-cosmic-galaxy', type = 'AVATAR_FRAME', display_order = 60 
WHERE name = N'Viền Dải Ngân Hà';

-- Tên Đại Dương
UPDATE items SET code = 'name_ocean_blue', theme = 'OCEAN', rarity = 'RARE', effect_key = 'css-color-ocean-blue', type = 'NAME_COLOR', display_order = 22 
WHERE name = N'Màu tên Đại Dương';

-- Tên Huyết Nguyệt
UPDATE items SET code = 'name_blood_moon', theme = 'SHADOW', rarity = 'EPIC', effect_key = 'css-color-blood-moon', type = 'NAME_COLOR', display_order = 40 
WHERE name = N'Màu tên Huyết Nguyệt';
GO

-- 3. Xóa các item rác không có mã code (nếu có) để chuẩn bị set NOT NULL
DELETE FROM items WHERE code IS NULL;
GO

-- 4. Ép kiểu dữ liệu an toàn (Set NOT NULL cho các cột nghiệp vụ)
ALTER TABLE items ALTER COLUMN code VARCHAR(50) NOT NULL;
ALTER TABLE items ALTER COLUMN theme VARCHAR(50) NOT NULL;
ALTER TABLE items ALTER COLUMN rarity VARCHAR(50) NOT NULL;
ALTER TABLE items ALTER COLUMN effect_key VARCHAR(100) NOT NULL;
GO

-- 5. Set Unique Key cho code
ALTER TABLE items ADD CONSTRAINT UQ_Item_Code UNIQUE (code);
GO

-- 6. Xóa cột image_url cũ không còn sử dụng
ALTER TABLE items DROP COLUMN image_url;
GO

-- 7. Seed bộ sưu tập mới (Cyber / Neon / Glitch)
INSERT INTO items (code, name, description, type, theme, rarity, collection_code, effect_key, price, active, display_order)
VALUES 
('frame_cyber_pulse', N'Viền Cyber Pulse', N'Nhịp đập của thế giới ngầm', 'AVATAR_FRAME', 'CYBER', 'EPIC', 'cyber_set', 'css-frame-cyber-pulse', 800, 1, 70),
('frame_hologram', N'Viền Hologram', N'Ảnh ảo không gian 3 chiều', 'AVATAR_FRAME', 'CYBER', 'LEGENDARY', 'cyber_set', 'css-frame-hologram', 1200, 1, 71),
('name_matrix', N'Tên Ma Trận', N'Lối thoát khỏi thế giới ảo', 'NAME_COLOR', 'CYBER', 'EPIC', 'cyber_set', 'css-color-matrix', 600, 1, 72);
GO