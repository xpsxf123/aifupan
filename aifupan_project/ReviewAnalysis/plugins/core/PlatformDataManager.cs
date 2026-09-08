using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using Newtonsoft.Json;
using ReviewAnalysis.plugins.constant;
using ReviewAnalysis.plugins.models;
using douyin.Utils;
using ReviewAnalysis.plugins.utils;

namespace ReviewAnalysis.plugins.core
{
    /// <summary>
    /// 平台数据管理器
    /// 负责各平台数据的文件读写、合并、归档
    /// </summary>
    public class PlatformDataManager
    {
        private const string UploadArchivePath = @"dataCollect\uploaded";

        /// <summary>
        /// 保存平台数据（自动合并过程数据）
        /// </summary>
        public void SavePlatformData(string platformId, string secUid, string roomId, UnifiedDataModel newData)
        {
            try
            {
                // 1. 查找该平台的未上传文件
                var pendingFile = FindPendingFile(platformId, secUid, roomId);

                // 2. 如果存在，读取并合并
                if (pendingFile != null)
                {
                    FileUtils.LogRpa($"找到未上传文件，进行数据合并: {pendingFile}", "PlatformDataManager");
                    var existingData = LoadDataFromFile(pendingFile);
                    var mergedData = MergeProcessData(existingData, newData);
                    SaveDataToFile(pendingFile, mergedData);
                    FileUtils.LogRpa($"数据已合并保存: {pendingFile}", "PlatformDataManager");
                }
                else
                {
                    // 3. 不存在则创建新文件
                    var filePath = GenerateFilePath(platformId, secUid, roomId);
                    SaveDataToFile(filePath, newData);
                    FileUtils.LogRpa($"创建新数据文件: {filePath}", "PlatformDataManager");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"保存平台数据异常: {ex.Message}", "PlatformDataManager错误");
            }
        }

        /// <summary>
        /// 查找未上传的文件
        /// </summary>
        private string FindPendingFile(string platformId, string secUid, string roomId)
        {
            var directory = Path.Combine(Environment.CurrentDirectory, PathUtils.PlatformPath);
            if (!Directory.Exists(directory))
            {
                return null;
            }

            var prefix = PlatformConstants.DataPrefix;
            var searchPattern = $"{prefix}_{secUid}_{roomId}_*.json";

            var files = Directory.GetFiles(directory, searchPattern);

            // 返回最新的文件（按修改时间）
            return files.OrderByDescending(f => File.GetLastWriteTime(f)).FirstOrDefault();
        }

        /// <summary>
        /// 生成文件路径
        /// </summary>
        private string GenerateFilePath(string platformId, string secUid, string roomId)
        {
            var directory = Path.Combine(Environment.CurrentDirectory, PathUtils.PlatformPath);
            if (!Directory.Exists(directory))
            {
                Directory.CreateDirectory(directory);
            }

            var prefix = PlatformConstants.DataPrefix;
            var timeStr = DateTime.Now.ToString("yyyyMMdd_HHmmss");
            var fileName = $"{prefix}_{secUid}_{roomId}_{timeStr}.json";

            return Path.Combine(directory, fileName);
        }

        /// <summary>
        /// 从文件加载数据
        /// </summary>
        public UnifiedDataModel LoadDataFromFile(string filePath)
        {
            try
            {
                var json = File.ReadAllText(filePath);
                return JsonConvert.DeserializeObject<UnifiedDataModel>(json);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"加载数据文件异常: {ex.Message}", "PlatformDataManager错误");
                return null;
            }
        }

