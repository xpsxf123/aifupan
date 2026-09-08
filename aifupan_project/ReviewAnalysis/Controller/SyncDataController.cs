using ReviewAnalysis.Asr;
using ReviewAnalysis.Attributes;
using ReviewAnalysis.Model;
using ReviewAnalysis.DataCache;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using ReviewAnalysis.Bll;
using douyin.Utils;
using Newtonsoft.Json;

namespace ReviewAnalysis.Controller
{
    [RestController("配置相关的接口", "api/sync")]
    public class SyncDataController
    {

        ///// <summary>
        ///// 同步数据
        ///// </summary>
        ///// <returns></returns>
        //[HttpGet("同步数据", "/syncdata")]
        //public void SyncData(string token)
        //{
        //    ReplayHttpUtils.Token = token;

        //    // 同步主播数据
        //    List<AnchorInfo> anchorInfos = AnchorCacheManager.GetAllAnchors();
        //    if (anchorInfos != null && anchorInfos.Count > 0)
        //    {
        //        List<string> secUidList = new List<string>();
        //        foreach (var anchorItem in anchorInfos)
        //        {
        //            // 删除服务器旧关联
        //            ReplayHttpUtils.DelUserAnchor(token, anchorItem.SecUid);

        //            secUidList.Add(anchorItem.SecUid);
        //        }
        //        // 添加新关联
        //        ReplayHttpUtils.SaveUserAnchor(token, secUidList, "1");
        //    }

        //    // 同步视频数据
        //    List<AnchorVideo> anchorVideos = AnchorVideoCacheManager.GetAllList();
        //    if (anchorVideos != null && anchorVideos.Count > 0)
        //    {
        //        foreach (var videoItem in anchorVideos)
        //        {
        //            videoItem.VideoId = Guid.NewGuid().ToString();
        //            // 同步视频
        //            ReplayHttpUtils.SaveVideoToServer(videoItem);

        //            // 同步分析数据
        //            SyncVideoAnalysis(videoItem);
        //        }
        //    }

        //    // 同步文件数据
        //    UploadFile uploadFile = new UploadFile();
        //    List<UploadFile> uploadFileList = uploadFile.GetList();
        //    if(uploadFileList != null && uploadFileList != null )
        //    {
        //        foreach (var uploadFileItem in uploadFileList)
        //        {
        //            uploadFileItem.FileId = Guid.NewGuid().ToString();
        //            // 同步文件
        //            ReplayHttpUtils.SaveFileToServer(uploadFileItem);
        //            // 同步文件分析数据
        //            SyncFileAnalysis(uploadFileItem);
        //        }
        //    }
        //}

        //private void SyncFileAnalysis(UploadFile uploadFile)
        //{
        //    UploadFileAlysis uploadFileAlysis = new UploadFileAlysis();
        //    uploadFileAlysis.FileId = uploadFile.Id + "";
        //    uploadFileAlysis.TradeId = uploadFile.TradeId;
        //    List<UploadFileAlysis> uploadFileAlysisList = uploadFileAlysis.GetList();
        //    if(uploadFileAlysisList != null && uploadFileAlysisList != null )
        //    {
        //        foreach (var uploadFileAlysisItem in uploadFileAlysisList)
        //        {
        //            uploadFileAlysisItem.FileId = uploadFile.FileId;
        //        }

        //        // 删除文件分析
        //        uploadFileAlysis.DeleteModelByFileId(uploadFile.Id + "");

        //        // 排序
        //        uploadFileAlysisList.Sort((p1, p2) => p1.Paragraph.CompareTo(p2.Paragraph));

        //        foreach (var item in uploadFileAlysisList)
        //        {
        //            int isLastParagraph = item.Paragraph == uploadFileAlysisList.Count ? 1 : 0;
        //            ASRResultEntity asrResult = new ASRResultEntity();

        //            // 配置序列化忽略大小写
        //            var settings = new JsonSerializerSettings
        //            {
        //                ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
        //            };
        //            SentenceMarkVo sentenceMarkVo = JsonConvert.DeserializeObject<SentenceMarkVo>(item.DataJson, settings);

