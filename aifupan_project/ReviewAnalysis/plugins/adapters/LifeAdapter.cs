using System;
using System.Collections.Generic;
using System.Linq;
using ReviewAnalysis.plugins.interfaces;
using ReviewAnalysis.plugins.models;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using douyin.Utils;

namespace ReviewAnalysis.plugins.adapters
{
    /// <summary>
    /// 来客数据适配器
    /// 将来客平台原始JSON转换为统一数据模型
    /// </summary>
    public class LifeAdapter : IDataAdapter
    {
        public string PlatformId => "life";
        public string PlatformName => "来客";

        /// <summary>
        /// 适配来客数据
        /// 根据 dataType 区分两种大屏数据：
        ///   - liveScreenOverviewData：线索大屏（团购版），key 为 PascalCase，value 含格式化字符串
        ///   - liveScreenKeyIndex：EOS 大屏核心指标，key 为 PascalCase，value 为原始数值字符串
        /// </summary>
        public UnifiedDataModel Adapt(string rawJson, string secUid, string roomId, string videoId = null)
        {
            var model = CreateBaseModel(secUid, roomId, videoId);

            if (string.IsNullOrEmpty(rawJson))
                return model;

            try
            {
                var jo = JObject.Parse(rawJson);
                string dataType = jo["dataType"]?.ToString();
                string collectTime = jo["collectTime"]?.ToString()
                    ?? DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");

                var data = jo["data"] as JObject;
                if (data == null) return model;

                // 过程数据临时变量
                int? currentOnline = null;
                int? fansClubJoin = null;

                // 统一结构：data.liveScreenOverviewData 或 data.liveScreenKeyIndex 下包含 keyIndex + productList
                JObject screenNode = null;
                if (dataType == "liveScreenOverviewData")
                    screenNode = data["liveScreenOverviewData"] as JObject;
                else if (dataType == "liveScreenKeyIndex")
                    screenNode = data["liveScreenKeyIndex"] as JObject;

                if (screenNode != null)
                {
                    var metrics = screenNode["keyIndex"] as JObject;
                    if (metrics != null)
                    {
                        if (dataType == "liveScreenOverviewData")
                        {
                            model.viewCount = TryParseLong(metrics["LiveWatchUvByRoom"]);
                            model.salesRevenue = TryParseDouble(metrics["LiveLifePayOrderGmvAll"]);
                            model.payComboCnt = TryParseInt(metrics["LiveLifePayOrderCountAll"]);
                            model.investment = TryParseDouble(metrics["StatCost"]);
                            model.averageOnlineNum = TryParseInt(metrics["LiveAvgOnlineUvByRoom"]);
                            model.maxOnline = TryParseInt(metrics["LiveMinuteMaxWatchUv"]);
                            model.followCount = TryParseInt(metrics["LiveFollowUvByRoom"]);
                            model.averageResidenceTime = ParseDuration(metrics["LiveAvgWatchDurationByRoom"]);
                            model.exposureCount = TryParseLong(metrics["LiveShowCount"])
                                                  ?? TryParseLong(metrics["LiveShowUvByRoom"]);
                            model.interactionRate = ParsePercent(metrics["LiveInteractionRate"]);
                            model.followRate = ParsePercent(metrics["LiveWatchFollowRate"]);
                            // 【修正】带货转化率：模型公式=成交单量÷观看人数×100，用 团购订单数(LiveLifePayOrderCountAll)÷累计观看人数(LiveWatchUvByRoom) 计算。
                            //   原取 ClueConvertRate 是"线索转化率=留资人数÷观看人数"（官方数据说明第19行），属留资指标非成交指标，口径错误已弃用
                            if (model.payComboCnt.HasValue && (model.viewCount ?? 0) > 0)
                                model.conversionRate = Math.Round((double)model.payComboCnt.Value / model.viewCount.Value * 100, 2);
                            model.thousandSales = TryParseDouble(metrics["PayOrderGmvPer1kWatchUv"]);
                            // 【补全#3-修正】点击-成交率：模型公式=区间订单量÷区间点击量，官方数据说明证实分子分母都有：
                            //   团购订单数(LiveLifePayOrderCountAll)÷商品点击次数(LiveLifeProductClickCountAll)×100，口径与模型定义一致。
                            //   原方案A直取 PayOrderUvConvertRate(订单人数÷观看人数)是误以为无分母时的妥协，已替换；点击量缺失/为0时留null不造数
                            var productClickCnt = TryParseDouble(metrics["LiveLifeProductClickCountAll"]);
                            if (model.payComboCnt.HasValue && (productClickCnt ?? 0) > 0)
                                model.clickPaymentRate = Math.Round(model.payComboCnt.Value / productClickCnt.Value * 100, 2);
                            // 【补全#4-修正】曝光-观看率(次数)：严格次数口径 = 观看次数(LiveWatchCount)÷曝光次数(LiveShowCount)×100。
                            //   原取 LiveEnterRate 是人数口径(观看人数÷曝光人数，官方数据说明第36行)，与模型"次数"字段名不符，保留作兑底
                            var watchCnt = TryParseDouble(metrics["LiveWatchCount"]);
                            var showCntTotal = TryParseDouble(metrics["LiveShowCount"]);
                            model.showWatchCntRatio = ParsePercent(metrics["LiveEnterRate"]);
                            if ((model.showWatchCntRatio??0) <= 0 && watchCnt.HasValue && (showCntTotal ?? 0) > 0)
                                model.showWatchCntRatio = Math.Round(watchCnt.Value / showCntTotal.Value * 100, 2);
                            currentOnline = TryParseInt(metrics["UvRealtime"]);
                            fansClubJoin = TryParseInt(metrics["LiveFansClubJoinUvByRoom"]);
                        }
                        else if (dataType == "liveScreenKeyIndex")
                        {
                            model.viewCount = TryParseLong(metrics["LiveServerWatchUcnt"]);
                            model.salesRevenue = TryParseDouble(metrics["PayGmv"]);
                            model.payComboCnt = TryParseInt(metrics["PayOrderCnt"]);
                            model.averageOnlineNum = TryParseInt(metrics["AcuTotalTd"]);
                            model.maxOnline = TryParseInt(metrics["PcuTotalTd"]);
                            model.followCount = TryParseInt(metrics["LiveFollowAnchorUcnt"]);
                            model.averageResidenceTime = TryParseInt(metrics["ClientAvgWatchDuration"]);
                            model.exposureCount = TryParseLong(metrics["ClientLiveShowCntTd"]);
                            // 【修正】带货转化率：模型公式=成交单量÷观看人数×100，用 成交订单数(PayOrderCnt)÷累计观看人数(LiveServerWatchUcnt) 计算。
                            //   原取 GoodsCvrUv ?? LiveCvr 两处都不对：GoodsCvrUv 在真实31项中不存在（永远落到兑底）；
                            //   LiveCvr 官方定义为"累计成功支付订单量÷累计看播次数"（分母是次数），与模型人数分母不符。修正后与线索版同口径
                            if (model.payComboCnt.HasValue && (model.viewCount ?? 0) > 0)
                                model.conversionRate = Math.Round((double)model.payComboCnt.Value / model.viewCount.Value * 100, 2);
                            model.thousandSales = TryParseDouble(metrics["GPM"]);
                            // 【补全#3】点击-成交率：EOS版 GoodsCvr 已验证 = PayUvAll÷ProductClickUvAll（样本7÷10=70%），
                            //   与巨量 product_click_pay_ucnt_ratio 人数口径一致；缺失时用分子分母兑底计算
                            model.clickPaymentRate = ParsePercent(metrics["GoodsCvr"]);
                            if (model.clickPaymentRate == null)
                            {
                                var payUvAll = TryParseDouble(metrics["PayUvAll"]);
                                var productClickUv = TryParseDouble(metrics["ProductClickUvAll"]);
                                if (payUvAll.HasValue && productClickUv.HasValue && productClickUv.Value > 0)
                                    model.clickPaymentRate = Math.Round(payUvAll.Value / productClickUv.Value * 100, 2);
                            }
                            // 【补全#4】曝光-观看率：EOS版 LiveCtr（直播曝光点击率）即"曝光→进入直播间"口径
                            model.showWatchCntRatio = ParsePercent(metrics["LiveCtr"]);
                            // 【补全#10】涨粉率：EOS版无直接字段，用 新增关注人数÷观看人数×100 计算（样本6÷451=1.33%）
                            var followUcnt = TryParseDouble(metrics["LiveFollowAnchorUcnt"]);
                            var watchUcnt = TryParseDouble(metrics["LiveServerWatchUcnt"]);
                            if (followUcnt.HasValue && watchUcnt.HasValue && watchUcnt.Value > 0)
                                model.followRate = Math.Round(followUcnt.Value / watchUcnt.Value * 100, 2);
                            // 【补全#9-修正】互动率：模型公式=区间互动次数÷区间观看人次。官方说明证实EOS版31项中有三个互动次数字段：
                            //   累计评论次数(ServerCommentCntTd) + 累计点赞次数(ServerLikeCntTotal) + 累计分享次数(ClientShareCntTd)
                            //   = 互动次数，除以累计观看人数×100。原方案A用"评论人数÷观看人数"是不知有这三个次数字段时的妥协，已替换
                            var commentCnt = TryParseDouble(metrics["ServerCommentCntTd"]);
                            var likeCnt = TryParseDouble(metrics["ServerLikeCntTotal"]);
                            var shareCnt = TryParseDouble(metrics["ClientShareCntTd"]);
                            var serverWatchCntTd = TryParseDouble(metrics["ServerWatchCntTd"]);
                            if ((commentCnt.HasValue || likeCnt.HasValue || shareCnt.HasValue)
                                && serverWatchCntTd.HasValue && serverWatchCntTd.Value > 0)
                            {
                                double interactTotal = (commentCnt ?? 0) + (likeCnt ?? 0) + (shareCnt ?? 0);
                                model.interactionRate = Math.Round(interactTotal / serverWatchCntTd.Value * 100, 2);
                            }
                            currentOnline = TryParseInt(metrics["CurrentUserCnt"]);
                            fansClubJoin = TryParseInt(metrics["FansClubJoinUv"]);
                        }
                    }

                    // 解析商品列表（传入 viewCount 用于计算曝光观看率，collectTime 用于商品过程快照时间戳）
                    var productsNode = screenNode["productList"] as JArray;
                    if (productsNode != null && productsNode.Count > 0)
                    {
                        model.productList = ParseProducts(productsNode, roomId, dataType, model.viewCount, collectTime);
                    }
                }

                // ==================== 顶层汇总/计算补全（两版通用） ====================

                // 【补全#1】退款金额（累计）：两版keyIndex均无直接字段，从商品列表 realRefundAmt 汇总（同巨量兑底模式）
                if (model.refund == null && model.productList != null && model.productList.Count > 0)
                {
                    var refundSum = model.productList.Where(p => p.realRefundAmt.HasValue).Sum(p => p.realRefundAmt.Value);
                    if (model.productList.Any(p => p.realRefundAmt.HasValue))
                        model.refund = Math.Round(refundSum, 2);
                }

                // 【补全#2】退款数量（累计）：从商品列表 refundCnt 汇总（同巨量L271-283兑底模式）
                if (model.refundQuantity == null && model.productList != null && model.productList.Count > 0)
                {
                    var refundCntSum = model.productList.Sum(p => p.refundCnt ?? 0);
                    if (refundCntSum > 0)
                        model.refundQuantity = (int)refundCntSum;
                }

                // 【补全-兑底】销售额：API返回0但商品有成交时，用商品 payAmt 汇总兑底（同巨量L256-268）
                if ((model.salesRevenue ?? 0) == 0 && model.productList != null && model.productList.Count > 0)
                {
                    var productPayTotal = model.productList.Sum(p => p.payAmt ?? 0);
                    if (productPayTotal > 0)
                        model.salesRevenue = Math.Round(productPayTotal, 2);
                }

                // 【补全#5】视频结束时间：来客无下播时间字段，用本次采集时间作快照结束时间
                //   （每次采集刷新；上传时 UploadScheduler.handleTime 会用过程数据 max 再校正，双重保障）
                if (string.IsNullOrEmpty(model.endTime))
                    model.endTime = collectTime;

                // 【补全#6】整体消耗ROI：销售金额÷投放金额（公式同巨量L115；EOS版无investment时保持null不造数）
                if ((model.investment ?? 0) > 0)
                    model.overallCostRoi = Math.Round((model.salesRevenue ?? 0.0) / model.investment.Value, 2);

                // 【补全#7】净成交ROI：(销售金额-退款金额)÷投放金额（公式同巨量L118）
                if ((model.investment ?? 0) > 0)
                    model.netTransactionRoi = Math.Round(((model.salesRevenue ?? 0.0) - (model.refund ?? 0.0)) / model.investment.Value, 2);

                // 构建过程数据条目
                model.oceanEngineProcessList.Add(new OceanEngineProcessBo
                {
                    gatherDateTime = collectTime,
                    viewCount = model.viewCount,
                    salesRevenue = model.salesRevenue,
                    investment = model.investment,
                    payComboCnt = model.payComboCnt,
                    followCount = model.followCount,
                    onlineCount = currentOnline,
                    fansClubJoinUcnt = fansClubJoin,
                    exposureCount = model.exposureCount,
                    // 【补全#11】退款金额：取顶层已汇总值（同巨量processItem L130）
                    refund = model.refund,
                    // 【补全#12】退款数量：取顶层已汇总值
                    refundQuantity = model.refundQuantity,
                    // 【补全#13】点击成交率：取顶层值（同巨量processItem L137）
                    clickPaymentRate = model.clickPaymentRate,
                    // 【补全#14】互动率：取顶层值（同巨量processItem L136）
                    interactionRate = model.interactionRate
                });

                // 按商品列表顺序分配排序号，从1开始
                if (model.productList != null && model.productList.Count > 0)
                {
                    for (int i = 0; i < model.productList.Count; i++)
                    {
                        model.productList[i].sort = i + 1;
                    }
                }

                model.CalculateDerivedFields();

                // ==================== 补全结果落日志（供后续核查字段填充情况） ====================
                // 第1条：摘要行，直接 grep "[来客适配摘要]" 即可快速看出哪些补全字段有值/为null，无需解析大JSON
                // 第2条：完整 UnifiedDataModel JSON（含商品与过程数据），供逐字段比对验证
                LogAdaptResult(model, dataType, secUid, roomId);
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"来客数据适配异常: {ex.Message}", "LifeAdapter");
            }

