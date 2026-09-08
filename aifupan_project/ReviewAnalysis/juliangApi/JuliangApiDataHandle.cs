using douyin.Utils;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.Asr;
using ReviewAnalysis.api;
using ReviewAnalysis.juliang;
using ReviewAnalysis.juliang.entity;
using ReviewAnalysis.Model;
using ReviewAnalysis.qianchuan;
using ReviewAnalysis.upload;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using ReviewAnalysis.Websocket.Entity;
using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.juliangApi
{
    /// <summary>
    /// 巨量百应无窗体模块 - 数据处理层
    /// </summary>
    public static class JuliangApiDataHandle
    {
        /// <summary>
        /// 从本地Cookie文件读取Cookie字典
        /// </summary>
        public static Dictionary<string, string> GetCookiesFromLocal(string secUid)
        {
            try
            {
                string cookiePath = JuliangUtils.getCookiePath(secUid);
                if (!File.Exists(cookiePath))
                {
                    return new Dictionary<string, string>();
                }

                var helper = new juliang.CookiePersistenceHelper(cookiePath);
                var cookies = helper.LoadCefCookiesFromLocal(false);
                if (cookies == null || cookies.Count == 0)
                {
                    return new Dictionary<string, string>();
                }

                var result = new Dictionary<string, string>();
                foreach (var cookie in cookies)
                {
                    result[cookie.Name] = cookie.Value;
                }
                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"从本地获取巨量Cookie异常: {ex.Message}", "巨量API");
                return new Dictionary<string, string>();
            }
        }

        /// <summary>
        /// 获取 COMPASS_LUOPAN_DT cookie 值
        /// </summary>
        public static string GetLuopanDt(string secUid)
        {
            var cookies = GetCookiesFromLocal(secUid);
            if (cookies.ContainsKey("COMPASS_LUOPAN_DT"))
                return cookies["COMPASS_LUOPAN_DT"];
            return null;
        }

        /// <summary>
        /// 从千川模块获取指定 Cookie 值
        /// </summary>
        public static string GetQianchuanCookie(string secUid, string cookieName)
        {
            try
            {
                string cookiePath = QianchuanUtils.getCookiePath(secUid);
                if (!File.Exists(cookiePath))
                {
                    return null;
                }

                var localCookies = new qianchuan.CookiePersistenceHelper(cookiePath).LoadCefCookiesFromLocal(false);
                if (localCookies == null || localCookies.Count == 0)
                {
                    return null;
                }

                var cookie = localCookies.FirstOrDefault(c => c.Name == cookieName);
                return cookie?.Value;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"从千川获取Cookie异常: {ex.Message}", "巨量API");
                return null;
            }
        }

        /// <summary>
        /// 构建完整的 Cookie 字符串（用于请求头）
        /// </summary>
        public static string BuildCookieString(Dictionary<string, string> cookies)
        {
            if (cookies == null || cookies.Count == 0) return string.Empty;
            return string.Join("; ", cookies.Select(kv => $"{kv.Key}={kv.Value}"));
        }

        /// <summary>
        /// 拉取全部巨量数据（6个API并行调用）
        /// </summary>
        public static async Task PullJuliangData(AnchorInfo anchorInfo, string roomId, string videoId)
        {
            try
            {
                string secUid = anchorInfo.SecUid;
                string luopanDt = GetLuopanDt(secUid);

                if (string.IsNullOrEmpty(luopanDt))
                {
                    FileUtils.LogRpa($"主播{anchorInfo.AnchorName}巨量LUOPAN_DT为空，无法拉取数据", "巨量API");
                    return;
                }

                // 获取完整 Cookie 字符串（合并巨量百应 + 千川的 sessionid）
                var allCookies = GetCookiesFromLocal(secUid);

                // 优先从千川模块获取 sessionid（与 juliang 窗体模块逻辑一致）
                string qianchuanSessionId = GetQianchuanCookie(secUid, "sessionid");
                if (!string.IsNullOrEmpty(qianchuanSessionId))
                {
                    allCookies["sessionid"] = qianchuanSessionId;
                    FileUtils.LogRpa($"从千川模块获取sessionid成功", "巨量API");
                }

                string fullCookieStr = BuildCookieString(allCookies);

                string batchNumber = anchorInfo.BatchNumber ?? roomId;

                FileUtils.LogRpa($"主播{anchorInfo.AnchorName}开始拉取巨量数据，roomId={roomId}", "巨量API");

                // 并行执行6个API调用
                var taskLiveTalent = JuliangApiDataApi.LiveTalentAsync(roomId, fullCookieStr);
                //var taskBasicLiveScreen = JuliangApiDataApi.BasicLiveScreenAsync(roomId, fullCookieStr);
                //var taskPayUserPortrait = JuliangApiDataApi.PayUserPortraitAsync(roomId, fullCookieStr);
                //var taskWatchUserPortrait = JuliangApiDataApi.WatchUserPortraitAsync(roomId, fullCookieStr);
                //var taskFlowOrderSource = JuliangApiDataApi.FlowOrderSourceAsync(roomId, fullCookieStr);
                var taskProductPayment = JuliangApiDataApi.ProductPaymentDataAsync(roomId, fullCookieStr);

                //await Task.WhenAll(taskLiveTalent, taskBasicLiveScreen, taskPayUserPortrait,
                //    taskWatchUserPortrait, taskFlowOrderSource, taskProductPayment);
                await Task.WhenAll(taskLiveTalent, taskProductPayment);

                string liveTalentJson = taskLiveTalent.Result;
                //string basicLiveScreenJson = taskBasicLiveScreen.Result;
                //string payUserPortraitJson = taskPayUserPortrait.Result;
                //string watchUserPortraitJson = taskWatchUserPortrait.Result;
                string flowOrderSourceJson = taskProductPayment.Result;

                // 检查授权是否过期
                if (JuliangApiDataApi.IsAuthExpired(liveTalentJson))
                {
                    FileUtils.LogRpa($"主播{anchorInfo.AnchorName}巨量授权已过期", "巨量API");
                    return;
                }

                // 处理专业版大屏数据
                if (!JuliangApiDataApi.IsErrorResponse(liveTalentJson))
                {
                    var proArgs = BuildDataCollectEventArgs(secUid, batchNumber, videoId, liveTalentJson);
                    JuliangDataHandle.writeGatherDataPro(proArgs);
                    FileUtils.LogRpa($"主播{anchorInfo.AnchorName}专业版大屏数据写入成功", "巨量API");
                }

                //// 处理基础版大屏数据
                //if (!JuliangApiDataApi.IsErrorResponse(basicLiveScreenJson))
                //{
                //    var baseArgs = BuildDataCollectEventArgs(secUid, batchNumber, videoId, basicLiveScreenJson);
                //    JuliangDataHandle.writeGatherDataBase(baseArgs);

                //    // 写入实时数据
                //    WriteRealTimeData(secUid, batchNumber, videoId, basicLiveScreenJson);
                //    FileUtils.LogRpa($"主播{anchorInfo.AnchorName}基础版大屏数据写入成功", "巨量API");
                //}

                //// 处理成交用户画像
                //if (!JuliangApiDataApi.IsErrorResponse(payUserPortraitJson))
                //{
                //    var payArgs = BuildDataCollectEventArgs(secUid, batchNumber, videoId, payUserPortraitJson);
                //    JuliangDataHandle.writeUserPortrait(payArgs, 0);
                //    FileUtils.LogRpa($"主播{anchorInfo.AnchorName}成交用户画像数据写入成功", "巨量API");
                //}

                //// 处理观看用户画像
                //if (!JuliangApiDataApi.IsErrorResponse(watchUserPortraitJson))
                //{
                //    var watchArgs = BuildDataCollectEventArgs(secUid, batchNumber, videoId, watchUserPortraitJson);
                //    JuliangDataHandle.writeUserPortrait(watchArgs, 1);
                //    FileUtils.LogRpa($"主播{anchorInfo.AnchorName}观看用户画像数据写入成功", "巨量API");
                //}

                // 处理流量结构
                if (!JuliangApiDataApi.IsErrorResponse(flowOrderSourceJson))
                {
                    var flowArgs = BuildDataCollectEventArgs(secUid, batchNumber, videoId, flowOrderSourceJson);
                    JuliangDataHandle.writeFlowSourceData(flowArgs);
                    FileUtils.LogRpa($"主播{anchorInfo.AnchorName}流量结构数据写入成功", "巨量API");
                }

                // 通知前端数据更新
                NotifyFrontend(secUid, batchNumber, videoId);

                FileUtils.LogRpa($"主播{anchorInfo.AnchorName}巨量数据拉取完成", "巨量API");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"拉取巨量数据异常: {ex.Message}\n{ex.StackTrace}", "巨量API");
            }
        }

        /// <summary>
        /// 写入实时数据（从基础版大屏数据中提取）
        /// </summary>
        private static void WriteRealTimeData(string secUid, string batchNumber, string videoId, string basicLiveScreenJson)
        {
            try
            {
                JObject jo = JObject.Parse(basicLiveScreenJson);
                var dataObj = jo["data"];
                if (dataObj == null) return;

                if (dataObj["pay_cnt"] == null || dataObj["pay_cnt"]["value"] == null ||
                    dataObj["gmv"] == null ||
                    dataObj["fans_club_ucnt"] == null || dataObj["fans_club_ucnt"]["value"] == null ||
                    dataObj["incr_fans_cnt"] == null || dataObj["incr_fans_cnt"]["value"] == null ||
                    dataObj["online_user_ucnt"] == null || dataObj["online_user_ucnt"]["value"] == null)
                    return;

                var realTimeData = new JuliangRealTimeDataEntity
                {
                    payComboCnt = (int)dataObj["pay_cnt"]["value"],
                    payAmt = (int)dataObj["gmv"],
                    fansClubJoinUcnt = (int)dataObj["fans_club_ucnt"]["value"],
                    followAnchorUcnt = (int)dataObj["incr_fans_cnt"]["value"],
                    watchNum = (int)dataObj["online_user_ucnt"]["value"]
                };

                long time = ServerTimeUtils.getCurrentTime();
                realTimeData.gatherTimeStamp = time;
                realTimeData.gatherDateTime = ServerTimeUtils.getTimeStrByTime(time);

                var queueEntity = new JuliangRealTimeDataQueueEntity
                {
                    secUid = secUid,
                    batchNumber = batchNumber,
                    videoId = GetFileIdentifier(batchNumber, videoId),
                    juliangRealTimeData = realTimeData
                };

                JuliangDataHandle.pushRealTimeDataQueue(queueEntity);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"写入巨量实时数据异常: {ex.Message}", "巨量API");
            }
        }

        /// <summary>
        /// 生成数据文件名标识（batchNumber_videoId 或 batchNumber）
        /// </summary>
        private static string GetFileIdentifier(string batchNumber, string videoId)
        {
            return string.IsNullOrEmpty(videoId) ? batchNumber : $"{batchNumber}_{videoId}";
        }

        /// <summary>
        /// 构建 DataCollectEventArgs（复用现有 JuliangDataHandle 写入方法）
        /// </summary>
        private static DataCollectEventArgs BuildDataCollectEventArgs(string secUid, string batchNumber, string videoId, string dataJson)
        {
            string fileId = GetFileIdentifier(batchNumber, videoId);
            return new DataCollectEventArgs
            {
                secUid = secUid,
                batchNumber = batchNumber,
                videoId = fileId,
                data = dataJson
            };
        }

        /// <summary>
        /// 通知前端巨量数据更新
        /// </summary>
        private static void NotifyFrontend(string secUid, string batchNumber, string videoId)
        {
            try
            {
                var frontNotice = new FrontNotice();
                var requestDataObj = new Dictionary<string, object>
                {
                    { "code", 0 },
                    { "status", 200 },
                    { "action", "juliangApiDataUpdate" },
                    { "data", new
                        {
                            secUid = secUid,
                            batchNumber = batchNumber,
                            videoId = videoId
                        }
                    }
                };
                frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"通知前端巨量数据异常: {ex.Message}", "巨量API");
            }
        }

        #region 千川数据采集

        /// <summary>
        /// 从文件中获取千川账户ID（aavid）
        /// </summary>
        public static string GetAavidFromConfig(string secUid)
        {
            try
            {
                if (string.IsNullOrEmpty(secUid))
                {
                    return null;
                }

                string aavidPath = GetAavidPath(secUid);
                if (File.Exists(aavidPath))
                {
                    string aavid = File.ReadAllText(aavidPath).Trim();
                    return aavid;
                }

                return null;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取aavid配置异常: {ex.Message}", "巨量API");
                return null;
            }
        }

        /// <summary>
        /// 获取aavid文件路径
        /// </summary>
        private static string GetAavidPath(string secUid)
        {
            try
            {
                string cachePath = Path.GetFullPath("dataCollect/config");
                string md5Str = MD5Utils.create($"{ReplayHttpUtils.ActiveTenantId}-{ReplayHttpUtils.UserId}-{secUid}");
                return Path.Combine(cachePath, $"qcaavid-{md5Str}");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取aavid文件路径异常: {ex.Message}", "巨量API");
                return null;
            }
        }

        /// <summary>
        /// 拉取千川全域大屏数据
        /// </summary>
        public static async Task PullQianchuanData(AnchorInfo anchorInfo, string roomId, string videoId)
        {
            try
            {
                string secUid = anchorInfo.SecUid;

                // 获取千川 sessionid
                string sessionid = GetQianchuanCookie(secUid, "sessionid");
                if (string.IsNullOrEmpty(sessionid))
                {
                    FileUtils.LogRpa($"主播{anchorInfo.AnchorName}千川sessionid为空，跳过千川数据", "巨量API");
                    return;
                }

                // 获取 aavid
                string aavid = GetAavidFromConfig(secUid);
                if (string.IsNullOrEmpty(aavid))
                {
                    FileUtils.LogRpa($"主播{anchorInfo.AnchorName}千川aavid为空，跳过千川数据", "巨量API");
                    return;
                }

                // 构建 Cookie 字符串
                string cookieStr = $"sessionid={sessionid}";

                string batchNumber = anchorInfo.BatchNumber ?? roomId;
                // 若 videoId 为空，使用 roomId 作为默认值（与巨量数据处理逻辑一致）
                string effectiveVideoId = !string.IsNullOrEmpty(videoId) ? videoId : roomId;

                FileUtils.LogRpa($"主播{anchorInfo.AnchorName}开始拉取千川数据，roomId={roomId}", "巨量API");

                // 获取千川账户列表
                await JuliangApiDataApi.GetQianchuanAccountListAsync(roomId, cookieStr);

                // 获取全域大屏数据
                var anchorId = anchorInfo.AnchorUserId;
                if (string.IsNullOrEmpty(anchorId))
                {
                    anchorId = "3160467642781443"; // 默认主播uid
                    FileUtils.LogRpa("未找到anchorId配置，使用默认值", "巨量API");
                }

                string statDataJson = await JuliangApiDataApi.GetQianchuanStatDataAsync(roomId, aavid, anchorId, cookieStr);

                // 处理千川全域大屏数据
                if (!string.IsNullOrEmpty(statDataJson))
                {
                    ProcessQianchuanStatData(secUid, batchNumber, effectiveVideoId, statDataJson);
                    FileUtils.LogRpa($"主播{anchorInfo.AnchorName}千川数据拉取完成", "巨量API");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"拉取千川数据异常: {ex.Message}", "巨量API");
            }
        }

        /// <summary>
        /// 处理千川全域大屏统计数据
        /// </summary>
        private static void ProcessQianchuanStatData(string secUid, string batchNumber, string videoId, string statDataJson)
        {
            try
            {
                var indexNameData = new Dictionary<string, string>
                {
                    { "board_roi2_overview_conf_next", "全域大屏-核心概览看板" },
                    { "total_order_settle_count_realtime_for_roi2_1h", "净成交订单数" },
                    { "total_cost_per_pay_order_settle_realtime_for_roi2_1h", "净成交订单成本" },
                    { "total_order_real_settle_amount_realtime_for_roi2_1h", "用户实际支付净成交金额" },
                    { "total_order_settle_amount_rate_realtime_for_roi2_1h", "净成交金额结算率" },
                    { "total_order_settle_count_rate_realtime_for_roi2_1h", "净成交订单结算率" },
                    { "total_refund_order_count_for_roi2_1h", "1小时内退款订单数" },
                    { "total_refund_order_gmv_for_roi2_1h_all", "1小时内退款金额" },
                    { "total_refund_order_gmv_for_roi2_1h_rate", "1小时内退款率" },
                    { "total_pay_order_count_realtime_for_roi2", "整体成交订单数" },
                    { "total_live_pay_order_gpm_realtime_for_roi2", "GPM" },
                    { "live_watch_to_pay_rate_for_roi2", "观看成交转化率" },
                    { "total_cost_per_pay_order_realtime_for_roi2", "整体成交订单成本" },
                    { "total_pay_order_gmv_realtime_for_roi2", "用户实际支付金额" },
                    { "total_pay_order_coupon_amount_realtime_for_roi2", "整体成交智能优惠券金额" },
                    { "total_ecom_platform_subsidy_amount_realtime_for_roi2", "电商平台补贴金额" },
                    { "total_unfinished_estimate_order_gmv_realtime_for_roi2", "整体未完结预售订单预估金额" },
                    { "total_refund_order_amount_for_roi2_90d_all", "直播间退款金额" },
                    { "total_refund_order_amount_rate_for_roi2_90d_all", "直播间退款率" },
                    { "live_online_user_count", "实时在线人数" },
                    { "live_show_count_for_roi2", "直播间整体曝光次数" },
                    { "total_show_to_watch_rate_for_roi2", "曝光观看率(次数)" },
                    { "live_watch_ucount_for_roi2", "直播间整体观看人数" },
                    { "live_duration_avg_ecom_for_roi2", "直播间平均停留时长(整场)" },
                    { "live_follow_count_for_roi2", "直播间整体新增粉丝数" },
                    { "live_comment_count_for_roi2", "直播间评论次数" },
                    { "live_product_show_count_for_roi2", "直播间商品曝光次数" },
                    { "live_product_click_count_for_roi2", "直播间商品点击次数" },
                    { "live_share_count_for_roi2", "分享次数" },
                    { "total_live_share_rate_for_roi2", "分享率" },
                    { "live_gift_count_for_roi2", "打赏次数" },
                    { "total_live_like_rate_for_roi2", "点赞率" },
                    { "stat_cost_for_roi2", "整体消耗" },
                    { "total_prepay_and_pay_order_realtime_roi2", "整体支付ROI" },
                    { "total_pay_order_gmv_include_coupon_realtime_for_roi2", "整体成交金额" },
                    { "total_prepay_and_pay_settle_realtime_roi2_1h", "净成交ROI" },
                    { "total_order_settle_amount_realtime_for_roi2_1h", "净成交金额" }
                };

                var jsonResponse = JObject.Parse(statDataJson);
                if (jsonResponse["status_code"]?.ToString() == "0" && jsonResponse["message"]?.ToString() == "success" && jsonResponse["data"] != null)
                {
                    var totals = jsonResponse["data"]?["StatsData"]?["Totals"] as JObject;
                    if (totals != null)
                    {
                        var result = new Dictionary<string, string>();
                        foreach (var item in totals.Properties())
                        {
                            var key = item.Name;
                            var valueObj = item.Value?["ValueStr"];
                            var value = valueObj?.ToString() ?? "";
                            if (indexNameData.ContainsKey(key))
                            {
                                result[indexNameData[key]] = value;
                            }
                            else
                            {
                                result[key] = value;
                            }
                        }

                        // 存储数据
                        var dataArgs = BuildDataCollectEventArgs(secUid, batchNumber, videoId, JsonConvert.SerializeObject(result));
                        JuliangDataHandle.writeGatherDataBase(dataArgs);

                        FileUtils.LogRpa("千川全域大屏数据写入成功", "巨量API");
                    }
                }
                else
                {
                    FileUtils.LogRpa($"千川全域大屏业务逻辑失败: {statDataJson}", "巨量API");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"处理千川全域大屏数据异常: {ex.Message}", "巨量API");
            }
        }

        #endregion
    }
}
