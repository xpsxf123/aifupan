using Newtonsoft.Json;
using ReviewAnalysis.Ai;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Attributes;
using ReviewAnalysis.Bll;
using ReviewAnalysis.bo.governance;
using ReviewAnalysis.bo.video;
using ReviewAnalysis.plugins.core;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Dto;
using ReviewAnalysis.entity.uploadFile;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.juliang.entity;
using ReviewAnalysis.juliang;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using ReviewAnalysis.vo.video;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using ReviewAnalysis.vo.socketCollectMessage;
using douyin.Utils;
using ReviewAnalysis.vo.oceanEngineData;

namespace ReviewAnalysis.Controller
{
    [RestController("关于主播和录屏的控制器", "api/anchorvideo")]
    public class AnchorVideoController
    {

        /// <summary>
        /// 取消视频分析
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <returns></returns>
        [HttpGet("取消视频分析", "/cancelVideoAnalysis")]
        public void cancelVideoAnalysis(string videoId)
        {

            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            anchorVideoBll.cancelVideoAnalysis(videoId);
        }

        /// <summary>
        /// 加速：录播识别进行中时，把该视频尚未识别的剩余分片切换到腾讯云识别。
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <returns>accepted=true 命中当前在飞视频并已登记；false 非当前在飞视频</returns>
        [HttpGet("加速识别", "/accelerate")]
        public Dictionary<string, object> Accelerate(string videoId)
        {
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            bool accepted = anchorVideoBll.RequestAccelerate(videoId);
            return new Dictionary<string, object> { { "accepted", accepted } };
        }

        /// <summary>
        /// 文案提取改智能分析
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <returns></returns>
        [HttpGet("文案提取改智能分析", "/intelligentAnalysis")]
        public void IntelligentAnalysis(string videoId)
        {

            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            anchorVideoBll.IntelligentAnalysis(videoId);
        }

        /// <summary>
        /// 修复视频
        /// </summary>
        /// <param name="type">类型 0：视频 1：文件</param>
        /// <param name="uuid">文件或视频的唯一标识</param>
        /// <returns></returns>
        [HttpGet("修复视频", "/repairVideo")]
        public void RepairVideo(int type, string uuid)
        {

            if(VideoUtils.reEncodeProcessId != -1)
            {
                throw new Exception("已存在正在修复的视频，请等待上一个视频修复完成再继续");
            }

            string videoPath = "";
            string targetPath = "";
            if(type == 0)
            {
                VideoEntity videoEntity = VideoApi.GetVideoByVideoIdSync(uuid);
                if(videoEntity != null)
                {
                    string oldPath = videoEntity.storagePath;

                    string path = oldPath.Substring(0, oldPath.LastIndexOf("."));
                    string suffix = oldPath.Substring(oldPath.LastIndexOf("."));

                    long time = ServerTimeUtils.getCurrentTime();
                    videoPath = path + "repair" + suffix;

                    videoEntity.storagePath = videoPath;

                    string oldName = videoEntity.videoName;
                    videoEntity.videoName = oldName.Substring(0, oldName.LastIndexOf(".")) + "repair" + oldName.Substring(oldName.LastIndexOf("."));

                    VideoApi.SaveOrUpdateVideo(videoEntity);

                    File.Copy(oldPath, videoPath, true);
                    File.Delete(oldPath);

                    string oldMp4Path = oldPath.Substring(0, oldPath.LastIndexOf("\\"));
                    oldMp4Path += "\\mp4\\";
                    oldMp4Path += oldName + ".mp4";
                    if(File.Exists(oldMp4Path))
                    {
                        File.Delete(oldMp4Path);
                    }


                    targetPath = videoPath.Substring(0, videoPath.LastIndexOf("\\"));
                    targetPath += "\\mp4\\";
                    targetPath += videoEntity.videoName + ".mp4";
                }
            }
            else
            {
                UploadFileEntity uploadFileEntity = UploadFileApi.GetFileByFileIdSync(uuid);
                if(uploadFileEntity != null)
                {
                    string oldPath = uploadFileEntity.nowPath;

                    string path = oldPath.Substring(0, oldPath.LastIndexOf("."));
                    string suffix = oldPath.Substring(oldPath.LastIndexOf("."));

                    videoPath = path + "repair" + suffix;

                    uploadFileEntity.nowPath = videoPath;

                    string oldName = uploadFileEntity.fileName;
                    uploadFileEntity.fileName = oldName.Substring(0, oldName.LastIndexOf(".")) + "repair" + oldName.Substring(oldName.LastIndexOf("."));

                    UploadFileApi.SaveOrUpdateFileSync(uploadFileEntity);

                    File.Copy(oldPath, videoPath, true);
                    File.Delete(oldPath);

                    string oldMp4Path = oldPath.Substring(0, oldPath.LastIndexOf("\\"));
                    oldMp4Path += "\\mp4\\";
                    oldMp4Path += oldName + ".mp4";
                    if (File.Exists(oldMp4Path))
                    {
                        File.Delete(oldMp4Path);
                    }

                    targetPath = videoPath.Substring(0, videoPath.LastIndexOf("\\"));
                    targetPath += "\\mp4\\" + uploadFileEntity.fileName + ".mp4";
                }
            }

            if(string.IsNullOrEmpty(videoPath))
            {
                throw new Exception("信息不存在");
            }
            if(!File.Exists(videoPath))
            {
                throw new Exception("本地不存在视频");
            }

            //new Thread(() => VideoUtils.RepairVideo(videoPath, targetPath)).Start();
            Task.Run(() =>
            {
                VideoUtils.RepairVideo(videoPath, targetPath, type, uuid);
            });

        }

