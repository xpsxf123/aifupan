using System;
using System.Collections;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using douyin.Utils;

namespace ReviewAnalysis.ShortVideo
{
    /// <summary>
    /// 热搜接口配置管理类（线程安全）
    /// 负责管理接口列表、关键词-接口使用记录字典的核心操作
    /// </summary>
    public static class HotSearchApiConfig
    {
        #region 1. 接口配置存储（替换原数组，支持动态添加）
        // 线程安全的接口配置集合（有序，替代原readonly数组）
        private static readonly HotSearchThreadSafeList<ApiConfig> _apiConfigs = new HotSearchThreadSafeList<ApiConfig>();

        // 静态构造函数：初始化默认接口配置
        static HotSearchApiConfig()
        {
            // 初始化默认3个接口
            _apiConfigs.Add(new ApiConfig { ApiId = "api_pc", ApiUrl = "https://pc" });
            _apiConfigs.Add(new ApiConfig { ApiId = "api_phone", ApiUrl = "https://phone" });
            _apiConfigs.Add(new ApiConfig { ApiId = "api_h5", ApiUrl = "https://h5" });
        }
        #endregion

        #region 2. 关键词-接口使用记录字典（线程安全）
        // 线程安全字典：Key=关键词，Value=该关键词的接口使用记录
        private static readonly ConcurrentDictionary<string, KeywordApiRecord> _keywordApiDict = new ConcurrentDictionary<string, KeywordApiRecord>();
        #endregion

        #region 一、对外提供：获取接口数量（属性）
        /// <summary>
        /// 获取当前已配置的接口总数（线程安全）
        /// </summary>
        public static int ApiConfigCount => _apiConfigs.Count;
        #endregion

        #region 二、对外提供：添加接口方法（支持动态新增，避免重复）
        /// <summary>
        /// 动态添加接口配置（线程安全，自动校验ApiId唯一性）
        /// </summary>
        /// <param name="apiConfig">待添加的接口配置</param>
        /// <returns>添加成功返回true；ApiId重复返回false</returns>
        /// <exception cref="ArgumentNullException">接口配置为空时抛出</exception>
        public static bool AddApiConfig(ApiConfig apiConfig)
        {
            // 参数校验
            if (apiConfig == null)
                throw new ArgumentNullException(nameof(apiConfig), "接口配置不能为空");
            if (string.IsNullOrWhiteSpace(apiConfig.ApiId))
                throw new ArgumentException("ApiId不能为空或空白", nameof(apiConfig.ApiId));

            // 校验ApiId是否已存在（ThreadSafeList内保证遍历线程安全）
            bool isExist = false;
            foreach (var api in _apiConfigs)
            {
                if (api.ApiId.Equals(apiConfig.ApiId, StringComparison.OrdinalIgnoreCase))
                {
                    isExist = true;
                    break;
                }
            }

            if (isExist)
            {
                FileUtils.log($"【警告】ApiId={apiConfig.ApiId} 已存在，添加失败");
                return false;
            }

            // 线程安全添加
            _apiConfigs.Add(apiConfig);
            FileUtils.log($"【成功】添加接口：{apiConfig.ApiId} → {apiConfig.ApiUrl}，当前接口总数：{ApiConfigCount}");
            return true;
        }
        #endregion

        #region 三、对外提供：字典操作方法（封装ConcurrentDictionary的核心操作）
        /// <summary>
        /// 添加/更新关键词的接口使用记录（线程安全）
        /// </summary>
        /// <param name="keyword">关键词</param>
        /// <param name="usedApiId">本次使用的接口ID</param>
        /// <param name="currentIndex">当前轮询索引</param>
        public static void AddOrUpdateKeywordRecord(string keyword, string usedApiId, int currentIndex)
        {
            // 参数校验
            if (string.IsNullOrWhiteSpace(keyword))
                throw new ArgumentException("关键词不能为空或空白", nameof(keyword));
            if (string.IsNullOrWhiteSpace(usedApiId))
                throw new ArgumentException("接口ID不能为空或空白", nameof(usedApiId));

            // 线程安全添加/更新：若关键词不存在则新增，存在则更新
            _keywordApiDict.AddOrUpdate(
                key: keyword,
                addValueFactory: (k) => new KeywordApiRecord
                {
                    UsedApiIds = new List<string> { usedApiId }, // 初始化已使用接口列表
                    CurrentIndex = currentIndex,
                    LastRequestTime = DateTime.Now
                },
                updateValueFactory: (k, existingRecord) =>
                {
                    // 避免重复添加已使用的接口ID
                    if (!existingRecord.UsedApiIds.Contains(usedApiId))
                    {
                        existingRecord.UsedApiIds.Add(usedApiId);
                    }
                    existingRecord.CurrentIndex = currentIndex;
                    existingRecord.LastRequestTime = DateTime.Now;
                    return existingRecord;
                });

            FileUtils.log($"【成功】更新关键词[{keyword}]的接口记录：本次使用{usedApiId}，当前轮询索引{currentIndex}");
        }