        //            asrResult.Result = sentenceMarkVo.Content;
        //            asrResult.Paragraph = item.Paragraph;
        //            List<ASRWordEntity> WordList = new List<ASRWordEntity>();
        //            if (sentenceMarkVo.Items != null && sentenceMarkVo.Items.Count > 0)
        //            {
        //                foreach (var words in sentenceMarkVo.Items)
        //                {
        //                    ASRWordEntity asrWordEntity = new ASRWordEntity();
        //                    asrWordEntity.Word = words.Word;
        //                    asrWordEntity.StartTime = words.StartTime;
        //                    asrWordEntity.EndTime = words.EndTime;
        //                    WordList.Add(asrWordEntity);
        //                }
        //            }
        //            asrResult.WordList = WordList;

        //            if (uploadFile.FileType == 0)
        //            {
        //                // 视频或音频
        //                List<SentenceMarkVo> sentenceMarkVoList = ReplayHttpUtils.WordsMarkSync(ReplayHttpUtils.Token, "DouYin", uploadFile.FileId, isLastParagraph, asrResult, uploadFile.TradeId, 1, 1);
        //                // 将结果存到数据库
        //                if (sentenceMarkVoList != null)
        //                {
        //                    foreach (var sentenceMark in sentenceMarkVoList)
        //                    {
        //                        SaveFileSentenceMarkToDb(sentenceMark, 0, uploadFile.TradeId);
        //                    }
        //                }
        //                else
        //                {
        //                    // 将文件分析状态改成失败，原因：关键词/敏感词识别失败
        //                    uploadFile.UpdateAnalysisStatus(uploadFile.FileId, 3, "关键词识别失败");
        //                }
        //            }
        //            else
        //            {
        //                // 文本文件
        //                string content = asrResult.Result;
        //                // 将文本内容传到服务器，进行关键词/敏感词识别
        //                SentenceMarkVo reAnalysisSentenceMarkVo = ReplayHttpUtils.WordsMarkByTextSync(ReplayHttpUtils.Token, "DouYin", uploadFile.FileId, content, uploadFile.TradeId, 1);
        //                if (reAnalysisSentenceMarkVo != null)
        //                {
        //                    // 将识别结果保存到数据库
        //                    SaveFileSentenceMarkToDb(reAnalysisSentenceMarkVo, 0, uploadFile.TradeId);
        //                }
        //                else
        //                {
        //                    // 将文件分析状态改成失败，原因：关键词/敏感词识别失败
        //                    uploadFile.UpdateAnalysisStatus(uploadFile.FileId, 3, "关键词识别失败");
        //                }
        //            }

        //        }

        //        // 将文件分析记录存到服务器
        //        UploadFileAlysis uploadFileAlysis1 = new UploadFileAlysis();
        //        uploadFileAlysis1.FileId = uploadFile.FileId;
        //        uploadFileAlysis1.TradeId = uploadFile.TradeId;
        //        List<UploadFileAlysis> serverUploadFileAlyses = uploadFileAlysis1.GetList();
        //        foreach(var item in serverUploadFileAlyses)
        //        {
        //            item.FileId = uploadFile.FileId;
        //        }
        //        ReplayHttpUtils.SaveFileAnalysisToServer(serverUploadFileAlyses);
        //    }
        //}

        //private void SyncVideoAnalysis(AnchorVideo videoItem)
        //{
        //    AudioaAlysis audioaAlysis = new AudioaAlysis();
        //    audioaAlysis.VideoId = videoItem.Id + "";
        //    audioaAlysis.TradeId = videoItem.TradeId;
        //    List<AudioaAlysis> audioaAlysisList = audioaAlysis.GetList();
        //    if (audioaAlysisList != null && audioaAlysisList.Count > 0)
        //    {
        //        foreach (var alysisItem in audioaAlysisList)
        //        {
        //            alysisItem.VideoId = videoItem.VideoId;
        //        }
        //        // 删除视频分析
        //        audioaAlysis.DeleteModelByVideoId(videoItem.Id + "");

        //        // 排序
        //        audioaAlysisList.Sort((p1, p2) => p1.Paragraph.CompareTo(p2.Paragraph));

        //        foreach (var item in audioaAlysisList)
        //        {
        //            int isLastParagraph = item.Paragraph == audioaAlysisList.Count ? 1 : 0;
        //            ASRResultEntity asrResult = new ASRResultEntity();

