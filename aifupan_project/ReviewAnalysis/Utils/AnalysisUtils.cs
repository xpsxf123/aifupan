using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Ai;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.bo.video;
using ReviewAnalysis.Dto;
using ReviewAnalysis.entity.uploadFile;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.Model;
using ReviewAnalysis.ShortVideo.Model;
using ReviewAnalysis.vo;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;

namespace ReviewAnalysis.Utils
{
    public class AnalysisUtils
    {

        /// <summary>
        /// 获取视频的mp4路径
        /// </summary>
        /// <param name="sourceVideoPath">原视频路径</param>
        /// <returns></returns>
        public static string getVideoMp4Path(string sourceVideoPath, string sourceVideoName)
        {
            string videoPath = sourceVideoPath.Substring(0, sourceVideoPath.LastIndexOf("\\"));
            videoPath += "\\mp4\\";
            videoPath += sourceVideoName + ".mp4";
            return videoPath;
        }

        /// <summary>
        /// 检测是否需要自动生成诊断报告/生成自然/优化原文
        /// </summary>
        /// <param name="video">视频信息</param>
        /// <returns></returns>
        public static async Task checkAutoGenerate(VideoEntity video)
        {
            new Thread(() =>
            {
                try
                {
                    // 诊断报告自动生成-是使用智能分析时长的才生成诊断报告
                    DiagnosisAuto.generateDiagnosis(video);
                }
                catch (Exception ex)
                {
                    FileUtils.LogError(ex.Message, "诊断报告自动生成失败");
                }

                // 检测是否要生成自然/优化原文
                try
                {
                    VideoContentAuto.generateContent(video);
                }
                catch (Exception ex)
                {
                    FileUtils.LogError(ex.Message, "生成自然/优化原文自动生成失败");
                }

            }).Start();
        }

        /// <summary>
        /// 获取视频本地分析数据
        /// </summary>
        /// <param name="video">视频信息</param>
        /// <returns></returns>
        public static AnalysisResultTxtVo getVideoLocalAnalysisData(VideoEntity video)
        {
            string filePath = Path.GetFullPath($"analysisData\\video\\{video.startTime.Substring(0, 10)}\\{video.videoId}.txt");
            if (File.Exists(filePath))
            {
                string content = File.ReadAllText(filePath);
                if (!string.IsNullOrEmpty(content))
                {
                    AnalysisResultTxtVo analysisResultTxtVo = JsonConvert.DeserializeObject<AnalysisResultTxtVo>(content);
                    return analysisResultTxtVo;
                }
            }

            return null;
        }

        /// <summary>
        /// 获取文件本地分析数据
        /// </summary>
        /// <param name="uploadFile">文件信息</param>
        /// <returns></returns>
        public static AnalysisResultTxtVo getFileLocalAnalysisData(UploadFileEntity uploadFile)
        {
            string filePath = Path.GetFullPath($"analysisData\\file\\{uploadFile.uploadTime.Substring(0, 10)}\\{uploadFile.fileId}.txt");
            if (File.Exists(filePath))
            {
                string content = File.ReadAllText(filePath);
                if (!string.IsNullOrEmpty(content))
                {
                    AnalysisResultTxtVo analysisResultTxtVo = JsonConvert.DeserializeObject<AnalysisResultTxtVo>(content);
                    return analysisResultTxtVo;
                }
            }

            return null;
        }