        /// <summary>
        /// 获取指定关键词的接口使用记录（线程安全）
        /// </summary>
        /// <param name="keyword">关键词</param>
        /// <returns>关键词的接口使用记录（不存在返回null）</returns>
        public static KeywordApiRecord GetKeywordRecord(string keyword)
        {
            if (string.IsNullOrWhiteSpace(keyword)) return null;

            _keywordApiDict.TryGetValue(keyword, out var record);
            return record;
        }

        /// <summary>
        /// 移除指定关键词的接口使用记录（线程安全）
        /// </summary>
        /// <param name="keyword">关键词</param>
        /// <returns>移除成功返回true，不存在返回false</returns>
        public static bool RemoveKeywordRecord(string keyword)
        {
            if (string.IsNullOrWhiteSpace(keyword)) return false;

            var isRemoved = _keywordApiDict.TryRemove(keyword, out _);
            if (isRemoved)
            {
                FileUtils.log($"【成功】移除关键词[{keyword}]的接口记录");
            }
            return isRemoved;
        }

        /// <summary>
        /// 清空所有关键词的接口使用记录（线程安全）
        /// </summary>
        public static void ClearAllKeywordRecords()
        {
            _keywordApiDict.Clear();
            FileUtils.log($"【成功】清空所有关键词的接口使用记录，当前字典数量：{_keywordApiDict.Count}");
        }
        #endregion

        #region 辅助方法：获取所有接口配置（只读）
        /// <summary>
        /// 获取所有接口配置的只读列表（避免外部修改原集合）
        /// </summary>
        /// <returns>接口配置只读列表</returns>
        public static IReadOnlyList<ApiConfig> GetAllApiConfigs()
        {
            // 复制ThreadSafeList到普通List，再返回只读列表
            var tempList = new List<ApiConfig>();
            foreach (var api in _apiConfigs)
            {
                tempList.Add(api);
            }
            return tempList.AsReadOnly();
        }
        #endregion

        #region 核心方法（字典+接口列表的核心逻辑）
        /// <summary>
        /// 为指定关键词获取下一个要调用的接口（核心逻辑，对外可改为public）
        /// </summary>
        /// <summary>
        /// 为指定关键词获取下一个要调用的接口（简化版，lock保证线程安全）
        /// </summary>
        public static ApiConfig GetNextApiForKeyword(string keyword)
        {
            if (string.IsNullOrWhiteSpace(keyword))
                throw new ArgumentException("关键词不能为空或空白", nameof(keyword));

            // 全局锁（低并发场景可用，高并发建议用Interlocked方案）
            lock (_keywordApiDict)
            {
                // 1. 初始化记录
                if (!_keywordApiDict.ContainsKey(keyword))
                {
                    _keywordApiDict[keyword] = new KeywordApiRecord
                    {
                        UsedApiIds = new List<string> { "api_phone" },
                        CurrentIndex = 0,
                        LastRequestTime = DateTime.Now
                    };
                }

                var keywordRecord = _keywordApiDict[keyword];
                // 2. 计算并更新索引（lock保证原子性）
                keywordRecord.CurrentIndex = (keywordRecord.CurrentIndex + 1) % _apiConfigs.Count;
                int nextIndex = keywordRecord.CurrentIndex;

                // 3. 获取目标接口
                var targetApi = _apiConfigs[nextIndex];
                UpdateKeywordApiRecord(keyword, targetApi.ApiId);

                FileUtils.log($"【轮询】关键词{keyword}下一个接口：{targetApi.ApiId}（索引{nextIndex}）");
                return targetApi;
            }
        }

