-- 参数说明
-- KEYS[1] = IP限流计数器key（可选，IP不可用时可不传）
-- KEYS[2] = 用户限流计数器key（必传）
-- ARGV[1] = IP限流窗口毫秒数
-- ARGV[2] = IP最大尝试次数
-- ARGV[3] = 用户限流窗口毫秒数
-- ARGV[4] = 用户最大尝试次数

local ipKey = KEYS[1]
local userKey = KEYS[2]

local ipWindowMillis = tonumber(ARGV[1] or '0')
local ipMaxAttempts = tonumber(ARGV[2] or '0')
local userWindowMillis = tonumber(ARGV[3] or '0')
local userMaxAttempts = tonumber(ARGV[4] or '0')

-- 返回码与 Java BaseCode 保持一致
local CODE_SUCCESS = 0
local CODE_IP_EXCEEDED = 10007
local CODE_USER_EXCEEDED = 10008

-- IP 限流（仅在提供了 ipKey 且配置有效时执行）
if ipKey ~= nil and ipKey ~= '' and ipWindowMillis > 0 and ipMaxAttempts > 0 then
    local ipCur = redis.call('INCRBY', ipKey, 1)
    if ipCur == 1 then
        redis.call('PEXPIRE', ipKey, ipWindowMillis)
    end
    if ipCur > ipMaxAttempts then
        return CODE_IP_EXCEEDED
    end
end

-- 用户限流（需提供 userKey 且配置有效）
if userKey ~= nil and userKey ~= '' and userWindowMillis > 0 and userMaxAttempts > 0 then
    local userCur = redis.call('INCRBY', userKey, 1)
    if userCur == 1 then
        redis.call('PEXPIRE', userKey, userWindowMillis)
    end
    if userCur > userMaxAttempts then
        return CODE_USER_EXCEEDED
    end
end

return CODE_SUCCESS