INSERT INTO template_category (id, code, name, sort_order, enabled)
VALUES
    (3, 'PETS', '萌宠写真', 30, 1),
    (4, 'WEDDING', '婚礼纪念', 40, 1),
    (5, 'HOLIDAY', '节日时刻', 50, 1),
    (6, 'PORTRAIT', '人像风格', 60, 1);

UPDATE material_template
SET cover_url = '/images/templates/photo/tropical-beach.jpg',
    result_url = '/images/templates/photo/tropical-beach.jpg',
    title = '海岛旅行明信片',
    description = '使用一张旅行照片制作清新明亮的海岛纪念卡片。',
    updated_at = UTC_TIMESTAMP(3)
WHERE id = 'travel-postcard';

UPDATE material_template
SET cover_url = '/images/templates/photo/mountain-traveler.jpg',
    result_url = '/images/templates/photo/mountain-traveler.jpg',
    title = '山野旅行拼贴',
    description = '使用两张照片记录山野旅途与沿途风景。',
    updated_at = UTC_TIMESTAMP(3)
WHERE id = 'travel-collage';

UPDATE material_template
SET cover_url = '/images/templates/photo/friends-cafe.jpg',
    result_url = '/images/templates/photo/friends-cafe.jpg',
    title = '好友咖啡日记',
    description = '使用三张照片制作适合社交分享的生活故事卡片。',
    updated_at = UTC_TIMESTAMP(3)
WHERE id = 'social-story';

INSERT INTO material_template (
    id, category_id, title, description, cover_url, result_url,
    required_image_count, accepted_mime_types, max_file_size_bytes,
    enabled, sort_order, created_at, updated_at
)
VALUES
    ('pet-dog-portrait', 3, '奶油系萌宠写真', '上传一张宠物照片，制作温暖自然的萌宠写真。',
     '/images/templates/photo/pet-dog.jpg', '/images/templates/photo/pet-dog.jpg',
     1, JSON_ARRAY('image/jpeg', 'image/png'), 10485760, 1, 10, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
    ('pet-cat-story', 3, '猫咪生活故事', '使用两张猫咪照片，记录安静又可爱的生活瞬间。',
     '/images/templates/photo/pet-cat.jpg', '/images/templates/photo/pet-cat.jpg',
     2, JSON_ARRAY('image/jpeg', 'image/png'), 10485760, 1, 20, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
    ('wedding-memory', 4, '黑白婚礼纪念', '使用两张照片制作经典克制的婚礼纪念作品。',
     '/images/templates/photo/wedding.jpg', '/images/templates/photo/wedding.jpg',
     2, JSON_ARRAY('image/jpeg', 'image/png'), 10485760, 1, 10, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
    ('holiday-family', 5, '圣诞家庭合影', '使用三张家庭照片制作温暖热闹的节日纪念卡。',
     '/images/templates/photo/holiday-family.jpg', '/images/templates/photo/holiday-family.jpg',
     3, JSON_ARRAY('image/jpeg', 'image/png'), 10485760, 1, 10, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
    ('urban-portrait', 6, '城市氛围人像', '上传一张人像照片，打造具有都市电影感的视觉作品。',
     '/images/templates/photo/fashion-portrait.jpg', '/images/templates/photo/fashion-portrait.jpg',
     1, JSON_ARRAY('image/jpeg', 'image/png'), 10485760, 1, 10, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3));