        /// <summary>
        /// 更新关键词的接口使用记录（添加已使用的接口ID）
        /// </summary>
        private static void UpdateKeywordApiRecord(string keyword, string apiId)
        {
            if (_keywordApiDict.TryGetValue(keyword, out var record))
            {
                // 加锁保证List添加的线程安全（List非线程安全）
                lock (record.UsedApiIds)
                {
                    if (!record.UsedApiIds.Contains(apiId))
                    {
                        record.UsedApiIds.Add(apiId);
                    }
                }
                record.LastRequestTime = DateTime.Now; // 更新最后请求时间
            }
        }

        /// <summary>
        /// 获取关键词已使用的接口ID列表（辅助方法）
        /// </summary>
        public static List<string> GetKeywordUsedApis(string keyword)
        {
            if (string.IsNullOrWhiteSpace(keyword))
                return new List<string>();

            if (_keywordApiDict.TryGetValue(keyword, out var record))
            {
                lock (record.UsedApiIds)
                {
                    // 返回副本，避免外部修改原列表
                    return new List<string>(record.UsedApiIds);
                }
            }
            return new List<string>();
        }

        /// <summary>
        /// 打印字典中的所有关键词接口记录（调试用）
        /// </summary>
        public static void PrintKeywordApiDict()
        {
            FileUtils.log("\n======== 关键词-接口使用记录（字典内容）========");
            // 遍历ConcurrentDictionary的快照，避免枚举时修改异常
            foreach (var kv in _keywordApiDict.ToArray())
            {
                FileUtils.log($"关键词：{kv.Key}");
                lock (kv.Value.UsedApiIds)
                {
                    FileUtils.log($"  - 已使用接口：{string.Join(",", kv.Value.UsedApiIds)}");
                }
                FileUtils.log($"  - 当前轮询索引：{kv.Value.CurrentIndex}");
                FileUtils.log($"  - 最后请求时间：{kv.Value.LastRequestTime:yyyy-MM-dd HH:mm:ss}");
            }
            FileUtils.log("==============================================\n");
        }
        #endregion
    }

    #region 自定义ThreadSafeList实现（必须添加，否则代码无法编译）
    /// <summary>
    /// 线程安全的有序列表（核心依赖，需放在同一命名空间）
    /// </summary>
    /// <typeparam name="T">元素类型</typeparam>
    public class HotSearchThreadSafeList<T> : IReadOnlyList<T>
    {
        private readonly List<T> _innerList = new List<T>();
        private readonly object _lockObj = new object();

        /// <summary>
        /// 添加元素（线程安全）
        /// </summary>
        public void Add(T item)
        {
            lock (_lockObj)
            {
                _innerList.Add(item);
            }
        }

        /// <summary>
        /// 索引访问（线程安全）
        /// </summary>
        public T this[int index]
        {
            get
            {
                lock (_lockObj)
                {
                    if (index < 0 || index >= _innerList.Count)
                        throw new ArgumentOutOfRangeException(nameof(index), "索引超出范围");
                    return _innerList[index];
                }
            }
        }

        /// <summary>
        /// 获取元素数量（线程安全）
        /// </summary>
        public int Count
        {
            get
            {
                lock (_lockObj)
                {
                    return _innerList.Count;
                }
            }
        }

        /// <summary>
        /// 枚举器（返回快照，避免锁持有时间过长）
        /// </summary>
        public IEnumerator<T> GetEnumerator()
        {
            lock (_lockObj)
            {
                return new List<T>(_innerList).GetEnumerator();
            }
        }

        IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();
    }
    #endregion

    #region 配套实体类（修正+完善）
    /// <summary>
    /// 接口配置实体
    /// </summary>
    public class ApiConfig
    {
        public string ApiId { get; set; }          // 接口唯一标识
        public string ApiUrl { get; set; }         // 接口地址
        public Dictionary<string, string> ExtraParams { get; set; } = new Dictionary<string, string>(); // 初始化，避免空引用
    }

    /// <summary>
    /// 单个关键词的接口使用记录
    /// </summary>
    public class KeywordApiRecord
    {
        public List<string> UsedApiIds { get; set; } = new List<string>(); // 初始化，避免空引用
        public int CurrentIndex { get; set; } = 0;
        public DateTime LastRequestTime { get; set; } = DateTime.Now;
    }
    #endregion
}