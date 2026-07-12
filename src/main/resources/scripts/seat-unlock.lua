-- KEYS: 풀 좌석 lock key들 (hold:screening:{id}:seat:{id})
-- ARGV[1]: lock value (예: "member:123") - 이 값이 저장된 값과 같을 때만 삭제(compare-and-delete)
for _, key in ipairs(KEYS) do
    if redis.call('get', key) == ARGV[1] then
        redis.call('del', key)
    end
end
return 1