            return model;
        }

        /// <summary>
        /// 将适配（字段补全）后的统一数据模型写入日志，供后续核查补全效果。
        /// 日志分两条：先摘要（关键补全字段键值），再完整JSON。
        /// </summary>
        private static void LogAdaptResult(UnifiedDataModel model, string dataType, string secUid, string roomId)
        {
            try
            {
                string version = dataType == "liveScreenOverviewData" ? "线索版" : "普通版EOS";
                string secUidShort = string.IsNullOrEmpty(secUid)
                    ? ""
                    : secUid.Substring(0, Math.Min(20, secUid.Length)) + "...";

                // 摘要：本次补全的关键字段（null 直接显示 null，便于判断是否成功填充）
                var p0 = model.productList != null && model.productList.Count > 0 ? model.productList[0] : null;
                FileUtils.LogRpa(
                    $"[来客适配摘要] {version} roomId={roomId} secUid={secUidShort} | " +
                    $"顶层: refund={model.refund?.ToString() ?? "null"}, refundQty={model.refundQuantity?.ToString() ?? "null"}, " +
                    $"clickPayRate={model.clickPaymentRate?.ToString() ?? "null"}, showWatchRatio={model.showWatchCntRatio?.ToString() ?? "null"}, " +
                    $"interactRate={model.interactionRate?.ToString() ?? "null"}, followRate={model.followRate?.ToString() ?? "null"}, " +
                    $"investment={model.investment?.ToString() ?? "null"}, overallRoi={model.overallCostRoi?.ToString() ?? "null"}, " +
                    $"netRoi={model.netTransactionRoi?.ToString() ?? "null"}, endTime={model.endTime ?? "null"} | " +
                    $"商品数={model.productList?.Count ?? 0}" +
                    (p0 == null ? "" :
                        $", 首个商品: gpm={p0.gpm?.ToString() ?? "null"}, showClickRatio={p0.productShowClickUcntRatio?.ToString() ?? "null"}, " +
                        $"clickPayRatio={p0.productClickPayUcntRatio?.ToString() ?? "null"}, avgPayPerOrder={p0.avgPayAmtPerOrder?.ToString() ?? "null"}, " +
                        $"maxPayMin={p0.avgMaxPayAmtMin?.ToString() ?? "null"}, createCnt={p0.createCnt?.ToString() ?? "null"}, " +
                        $"payCnt={p0.payCnt?.ToString() ?? "null"}, refundAmt={p0.realRefundAmt?.ToString() ?? "null"}, " +
                        $"downTime={p0.productDownTime ?? "null"}, explainCnt={p0.explainCnt?.ToString() ?? "null"}, " +
                        $"快照数={p0.commodityProcessDataList?.Count ?? 0}"),
                    "来客适配器");

                // 完整统一模型JSON（不缩进，单行存储便于后续 grep 与提取）
                FileUtils.LogRpa(
                    $"[来客适配结果JSON] {version} roomId={roomId} | " +
                    JsonConvert.SerializeObject(model, Formatting.None),
                    "来客适配器");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"[来客适配结果JSON] 写日志异常: {ex.Message}", "来客适配器");
            }
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
                startTime = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"),
                oceanEngineProcessList = new System.Collections.Generic.List<OceanEngineProcessBo>(),
                productList = new System.Collections.Generic.List<VideoProductRequest>()
            };
        }

        /// <summary>
        /// 解析商品列表，兼容线索版（中文key）和EOS版（中文key）两种格式
        /// </summary>
        /// <param name="roomViewCount">直播间观看人数（用于计算商品曝光观看率）</param>
        /// <param name="collectTime">本次采集时间（用于商品过程快照时间戳）</param>
        private List<VideoProductRequest> ParseProducts(JArray productsNode, string roomId, string dataType, long? roomViewCount, string collectTime)
        {
            // 版本硬隔离：线索版与EOS版商品字段名与口径完全不同，各走独立解析方法，
            //   避免候选key混写造成两版互相影响（以前 dataType 参数传入后未使用，仅靠key名巧合互斥，脉络脆弱）
            bool isClue = dataType == "liveScreenOverviewData";
            var result = new List<VideoProductRequest>();

            foreach (var item in productsNode)
            {
                var productNode = item as JObject;
                if (productNode == null) continue;

                var product = new VideoProductRequest
                {
                    batchNumber = roomId
                };

                if (isClue)
                    ParseClueProduct(productNode, product);
                else
                    ParseEosProduct(productNode, product);

                // 两版通用的派生计算与过程快照（仅依赖已填充好的 product 字段，不再读原始key）
                FillCommonProductFields(product, roomViewCount, collectTime);

                result.Add(product);
            }

            return result;
        }

        /// <summary>
        /// 【线索版】商品字段解析。依据官方《线索版-商品说明》，接口固定返嚀17个中文key：
        /// 商品id/商品创建时间/商品更新时间/价格/商品名称/商品图片/详情页曝光人数/订单人数/填手机号数/
        /// 曝光人数/曝光次数/GMV/点击次数/详情页曝光次数/点击人数/线索留资次数/订单数
        /// </summary>
        private static void ParseClueProduct(JObject productNode, VideoProductRequest product)
        {
            // 商品基本信息
            product.productId = GetStringValue(productNode, "商品id", "productID");
            product.title = GetStringValue(productNode, "商品名称", "productName");
            product.imageUri = GetStringValue(productNode, "商品图片", "coverURL");

            // 价格：线索版经 LifeDataApi.FormatPrice 处理为 "¥x.xx" 字符串，需去符号
            var priceStr = GetStringValue(productNode, "价格", "price");
            if (!string.IsNullOrEmpty(priceStr))
                product.marketPrice = TryParseDoubleStr(priceStr.Replace("¥", ""));

            // 上架时间：线索版只有"商品创建时间"（无上架时间字段，取创建时间作近似）
            product.productBindTime = GetStringValue(productNode, "商品创建时间", "createTime");

            // 曝光/点击：线索版同时提供人数与次数，模型Ucnt字段取人数（口径精确对齐）
            product.productShowUcnt = TryParseLongFromNode(productNode, "曝光人数", "product_show_uv_all");
            product.productClickUcnt = TryParseLongFromNode(productNode, "点击人数", "product_click_uv_all");

            // 成交金额与订单数
            product.payAmt = TryParseDoubleStr(GetStringValue(productNode, "GMV", "live_life_pay_order_gmv_all"));
            product.payComboCnt = TryParseLongFromNode(productNode, "订单数", "live_life_pay_order_count_all");
            // 【#22】累计成交订单数：线索版无"件数vs单数"之分，与 payComboCnt 同源同值
            product.payCnt = product.payComboCnt;

            // 【#17】曝光-点击转化率：线索版无现成比率，用 点击人数÷曝光人数×100 计算（人数口径，与模型Ucnt名称一致）
            if (product.productClickUcnt.HasValue && (product.productShowUcnt ?? 0) > 0)
                product.productShowClickUcntRatio = Math.Round(
                    (double)product.productClickUcnt.Value / product.productShowUcnt.Value * 100, 2);

            // 【#18】曝光-成交转化率：用 订单人数÷曝光人数×100（人数口径，与巨量 product_show_pay_ucnt_ratio 一致）
            var payOrderUv = TryParseLongFromNode(productNode, "订单人数", "live_life_pay_order_uv_all");
            if (payOrderUv.HasValue && (product.productShowUcnt ?? 0) > 0)
                product.productShowPayUcntRatio = Math.Round(
                    (double)payOrderUv.Value / product.productShowUcnt.Value * 100, 2);

            // 【#19】点击-成交转化率：用 订单人数÷点击人数×100（人数口径，同巨量）
            if (payOrderUv.HasValue && (product.productClickUcnt ?? 0) > 0)
                product.productClickPayUcntRatio = Math.Round(
                    (double)payOrderUv.Value / product.productClickUcnt.Value * 100, 2);

            // 【#20】GPM：无现成字段，用 成交金额÷曝光次数×1000 计算
            var showCnt = TryParseLongFromNode(productNode, "曝光次数", "live_life_product_show_count_all");
            if (product.payAmt.HasValue && (showCnt ?? 0) > 0)
                product.gpm = Math.Round(product.payAmt.Value / showCnt.Value * 1000, 2);

            // 线索版确认无源的字段（保持null不造数）：
            //   productDownTime(无下架时间)、explainCnt(无讲解次数)、avgMaxPayAmtMin(无分钟最高成交)、
            //   createCnt/createPayUcntRatio(无未支付订单数与下单率)、
            //   refundCnt/refundRate/realRefundAmt(17键中完全无退款类字段)、预售三项(本地生活无此业务)
            // 另有 详情页曝光人数/详情页曝光次数/填手机号数/线索留资次数/商品更新时间 5个留资类指标，
            //   VideoProductRequest 无语义对应字段，故不映射（强塞会污染口径）
        }

        /// <summary>
        /// 【普通版EOS】商品字段解析。依据官方《普通版-商品列表说明》与真实采集样本。
        /// 注意：EOS版商品只有次数口径（goods_show_cnt/goods_click_cnt），没有商品级曝光/点击人数，
        /// 因此模型中 Ucnt(人数)命名的字段实际承载次数口径（已确认接受，宁可口径名不符也不丢数据）。
        /// </summary>
        private static void ParseEosProduct(JObject productNode, VideoProductRequest product)
        {
            // 商品基本信息
            product.productId = GetStringValue(productNode, "商品 ID", "product_id");
            product.title = GetStringValue(productNode, "商品名称", "product_name");
            product.imageUri = GetStringValue(productNode, "商品图片", "product_image");

            // 价格：EOS版直接返回数字（单位元），优先取商品售价，兑底秒杀价/原价
            product.marketPrice = TryParseDoubleStr(GetStringValue(productNode, "商品售价", "sell_price"))
                                  ?? TryParseDoubleStr(GetStringValue(productNode, "秒杀价", "seckill_price"))
                                  ?? TryParseDoubleStr(GetStringValue(productNode, "origin_price"));

            // 上架时间：EOS版有真正的"上架时间"字段
            product.productBindTime = GetStringValue(productNode, "上架时间", "sold_start_time");

            // 曝光/点击：EOS版仅有次数（官方 goods_show_cnt / goods_click_cnt），无人数口径可用
            product.productShowUcnt = TryParseLongFromNode(productNode, "商品曝光次数", "商品曝光人数", "goods_show_cnt");
            product.productClickUcnt = TryParseLongFromNode(productNode, "商品点击次数", "商品点击人数", "goods_click_cnt");

            // 成交金额与订单数
            product.payAmt = TryParseDoubleStr(GetStringValue(productNode, "支付金额", "pay_order_gmv_all"));
            product.payComboCnt = TryParseLongFromNode(productNode, "支付订单", "pay_order_cnt_all");
            // 【#22】累计成交订单数：与 payComboCnt 同源（均为支付订单数）
            product.payCnt = product.payComboCnt;

            // 退款三项：EOS版均有字段（线索版完全没有）
            product.refundCnt = TryParseLongFromNode(productNode, "退款订单数", "refund_order_cnt_all");
            product.refundRate = TryParseDoubleStr(GetStringValue(productNode, "退款率", "refund_order_ratio"));
            // 【#28】退款金额
            product.realRefundAmt = TryParseDoubleStr(GetStringValue(productNode, "退款金额", "refund_gmv_all"));

            // 【#17】曝光-点击转化率：直取 goods_ctr（小数×100）。官方口径为点击次数÷曝光次数
            //   （样本验证 7÷359=0.0195 与 goods_ctr 一致），为次数口径，与模型Ucnt命名不符但数据真实
            var goodsCtr = TryParseDoubleStr(GetStringValue(productNode, "goods_ctr"));
            if (goodsCtr.HasValue)
                product.productShowClickUcntRatio = Math.Round(goodsCtr.Value * 100, 2);
            else if (product.productClickUcnt.HasValue && (product.productShowUcnt ?? 0) > 0)
                product.productShowClickUcntRatio = Math.Round(
                    (double)product.productClickUcnt.Value / product.productShowUcnt.Value * 100, 2);

            // 【#18】曝光-成交转化率：直取 goods_show_pay_pv_ratio（官方=成交次数÷曝光次数，次数口径）
            var showPayRatio = TryParseDoubleStr(GetStringValue(productNode, "商品曝光 - 成交转化率", "goods_show_pay_pv_ratio"));
            if (showPayRatio.HasValue)
                product.productShowPayUcntRatio = Math.Round(showPayRatio.Value * 100, 2);

            // 【#19】点击-成交转化率：直取 goods_cvr（官方=成交次数÷点击次数，次数口径；样本0.42857=3÷7）
            var clickPayRatio = TryParseDoubleStr(GetStringValue(productNode, "商品转化率", "goods_cvr"));
            if (clickPayRatio.HasValue)
                product.productClickPayUcntRatio = Math.Round(clickPayRatio.Value * 100, 2);

            // 【#20】GPM：无现成字段，用 支付金额÷商品曝光次数×1000 计算
            var showCnt = TryParseLongFromNode(productNode, "商品曝光次数", "goods_show_cnt");
            if (product.payAmt.HasValue && (showCnt ?? 0) > 0)
                product.gpm = Math.Round(product.payAmt.Value / showCnt.Value * 1000, 2);

            // 【#21】分钟最高成交金额：EOS版 max_gmv（样本295/599，单位元）
            product.avgMaxPayAmtMin = TryParseDoubleStr(GetStringValue(productNode, "max_gmv"));

            // 【#23】创建订单数：支付订单数 + 未支付订单数(unpaid_order_cnt_all)
            var unpaidCnt = TryParseLongFromNode(productNode, "unpaid_order_cnt_all");
            if (product.payCnt.HasValue || unpaidCnt.HasValue)
                product.createCnt = (product.payCnt ?? 0) + (unpaidCnt ?? 0);

            // 【#24】订单支付率：直取 pay_order_ratio（官方名"下单率"，小数×100；样本1→100%）
            var payOrderRatio = TryParseDoubleStr(GetStringValue(productNode, "pay_order_ratio"));
            if (payOrderRatio.HasValue)
                product.createPayUcntRatio = Math.Round(payOrderRatio.Value * 100, 2);

            // EOS版确认无源的字段（保持null）：productDownTime(无下架时间)、
            //   explainCnt(仅有introducing布尔无次数)、预售三项(本地生活无此业务)
            // 另有 商品详情页访问量/提单页访问量/商品提单率/库存/三级品类/商品标签类型链接 等字段，
            //   VideoProductRequest 无对应字段，故不映射
        }

        /// <summary>
        /// 两版通用：基于已填充的商品字段做派生计算，并生成本次采集的商品过程快照。
        /// 不读取任何原始JSON key，因此对两版完全中立。
        /// </summary>
        private static void FillCommonProductFields(VideoProductRequest product, long? roomViewCount, string collectTime)
        {
            // 【#29】曝光观看率：商品曝光÷直播间观看人数×100（公式同巨量L220-224）
            if (product.productShowUcnt.HasValue && (roomViewCount ?? 0) > 0)
                product.productViewShowRatio = Math.Round(
                    (double)product.productShowUcnt.Value / roomViewCount.Value * 100, 2);

            // 【#30】成交单价：优先 成交金额÷成交订单数 精确计算（比巨量直接取marketPrice更准），无成交时兑底单价
            if (product.payAmt.HasValue && (product.payCnt ?? 0) > 0)
                product.avgPayAmtPerOrder = Math.Round(product.payAmt.Value / product.payCnt.Value, 2);
            else
                product.avgPayAmtPerOrder = product.marketPrice;

            // 【#31】商品过程数据列表：每次采集为每个商品生成一条快照，
            //   后续由 PlatformDataManager.MergeProductList 按 productId 追加（模式同巨量L227-248）
            product.commodityProcessDataList = new List<CommodityProcessDataBo>
            {
                new CommodityProcessDataBo
                {
                    dateTime = collectTime,
                    productShowUcnt = product.productShowUcnt,
                    productClickUcnt = product.productClickUcnt,
                    viewCount = (int?)roomViewCount,
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
        }

        /// <summary>
        /// 从JObject中按多个候选key依次取字符串值
        /// </summary>
        private static string GetStringValue(JObject obj, params string[] keys)
        {
            foreach (var key in keys)
            {
                if (obj[key] != null)
                {
                    var val = obj[key].ToString();
                    if (!string.IsNullOrEmpty(val))
                        return val;
                }
            }
            return null;
        }

        /// <summary>
        /// 从JObject中按多个候选key取值并转为 long?
        /// </summary>
        private static long? TryParseLongFromNode(JObject obj, params string[] keys)
        {
            var s = GetStringValue(obj, keys);
            return TryParseLongStr(s);
        }

        /// <summary>
        /// 字符串转 long?（去除逗号、百分号等）
        /// </summary>
        private static long? TryParseLongStr(string s)
        {
            var d = TryParseDoubleStr(s);
            return d.HasValue ? (long?)d.Value : null;
        }

        /// <summary>
        /// 字符串转 double?（去除逗号、百分号等）
        /// </summary>
        private static double? TryParseDoubleStr(string s)
        {
            if (string.IsNullOrEmpty(s)) return null;
            s = s.Trim().Replace(",", "").Replace("%", "");
            if (double.TryParse(s, out double val)) return val;
            return null;
        }

        #region 解析辅助方法

        /// <summary>
        /// 尝试解析为 long（自动去除逗号、百分号等非数字字符）
        /// </summary>
        private static long? TryParseLong(JToken token)
        {
            double? val = TryParseDouble(token);
            return val.HasValue ? (long)val.Value : (long?)null;
        }

        /// <summary>
        /// 尝试解析为 int
        /// </summary>
        private static int? TryParseInt(JToken token)
        {
            double? val = TryParseDouble(token);
            return val.HasValue ? (int)val.Value : (int?)null;
        }

        /// <summary>
        /// 尝试解析为 double（自动去除逗号、百分号等非数字字符）
        /// </summary>
        private static double? TryParseDouble(JToken token)
        {
            if (token == null) return null;
            string s = token.ToString().Trim().Replace(",", "").Replace("%", "");
            if (double.TryParse(s, out double val)) return val;
            return null;
        }

        /// <summary>
        /// 解析百分比字符串（"12.34%" → 12.34，纯数字直接返回）
        /// </summary>
        private static double? ParsePercent(JToken token)
        {
            return TryParseDouble(token);
        }

        /// <summary>
        /// 解析时长字符串为秒数（"3分8秒" → 188，"45秒" → 45，纯数字直接返回）
        /// </summary>
        private static int? ParseDuration(JToken token)
        {
            if (token == null) return null;
            string s = token.ToString().Trim();

            // 纯数字直接返回
            if (double.TryParse(s, out double directVal))
                return (int)directVal;

            // 解析 "x分y秒" 或 "y秒"
            int totalSeconds = 0;
            bool matched = false;

            int fenIdx = s.IndexOf('分');
            if (fenIdx > 0)
            {
                string fenStr = s.Substring(0, fenIdx);
                if (int.TryParse(fenStr, out int fen))
                {
                    totalSeconds += fen * 60;
                    matched = true;
                }
                s = s.Substring(fenIdx + 1);
            }

            int miaoIdx = s.IndexOf('秒');
            if (miaoIdx > 0)
            {
                string miaoStr = s.Substring(0, miaoIdx);
                if (int.TryParse(miaoStr, out int miao))
                {
                    totalSeconds += miao;
                    matched = true;
                }
            }

            return matched ? totalSeconds : (int?)null;
        }

        #endregion
    }
}