        /// <summary>
        /// 取消视频修复
        /// </summary>
        [HttpGet("取消视频修复", "/cancelRepairVideo")]
        public void CancelRepairVideo()
        {
            VideoUtils.normalEnd = false;
            if(VideoUtils.reEncodeProcessId != -1)
            {
                Process process = Process.GetProcessById(VideoUtils.reEncodeProcessId);
                if(process != null)
                {
                    process.Kill();
                    process.WaitForExit();
                }
                VideoUtils.reEncodeProcessId = -1;
            }
            
        }


        /// <summary>
        /// 压缩视频
        /// </summary>
        [HttpGet("压缩视频", "/compress")]
        public Dictionary<string, object> Compress(string videoId)
        {
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            return anchorVideoBll.Compress(videoId);

        }

        /// <summary>
        /// 分享文件复盘
        /// </summary>
        /// <param name="videoId">视频唯一标识</param>
        [HttpGet("分享文件复盘", "/shareanalysis")]
        public string ShareAnalysis(string videoId, string onlineFileUrl)
        {
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            return anchorVideoBll.ShareAnalysis(videoId, onlineFileUrl);

        }


        /// <summary>
        /// 停止自动分析
        /// </summary
        [HttpGet("分页获取主播视频", "/stopAutoAnalysis")]
        public void StopAutoAnalysis()
        {
            AnchorVideoBll.autoAnalysis = false;
        }


        /// <summary>
        /// 生成视频分析
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <param name="token">向服务器发请求的token</param>
        /// <param name="tradeId">行业id</param>
        [HttpGet("生成视频分析", "/createanalysis")]
        public string CreateAnalysis(string videoId, string token, string tradeId)
        {
            ServerTimeUtils.checkTimeAccurate();
            if (!ServerTimeUtils.timeAccurate)
            {
                throw new CustomException("本地电脑时间与北京时间不一致，请校准后再使用", 5601);
            }

            if (AnchorVideoBll.isAnalysis)
            {
                // 有视频还没分析完，不给分析
                return null;
            }
            else
            {
                AnchorVideoBll.isAnalysis = true;
                AnchorVideoBll.currentAnalysisId = videoId;
            }

            VideoEntity videoEntity = VideoApi.GetVideoByVideoIdSync(videoId);
            if(videoEntity == null)
            {
                throw new Exception("视频不存在");
            }

            if (!File.Exists(videoEntity.storagePath))
            {
                AnchorVideoBll.isAnalysis = false;
                // 文件不存在
                throw new Exception("本地磁盘没有视频文件");
            }

            // 判断用户是否有足够的分析时长余额
            int minute = minute = Convert.ToInt32(Convert.ToInt32(videoEntity.duration) / 60) < 1 ? 1 : Convert.ToInt32(Convert.ToInt32(videoEntity.duration) / 60);
            
            int analysisStatus = UserPropertyHttpUtils.CheckAnalysisMinute(minute);
            if (analysisStatus == 2)
            {
                AnchorVideoBll.isAnalysis = false;
                throw new Exception("可用的分析资源不足，请前往购买");
            }
            else if (analysisStatus == 0)
            {
                AnchorVideoBll.isAnalysis = false;
                throw new Exception("网络不佳，请重试");
            }

            // 异步线程，执行视频分析
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            new Thread(() => anchorVideoBll.Analysis(videoEntity)).Start();

            // 睡眠2.5秒
            Thread.Sleep(2500);

            return "success";
        }

