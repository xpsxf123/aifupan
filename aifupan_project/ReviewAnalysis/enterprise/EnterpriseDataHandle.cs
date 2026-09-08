using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Model;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using ServerTimeUtils = ReviewAnalysis.Utils.ServerTimeUtils;
using FrontNotice = ReviewAnalysis.Utils.FrontNotice;
using ReviewAnalysis.api;
using ReviewAnalysis.vo;
using ReviewAnalysis.upload;
using System.IO.Compression;

namespace ReviewAnalysis.enterprise
{
    /// <summary>
    /// 企业号数据处理层
    /// </summary>
    public class EnterpriseDataHandle
    {
        private const string dataCollectPath = "dataCollect";
        private const string enterpriseDataCollectPath = @"dataCollect\Enterprise";
        private const string realTimeDirectory = "realTime";
        private const string finishDirectory = "finish";

        /// <summary>
        /// 生成数据文件名（batchNumber_videoId 或 batchNumber）
        /// </summary>
        private static string GetDataFileName(string batchNumber, string videoId)
        {
            return string.IsNullOrEmpty(videoId) ? $"{batchNumber}.txt" : $"{batchNumber}_{videoId}.txt";
        }

        /// <summary>
        /// 写入实时数据
        /// </summary>
        public static void WriteRealTimeData(EnterpriseDataCollectEventArgs data)
        {
            try
            {
                if (string.IsNullOrEmpty(data.batchNumber))
                {
                    FileUtils.LogRpa("企业号batchNumber为空，跳过实时数据写入", "企业号数据处理");
                    return;
                }

                var realTimeFileFullPath = Path.Combine(Environment.CurrentDirectory, enterpriseDataCollectPath, realTimeDirectory, GetDataFileName(data.batchNumber, data.videoId));
                CheckFilePath(realTimeFileFullPath);

                var realTimeData = new EnterpriseRealTimeDataEntity
                {
                    roomId = data.batchNumber,
                    videoId = data.videoId,
                    secUid = data.secUid,
                    dataType = data.key,
                    dataJson = data.dataJson,
                    createTime = ServerTimeUtils.getCurrentTimeStr()
                };

                using (StreamWriter writer = File.AppendText(realTimeFileFullPath))
                {
                    writer.WriteLine(JsonConvert.SerializeObject(realTimeData));
                }

                // 通知前端
                FrontNotice.sendEnterpriseDataUpdate(data.videoId, data.key, data.dataJson);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"写入企业号实时数据异常: {ex.Message}", "企业号数据处理");
            }
        }

