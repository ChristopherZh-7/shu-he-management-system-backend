#!/bin/bash
# 清除权限相关 Redis 缓存，使数据库中的 role_menu 变更生效
# 执行方式：在服务器上运行，或 ssh shkj@10.40.88.38 'bash -s' < sql/clear_permission_cache.sh
# Redis 配置需与 application-prod.yaml 一致

REDIS_HOST="${REDIS_HOST:-10.40.88.37}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASS="${REDIS_PASS:-Redis@2026!}"

echo "Clearing permission cache (menu_role_ids, permission_menu_ids, user_role_ids:226)..."
redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASS" --no-auth-warning 2>/dev/null << 'EOF'
EVAL "
-- 匹配可能带前缀的 key（Spring Cache 可能加 prefix）
local patterns = {'*menu_role_ids*', '*permission_menu_ids*', '*user_role_ids*'}
for _, p in ipairs(patterns) do
  local keys = redis.call('KEYS', p)
  for i=1,#keys do redis.call('DEL', keys[i]) end
end
return 'OK'
" 0
EOF
echo "Done. 请让詹裕文(226)重新登录后测试。"
exit 0