        /// <summary>
        /// 重新生成视频分析
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <param name="token">向服务器发请求的token</param>
        /// <param name="tradeId">行业id</param>
        [HttpGet("生成视频分析", "/reanalysis")]
        public string ReAnalysis(string videoId, string token, string tradeId)
        {

            ServerTimeUtils.checkTimeAccurate();
            if (!ServerTimeUtils.timeAccurate)
            {
                throw new CustomException("本地电脑时间与北京时间不一致，请校准后再使用", 5601);
            }

            // 获取视频信息
            VideoEntity videoEntity = VideoApi.GetVideoByVideoIdSync(videoId);
            if (videoEntity == null)
            {
                throw new Exception("视频不存在");
            }

            if (videoEntity.isDownloaded == 1 && !File.Exists(videoEntity.storagePath))
            {
                // 文件不存在
                throw new Exception("本地视频文件不存在");
            }

            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            // 清除本地旧数据
            anchorVideoBll.DelVideoLocalData(videoEntity);

            // 修改视频的行业
            videoEntity.tradeId = tradeId;
            VideoApi.SaveOrUpdateVideo(videoEntity);

            // 标记本次重新分析强制走 Tencent（由 AutoAnalysis 队列捡起时消费，排队语义不变）
            AnchorVideoBll.MarkRetryTencent(videoEntity.videoId);

            // 设置视频的状态，使其重新加入到自动分析队列中
            VideoApi.UpdateVideoAnalysisStatusSync(videoEntity.videoId, 0, "");

            return "success";

        }

        ///// <summary>
        ///// 查看视频分析
        ///// </summary>
        ///// <param name="videoId">视频id</param>
        //[HttpGet("查看视频分析", "/lockanalysis")]
        //public SentenceMarkDto LockAnalysis(string videoId)
        //{
        //    AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
        //    return anchorVideoBll.LockAnalysis(videoId);
        //}
        ///// <summary>
        ///// 异步查看视频分析
        ///// </summary>
        ///// <param name="videoId">视频id</param>
        //[HttpGet("查看视频分析", "/lockanalysis")]
        //public async Task<SentenceMarkDto> LockAnalysis(string videoId)
        //{
        //    AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
        //    return await anchorVideoBll.LockVideoSisAnalyAsync(videoId);
        //}
        /// <summary>
        /// 异步查看视频分析
        /// </summary>
        /// <param name="videoId">视频id</param>
        [HttpGet("查看视频分析", "/lockanalysis")]
        public async Task<SentenceMarkDto> LockAnalysis(string videoId)
        {
            if (string.IsNullOrWhiteSpace(videoId))
            {
                return new SentenceMarkDto();
            }
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            SentenceMarkDto result = await anchorVideoBll.LockVideoSisAnalyAsync(videoId).ConfigureAwait(false);
            return result ?? new SentenceMarkDto();
        }

        /// <summary>
        /// 获取视频的播放地址
        /// </summary>
        /// <param name="videoId">视频id</param>
        [HttpGet("获取视频的播放地址", "/getVideoPayUrl")]
        public string getVideoPayUrl(string videoId)
        {
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            return anchorVideoBll.getVideoPayUrl(videoId);
        }

