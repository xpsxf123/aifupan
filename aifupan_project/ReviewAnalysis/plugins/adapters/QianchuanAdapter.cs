using System;
using ReviewAnalysis.plugins.interfaces;
using ReviewAnalysis.plugins.models;
using Newtonsoft.Json.Linq;
using douyin.Utils;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.plugins.adapters
{
    /// <summary>
    /// 千川数据适配器
    /// 将千川平台原始JSON转换为统一数据模型
    /// </summary>
    public class QianchuanAdapter : IDataAdapter
    {
        public string PlatformId => "qianchuan";
        public string PlatformName => "千川";

        /// <summary>
        /// 适配千川数据
        /// </summary>
        public UnifiedDataModel Adapt(string rawJson, string secUid, string roomId, string videoId = null)
        {
            UnifiedDataModel model = CreateBaseModel(secUid, roomId, videoId);

            if (string.IsNullOrEmpty(rawJson))
            {
                return model;
            }

            try
            {
                var jo = JObject.Parse(rawJson);

                // 获取 overviewBoard 数据
                var overviewBoard = jo["data"]?["overviewBoard"] as JObject;
                int? onlineCountInt = null;
                if (overviewBoard != null)
                {
                    // 整体消耗 → investment（投放金额）
                    string statCost = overviewBoard["整体消耗"]?.ToString();
                    if (!string.IsNullOrEmpty(statCost))
                    {
                        statCost = statCost.Replace(",", "");
                        if (double.TryParse(statCost, out double cost))
                        {
                            model.investment = cost;
                        }
                    }

                    // 整体成交金额 → salesRevenue（销售额）
                    string totalGmv = overviewBoard["整体成交金额"]?.ToString();
                    if (!string.IsNullOrEmpty(totalGmv))
                    {
                        totalGmv = totalGmv.Replace(",", "");
                        if (double.TryParse(totalGmv, out double gmv))
                        {
                            model.salesRevenue = gmv;
                        }
                    }
                    // 整体成交金额 → refund（销售额）
                    string totalRefund = overviewBoard["直播间退款金额"]?.ToString();
                    if (!string.IsNullOrEmpty(totalRefund))
                    {
                        totalRefund = totalRefund.Replace(",", "");
                        if (double.TryParse(totalRefund, out double refund))
                        {
                            model.refund = refund;
                        }
                    }

                    // 实时在线人数 → maxOnline
                    string onlineUser = overviewBoard["实时在线人数"]?.ToString();
                    if (!string.IsNullOrEmpty(onlineUser))
                    {
                        onlineUser = onlineUser.Replace(",", "");
                        if (int.TryParse(onlineUser, out int online))
                        {
                            model.maxOnline = online;
                            model.averageOnlineNum = online;  // 与巨量保持一致
                        }
                    }

                    // 直播间平均停留时长(整场) → averageResidenceTime（单位：秒）
                    string avgDuration = overviewBoard["直播间平均停留时长(整场)"]?.ToString();
                    if (!string.IsNullOrEmpty(avgDuration))
                    {
                        avgDuration = avgDuration.Replace(",", "");
                        if (int.TryParse(avgDuration, out int duration))
                        {
                            model.averageResidenceTime = duration;
                        }
                    }

                    // 观看成交转化率 → conversionRate（带货转化率）
                    string watchToPayRate = overviewBoard["观看成交转化率"]?.ToString();
                    if (!string.IsNullOrEmpty(watchToPayRate))
                    {
                        // 去除 % 符号和千分位逗号
                        watchToPayRate = watchToPayRate.Replace("%", "").Replace(",", "");
                        if (double.TryParse(watchToPayRate, out double rate))
                        {
                            model.conversionRate = rate;
                        }
                    }

                    // 整体成交订单数 → payComboCnt（销售单量）
                    string payOrderCount = overviewBoard["整体成交订单数"]?.ToString();
                    if (!string.IsNullOrEmpty(payOrderCount))
                    {
                        payOrderCount = payOrderCount.Replace(",", "");
                        if (int.TryParse(payOrderCount, out int count))
                        {
                            model.payComboCnt = count;
                        }
                    }
                    
                    // 直播间整体新增粉丝数 → followCount（直播间整体新增粉丝数）
                    string followCount = overviewBoard["直播间整体新增粉丝数"]?.ToString();
                    if (!string.IsNullOrEmpty(followCount))
                    {
                        followCount = followCount.Replace(",", "");
                        if (int.TryParse(followCount, out int followCountTemp))
                        {
                            model.followCount = followCountTemp;
                        }
                    }
                    
                    // 直播间整体曝光次数 → exposureCount（直播间整体曝光次数）
                    string exposureCount = overviewBoard["直播间整体曝光次数"]?.ToString();
                    if (!string.IsNullOrEmpty(exposureCount))
                    {
                        exposureCount = exposureCount.Replace(",", "");
                        if (int.TryParse(exposureCount, out int exposureCountTemp))
                        {
                            model.exposureCount = exposureCountTemp;
                        }
                    }

                    // 实时在线人数 → onlineCount（实时在线人数）
                    string onlineCount = overviewBoard["实时在线人数"]?.ToString();
                    if (!string.IsNullOrEmpty(onlineCount))
                    {
                        onlineCount = onlineCount.Replace(",", "");
                        if (int.TryParse(onlineCount, out int onlineCountTemp))
                        {
                            onlineCountInt = onlineCountTemp;
                        }
                    }

                    // 直播间整体观看人数 → viewCount（场观）
                    string watchUcount = overviewBoard["直播间整体观看人数"]?.ToString();
                    if (!string.IsNullOrEmpty(watchUcount))
                    {
                        watchUcount = watchUcount.Replace(",", "");
                        if (int.TryParse(watchUcount, out int viewCount))
                        {
                            model.viewCount = viewCount;
                        }
                    }

                    // GPM → thousandSales（千次成交）
                    string gpm = overviewBoard["GPM"]?.ToString();
                    if (!string.IsNullOrEmpty(gpm))
                    {
                        gpm = gpm.Replace(",", "");
                        if (double.TryParse(gpm, out double gpmValue))
                        {
                            model.thousandSales = gpmValue;
                        }
                    }

                    // 整体支付ROI → roi（投资回报率） || 整体消耗
                    string roi = overviewBoard["整体支付ROI"]?.ToString();
                    if (!string.IsNullOrEmpty(roi))
                    {
                        roi = roi.Replace(",", "");
                        if (double.TryParse(roi, out double roiValue))
                        {
                            model.roi = roiValue;
                            model.overallCostRoi = roiValue;
                        }
                    }

                    // 净成交ROI -> 净成交ROI
                    String netTransactionRoi = overviewBoard["净成交ROI"]?.ToString();
                    if (!string.IsNullOrEmpty(netTransactionRoi))
                    {
                        netTransactionRoi = netTransactionRoi.Replace(",", "");
                        if (double.TryParse(netTransactionRoi, out double netTransactionRoiValue))
                        {
                            model.netTransactionRoi = netTransactionRoiValue;
                        }
                    }
                    
                    // 曝光观看率(次数) → interactionRate（互动率）
                    string showToWatchRate = overviewBoard["曝光观看率(次数)"]?.ToString();
                    if (!string.IsNullOrEmpty(showToWatchRate))
                    {
                        showToWatchRate = showToWatchRate.Replace("%", "").Replace(",", "");
                        if (double.TryParse(showToWatchRate, out double rate))
                        {
                            model.interactionRate = rate;
                        }
                    }
                }

                // 解析直播间详情数据（roomDetail）
                var roomDetail = jo["data"]?["roomDetail"] as JObject;
                if (roomDetail != null)
                {
                    // 解析结束时间
                    string roomEndTime = roomDetail["room_end_time"]?.ToString();
                    if (!string.IsNullOrEmpty(roomEndTime) && roomEndTime != "-")
                    {
                        // 场景1：直播已结束，使用 API 返回的实际结束时间
                        if (DateTime.TryParse(roomEndTime, out DateTime endTime))
                        {
                            model.endTime = endTime.ToString("yyyy-MM-dd HH:mm:ss");
                        }
                    }
                    else
                    {
                        // 场景2：直播进行中，尝试从 room_icon 提取 x-expires 时间戳
                        bool hasExtractedExpires = false;
                        
                        string roomIconValue = roomDetail["room_icon"]?.ToString();
                        if (!string.IsNullOrEmpty(roomIconValue))
                        {
                            // 解析 JSON 字符串
                            try
                            {
                                JObject roomIconJson = JObject.Parse(roomIconValue);
                                JArray urlList = roomIconJson["UrlList"] as JArray;
                                
                                if (urlList != null && urlList.Count > 0)
                                {
                                    string firstUrl = urlList[0].ToString();
                                    
                                    // 从 URL 中提取 x-expires 参数
                                    int expiresIndex = firstUrl.IndexOf("x-expires=");
                                    if (expiresIndex > 0)
                                    {
                                        string expiresStr = firstUrl.Substring(expiresIndex + 10); // "x-expires=" 长度为 10
                                        int endIndex = expiresStr.IndexOf("&");
                                        if (endIndex > 0)
                                        {
                                            expiresStr = expiresStr.Substring(0, endIndex);
                                        }
                                        
                                        // 解析 Unix 时间戳
                                        if (long.TryParse(expiresStr, out long expiresTimestamp))
                                        {
                                            var expiresTime = DateTimeOffset.FromUnixTimeSeconds(expiresTimestamp).LocalDateTime;
                                            model.endTime = expiresTime.ToString("yyyy-MM-dd HH:mm:ss");
                                            hasExtractedExpires = true;
                                        }
                                    }
                                }
                            }
                            catch
                            {
                                // 解析失败，使用兜底逻辑
                            }
                        }
                        
                        // 兜底：如果未能提取 x-expires，使用当前抓取时间
                        if (!hasExtractedExpires)
                        {
                            model.endTime = ServerTimeUtils.getCurrentTimeStr();
                        }
                    }
                    
                    // 解析开始时间（如果千川有值，则覆盖默认值）
                    string roomStartTime = roomDetail["room_start_time"]?.ToString();
                    if (!string.IsNullOrEmpty(roomStartTime) && roomStartTime != "-")
                    {
                        // API 返回格式为 "yyyy-MM-dd HH:mm:ss"，直接解析
                        if (DateTime.TryParse(roomStartTime, out DateTime startTime))
                        {
                            model.startTime = startTime.ToString("yyyy-MM-dd HH:mm:ss");
                        }
                    }
                }

                // 添加过程数据
                var processItem = new OceanEngineProcessBo
                {
                    gatherDateTime = ServerTimeUtils.getCurrentTimeStr(),
                    viewCount = model.viewCount,
                    salesRevenue = model.salesRevenue,
                    refund = model.refund,
                    investment = model.investment,
                    payComboCnt = model.payComboCnt,
                    followCount = model.followCount,
                    exposureCount = model.exposureCount,
                    onlineCount = onlineCountInt,
                    interactionRate = model.interactionRate,
                    clickPaymentRate = model.clickPaymentRate,
                    fansClubJoinUcnt = model.followCount
                };

                model.oceanEngineProcessList.Add(processItem);

                // 按商品列表顺序分配排序号，从1开始
                if (model.productList != null && model.productList.Count > 0)
                {
                    for (int i = 0; i < model.productList.Count; i++)
                    {
                        model.productList[i].sort = i + 1;
                    }
                }

                model.CalculateDerivedFields();
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"千川数据适配异常: {ex.Message}", "QianchuanAdapter");
            }

            return model;
        }

        /// <summary>
        /// 创建基础模型
        /// </summary>
        private UnifiedDataModel CreateBaseModel(string secUid, string roomId, string videoId)
        {
            return new UnifiedDataModel
            {
                secUid = secUid,
                batchNumber = roomId,
                platform = 0,
                startTime = ServerTimeUtils.getCurrentTimeStr(),
                oceanEngineProcessList = new System.Collections.Generic.List<OceanEngineProcessBo>(),
                productList = new System.Collections.Generic.List<VideoProductRequest>()
            };
        }
    }
}
