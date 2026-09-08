using System;
using System.Collections.Generic;
using ReviewAnalysis.plugins.models;

namespace ReviewAnalysis.plugins.processors
{
    /// <summary>
    /// 数据合并器
    /// 职责：巨量+千川数据合并（纯逻辑，无IO操作）
    /// </summary>
    public class DataMerger
    {

        /// <summary>
        /// 合并多个模型数据（多平台/多批次汇总）
        /// 以第一个模型为基础，字段为0或null时依次向后取值
        /// </summary>
        public static UnifiedDataModel MergeAll(List<UnifiedDataModel> models)
        {
            if (models == null || models.Count == 0)
            {
                return null;
            }

            if (models.Count == 1)
            {
                return models[0];
            }

            var baseModel = models[0];

            // 基础字段：为0或null时向后取第一个有效值
            baseModel.viewCount = CoalesceLong(models, m => m.viewCount);
            baseModel.salesRevenue = CoalesceDouble(models, m => m.salesRevenue);
            baseModel.refund = CoalesceDouble(models, m => m.refund);
            baseModel.investment = CoalesceDouble(models, m => m.investment);
            baseModel.refundQuantity = CoalesceInt(models, m => m.refundQuantity);
            baseModel.payComboCnt = CoalesceInt(models, m => m.payComboCnt);
            baseModel.netSales = CoalesceDouble(models, m => m.netSales);
            baseModel.refundRate = CoalesceDouble(models, m => m.refundRate);
            baseModel.roi = CoalesceDouble(models, m => m.roi);
            baseModel.thousandSales = CoalesceDouble(models, m => m.thousandSales);
            baseModel.exposureCount = CoalesceLong(models, m => m.exposureCount);
            baseModel.followCount = CoalesceInt(models, m => m.followCount);
            baseModel.clickPaymentRate = CoalesceDouble(models, m => m.clickPaymentRate);
            baseModel.interactionRate = CoalesceDouble(models, m => m.interactionRate);
            baseModel.maxOnline = CoalesceInt(models, m => m.maxOnline);
            baseModel.averageOnlineNum = CoalesceInt(models, m => m.averageOnlineNum);
            baseModel.averageResidenceTime = CoalesceInt(models, m => m.averageResidenceTime);
            baseModel.customerUnitPrice = CoalesceDouble(models, m => m.customerUnitPrice);
            baseModel.conversionRate = CoalesceDouble(models, m => m.conversionRate);
            baseModel.uvValue = CoalesceDouble(models, m => m.uvValue);
            baseModel.followRate = CoalesceDouble(models, m => m.followRate);
            baseModel.startTime = CoalesceString(models, m => m.startTime);
            baseModel.endTime = CoalesceString(models, m => m.endTime);

            // oceanEngineProcessList：参考Merge方法，追加并补充
            MergeProcessLists(models, baseModel);

            // productList：参考Merge方法，按productId合并
            MergeProductLists(models, baseModel);

            baseModel.CalculateDerivedFields();

            return baseModel;
        }

        #region MergeAll 辅助方法

        private static long? CoalesceLong(List<UnifiedDataModel> models, Func<UnifiedDataModel, long?> selector)
        {
            foreach (var m in models)
            {
                var val = selector(m);
                if (val.HasValue && val.Value != 0) return val;
            }
            return selector(models[0]);
        }

        private static int? CoalesceInt(List<UnifiedDataModel> models, Func<UnifiedDataModel, int?> selector)
        {
            foreach (var m in models)
            {
                var val = selector(m);
                if (val.HasValue && val.Value != 0) return val;
            }
            return selector(models[0]);
        }

        private static double? CoalesceDouble(List<UnifiedDataModel> models, Func<UnifiedDataModel, double?> selector)
        {
            foreach (var m in models)
            {
                var val = selector(m);
                if (val.HasValue && Math.Abs(val.Value) > 0.0001) return val;
            }
            return selector(models[0]);
        }

        private static string CoalesceString(List<UnifiedDataModel> models, Func<UnifiedDataModel, string> selector)
        {
            foreach (var m in models)
            {
                var val = selector(m);
                if (!string.IsNullOrEmpty(val)) return val;
            }
            return selector(models[0]);
        }

