using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using ReviewAnalysis.plugins.interfaces;
using ReviewAnalysis.plugins.models;
using ReviewAnalysis.juliang;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using douyin.Utils;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.plugins.adapters
{
    /// <summary>
    /// 巨量数据适配器
    /// 将巨量平台原始JSON转换为统一数据模型
    /// </summary>
    public class JuliangAdapter : IDataAdapter
    {
        public string PlatformId => "juliang";
        public string PlatformName => "巨量百应";

        /// <summary>
        /// 适配巨量数据
        /// </summary>
        public UnifiedDataModel Adapt(string rawJson, string secUid, string roomId, string videoId = null)
        {
            var model = CreateBaseModel(secUid, roomId, videoId);

            if (string.IsNullOrEmpty(rawJson))
            {
                return model;
            }

            try
            {
                var jo = JObject.Parse(rawJson);

                // 解析巨量数据（JuliangedDataPuller返回的结构）
                // 结构: { "data": { "liveTalent": { "data": { "core_data": [...], "pay_amt": {...} } } } }
                var liveTalentData = jo["data"]?["liveTalent"]?["data"];
                if (liveTalentData != null)
                {
                    // 解析 core_data 数组为字典（以 index_name 为key）
                    var coreData = liveTalentData["core_data"] as JArray;
                    var dataDict = new Dictionary<string, JToken>();
                    if (coreData != null)
                    {
                        foreach (var item in coreData)
                        {
                            var indexName = item["index_name"]?.ToString();
                            if (!string.IsNullOrEmpty(indexName))
                            {
                                dataDict[indexName] = item["value"];
                            }
                        }
                    }

                    // 场观（累计观看人数）watch_ucnt
                    dataDict.TryGetValue("watch_ucnt", out var watchUcnt);
                    model.viewCount = watchUcnt?["value"]?.Value<long?>() ?? 0;

                    // 销售额 pay_amt（data顶层字段，单位分，需/100转元）
                    var payAmtToken = liveTalentData["pay_amt"];
                    var payAmtRaw = payAmtToken?["value"]?.Value<double?>() ?? 0;
                    model.salesRevenue = Math.Round(payAmtRaw / 100.0, 2);

                    // 退款金额 real_refund_amt（单位分，需/100转元）
                    dataDict.TryGetValue("real_refund_amt", out var refundAmt);
                    model.refund = Math.Round((refundAmt?["value"]?.Value<double?>() ?? 0) / 100.0, 2);

                    // 千川消耗/投放 stat_cost（单位分，需/100转元）
                    dataDict.TryGetValue("stat_cost", out var statCost);
                    model.investment = Math.Round((statCost?["value"]?.Value<double?>() ?? 0) / 100.0, 2);

                    // 成交件数 pay_combo_cnt
                    dataDict.TryGetValue("pay_combo_cnt", out var payComboCnt);
                    model.payComboCnt = payComboCnt?["value"]?.Value<int?>() ?? 0;

                    // 新增粉丝数 follow_anchor_ucnt
                    dataDict.TryGetValue("follow_anchor_ucnt", out var followAnchorUcnt);
                    model.followCount = followAnchorUcnt?["value"]?.Value<int?>() ?? 0;

                    // 曝光次数 live_show_cnt
                    dataDict.TryGetValue("live_show_cnt", out var liveShowCnt);
                    model.exposureCount = liveShowCnt?["value"]?.Value<long?>() ?? 0;

                    // 千次观看成交金额 gpm（单位分，需/100转元）
                    dataDict.TryGetValue("gpm", out var gpm);
                    model.thousandSales = Math.Round((gpm?["value"]?.Value<double?>() ?? 0) / 100.0, 2);

                    // 商品点击-成交率 product_click_pay_ucnt_ratio（API返回小数，需*100转百分比）
                    dataDict.TryGetValue("product_click_pay_ucnt_ratio", out var clickPayRatio);
                    model.clickPaymentRate = Math.Round((clickPayRatio?["value"]?.Value<double?>() ?? 0) * 100, 2);
                    
                    // 曝光-观看率(次数) live_show_watch_cnt_ratio（API返回小数，需*100转百分比）
                    dataDict.TryGetValue("live_show_watch_cnt_ratio", out var showWatchCntRatio);
                    model.showWatchCntRatio = Math.Round((showWatchCntRatio?["value"]?.Value<double?>() ?? 0) * 100, 2);

                    // 互动率 watch_interact_ucnt_ratio（API返回小数，需*100转百分比）
                    dataDict.TryGetValue("watch_interact_ucnt_ratio", out var interactRatio);
                    model.interactionRate = Math.Round((interactRatio?["value"]?.Value<double?>() ?? 0) * 100, 2);

                    // 实时在线人数 online_user_cnt（近似最高在线）
                    dataDict.TryGetValue("online_user_cnt", out var onlineUserCnt);
                    model.maxOnline = onlineUserCnt?["value"]?.Value<int?>();
                    model.averageOnlineNum = model.maxOnline;  // 保持 null，让后续逻辑处理

                    // 平均停留时长 avg_watch_duration（单位：秒）
                    dataDict.TryGetValue("avg_watch_duration", out var avgWatchDuration);
                    model.averageResidenceTime = avgWatchDuration?["value"]?.Value<int?>();

                    // 整体消耗
                    model.overallCostRoi = (model.investment??0) > 0 ? Math.Round((model.salesRevenue??0.0) / (model.investment??0), 2) : 0;

                    // 净成交ROI
                    model.netTransactionRoi = (model.investment??0) > 0 ? Math.Round(((model.salesRevenue??0.0) - (model.refund??0.0)) / (model.investment??0), 2) : 0;

                    // 新增直播团人数 fans_club_join_ucnt
                    dataDict.TryGetValue("fans_club_join_ucnt", out var fansClubJoinUcnt);
                    var fansClubJoinUcntValue = fansClubJoinUcnt?["value"]?.Value<int?>() ?? 0;

                    // 创建过程数据条目
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
                        onlineCount = model.maxOnline,
                        interactionRate = model.interactionRate,
                        clickPaymentRate = model.clickPaymentRate,
                        fansClubJoinUcnt = fansClubJoinUcntValue
                    };
                    model.oceanEngineProcessList.Add(processItem);
                }

                // 解析 liveBasicScreen 数据（直播开始/结束时间）
                var liveBasicScreenData = jo["data"]?["liveBasicScreen"]?["data"];
                if (liveBasicScreenData != null)
                {
                    // 开始时间 live_start_time 格式: "2026/04/17 10:52:25"
                    string startTimeStr = liveBasicScreenData["live_start_time"]?.ToString();
                    if (!string.IsNullOrEmpty(startTimeStr)
                        && DateTime.TryParseExact(startTimeStr, "yyyy/MM/dd HH:mm:ss",
                            CultureInfo.InvariantCulture, DateTimeStyles.None, out DateTime startTime))
                    {
                        model.startTime = startTime.ToString("yyyy-MM-dd HH:mm:ss");
                    }

                    // 结束时间：优先使用 live_end_ts（直播已结束），否则用 server_cur_time（直播中）
                    long liveEndTs = liveBasicScreenData["live_end_ts"]?.Value<long>() ?? 0;
                    if (liveEndTs > 0)
                    {
                        // live_end_ts 是 Unix 秒级时间戳
                        DateTime endTime = DateTimeOffset.FromUnixTimeSeconds(liveEndTs).DateTime.ToLocalTime();
                        model.endTime = endTime.ToString("yyyy-MM-dd HH:mm:ss");
                    }
                    else
                    {
                        // 直播中，用服务器当前时间作为快照结束时间
                        string serverCurTimeStr = liveBasicScreenData["server_cur_time"]?.ToString();
                        if (!string.IsNullOrEmpty(serverCurTimeStr)
                            && DateTime.TryParseExact(serverCurTimeStr, "yyyy/MM/dd HH:mm:ss",
                                CultureInfo.InvariantCulture, DateTimeStyles.None, out DateTime serverCurTime))
                        {
                            model.endTime = serverCurTime.ToString("yyyy-MM-dd HH:mm:ss");
                        }
                    }
                }

                // 解析商品列表（ProductPaymentDataAsync 返回的数据）
                var productData = jo["data"]?["liveProduct"];
                if (productData != null)
                {
                    // 兼容两种数据结构：{"product_list": [...]} 或 {"data": {"product_list": [...]}}
                    JArray productList = productData["product_list"] as JArray;
                    if (productList == null && productData["data"] != null)
                    {
                        productList = productData["data"]["product_list"] as JArray;
                    }
                    
                    if (productList != null)
                    {
                        foreach (var item in productList)
                        {
                            var product = new VideoProductRequest
                            {
                                productId = GetProductFieldValue(item, "product_id"),
                                title = GetProductFieldValue(item, "title")?.ToString(),
                                imageUri = GetProductFieldValue(item, "image_uri")?.ToString(),
                                marketPrice = DivideBy100(GetProductFieldValueAsDouble(item, "market_price")),
                                productBindTime = FormatUnixTimestamp(GetProductFieldValue(item, "product_bind_time")),
                                explainCnt = (int?)GetProductFieldValueAsLong(item, "explain_cnt"),
                                productShowUcnt = GetProductFieldValueAsLong(item, "product_show_ucnt"),
                                productClickUcnt = GetProductFieldValueAsLong(item, "product_click_ucnt"),
                                productShowClickUcntRatio = MultiplyBy100(GetProductFieldValueAsDouble(item, "product_show_click_ucnt_ratio")),
                                productShowPayUcntRatio = MultiplyBy100(GetProductFieldValueAsDouble(item, "product_show_pay_ucnt_ratio")),
                                productClickPayUcntRatio = MultiplyBy100(GetProductFieldValueAsDouble(item, "product_click_pay_ucnt_ratio")),
                                gpm = DivideBy100(GetProductFieldValueAsDouble(item, "gpm")),
                                payAmt = DivideBy100(GetProductFieldValueAsDouble(item, "pay_amt")),
                                avgMaxPayAmtMin = DivideBy100(GetProductFieldValueAsDouble(item, "avg_max_pay_amt_min")),
                                payComboCnt = GetProductFieldValueAsLong(item, "pay_combo_cnt"),
                                payCnt = GetProductFieldValueAsLong(item, "pay_cnt"),
                                createCnt = GetProductFieldValueAsLong(item, "create_cnt"),
                                createPayUcntRatio = MultiplyBy100(GetProductFieldValueAsDouble(item, "create_pay_ucnt_ratio")),
                                payDepositPreOrderCnt = GetProductFieldValueAsLong(item, "pay_deposit_pre_order_cnt"),
                                presaleDepayDeamt = DivideBy100(GetProductFieldValueAsDouble(item, "presale_depay_deamt")),
                                payDepositPreOrderAmt = DivideBy100(GetProductFieldValueAsDouble(item, "pay_deposit_pre_order_amt")),
                                refundCnt = GetProductFieldValueAsLong(item, "refund_cnt"),
                                realRefundAmt = DivideBy100(GetProductFieldValueAsDouble(item, "real_refund_amt")),
                                refundRate = MultiplyBy100(GetProductFieldValueAsDouble(item, "refund_rate"))
                            };
                            // 计算衍生字段
                            if (product.productShowUcnt.HasValue && model.viewCount.HasValue && model.viewCount.Value > 0)
                            {
                                product.productViewShowRatio = Math.Round(
                                    (double)product.productShowUcnt.Value / model.viewCount.Value * 100, 2);
                            }
                            product.avgPayAmtPerOrder = product.marketPrice;

                            product.commodityProcessDataList = new List<CommodityProcessDataBo>
                            {
                                new CommodityProcessDataBo
                                {
                                    dateTime = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"),
                                    productShowUcnt = product.productShowUcnt,
                                    productClickUcnt = product.productClickUcnt,
                                    viewCount = (int?)model.viewCount,
                                    explainCnt = product.explainCnt,
                                    payComboCnt = (int?)product.payComboCnt,
                                    payAmt = product.payAmt,
                                    gpm = product.gpm,
                                    avgMaxPayAmtMin = product.avgMaxPayAmtMin,
                                    createCnt = product.createCnt,
                                    createPayUcntRatio = product.createPayUcntRatio,
                                    payDepositPreOrderCnt = product.payDepositPreOrderCnt,
                                    presaleDepayDeamt = product.presaleDepayDeamt,
                                    payDepositPreOrderAmt = product.payDepositPreOrderAmt,
                                    refundCnt = product.refundCnt,
                                    realRefundAmt = product.realRefundAmt
                                }
                            };

                            model.productList.Add(product);
                        }
                    }
                }

                // 当 API 返回的 salesRevenue 为 0 但商品有销售数据时，同步兜底 processItem
                if ((model.salesRevenue ?? 0) == 0 && model.productList != null && model.productList.Count > 0)
                {
                    var productTotal = model.productList.Sum(p => p.payAmt ?? 0);
                    if (productTotal > 0)
                    {
                        model.salesRevenue = productTotal;
                        // 同步更新最新一条过程数据的 salesRevenue
                        if (model.oceanEngineProcessList != null && model.oceanEngineProcessList.Count > 0)
                        {
                            model.oceanEngineProcessList[model.oceanEngineProcessList.Count - 1].salesRevenue = productTotal;
                        }
                    }
                }

                // 当 API 返回的 refundQuantity 为 null/0 但商品有退款数据时，从商品汇总补充
                if ((model.refundQuantity ?? 0) == 0 && model.productList != null && model.productList.Count > 0)
                {
                    var refundTotal = model.productList.Sum(p => p.refundCnt ?? 0);
                    if (refundTotal > 0)
                    {
                        model.refundQuantity = (int)refundTotal;
                        // 同步更新最新一条过程数据的 refundQuantity
                        if (model.oceanEngineProcessList != null && model.oceanEngineProcessList.Count > 0)
                        {
                            model.oceanEngineProcessList[model.oceanEngineProcessList.Count - 1].refundQuantity = (int)refundTotal;
                        }
                    }
                }

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
                // 解析失败，返回空模型
                FileUtils.LogError($"巨量数据适配异常: {ex.Message}", "JuliangAdapter");
            }

            return model;
        }

        /// <summary>
        /// 获取商品字段值（支持直接值或 {value: ...} 格式）
        /// </summary>
        private string GetProductFieldValue(JToken item, string fieldName)
        {
            var field = item[fieldName];
            if (field == null) return null;

            // 如果是对象且有 value 属性
            if (field is JObject obj && obj["value"] != null)
            {
                return obj["value"]?.ToString();
            }

            return field.ToString();
        }

        /// <summary>
        /// 获取商品字段值作为 double（支持直接值或 {value: ...} 格式）
        /// </summary>
        private double? GetProductFieldValueAsDouble(JToken item, string fieldName)
        {
            var field = item[fieldName];
            if (field == null) return null;

            // 如果是对象且有 value 属性
            if (field is JObject obj && obj["value"] != null)
            {
                return obj["value"]?.Value<double?>();
            }

            return field.Value<double?>();
        }

        /// <summary>
        /// 获取商品字段值作为 long（支持直接值或 {value: ...} 格式）
        /// </summary>
        private long? GetProductFieldValueAsLong(JToken item, string fieldName)
        {
            var field = item[fieldName];
            if (field == null) return null;

            // 如果是对象且有 value 属性
            if (field is JObject obj && obj["value"] != null)
            {
                return obj["value"]?.Value<long?>();
            }

            return field.Value<long?>();
        }

        /// <summary>
        /// 将以分为单位的金额值转换为元（÷100）
        /// </summary>
        private double? DivideBy100(double? value)
            => value.HasValue ? Math.Round(value.Value / 100.0, 2) : (double?)null;

        /// <summary>
        /// 将小数转换为百分比（×100）
        /// </summary>
        private double? MultiplyBy100(double? value)
            => value.HasValue ? Math.Round(value.Value * 100.0, 2) : (double?)null;

        /// <summary>
        /// 将Unix时间戳（秒）转换为 yyyy-MM-dd HH:mm:ss 格式
        /// </summary>
        private string FormatUnixTimestamp(string timestampStr)
        {
            if (string.IsNullOrEmpty(timestampStr)) return null;
            if (long.TryParse(timestampStr, out long timestamp))
            {
                try
                {
                    return DateTimeOffset.FromUnixTimeSeconds(timestamp).DateTime.ToLocalTime()
                        .ToString("yyyy-MM-dd HH:mm:ss");
                }
                catch { return timestampStr; }
            }
            // 已经是日期格式则原样返回
            return timestampStr;
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
                platform = 0, // 抖音
                startTime = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"),
                oceanEngineProcessList = new System.Collections.Generic.List<OceanEngineProcessBo>(),
                productList = new System.Collections.Generic.List<VideoProductRequest>()
            };
        }
    }
}