        /// <summary>
        /// 写入汇总数据
        /// </summary>
        public static void WriteGatherData(EnterpriseDataCollectEventArgs data)
        {
            try
            {
                if (string.IsNullOrEmpty(data.batchNumber))
                {
                    FileUtils.LogRpa("企业号batchNumber为空，跳过汇总数据写入", "企业号数据处理");
                    return;
                }

                var finishFileFullPath = Path.Combine(Environment.CurrentDirectory, enterpriseDataCollectPath, finishDirectory, GetDataFileName(data.batchNumber, data.videoId));
                CheckFilePath(finishFileFullPath);

                var gatherData = new EnterpriseGatherDataEntity
                {
                    videoId = data.videoId,
                    secUid = data.secUid,
                    batchNumber = data.batchNumber,
                    dataType = data.key,
                    dataJson = data.dataJson,
                    createTime = ServerTimeUtils.getCurrentTimeStr()
                };

                File.WriteAllText(finishFileFullPath, JsonConvert.SerializeObject(gatherData), System.Text.Encoding.UTF8);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"写入企业号汇总数据异常: {ex.Message}", "企业号数据处理");
            }
        }

        /// <summary>
        /// 从本地读取Cookie
        /// </summary>
        public static Dictionary<string, string> GetCookiesFromLocal(string secUid)
        {
            try
            {
                string cookiePath = EnterpriseUtils.getCookiePath(secUid);
                if (!File.Exists(cookiePath))
                {
                    FileUtils.LogRpa($"企业号Cookie文件不存在: {cookiePath}", "企业号数据处理");
                    return new Dictionary<string, string>();
                }

                string cookieContent = File.ReadAllText(cookiePath);
                var cookies = JsonConvert.DeserializeObject<List<EnterpriseCookieDto>>(cookieContent);
                
                if (cookies == null || cookies.Count == 0)
                {
                    return new Dictionary<string, string>();
                }

                return cookies.ToDictionary(c => c.name, c => c.value);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"从本地获取企业号Cookie异常: {ex.Message}", "企业号数据处理");
                return new Dictionary<string, string>();
            }
        }

        /// <summary>
        /// 拉取企业号直播数据
        /// </summary>
        public static async Task PullEnterpriseData(AnchorInfo anchorInfo, string roomId, string videoId)
        {
            try
            {
                var cookies = GetCookiesFromLocal(anchorInfo.SecUid);
                if (cookies.Count == 0)
                {
                    FileUtils.LogRpa("本地无企业号Cookie，无法拉取数据", "企业号数据处理");
                    return;
                }

                // 获取直播数据
                var liveData = await EnterpriseDataApi.GetLiveData(cookies, roomId);
                if (liveData != null && liveData.Count > 0)
                {
                    FileUtils.LogRpa("获取企业号直播大屏数据成功", "企业号数据处理");

                    var dataArgs = new EnterpriseDataCollectEventArgs
                    {
                        batchNumber = roomId,
                        videoId = videoId,
                        secUid = anchorInfo.SecUid,
                        key = "liveScreenData",
                        dataJson = JsonConvert.SerializeObject(liveData),
                        enterpriseForm = null
                    };

                    WriteRealTimeData(dataArgs);
                    WriteGatherData(dataArgs);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"拉取企业号数据异常: {ex.Message}", "企业号数据处理");
            }
        }

        /// <summary>
        /// 检查并创建文件路径
        /// </summary>
        private static void CheckFilePath(string filePath)
        {
            var directory = Path.GetDirectoryName(filePath);
            if (!Directory.Exists(directory))
            {
                Directory.CreateDirectory(directory);
            }
        }

        /// <summary>
        /// 数据上传
        /// </summary>
        public static bool dataUpload(string batchNumber, string videoId, string secUid)
        {
            try
            {
                // 获取汇总数据文件路径
                var finishFileFullPath = Path.Combine(Environment.CurrentDirectory, enterpriseDataCollectPath, finishDirectory, GetDataFileName(batchNumber, videoId));
                var realTimeFileFullPath = Path.Combine(Environment.CurrentDirectory, enterpriseDataCollectPath, realTimeDirectory, GetDataFileName(batchNumber, videoId));

                string dataJson = null;
                if (File.Exists(finishFileFullPath))
                {
                    try
                    {
                        dataJson = File.ReadAllText(finishFileFullPath, System.Text.Encoding.UTF8);
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRecrd($"{ex.Message}", "读取企业号汇总数据报错");
                    }
                }

                dataJson = dataJson == null ? null : dataJson.Trim();
                if (string.IsNullOrEmpty(dataJson))
                {
                    FileUtils.LogRecrd($"企业号汇总数据没有获取到 videoId = {videoId}");
                    return true;
                }

                EnterpriseGatherDataEntity enterpriseGatherDataEntity = null;
                try
                {
                    enterpriseGatherDataEntity = JsonConvert.DeserializeObject<EnterpriseGatherDataEntity>(dataJson);
                }
                catch (Exception ex)
                {
                    FileUtils.LogRecrd($"格式化json报错{ex}", $"企业号汇总数据格式化json报错==={videoId}");
                }

                if (enterpriseGatherDataEntity == null)
                {
                    FileUtils.LogRecrd("enterpriseGatherDataEntity == null");
                    return true;
                }

                // 判断实时数据是否存在
                string cosPath = null;
                if (File.Exists(realTimeFileFullPath))
                {
                    DirectoryInfo parentDir = new FileInfo(realTimeFileFullPath).Directory;
                    string zipName = $"{Guid.NewGuid().ToString()}.zip";
                    string zipNamePath = $"{parentDir.FullName}/{zipName}";

                    try
                    {
                        // 获取预上传url
                        SignUploadUrlVo signUploadUrlVo = OceanEngineDataApi.getDiagnosisSignUploadUrl(videoId);

                        if (signUploadUrlVo != null)
                        {
                            FileUtils.ZipFileToOneFile(realTimeFileFullPath, zipNamePath);

                            bool uploadFlag = UploadUtils.UploadFileAsync(signUploadUrlVo.signedUrl, zipNamePath);
                            if (uploadFlag)
                            {
                                cosPath = signUploadUrlVo.ossKey;
                            }
                        }
                        else
                        {
                            FileUtils.LogRecrd($"videoId = {videoId}", "获取企业号实时数据预上传url失败");
                        }
                    }
                    catch (Exception e)
                    {
                        FileUtils.LogRpa($"{e.Message}", "上传企业号实时数据报错");
                    }
                    finally
                    {
                        if (File.Exists(zipNamePath))
                        {
                            File.Delete(zipNamePath);
                        }
                    }
                }

                if (cosPath == null && dataJson == null)
                {
                    FileUtils.log($"videoId = {videoId},cosPath和dataJson都是为null，没有企业号数据");
                    return true;
                }

                enterpriseGatherDataEntity.ossPath = cosPath;
                string id = EnterpriseDataApi.updateEnterpriseEngine(enterpriseGatherDataEntity);

                if (!string.IsNullOrEmpty(id))
                {
                    return true;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"message = {ex.Message}", "企业号数据上传报错");
            }

            return false;
        }

        /// <summary>
        /// 保存企业号数据到本地文件
        /// </summary>
        public static void SaveEnterpriseData(string videoId, string secUid, string batchNumber, Dictionary<string, string> liveData)
        {
            try
            {
                var gatherData = new EnterpriseGatherDataEntity
                {
                    videoId = videoId,
                    secUid = secUid,
                    batchNumber = batchNumber,
                    createTime = ServerTimeUtils.getCurrentTimeStr()
                };

                // 映射字段（使用API原始字段名）
                if (liveData.ContainsKey("lp_screen_live_avg_watch_duration")) gatherData.lp_screen_live_avg_watch_duration = liveData["lp_screen_live_avg_watch_duration"];
                if (liveData.ContainsKey("lp_screen_live_fans_avg_watch_duration")) gatherData.lp_screen_live_fans_avg_watch_duration = liveData["lp_screen_live_fans_avg_watch_duration"];
                if (liveData.ContainsKey("lp_screen_clue_uv")) gatherData.lp_screen_clue_uv = liveData["lp_screen_clue_uv"];
                if (liveData.ContainsKey("lp_screen_live_clue_convert_ratio")) gatherData.lp_screen_live_clue_convert_ratio = liveData["lp_screen_live_clue_convert_ratio"];
                if (liveData.ContainsKey("lp_screen_msg_conversation_count")) gatherData.lp_screen_msg_conversation_count = liveData["lp_screen_msg_conversation_count"];
                if (liveData.ContainsKey("lp_screen_longterm_msg_clue_uv")) gatherData.lp_screen_longterm_msg_clue_uv = liveData["lp_screen_longterm_msg_clue_uv"];
                if (liveData.ContainsKey("lp_screen_live_user_realtime")) gatherData.lp_screen_live_user_realtime = liveData["lp_screen_live_user_realtime"];
                if (liveData.ContainsKey("lp_screen_uv_with_preview")) gatherData.lp_screen_uv_with_preview = liveData["lp_screen_uv_with_preview"];
                if (liveData.ContainsKey("lp_screen_card_clue_uv")) gatherData.lp_screen_card_clue_uv = liveData["lp_screen_card_clue_uv"];
                if (liveData.ContainsKey("lp_screen_ad_biz_wechat_add_count")) gatherData.lp_screen_ad_biz_wechat_add_count = liveData["lp_screen_ad_biz_wechat_add_count"];
                if (liveData.ContainsKey("lp_screen_ad_biz_wechat_cost")) gatherData.lp_screen_ad_biz_wechat_cost = liveData["lp_screen_ad_biz_wechat_cost"];
                if (liveData.ContainsKey("lp_screen_ad_form_count")) gatherData.lp_screen_ad_form_count = liveData["lp_screen_ad_form_count"];
                if (liveData.ContainsKey("lp_screen_ad_form_cost")) gatherData.lp_screen_ad_form_cost = liveData["lp_screen_ad_form_cost"];
                if (liveData.ContainsKey("lp_screen_live_watch_uv")) gatherData.lp_screen_live_watch_uv = liveData["lp_screen_live_watch_uv"];
                if (liveData.ContainsKey("lp_screen_live_fans_watch_ratio")) gatherData.lp_screen_live_fans_watch_ratio = liveData["lp_screen_live_fans_watch_ratio"];
                if (liveData.ContainsKey("lp_screen_live_enter_ratio")) gatherData.lp_screen_live_enter_ratio = liveData["lp_screen_live_enter_ratio"];
                if (liveData.ContainsKey("lp_screen_live_max_watch_uv_by_minute")) gatherData.lp_screen_live_max_watch_uv_by_minute = liveData["lp_screen_live_max_watch_uv_by_minute"];
                if (liveData.ContainsKey("lp_screen_live_avg_online_uv_by_room")) gatherData.lp_screen_live_avg_online_uv_by_room = liveData["lp_screen_live_avg_online_uv_by_room"];
                if (liveData.ContainsKey("lp_screen_live_stat_cost")) gatherData.lp_screen_live_stat_cost = liveData["lp_screen_live_stat_cost"];
                if (liveData.ContainsKey("lp_screen_clue_cost")) gatherData.lp_screen_clue_cost = liveData["lp_screen_clue_cost"];
                if (liveData.ContainsKey("lp_screen_live_icon_click_count")) gatherData.lp_screen_live_icon_click_count = liveData["lp_screen_live_icon_click_count"];
                if (liveData.ContainsKey("lp_screen_live_icon_click_rate")) gatherData.lp_screen_live_icon_click_rate = liveData["lp_screen_live_icon_click_rate"];
                if (liveData.ContainsKey("lp_screen_live_follow_uv")) gatherData.lp_screen_live_follow_uv = liveData["lp_screen_live_follow_uv"];
                if (liveData.ContainsKey("lp_screen_live_follow_ratio")) gatherData.lp_screen_live_follow_ratio = liveData["lp_screen_live_follow_ratio"];
                if (liveData.ContainsKey("lp_screen_live_share_uv")) gatherData.lp_screen_live_share_uv = liveData["lp_screen_live_share_uv"];
                if (liveData.ContainsKey("lp_screen_live_like_uv")) gatherData.lp_screen_live_like_uv = liveData["lp_screen_live_like_uv"];
                if (liveData.ContainsKey("lp_screen_live_comment_uv")) gatherData.lp_screen_live_comment_uv = liveData["lp_screen_live_comment_uv"];
                if (liveData.ContainsKey("lp_screen_live_show_count")) gatherData.lp_screen_live_show_count = liveData["lp_screen_live_show_count"];
                if (liveData.ContainsKey("lp_screen_live_watch_count")) gatherData.lp_screen_live_watch_count = liveData["lp_screen_live_watch_count"];
                if (liveData.ContainsKey("lp_screen_live_share_count")) gatherData.lp_screen_live_share_count = liveData["lp_screen_live_share_count"];
                if (liveData.ContainsKey("lp_screen_live_like_count")) gatherData.lp_screen_live_like_count = liveData["lp_screen_live_like_count"];
                if (liveData.ContainsKey("lp_screen_live_comment_count")) gatherData.lp_screen_live_comment_count = liveData["lp_screen_live_comment_count"];
                if (liveData.ContainsKey("lp_screen_live_interaction_count")) gatherData.lp_screen_live_interaction_count = liveData["lp_screen_live_interaction_count"];
                if (liveData.ContainsKey("lp_screen_live_show_uv")) gatherData.lp_screen_live_show_uv = liveData["lp_screen_live_show_uv"];
                if (liveData.ContainsKey("live_duration")) gatherData.live_duration = liveData["live_duration"];

                // 保存汇总数据
                var finishFileFullPath = Path.Combine(Environment.CurrentDirectory, enterpriseDataCollectPath, finishDirectory, GetDataFileName(batchNumber, videoId));
                CheckFilePath(finishFileFullPath);
                File.WriteAllText(finishFileFullPath, JsonConvert.SerializeObject(gatherData), System.Text.Encoding.UTF8);

                // 保存实时数据
                var realTimeFileFullPath = Path.Combine(Environment.CurrentDirectory, enterpriseDataCollectPath, realTimeDirectory, GetDataFileName(batchNumber, videoId));
                CheckFilePath(realTimeFileFullPath);

                var realTimeData = new EnterpriseRealTimeDataEntity
                {
                    roomId = batchNumber,
                    videoId = videoId,
                    secUid = secUid,
                    dataType = "liveScreenOverview",
                    dataJson = JsonConvert.SerializeObject(liveData),
                    createTime = ServerTimeUtils.getCurrentTimeStr()
                };

                using (StreamWriter writer = File.AppendText(realTimeFileFullPath))
                {
                    writer.WriteLine(JsonConvert.SerializeObject(realTimeData));
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"保存企业号数据异常: {ex.Message}", "企业号数据处理");
            }
        }
    }
}
