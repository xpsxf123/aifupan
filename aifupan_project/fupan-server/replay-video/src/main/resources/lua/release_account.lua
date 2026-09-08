--[[
热搜邮箱账号释放 Lua 脚本
作者: RayChou
日期: 2025-11-28
描述: 释放账号，可用次数 +1，检查是否所有IP都释放完毕，完全释放时重新加入对应的池

参数:
  KEYS[1]: 账号 Hash key
  KEYS[2]: 全局池 ZSet key
  KEYS[3]: 同城池 key 前缀
  ARGV[1]: accountId
  ARGV[2]: 当前时间戳（毫秒）
  ARGV[3]: maxConcurrentUsers（最大并发数）
  ARGV[4]: TTL 秒数（7天）

返回值:
  成功: "OK"
  失败: nil
--]]

-- 1. 从账号 Hash 中获取账号详情
local accountJson = redis.call('HGET', KEYS[1], ARGV[1])
if not accountJson then
    return nil
end

-- 2. 解析账号详情
local account = cjson.decode(accountJson)
local maxConcurrentUsers = tonumber(ARGV[3])
local currentTime = tonumber(ARGV[2])
local ttl = ARGV[4] and ARGV[4] ~= '' and tonumber(ARGV[4]) or nil

-- 3. 可用次数 +1
account.availableCount = account.availableCount + 1

-- 4. 确定目标池
local function getTargetPool()
    if account.currentPool and account.currentPool ~= '' then
        return account.currentPool
    elseif account.lastPool and account.lastPool ~= '' then
        return account.lastPool
    elseif account.city and account.city ~= '' then
        return KEYS[3] .. account.city
    else
        return KEYS[2]
    end
end

-- 5. 判断是否所有IP都释放完毕
if account.availableCount < maxConcurrentUsers then
    -- 还有其他IP在使用，重新加入池
    local poolKey = getTargetPool()
    account.currentPool = poolKey

    redis.call('ZADD', poolKey, currentTime, ARGV[1])
    if ttl then
        redis.call('EXPIRE', poolKey, ttl)
    end

    redis.call('HSET', KEYS[1], ARGV[1], cjson.encode(account))
    return 'OK'
end

-- 6. 所有IP都释放了，从当前所在池中删除
if account.currentPool and account.currentPool ~= '' then
    redis.call('ZREM', account.currentPool, ARGV[1])
    -- 清空城市
    account.city = ''
end

-- 7. 判断加入哪个池
local poolKey
if account.city and account.city ~= '' then
    poolKey = KEYS[3] .. account.city
    redis.call('ZREM', KEYS[2], ARGV[1])
else
    poolKey = KEYS[2]
end

-- 8. 加入对应的池并设置过期时间
redis.call('ZADD', poolKey, currentTime, ARGV[1])
if ttl then
    redis.call('EXPIRE', poolKey, ttl)
end

-- 9. 更新账号状态
account.currentPool = poolKey
account.lastPool = nil

-- 10. 更新账号 Hash
redis.call('HSET', KEYS[1], ARGV[1], cjson.encode(account))

return 'OK'