        /// <summary>
        /// 合并多个模型的过程数据列表
        /// </summary>
        private static void MergeProcessLists(List<UnifiedDataModel> models, UnifiedDataModel baseModel)
        {
            var merged = new List<OceanEngineProcessBo>();

            // 以第一个模型的过程数据为基础
            if (baseModel.oceanEngineProcessList != null)
            {
                foreach (var item in baseModel.oceanEngineProcessList)
                {
                    merged.Add(item);
                }
            }

            // 后续模型的过程数据：追加并补充字段
            for (int i = 1; i < models.Count; i++)
            {
                var model = models[i];
                if (model.oceanEngineProcessList == null) continue;

                foreach (var process in model.oceanEngineProcessList)
                {
                    if (merged.Count > 0)
                    {
                        var last = merged[merged.Count - 1];
                        if ((last.investment ?? 0) == 0) last.investment = process.investment;
                        if ((last.refund ?? 0) == 0) last.refund = process.refund;
                        if ((last.refundQuantity ?? 0) == 0) last.refundQuantity = process.refundQuantity;
                        if ((last.clickPaymentRate ?? 0) == 0) last.clickPaymentRate = process.clickPaymentRate;
                        if ((last.interactionRate ?? 0) == 0) last.interactionRate = process.interactionRate;
                    }
                }
            }

            baseModel.oceanEngineProcessList = merged;
        }

        /// <summary>
        /// 合并多个模型的商品列表（按productId匹配合并）
        /// </summary>
        private static void MergeProductLists(List<UnifiedDataModel> models, UnifiedDataModel baseModel)
        {
            if (baseModel.productList == null)
            {
                baseModel.productList = new List<VideoProductRequest>();
            }

            for (int i = 1; i < models.Count; i++)
            {
                var model = models[i];
                if (model.productList == null) continue;

                foreach (var product in model.productList)
                {
                    var exist = baseModel.productList.Find(p => p.productId == product.productId);
                    if (exist != null)
                    {
                        // 商品外层字段：补充为0或null的字段
                        if ((exist.payAmt ?? 0) == 0) exist.payAmt = product.payAmt;
                        if ((exist.payComboCnt ?? 0) == 0) exist.payComboCnt = product.payComboCnt;
                        if ((exist.realRefundAmt ?? 0) == 0) exist.realRefundAmt = product.realRefundAmt;
                        if ((exist.refundCnt ?? 0) == 0) exist.refundCnt = product.refundCnt;
                        if ((exist.refundRate ?? 0) == 0) exist.refundRate = product.refundRate;
                        if ((exist.productViewShowRatio ?? 0) == 0) exist.productViewShowRatio = product.productViewShowRatio;
                        if ((exist.avgPayAmtPerOrder ?? 0) == 0) exist.avgPayAmtPerOrder = product.avgPayAmtPerOrder;

                        // 合并 commodityProcessDataList
                        if (product.commodityProcessDataList != null)
                        {
                            if (exist.commodityProcessDataList == null)
                            {
                                exist.commodityProcessDataList = product.commodityProcessDataList;
                            }
                            else
                            {
                                foreach (var procData in product.commodityProcessDataList)
                                {
                                    var existProc = exist.commodityProcessDataList.Find(d => d.dateTime == procData.dateTime);
                                    if (existProc != null)
                                    {
                                        if (!existProc.payAmt.HasValue) existProc.payAmt = procData.payAmt;
                                        if (!existProc.payComboCnt.HasValue) existProc.payComboCnt = procData.payComboCnt;
                                        if (!existProc.productShowUcnt.HasValue) existProc.productShowUcnt = procData.productShowUcnt;
                                        if (!existProc.productClickUcnt.HasValue) existProc.productClickUcnt = procData.productClickUcnt;
                                        if (!existProc.viewCount.HasValue) existProc.viewCount = procData.viewCount;
                                        if (!existProc.explainCnt.HasValue) existProc.explainCnt = procData.explainCnt;
                                        if (!existProc.gpm.HasValue) existProc.gpm = procData.gpm;
                                        if (!existProc.avgMaxPayAmtMin.HasValue) existProc.avgMaxPayAmtMin = procData.avgMaxPayAmtMin;
                                        if (!existProc.createCnt.HasValue) existProc.createCnt = procData.createCnt;
                                        if (!existProc.createPayUcntRatio.HasValue) existProc.createPayUcntRatio = procData.createPayUcntRatio;
                                        if (!existProc.payDepositPreOrderCnt.HasValue) existProc.payDepositPreOrderCnt = procData.payDepositPreOrderCnt;
                                        if (!existProc.presaleDepayDeamt.HasValue) existProc.presaleDepayDeamt = procData.presaleDepayDeamt;
                                        if (!existProc.payDepositPreOrderAmt.HasValue) existProc.payDepositPreOrderAmt = procData.payDepositPreOrderAmt;
                                        if (!existProc.refundCnt.HasValue) existProc.refundCnt = procData.refundCnt;
                                        if (!existProc.realRefundAmt.HasValue) existProc.realRefundAmt = procData.realRefundAmt;
                                    }
                                    else
                                    {
                                        exist.commodityProcessDataList.Add(procData);
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        baseModel.productList.Add(product);
                    }
                }
            }
        }

        #endregion
    }
}