        /// <summary>
        /// 查看云空间视频分析
        /// </summary>
        /// <param name="videoId">视频id</param>
        [HttpGet("查看云空间视频分析", "/lockCloudAnalysis")]
        public SentenceMarkDto LockCloudAnalysis(string videoId)
        {
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            SentenceMarkDto sentenceMarkDto = anchorVideoBll.LockAnalysis(videoId);
            if(!File.Exists(sentenceMarkDto.videoInfo.StoragePath))
            {
                sentenceMarkDto.playUrl = sentenceMarkDto.videoInfo.PlayUrl;
            }
            
            return sentenceMarkDto;

            //AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            //CloudAnalysisVo cloudAnalysisVo = anchorVideoBll.LockCloudAnalysis(videoId);

            //var settings = new JsonSerializerSettings
            //{
            //    ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
            //};

            //string jsonStr = JsonConvert.SerializeObject(cloudAnalysisVo);
            //SentenceMarkDto sentenceMarkDto = JsonConvert.DeserializeObject<SentenceMarkDto>(jsonStr, settings);

            //List<AudioaAlysis> audioaAlyses = new List<AudioaAlysis>();
            //foreach (var item in cloudAnalysisVo.AnalysisList)
            //{
            //    AudioaAlysis audioaAlysis = new AudioaAlysis();
            //    audioaAlysis.VideoId = item.FileUuid;
            //    audioaAlysis.Paragraph = item.Paragraph;
            //    audioaAlysis.DataJson = item.DataJson;
            //    audioaAlysis.Status = item.Status;
            //    audioaAlyses.Add(audioaAlysis);

            //}
            //sentenceMarkDto.audioaAlyses = audioaAlyses;

            //// 查询是否巨量授权
            //if (ReplayHttpUtils.UserId == sentenceMarkDto.videoInfo.UserId)
            //{
            //    AnchorInfo temp = AnchorCacheManager.GetAnchorByIdFromCache(sentenceMarkDto?.anchorInfo?.SecUid ?? "");
            //    sentenceMarkDto.anchorInfo.juliangAuthStatus = temp?.juliangAuthStatus ?? 0;
            //    sentenceMarkDto.anchorInfo.IsRemoveRecord = temp?.IsRemoveRecord ?? 1;
            //}

            //// 获取巨量的数据
            //VideoDataViewingConfuseVo dataViewingConfuseVo = OceanEngineDataApi.videodataviewingInfoByVideoId(videoId);
            //if (dataViewingConfuseVo != null)
            //{
            //    // 互动率
            //    sentenceMarkDto.interactionPercent = dataViewingConfuseVo.interactionPercent;

            //    // 成交量
            //    sentenceMarkDto.purchaseCountStart = dataViewingConfuseVo.purchaseCountStart;
            //    sentenceMarkDto.purchaseCountEnd = dataViewingConfuseVo.purchaseCountEnd;
            //    sentenceMarkDto.purchaseCount = dataViewingConfuseVo.purchaseCountStart;

            //    // 总观看人次
            //    sentenceMarkDto.totalWatchNum = dataViewingConfuseVo.totalWatchNum;

            //    // 数据来源类型
            //    sentenceMarkDto.dataSourceType = dataViewingConfuseVo.dataSourceType;
            //}
            //// 对应分钟段落的成交数量列表
            //sentenceMarkDto.juLiangDataList = JuliangDataHandle.getParagraphJuLiang(videoId, sentenceMarkDto.audioaAlyses, sentenceMarkDto?.videoInfo?.StartTime ?? "");

            //// 计算互动率
            //if (sentenceMarkDto.interactionPercent == null && sentenceMarkDto.totalBarrageNum != null && sentenceMarkDto.totalBarrageNum > 0)
            //{
            //    try
            //    {
            //        // 获取场观
            //        SocketCollectMessageVo socketMessage = WordApi.socketMessageInfoNotJson(null, videoId, sentenceMarkDto?.videoInfo?.BatchNumber ?? null);
            //        if (socketMessage != null)
            //        {
            //            int observationNum = -1;
            //            int.TryParse(socketMessage.observationNum, out observationNum);
            //            if (observationNum != -1)
            //            {
            //                sentenceMarkDto.interactionPercent = Math.Round((double)sentenceMarkDto.totalBarrageNum / (double)observationNum * 100, 4);
            //            }
            //        }
            //    }
            //    catch (Exception ex)
            //    {
            //        FileUtils.LogError($"msg = {ex.Message}, StackTrace = {ex.StackTrace}", "获取场观失败");
            //    }

            //}

            //return sentenceMarkDto;
        }

        /// <summary>
        /// 预览视频
        /// </summary>
        /// <param name="videoId">视频id</param>
        [HttpGet("预览视频", "/preview")]
        public string preview(string videoId)
        {
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            return anchorVideoBll.preview(videoId);

        }

        /// <summary>
        /// 打开视频所在的目录
        /// </summary>
        /// <param name="videoId">视频Id</param>
        /// <returns></returns>
        [HttpGet("打开视频所在的目录", "/openfolder")]
        public bool OpenFolder(string videoId)
        {
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            return anchorVideoBll.OpenFolder(videoId);
        }

