--[[
热搜邮箱账号分配 Lua 脚本（统一版）
作者: RayChou
日期: 2025-12-05
描述: 统一处理所有分配场景，支持无城市、有城市等各种情况

参数:
  KEYS[1]: 同城池池 key（如果有城市，否则传入空字符串）
  KEYS[2]: 全局池 key
  KEYS[3]: 账号 Hash key
  KEYS[4]: IP 映射 key 前缀
  ARGV[1]: 当前时间戳（毫秒）
  ARGV[2]: 客户端 IP
  ARGV[3]: 客户端城市（有城市则传入，无则传入空字符串）
  ARGV[4]: Redis TTL（秒）

返回值:
  成功: JSON 字符串 {"accountId": "123", "city": "上海", "lastUseTime": 1732780800000, "lastIp": "1.1.1.1", "fromCache": false}
  失败: nil
--]]

-- 1. 检查 IP 映射缓存（1 小时内是否使用过账号）
local ipMappingKey = KEYS[4] .. ARGV[2]
local cachedAccountId = redis.call('GET', ipMappingKey)

if cachedAccountId then
    redis.call('ECHO', '[Lua] IP映射缓存命中: ip=' .. ARGV[2] .. ', accountId=' .. cachedAccountId)
    -- 从账号 Hash 中获取账号详情
    local cachedAccountJson = redis.call('HGET', KEYS[3], cachedAccountId)
    if cachedAccountJson then
        local cachedAccount = cjson.decode(cachedAccountJson)
        redis.call('ECHO', '[Lua] 返回缓存账号: accountId=' .. cachedAccountId .. ', availableCount=' .. cachedAccount.availableCount)
        -- 直接返回缓存账号，不扣减可用次数（因为该 IP 已经在使用中）
        return cjson.encode({
            accountId = cachedAccountId,
            city = cachedAccount.city,
            lastUseTime = cachedAccount.lastUseTime,
            lastIp = cachedAccount.lastIp,
            availableCount = cachedAccount.availableCount,
            fromCache = true
        })
    else
        -- 如果在Hash中找不到该账号，说明该账号已经失效，需要清除IP映射缓存
        redis.call('DEL', ipMappingKey)
        redis.call('ECHO', '[Lua] IP映射缓存中的账号已失效，清除IP映射: ip=' .. ARGV[2] .. ', accountId=' .. cachedAccountId)
        -- 继续执行正常的分配逻辑
    end
end

redis.call('ECHO', '[Lua] 开始分配账号: clientIp=' .. ARGV[2] .. ', city=' .. (ARGV[3] or ''))

local accountId = nil
local sourcePool = nil
local shouldMigrate = false
local cityPoolKey = KEYS[1]
local hasCityPool = cityPoolKey and cityPoolKey ~= '' and cityPoolKey ~= 'nil'

-- 2. 如果有同城池池key，先尝试从同城池池获取
if hasCityPool then
    redis.call('ECHO', '[Lua] 尝试从同城池池获取: cityPoolKey=' .. cityPoolKey)
    local cityAccountIds = redis.call('ZRANGE', cityPoolKey, 0, 0)

    if cityAccountIds and #cityAccountIds > 0 then
        accountId = cityAccountIds[1]
        sourcePool = cityPoolKey
        redis.call('ECHO', '[Lua] 从同城池池获取到账号: accountId=' .. accountId .. ', poolKey=' .. sourcePool)
    end
end

-- 3. 如果同城池池没有或没有同城池池key，从全局池获取
if not accountId then
    redis.call('ECHO', '[Lua] 尝试从全局池获取: globalPoolKey=' .. KEYS[2])
    local globalAccountIds = redis.call('ZRANGE', KEYS[2], 0, 0)

    if globalAccountIds and #globalAccountIds > 0 then
        accountId = globalAccountIds[1]
        sourcePool = KEYS[2]
        -- 如果有同城池池key，并且客户端城市不为空，才需要迁移
        shouldMigrate = hasCityPool and ARGV[3] and ARGV[3] ~= ''
        redis.call('ECHO', '[Lua] 从全局池获取到账号: accountId=' .. accountId .. ', poolKey=' .. sourcePool .. ', shouldMigrate=' .. tostring(shouldMigrate))
    end
