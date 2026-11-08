INSERT INTO app_user (id, username, display_name, password_hash, status, created_at, updated_at)
VALUES
    (1, 'demo_user', '演示用户', '$2a$10$dRvsja/We0MehL8m8VB5Lek2JIIP/K11S9dPcULN8ixEq9v5oCSPW', 'ACTIVE', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
    (2, 'demo_user_b', '演示用户B', '$2a$10$dCY1MByK.ZwCnFt9lkDGlu1igx4Ze96CRsH7qSR7WVbvUquutWpPa', 'ACTIVE', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3));

INSERT INTO template_category (id, code, name, sort_order, enabled)
VALUES
    (1, 'TRAVEL', '旅行纪念', 10, 1),
    (2, 'SOCIAL', '社交分享', 20, 1);

INSERT INTO material_template (
    id, category_id, title, description, cover_url, result_url,
    required_image_count, accepted_mime_types, max_file_size_bytes,
    enabled, sort_order, created_at, updated_at
)
VALUES
    ('travel-postcard', 1, '旅行明信片', '使用一张旅行照片制作简洁的纪念明信片。',
     '/images/templates/travel-postcard.svg', '/images/results/travel-postcard-result.svg',
     1, JSON_ARRAY('image/jpeg', 'image/png'), 10485760, 1, 10, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
    ('travel-collage', 1, '双图旅行拼贴', '使用两张照片生成左右布局的旅行拼贴。',
     '/images/templates/travel-collage.svg', '/images/results/travel-collage-result.svg',
     2, JSON_ARRAY('image/jpeg', 'image/png'), 10485760, 1, 20, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3)),
    ('social-story', 2, '三图社交故事', '使用三张照片生成适合社交分享的故事卡片。',
     '/images/templates/social-story.svg', '/images/results/social-story-result.svg',
     3, JSON_ARRAY('image/jpeg', 'image/png'), 10485760, 1, 10, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3));