        /// <summary>
        /// 重新选择行业分析
        /// </summary>
        /// <param name="videoId">视频Id</param>
        /// <param name="tradeId">行业id</param>
        /// <param name="platformType">平台类型 0：全平台 1：...</param>
        /// <param name="duration">消耗时长，单位：分钟</param>
        [HttpGet("重新选择行业分析", "/reanalysisbytrade")]
        public async Task<SentenceMarkDto> ReAnalysisByTrade(string videoId, string tradeId, string platformType)
        {
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            return await anchorVideoBll.ReAnalysisByTradeAsync(videoId, tradeId, platformType).ConfigureAwait(false);
        }

        /// <summary>
        /// 根据视频id集合删除视频
        /// <param name="ids">视频id集合</param>
        /// </summary>
        /// <param name="ids">根据视频id集合</param>
        [HttpPost("根据视频id集合删除视频", "/deletebyids")]
        public async Task<string> DeleteByIds(List<string> ids)
        {
            if(ids == null || ids.Count == 0)
            {
                throw new Exception("视频id集合不能为空");
            }

            if (ids.Contains(AnchorVideoBll.currentAnalysisId) && AnchorVideoBll.isAnalysis)
            {
                throw new Exception("不允许删除分析中的视频");
            }

            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            await anchorVideoBll.DeleteAsync(ids).ConfigureAwait(false);

            return "success";
        }

        /// <summary>
        /// 根据视频id集合删除视频-仅删除视频
        /// <param name="ids">视频id集合</param>
        /// </summary>
        [HttpPost("根据视频id集合删除视频-仅删除视频", "/deleteLocalVideoByIds")]
        public string DeleteLocalVideoByIds(List<string> ids)
        {
            if (ids.Contains(AnchorVideoBll.currentAnalysisId) && AnchorVideoBll.isAnalysis)
            {
                throw new Exception("分析中的视频不允许删除");
            }

            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            anchorVideoBll.DeleteLocalVideoByIds(ids);

            return "success";
        }

        [HttpGet("分析详情页的数据曲线", "/onlineChartData")]
        public async Task<dynamic> OnlineChartData(string videoId)
        {
            return await BarrageApi.OnlineChartData2Async(videoId).ConfigureAwait(false);
        }

        [HttpGet("导出视频话术", "/exportVideoContent")]
        public async Task exportVideoContent(ExportVideoContentVo vo)
        {
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            await anchorVideoBll.exportVideoContentAsync(vo).ConfigureAwait(false);
        }

        [HttpPost("生成视频自然、优化原文内容", "/generateVideoContent")]
        public void generateVideoContent(GenerateVideoContentBo bo)
        {
            VideoContentAuto.add(bo);
        }

        /// <summary>
        /// 新增视频切片
        /// </summary>
        /// <param name="addVideoSliceBo">新增视频切片数据</param>
        /// <returns></returns>
        [HttpPost("新增视频切片", "/addVideoSlice")]
        public void addVideoSlice(AddVideoSliceBo addVideoSliceBo)
        {
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            anchorVideoBll.addVideoSlice(addVideoSliceBo);

        }

        [HttpPost("弹幕数据导出", "/queryDanMuExport")]
        public void queryDanMuExport(dynamic bo)
        {
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            anchorVideoBll.queryDanMuExport(bo);
        }

        /// <summary>
        /// 修改视频名称
        /// </summary>
        /// <param name="renameVideoBo">修改视频名称参数</param>
        /// <returns></returns>
        [HttpPost("修改视频名称", "/renameVideo")]
        public async Task RenameVideo(RenameVideoBo renameVideoBo)
        {
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            await anchorVideoBll.RenameVideoAsync(renameVideoBo.videoId, renameVideoBo.newVideoName).ConfigureAwait(false);
        }

        [HttpPost("修改数据诊断配置", "/updateDataDiagnosisConfig")]
        public DataDiagnosisConfigVo updateDataDiagnosisConfig(DataDiagnosisConfigVo bo)
        {
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            return anchorVideoBll.updateDataDiagnosisConfig(bo);
        }

        /// <summary>
        /// 拉取商品数据（统一采集+上传，同时上报 fupan-governance-server 和 fupan-server）
        /// </summary>
        /// <param name="bo">请求参数（secUid、batchNumber、videoId）</param>
        [HttpPost("拉取商品数据", "/pullProduct")]
        public async Task pullProduct(PullProductBo bo)
        {
            var anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(bo.secUid);
            if (anchorInfo == null) return;

            await AnchorInstance.CollectAndUploadAsync(anchorInfo, bo.batchNumber, bo.videoId);
        }

    }
}
