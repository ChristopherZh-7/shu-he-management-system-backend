-- =============================================================================
-- 钉钉扫码仍显示「芋道」：社交登录实际使用的 AppKey 以本表为准，会覆盖 application.yaml
-- 见 SocialClientServiceImpl#buildAuthRequest：先读 yaml，再用 DB 覆盖 clientId/clientSecret
-- =============================================================================
-- 执行前请确认：钉钉开放平台里 AppKey=dingt49strlwu53ezfbj 对应的 AppSecret
-- 若与 system_dingtalk_config 中同 AppKey 的密钥一致，可从该表复制 client_secret
-- =============================================================================

UPDATE system_social_client
SET client_id = 'dingt49strlwu53ezfbj',
    client_secret = 'REPLACE_WITH_YOUR_APP_SECRET',
    updater = '1',
    update_time = NOW()
WHERE social_type = 20   -- DINGTALK
  AND user_type = 2      -- 管理端 Admin
  AND deleted = 0;

-- 验证
-- SELECT id, name, social_type, user_type, client_id, LEFT(client_secret, 8) AS secret_prefix, status
-- FROM system_social_client WHERE social_type = 20 AND deleted = 0;
