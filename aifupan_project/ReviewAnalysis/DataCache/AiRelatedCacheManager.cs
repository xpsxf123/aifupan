using ReviewAnalysis.Ai;
using ReviewAnalysis.Ai.Model;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Dto;
using Swan.Parsers;
using System;
using System.Collections.Concurrent;
using System.IO;
using System.Runtime.Caching;
using douyin.Utils;

namespace ReviewAnalysis.DataCache
{
    public class AiRelatedCacheManager
    {

        public static ConcurrentDictionary<string, AIRelatedCacheDto> aiRelatedCacheDictionary = new ConcurrentDictionary<string, AIRelatedCacheDto>();

        public static string cacheKey = "replay:ai:context:";
        public static string tempCacheKey = "replay:ai:tempToken";
        public static string likesCacheKey = "replay:ai:likes";
        public static string deepseekKey = "replay:ai:contextId:";
        private static ObjectCache cache = MemoryCache.Default;

        /// <summary>
        /// 获取ai接口的临时token
        /// </summary>
        /// <returns></returns>
        public static AiTempTokenDto GetTempToken()
        {
            if (!(cache[tempCacheKey] is AiTempTokenDto token))
            {
                token = AiUtils.getTempArkToekn();

                // 添加一个缓存项，设置绝对过期时间为 10 秒后，并指定回调
                CacheItemPolicy policy = new CacheItemPolicy();
                policy.AbsoluteExpiration = DateTimeOffset.Now.AddSeconds(token.tempTokenSaveTime);

                cache.Set(new CacheItem(tempCacheKey, token), policy);
            }
            return token;
        }

        /// <summary>
        /// 设置点赞key
        /// </summary>
        public static void setLikesKey()
        {
            // 添加一个缓存项，设置绝对过期时间为 1 秒后，并指定回调
            CacheItemPolicy policy = new CacheItemPolicy();
            policy.AbsoluteExpiration = DateTimeOffset.Now.AddSeconds(1);

            cache.Set(new CacheItem(likesCacheKey, 1), policy);
        }

        /// <summary>
        /// 判断是否能点赞
        /// </summary>
        /// <returns></returns>
        public static bool HasLikes()
        {
            return cache.Contains(likesCacheKey);
        }

        /// <summary>
        /// 设置ai会话缓存
        /// </summary>
        /// <param name="dto"></param>
        public static void SetAiContextCache(AIRelatedCacheDto dto, bool expiration = true)
        {
            CacheItemPolicy policy = new CacheItemPolicy()
            {
                AbsoluteExpiration = DateTimeOffset.Now.AddSeconds(GetTempToken().contextSaveTime),
                RemovedCallback = OnContextCacheRemoved
            };
            if (expiration)
            {
                CacheItem cacheItem = cache.GetCacheItem(cacheKey + dto.contextId);
                if(cacheItem != null)
                {
                    cache.Set(cacheItem, policy);
                }
                else
                {
                    cache.Set(cacheKey + dto.contextId, dto, policy);
                }
            }
            else
            {
                CacheItem cacheItem = cache.GetCacheItem(cacheKey + dto.contextId);
                if(cacheItem == null)
                {
                    //cache.AddOrGetExisting(cacheKey + dto.contextId, dto, policy);
                }
                else
                {
                    if(cacheItem.Value is AIRelatedCacheDto temp)
                    {
                        temp.giveStatuc = dto.giveStatuc;
                    }
                }
            }
        }

        /// <summary>
        /// 获取缓存的ai会话缓存
        /// </summary>
        /// <param name="key"></param>
        /// <returns></returns>
        public static AIRelatedCacheDto GetAiContextCache(string key)
        {
            if(cache.GetValues(cacheKey + key) is AIRelatedCacheDto temp)
            {
                return temp;
            }
            return null;
        }

        /// <summary>
        /// 获取ai会话缓存
        /// </summary>
        /// <param name="key"></param>
        /// <param name="resetExpiration"></param>
        /// <returns></returns>
        public static AIRelatedCacheDto GetContextCache(string key, bool resetExpiration = true)
        {
            if (!(cache[tempCacheKey] is AIRelatedCacheDto value))
            {
                value = null;
            }
            if(value == null)
            {
                // 获取缓存项 
                CacheItem cacheItem = cache.GetCacheItem(key);
                // 如果需要重置过期时间，则更新策略
                // 添加一个缓存项，设置绝对过期时间为 10 秒后，并指定回调
                CacheItemPolicy policy = new CacheItemPolicy()
                {
                    AbsoluteExpiration = DateTimeOffset.Now.AddSeconds(GetTempToken().contextSaveTime),
                    RemovedCallback = OnContextCacheRemoved
                };

                cache.Set(cacheItem, policy);
            }
            return value;
        }

        /// <summary>
        /// 定义独立的回调方法
        /// </summary>
        /// <param name="arguments"></param>
        private static void OnContextCacheRemoved(CacheEntryRemovedArguments arguments)
        {
            // 获取被移除的缓存项
            CacheItem removedItem = arguments.CacheItem;

            // 输出 Key 和 Value
            FileUtils.log($"缓存项已移除，Key: {removedItem.Key}, Value: {removedItem.Value}");
            FileUtils.log($"移除原因: {arguments.RemovedReason}");

            if(removedItem.Value is AIRelatedCacheDto dto)
            {
                if (dto.giveStatuc == 0 && ReplayHttpUtils.HasUploadCosThumbsFile(dto.contextId))
                {
                    string path = AiUtils.GetContextFilePath(dto.type, dto.sourceId, dto.sourceType, dto.contextId);
                    if (File.Exists(path))
                    {
                        // 上传cos
                        string cosKey = TencentCosUtils.uploadFile(path, "AiData");

                        ReplayHttpUtils.updateCosThumbsFile(dto.type, ReplayHttpUtils.UserId, dto.sourceId, dto.sourceType, dto.contextId, cosKey, 1);
                    }
                }
            }
        }

        public static string GetFalseContextId(string sourceId, int sourceType, int type)
        {
            string cacheKey = $"{deepseekKey}{sourceType}-{type}-{sourceId}";
            string value = "";
            if (cache.Contains(cacheKey))
            {
                value = cache.Get(cacheKey).ToString();
            }
            else
            {
                // 定义缓存策略
                CacheItemPolicy policy = new CacheItemPolicy
                {
                    AbsoluteExpiration = DateTimeOffset.MaxValue, // 不设置绝对过期时间
                    SlidingExpiration = TimeSpan.Zero // 不使用滑动过期时间
                };
                value = Guid.NewGuid().ToString("N");
                cache.Set(cacheKey, value, policy);
            }
            return value;
        }
    }
}