        /// <summary>
        /// 将视频分析数据存到本地文件
        /// </summary>
        /// <param name="analysisResultVo">分析结果数据</param>
        /// <param name="tradeId">行业id</param>
        /// <param name="videoEntity">视频信息</param>
        /// <returns></returns>
        public static AnalysisResultTxtVo saveVideoLocalAnalysisData(AnalysisResultVo analysisResultVo, string tradeId, VideoEntity videoEntity)
        {
            // 存到本地文件的数据
            AnalysisResultTxtVo analysisResultTxtVo = new AnalysisResultTxtVo();

            // 整理分析数据
            if (analysisResultVo.sentenceMarkVos != null && analysisResultVo.sentenceMarkVos.Count > 0)
            {
                List<AudioaAlysis> audioaAlyses = new List<AudioaAlysis>();
                foreach (var item in analysisResultVo.sentenceMarkVos)
                {
                    AudioaAlysis audioaAlysis = new AudioaAlysis();
                    audioaAlysis.Paragraph = item.CurrentSort;
                    audioaAlysis.VideoId = item.VideoId;
                    audioaAlysis.Status = 0;
                    audioaAlysis.DataJson = item == null ? "" : JsonConvert.SerializeObject(item);
                    audioaAlysis.TradeId = tradeId;
                    audioaAlyses.Add(audioaAlysis);
                }

                analysisResultTxtVo.audioaAlyses = audioaAlyses;
            }

            analysisResultTxtVo.cruxTypeList = analysisResultVo.cruxTypeList;
            analysisResultTxtVo.wordsCollect = analysisResultVo.wordsCollect;
            analysisResultTxtVo.wordsTabList = analysisResultVo.wordsTabList;

            // 将分析结果存到本地文件
            string jsonStr = JsonConvert.SerializeObject(analysisResultTxtVo);
            string filePath = Path.GetFullPath($"analysisData\\video\\{videoEntity.startTime.Substring(0, 10)}\\{videoEntity.videoId}.txt");
            FileUtils.WriteTxtFile(filePath, jsonStr);

            return analysisResultTxtVo;
        }

        /// <summary>
        /// 将文件分析数据存到本地文件
        /// </summary>
        /// <param name="analysisResultVo">分析结果数据</param>
        /// <param name="tradeId">行业id</param>
        /// <param name="uploadFile">文件信息</param>
        /// <returns></returns>
        public static AnalysisResultTxtVo saveFileLocalAnalysisData(AnalysisResultVo analysisResultVo, string tradeId, UploadFileEntity uploadFile)
        {
            // 存到本地文件的数据
            AnalysisResultTxtVo analysisResultTxtVo = new AnalysisResultTxtVo();

            // 整理分析数据
            if (analysisResultVo.sentenceMarkVos != null && analysisResultVo.sentenceMarkVos.Count > 0)
            {
                List<UploadFileAlysis> audioaAlyses = new List<UploadFileAlysis>();
                foreach (var item in analysisResultVo.sentenceMarkVos)
                {
                    UploadFileAlysis audioaAlysis = new UploadFileAlysis();
                    audioaAlysis.Paragraph = item.CurrentSort;
                    audioaAlysis.FileId = item.VideoId;
                    audioaAlysis.Status = 0;
                    audioaAlysis.DataJson = item == null ? "" : JsonConvert.SerializeObject(item);
                    audioaAlysis.TradeId = tradeId;
                    audioaAlyses.Add(audioaAlysis);
                }
                analysisResultTxtVo.fileAudioaAlyses = audioaAlyses;
            }

            analysisResultTxtVo.cruxTypeList = analysisResultVo.cruxTypeList;
            analysisResultTxtVo.wordsCollect = analysisResultVo.wordsCollect;
            analysisResultTxtVo.wordsTabList = analysisResultVo.wordsTabList;

            // 将分析结果存到本地文件
            string jsonStr = JsonConvert.SerializeObject(analysisResultTxtVo);
            string filePath = Path.GetFullPath($"analysisData\\file\\{uploadFile.uploadTime.Substring(0, 10)}\\{uploadFile.fileId}.txt");
            FileUtils.WriteTxtFile(filePath, jsonStr);

            return analysisResultTxtVo;
        }

        /// <summary>
        /// 检查asr是否有错误
        /// </summary>
        /// <param name="asrResultList">识别结果列表</param>
        /// <returns>错误提示，为空表示没有错误</returns>
        public static async Task<string> checkAsrError(Dictionary<string, object> asrResultList)
        {
            if (asrResultList == null)
            {
                return "网络不佳";
            }

            int code = (int)asrResultList["code"];
            List<ASRResultEntity> asrResultEntities = (List<ASRResultEntity>)asrResultList["data"];

            if (code == 500)
            {
                return "网络不佳";
            }
            else if (code == 601)
            {
                return "电脑时间不正确";
            }
            else if (code == 602)
            {
                return "凭证无效";
            }
            else if (code == 603)
            {
                return "QPS已满";
            }
            else if (code == 701)
            {
                return "音频文件不存在";
            }
            else if (code == 702)
            {
                return "模型文件未就绪，请先下载模型";
            }
            else if (code == 703)
            {
                return "音频解码失败";
            }
            else if (code == 704)
            {
                return "音频超过60秒限制";
            }

            return null;
        }
    }
}
