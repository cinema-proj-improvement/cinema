-- KEYS: 잠글 좌석 lock key들 (hold:screening:{id}:seat:{id})
-- ARGV[1]: lock value (예: "member:123")
-- ARGV[2]: TTL(ms)
-- 반환: 1 = 전부 성공, 0 = 하나라도 실패(이 실행 안에서 잠갔던 것들은 롤백됨)
local locked = {}
for i, key in ipairs(KEYS) do
    local ok = redis.call('set', key, ARGV[1], 'NX', 'PX', ARGV[2])
    if not ok then
        for _, k in ipairs(locked) do
            redis.call('del', k)
        end
        return 0
    end
    table.insert(locked, key)
end
return 1