end

-- 4. 如果都没有账号，返回nil
if not accountId then
    redis.call('ECHO', '[Lua] 同城池池和全局池都为空')
    return nil
end

-- 5. 从账号 Hash 中获取账号详情
local accountJson = redis.call('HGET', KEYS[3], accountId)
if not accountJson then
    redis.call('ECHO', '[Lua] 账号Hash不存在: accountId=' .. accountId)
    -- 账号不存在，从源池中移除
    if sourcePool then
        redis.call('ZREM', sourcePool, accountId)
    end
    return nil
end

-- 6. 解析账号详情
local account = cjson.decode(accountJson)
redis.call('ECHO', '[Lua] 账号详情: accountId=' .. accountId .. ', availableCount=' .. account.availableCount)

-- 7. 检查可用次数
if account.availableCount <= 0 then
    redis.call('ECHO', '[Lua] 可用次数不足: accountId=' .. accountId .. ', availableCount=' .. account.availableCount)
    if sourcePool then
        redis.call('ZREM', sourcePool, accountId)
    end
    return nil
end

-- 8. 可用次数 -1，更新时间和IP
local currentTime = tonumber(ARGV[1])
local oldAvailableCount = account.availableCount
account.availableCount = account.availableCount - 1
account.lastUseTime = currentTime
account.lastIp = ARGV[2]
if account.city == '' or account.city == 'nil' then
    account.city = ARGV[3]
end

redis.call('ECHO', '[Lua] 扣减可用次数: accountId=' .. accountId .. ', ' .. oldAvailableCount .. ' -> ' .. account.availableCount)

-- 9. 判断如何处理账号（迁移、留在当前池、或从池中移除）
local finalPoolKey = sourcePool

if account.availableCount <= 0 then
    -- 可用次数耗尽，从池中移除
    redis.call('ECHO', '[Lua] 可用次数耗尽，从池中移除: accountId=' .. accountId .. ', poolKey=' .. sourcePool)
    redis.call('ZREM', sourcePool, accountId)
    account.lastPool = sourcePool
    account.currentPool = nil

elseif shouldMigrate then
    -- 从全局池迁移到同城池池
    redis.call('ECHO', '[Lua] 迁移到同城池: accountId=' .. accountId .. ', from=' .. sourcePool .. ', to=' .. cityPoolKey)
    redis.call('ZREM', sourcePool, accountId)
    redis.call('ZADD', cityPoolKey, currentTime, accountId)
    if ARGV[4] and ARGV[4] ~= '' then
        redis.call('EXPIRE', cityPoolKey, tonumber(ARGV[4]))
    end
    finalPoolKey = cityPoolKey
    account.currentPool = finalPoolKey

else
    -- 留在当前池，更新 score
    redis.call('ECHO', '[Lua] 留在当前池: accountId=' .. accountId .. ', poolKey=' .. sourcePool)
    redis.call('ZADD', sourcePool, currentTime, accountId)
    account.currentPool = sourcePool
end

-- 10. 更新账号 Hash
redis.call('HSET', KEYS[3], accountId, cjson.encode(account))

-- 11. 设置 IP 映射（1 小时 = 3600 秒）
redis.call('SETEX', ipMappingKey, 3600, accountId)

redis.call('ECHO', '[Lua] 分配成功: accountId=' .. accountId .. ', availableCount=' .. account.availableCount .. ', currentPool=' .. (account.currentPool or 'nil'))

-- 12. 返回账号信息
return cjson.encode({
    accountId = accountId,
    city = account.city,
    lastUseTime = account.lastUseTime,
    lastIp = account.lastIp,
    availableCount = account.availableCount,
    currentPool = account.currentPool,
    fromCache = false
})
