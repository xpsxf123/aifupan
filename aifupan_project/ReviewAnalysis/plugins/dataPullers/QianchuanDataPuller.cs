using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.Model;
using ReviewAnalysis.qianchuan;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;
using douyin.Utils;

using ReviewAnalysis.juliangApi;

namespace ReviewAnalysis.plugins.dataPullers
{
    /// <summary>
    /// 千川数据拉取器
    /// 职责：调用千川API获取数据，返回原始JSON
    /// </summary>
    public class QianchuanDataPuller : BaseDataPuller
    {
        public QianchuanDataPuller() : base("qianchuan", "千川") { }

        /// <summary>
        /// 拉取千川数据
        /// </summary>
        public override async Task<string> PullDataAsync(AnchorInfo anchorInfo, string roomId, string videoId)
        {
            try
            {
                if (string.IsNullOrEmpty(roomId))
                {
                    FileUtils.LogRpa("roomId为空，无法拉取千川数据", _platformName);
                    return null;
                }

                var cookies = GetCookies(anchorInfo.SecUid);
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa($"Cookie获取失败，主播【{anchorInfo?.AnchorName}】", _platformName);
                    return null;
                }

                FileUtils.LogRpa($"开始拉取千川数据，主播【{anchorInfo?.AnchorName}】，roomId={roomId}", _platformName);

                // 获取千川账户列表
                var accountList = await CallApiWithRetry(async () =>
                {
                    var result = await QianchuanDataApi.GetAccountUserList(roomId, cookies, anchorInfo.SecUid);
                    return result.Count > 0 ? JsonConvert.SerializeObject(result) : null;
                });

                // 获取全域大屏数据和直播间详情数据（并行调用）
                string aavid = JuliangApiDataHandle.GetAavidFromConfig(anchorInfo.SecUid);
                string anchorId = anchorInfo.AnchorUserId;

                string overviewData = null;
                string roomDetailData = null;

                if (!string.IsNullOrEmpty(aavid) && !string.IsNullOrEmpty(anchorId))
                {
                    var tasks = new Task<string>[]
                    {
                        CallApiWithRetry(async () =>
                        {
                            var result = await QianchuanDataApi.GetOverviewBoardData(roomId, aavid, anchorId, cookies, anchorInfo.SecUid);
                            return result.Count > 0 ? JsonConvert.SerializeObject(result) : null;
                        }),
                        CallApiWithRetry(async () =>
                        {
                            var result = await QianchuanDataApi.GetRoomDetailData(roomId, aavid, anchorId, cookies, anchorInfo.SecUid);
                            return result.Count > 0 ? JsonConvert.SerializeObject(result) : null;
                        })
                    };

                    await Task.WhenAll(tasks);
                    overviewData = tasks[0].Result;
                    roomDetailData = tasks[1].Result;
                }
                else
                {
                    FileUtils.LogRpa("千川aavid或anchorId为空，跳过数据拉取", _platformName);
                }

                // 合并结果
                var mergedData = new JObject
                {
                    ["roomId"] = roomId,
                    ["videoId"] = videoId,
                    ["collectTime"] = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"),
                    ["data"] = new JObject()
                };

                if (!string.IsNullOrEmpty(accountList))
                {
                    mergedData["data"]["accountList"] = JArray.Parse(accountList);
                }

                if (!string.IsNullOrEmpty(overviewData))
                {
                    mergedData["data"]["overviewBoard"] = JObject.Parse(overviewData);
                }

                if (!string.IsNullOrEmpty(roomDetailData))
                {
                    mergedData["data"]["roomDetail"] = JObject.Parse(roomDetailData);
                }

                string json = JsonConvert.SerializeObject(mergedData);

                FileUtils.LogRpa($"千川数据拉取完成，roomId={roomId}", _platformName);

                return json;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"拉取千川数据异常: {ex.Message}", _platformName);
                return null;
            }
        }

        /// <summary>
        /// 获取Cookie（内联实现）
        /// </summary>
        protected override Dictionary<string, string> GetCookies(string secUid)
        {
            try
            {
                return GetQianchuanCookiesFromLocal(secUid) ?? new Dictionary<string, string>();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取Cookie异常: {ex.Message}", _platformName);
                return new Dictionary<string, string>();
            }
        }

        /// <summary>
        /// 从本地获取千川Cookie
        /// </summary>
        private Dictionary<string, string> GetQianchuanCookiesFromLocal(string secUid)
        {
            try
            {
                string cookiePath = GetQianchuanCookiePath(secUid);
                if (!File.Exists(cookiePath))
                {
                    return new Dictionary<string, string>();
                }

                var helper = new CookiePersistenceHelper(cookiePath);
                var localCookies = helper.LoadCefCookiesFromLocal(false);
                if (localCookies == null || localCookies.Count == 0)
                {
                    return new Dictionary<string, string>();
                }

                var result = new Dictionary<string, string>();
                foreach (var cookie in localCookies)
                {
                    result[cookie.Name] = cookie.Value;
                }
                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"从本地获取千川Cookie异常: {ex.Message}", _platformName);
                return new Dictionary<string, string>();
            }
        }

        /// <summary>
        /// 获取千川Cookie路径
        /// </summary>
        private string GetQianchuanCookiePath(string secUid)
        {
            string cachePath = Path.GetFullPath("dataCollect\\config");
            string md5Str = GetCachePathMd5(secUid);
            return $"{cachePath}\\qianchuan-{md5Str}";
        }

        /// <summary>
        /// 计算缓存路径MD5（与 QianchuanUtils.getCachePathMd5 保持一致）
        /// </summary>
        private string GetCachePathMd5(string secUid)
        {
            return MD5Utils.create($"{ReplayHttpUtils.ActiveTenantId}-{ReplayHttpUtils.UserId}-{secUid}");
        }
    }
}