        //            // 配置序列化忽略大小写
        //            var settings = new JsonSerializerSettings
        //            {
        //                ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
        //            };
        //            SentenceMarkVo sentenceMarkVo = JsonConvert.DeserializeObject<SentenceMarkVo>(item.DataJson, settings);

        //            asrResult.Result = sentenceMarkVo.Content;
        //            asrResult.Paragraph = item.Paragraph;
        //            List<ASRWordEntity> WordList = new List<ASRWordEntity>();
        //            if (sentenceMarkVo.Items != null && sentenceMarkVo.Items.Count > 0)
        //            {
        //                foreach (var words in sentenceMarkVo.Items)
        //                {
        //                    ASRWordEntity asrWordEntity = new ASRWordEntity();
        //                    asrWordEntity.Word = words.Word;
        //                    asrWordEntity.StartTime = words.StartTime;
        //                    asrWordEntity.EndTime = words.EndTime;
        //                    WordList.Add(asrWordEntity);
        //                }
        //            }
        //            asrResult.WordList = WordList;

        //            List<SentenceMarkVo> sentenceMarkVoList = ReplayHttpUtils.WordsMarkSync(ReplayHttpUtils.Token, "DouYin", videoItem.VideoId, isLastParagraph, asrResult, videoItem.TradeId, 1, 0);
        //            FileUtils.LogAnalysis($"重新选择行业分析-识别结果：{JsonConvert.SerializeObject(sentenceMarkVoList)}");
        //            // 将结果存到数据库
        //            if (sentenceMarkVoList != null)
        //            {
        //                foreach (var sentenceMark in sentenceMarkVoList)
        //                {
        //                    SaveSentenceMarkToDb(sentenceMark, 0, videoItem.TradeId);
        //                }
        //                FileUtils.LogAnalysis($"重新选择行业分析-将结果存到数据库成功");
        //            }
        //        }

        //        // 同步视频分析数据到服务器
        //        AudioaAlysis audioaAlysis1 = new AudioaAlysis();
        //        audioaAlysis1.VideoId = videoItem.VideoId;
        //        audioaAlysis1.TradeId = videoItem.TradeId;
        //        List<AudioaAlysis> serverAudioaAlysisList = audioaAlysis1.GetList();
        //        foreach (var item in serverAudioaAlysisList)
        //        {
        //            item.VideoId = videoItem.VideoId;
        //        }

        //        ReplayHttpUtils.SaveVideoAnalysisToServer(serverAudioaAlysisList);

        //    }
        //}


        //public void SaveFileSentenceMarkToDb(SentenceMarkVo item, int status, string tradeId)
        //{
        //    try
        //    {
        //        UploadFileAlysis uploadFileAlysis = new UploadFileAlysis();
        //        uploadFileAlysis.Paragraph = item.CurrentSort;
        //        uploadFileAlysis.FileId = item.VideoId;
        //        uploadFileAlysis.Status = status;
        //        uploadFileAlysis.DataJson = item == null ? "" : JsonConvert.SerializeObject(item);
        //        uploadFileAlysis.TradeId = tradeId;
        //        uploadFileAlysis.save();
        //    }
        //    catch (Exception ex)
        //    {
        //        FileUtils.LogAnalysis($"将词语分析结果存到数据库异常===={JsonConvert.SerializeObject(item)}====={status}");
        //        throw new Exception();
        //    }
        //}

        //public void SaveSentenceMarkToDb(SentenceMarkVo item, int status, string tradeId)
        //{
        //    try
        //    {
        //        AudioaAlysis audioaAlysis = new AudioaAlysis();
        //        audioaAlysis.Paragraph = item.CurrentSort;
        //        audioaAlysis.VideoId = item.VideoId;
        //        audioaAlysis.Status = status;
        //        audioaAlysis.DataJson = item == null ? "" : JsonConvert.SerializeObject(item);
        //        audioaAlysis.TradeId = tradeId;
        //        audioaAlysis.save();
        //    }
        //    catch (Exception ex)
        //    {
        //        FileUtils.LogAnalysis($"将词语分析结果存到数据库异常===={JsonConvert.SerializeObject(item)}====={status},错误信息：{ex.Message.ToString()}");
        //        throw new Exception();
        //    }

        //}


    }
}