        /// <summary>
        /// 保存数据到文件
        /// </summary>
        private void SaveDataToFile(string filePath, UnifiedDataModel data)
        {
            try
            {
                var json = JsonConvert.SerializeObject(data, Formatting.Indented);
                File.WriteAllText(filePath, json);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"保存数据文件异常: {ex.Message}", "PlatformDataManager错误");
            }
        }

        /// <summary>
        /// 合并过程数据（oceanEngineProcessList）
        /// </summary>
        private UnifiedDataModel MergeProcessData(UnifiedDataModel existing, UnifiedDataModel newData)
        {
            if (existing == null)
            {
                return newData;
            }

            // 创建新的合并对象，基础字段取最新的（newData）
            var merged = new UnifiedDataModel
            {
                // 基础字段取最新值
                viewCount = newData.viewCount ?? existing.viewCount,
                salesRevenue = newData.salesRevenue ?? existing.salesRevenue,
                refund = newData.refund ?? existing.refund,
                investment = newData.investment ?? existing.investment,
                refundQuantity = newData.refundQuantity ?? existing.refundQuantity,
                payComboCnt = newData.payComboCnt ?? existing.payComboCnt,
                netSales = newData.netSales ?? existing.netSales,
                refundRate = newData.refundRate ?? existing.refundRate,
                roi = newData.roi ?? existing.roi,
                thousandSales = newData.thousandSales ?? existing.thousandSales,
                exposureCount = newData.exposureCount ?? existing.exposureCount,
                followCount = newData.followCount ?? existing.followCount,
                clickPaymentRate = newData.clickPaymentRate ?? existing.clickPaymentRate,
                interactionRate = newData.interactionRate ?? existing.interactionRate,
                maxOnline = newData.maxOnline ?? existing.maxOnline,
                averageOnlineNum = (newData.averageOnlineNum.HasValue && newData.averageOnlineNum.Value > 0) 
                    ? newData.averageOnlineNum 
                    : existing.averageOnlineNum,
                averageResidenceTime = newData.averageResidenceTime ?? existing.averageResidenceTime,
                customerUnitPrice = newData.customerUnitPrice ?? existing.customerUnitPrice,
                conversionRate = newData.conversionRate ?? existing.conversionRate,
                uvValue = newData.uvValue ?? existing.uvValue,
                followRate = newData.followRate ?? existing.followRate,
                batchNumber = newData.batchNumber ?? existing.batchNumber,
                startTime = newData.startTime ?? existing.startTime,
                endTime = newData.endTime ?? existing.endTime,
                secUid = newData.secUid ?? existing.secUid,
                platform = newData.platform != 0 ? newData.platform : existing.platform,
                productList = MergeProductList(existing.productList, newData.productList),

                // 过程数据需要合并
                oceanEngineProcessList = new List<OceanEngineProcessBo>()
            };

            // 合并 oceanEngineProcessList（直接追加，不去重）
            if (existing.oceanEngineProcessList != null)
            {
                merged.oceanEngineProcessList.AddRange(existing.oceanEngineProcessList);
            }

            if (newData.oceanEngineProcessList != null)
            {
                merged.oceanEngineProcessList.AddRange(newData.oceanEngineProcessList);
            }

            FileUtils.LogRpa($"过程数据合并完成：原有 {existing.oceanEngineProcessList?.Count ?? 0} 条，新增 {newData.oceanEngineProcessList?.Count ?? 0} 条，合并后 {merged.oceanEngineProcessList.Count} 条", "PlatformDataManager");

            return merged;
        }

        /// <summary>
        /// 合并商品列表（按productId匹配，追加commodityProcessDataList，外层字段取最新值）
        /// </summary>
        private List<VideoProductRequest> MergeProductList(List<VideoProductRequest> existing, List<VideoProductRequest> newData)
        {
            if (existing == null || existing.Count == 0) return newData;
            if (newData == null || newData.Count == 0) return existing;

            var result = new List<VideoProductRequest>();
            foreach (var newProduct in newData)
            {
                var existProduct = existing.Find(p => p.productId == newProduct.productId);
                if (existProduct != null)
                {
                    var merged = new VideoProductRequest
                    {
                        productId = newProduct.productId ?? existProduct.productId,
                        title = newProduct.title ?? existProduct.title,
                        imageUri = newProduct.imageUri ?? existProduct.imageUri,
                        marketPrice = newProduct.marketPrice ?? existProduct.marketPrice,
                        productBindTime = newProduct.productBindTime ?? existProduct.productBindTime,
                        explainCnt = newProduct.explainCnt ?? existProduct.explainCnt,
                        productShowUcnt = newProduct.productShowUcnt ?? existProduct.productShowUcnt,
                        productClickUcnt = newProduct.productClickUcnt ?? existProduct.productClickUcnt,
                        productShowClickUcntRatio = newProduct.productShowClickUcntRatio ?? existProduct.productShowClickUcntRatio,
                        productShowPayUcntRatio = newProduct.productShowPayUcntRatio ?? existProduct.productShowPayUcntRatio,
                        productClickPayUcntRatio = newProduct.productClickPayUcntRatio ?? existProduct.productClickPayUcntRatio,
                        gpm = newProduct.gpm ?? existProduct.gpm,
                        payAmt = newProduct.payAmt ?? existProduct.payAmt,
                        avgMaxPayAmtMin = newProduct.avgMaxPayAmtMin ?? existProduct.avgMaxPayAmtMin,
                        payComboCnt = newProduct.payComboCnt ?? existProduct.payComboCnt,
                        payCnt = newProduct.payCnt ?? existProduct.payCnt,
                        createCnt = newProduct.createCnt ?? existProduct.createCnt,
                        createPayUcntRatio = newProduct.createPayUcntRatio ?? existProduct.createPayUcntRatio,
                        payDepositPreOrderCnt = newProduct.payDepositPreOrderCnt ?? existProduct.payDepositPreOrderCnt,
                        presaleDepayDeamt = newProduct.presaleDepayDeamt ?? existProduct.presaleDepayDeamt,
                        payDepositPreOrderAmt = newProduct.payDepositPreOrderAmt ?? existProduct.payDepositPreOrderAmt,
                        refundCnt = newProduct.refundCnt ?? existProduct.refundCnt,
                        realRefundAmt = newProduct.realRefundAmt ?? existProduct.realRefundAmt,
                        refundRate = newProduct.refundRate ?? existProduct.refundRate,
                        productViewShowRatio = newProduct.productViewShowRatio ?? existProduct.productViewShowRatio,
                        avgPayAmtPerOrder = newProduct.avgPayAmtPerOrder ?? existProduct.avgPayAmtPerOrder,
                        sort = newProduct.sort ?? existProduct.sort,
                        commodityProcessDataList = new List<CommodityProcessDataBo>()
                    };
                    // 追加已有过程数据
                    if (existProduct.commodityProcessDataList != null)
                        merged.commodityProcessDataList.AddRange(existProduct.commodityProcessDataList);
                    // 追加新过程数据
                    if (newProduct.commodityProcessDataList != null)
                        merged.commodityProcessDataList.AddRange(newProduct.commodityProcessDataList);
                    result.Add(merged);
                }
                else
                {
                    result.Add(newProduct);
                }
            }
            return result;
        }

        /// <summary>
        /// 上传完成后归档文件
        /// </summary>
        public void ArchiveUploadedFile(string filePath, string platformId)
        {
            try
            {
                if (!File.Exists(filePath))
                {
                    return;
                }

                if (Global.Constant.GetDebuggerEnv())
                {
                    string prefix = PlatformConstants.DataPrefix;
                    var dateStr = DateTime.Now.ToString("yyyy-MM-dd");
                    var archiveDir = Path.Combine(
                        Environment.CurrentDirectory,
                        UploadArchivePath,
                        $"{prefix}Up",
                        dateStr);

                    if (!Directory.Exists(archiveDir))
                    {
                        Directory.CreateDirectory(archiveDir);
                    }

                    var fileName = Path.GetFileName(filePath);
                    var destPath = Path.Combine(archiveDir, fileName);

                    // 如果目标文件已存在，添加序号
                    int counter = 1;
                    var originalDestPath = destPath;
                    while (File.Exists(destPath))
                    {
                        var nameWithoutExt = Path.GetFileNameWithoutExtension(originalDestPath);
                        var ext = Path.GetExtension(originalDestPath);
                        destPath = Path.Combine(archiveDir, $"{nameWithoutExt}_{counter}{ext}");
                        counter++;
                    }

                    File.Move(filePath, destPath);
                    FileUtils.LogRpa($"文件已归档: {filePath} -> {destPath}", "PlatformDataManager");
                }
                else
                {
                    File.Delete(filePath);
                    FileUtils.LogRpa($"文件已删除（生产环境不归档）: {filePath}", "PlatformDataManager");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"归档文件异常: {ex.Message}", "PlatformDataManager错误");
            }
        }

        /// <summary>
        /// 归档上传失败的文件（文件名添加unUp后缀，表示数据问题无法上传）
        /// </summary>
        public void ArchiveFailedFile(string filePath, string platformId)
        {
            try
            {
                if (!File.Exists(filePath))
                {
                    return;
                }

                string prefix = PlatformConstants.DataPrefix;
                var dateStr = DateTime.Now.ToString("yyyy-MM-dd");
                var archiveDir = Path.Combine(
                    Environment.CurrentDirectory,
                    UploadArchivePath,
                    $"{prefix}Up",
                    dateStr);

                if (!Directory.Exists(archiveDir))
                {
                    Directory.CreateDirectory(archiveDir);
                }

                // 文件名添加unUp后缀，如 data_xxx.json -> data_xxx_unUp.json
                var fileName = Path.GetFileNameWithoutExtension(filePath) + "_unUp" + Path.GetExtension(filePath);
                var destPath = Path.Combine(archiveDir, fileName);

                // 如果目标文件已存在，添加序号
                int counter = 1;
                var originalDestPath = destPath;
                while (File.Exists(destPath))
                {
                    var nameWithoutExt = Path.GetFileNameWithoutExtension(originalDestPath);
                    var ext = Path.GetExtension(originalDestPath);
                    destPath = Path.Combine(archiveDir, $"{nameWithoutExt}_{counter}{ext}");
                    counter++;
                }

                File.Move(filePath, destPath);
                FileUtils.LogRpa($"文件上传失败已归档（数据问题）: {filePath} -> {destPath}", "PlatformDataManager");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"归档失败文件异常: {ex.Message}", "PlatformDataManager错误");
            }
        }
    }
}
