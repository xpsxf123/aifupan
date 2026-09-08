using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Attributes;
using ReviewAnalysis.Bll;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Model;
using ReviewAnalysis.vo;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Controller
{
    [RestController("对比数据", "api/contrast")]
    public class ContrastController
    {
        /// <summary>
        /// 分享对比数据
        /// </summary>
        /// <param name="contrastId">对比唯一标识</param>
        [HttpGet("分享对比数据", "/shareanalysis")]
        public string ShareAnalysis(string contrastId)
        {
            VideoContrastBll videoContrastBll = new VideoContrastBll();
            return videoContrastBll.ShareAnalysis(contrastId);

        }


        /// <summary>
        /// 创建视频分析对比
        /// </summary>
        /// <param name="videoContrast"></param>
        [HttpPost("创建视频分析对比", "/savecontrast")]
        public void SaveContrast(VideoContrast videoContrast)
        {
            VideoContrastBll videoContrastBll = new VideoContrastBll();

            videoContrastBll.Save(videoContrast);
        }


        /// <summary>
        /// 查看视频分析对比
        /// </summary>
        /// <param name="contrastId">对比id</param>
        [HttpGet("查看视频分析对比", "/lockanalysiscontrast")]
        public async Task<SentenceMarkContrastDto> LockAnalysisContrast(string contrastId)
        {
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            // 使用异步方法
            return await anchorVideoBll.LockAnalysisContrastAsync(contrastId).ConfigureAwait(false);
        }

        /// <summary>
        /// 查看云空间分析对比
        /// </summary>
        /// <param name="contrastId">对比id</param>
        [HttpGet("查看云空间分析对比", "/lockCloudContrast")]
        public async Task<SentenceMarkContrastDto> lockCloudContrast(string contrastId)
        {
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            // 使用异步方法
            SentenceMarkContrastDto sentenceMarkContrastDto = await anchorVideoBll.LockAnalysisContrastAsync(contrastId).ConfigureAwait(false);

            if(!string.IsNullOrEmpty(sentenceMarkContrastDto.VideoContrast.VideoOneId))
            {
                if (!File.Exists(sentenceMarkContrastDto.SentenceMark1.videoInfo.StoragePath))
                {
                    sentenceMarkContrastDto.SentenceMark1.playUrl = sentenceMarkContrastDto.SentenceMark1.videoInfo.PlayUrl;
                }
                if (!File.Exists(sentenceMarkContrastDto.SentenceMark2.videoInfo.StoragePath))
                {
                    sentenceMarkContrastDto.SentenceMark2.playUrl = sentenceMarkContrastDto.SentenceMark2.videoInfo.PlayUrl;
                }
            }
            return sentenceMarkContrastDto;

            //VideoContrastBll videoContrastBll = new VideoContrastBll();
            //CloudContrastAnalysisVo cloudContrastAnalysisVo = videoContrastBll.lockCloudContrast(contrastId);

            //var settings = new JsonSerializerSettings
            //{
            //    ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
            //};

            //SentenceMarkContrastDto sentenceMarkContrastDto = new SentenceMarkContrastDto();

            //string jsonStr1 = JsonConvert.SerializeObject(cloudContrastAnalysisVo.SentenceMark1);
            //SentenceMarkDto sentenceMarkDto1 = JsonConvert.DeserializeObject<SentenceMarkDto>(jsonStr1, settings);

            //List<AudioaAlysis> audioaAlyses1 = new List<AudioaAlysis>();
            //foreach (var item in cloudContrastAnalysisVo.SentenceMark1.AnalysisList)
            //{
            //    AudioaAlysis audioaAlysis = new AudioaAlysis();
            //    audioaAlysis.VideoId = item.FileUuid;
            //    audioaAlysis.Paragraph = item.Paragraph;
            //    audioaAlysis.DataJson = item.DataJson;
            //    audioaAlysis.Status = item.Status;
            //    audioaAlyses1.Add(audioaAlysis);

            //}
            //sentenceMarkDto1.audioaAlyses = audioaAlyses1;
            //sentenceMarkContrastDto.SentenceMark1 = sentenceMarkDto1;

            //string jsonStr2 = JsonConvert.SerializeObject(cloudContrastAnalysisVo.SentenceMark2);
            //SentenceMarkDto sentenceMarkDto2 = JsonConvert.DeserializeObject<SentenceMarkDto>(jsonStr2, settings);

            //List<AudioaAlysis> audioaAlyses2 = new List<AudioaAlysis>();
            //foreach (var item in cloudContrastAnalysisVo.SentenceMark2.AnalysisList)
            //{
            //    AudioaAlysis audioaAlysis = new AudioaAlysis();
            //    audioaAlysis.VideoId = item.FileUuid;
            //    audioaAlysis.Paragraph = item.Paragraph;
            //    audioaAlysis.DataJson = item.DataJson;
            //    audioaAlysis.Status = item.Status;
            //    audioaAlyses2.Add(audioaAlysis);

            //}
            //sentenceMarkDto2.audioaAlyses = audioaAlyses2;
            //sentenceMarkContrastDto.SentenceMark2 = sentenceMarkDto2;


            //CloudContrastInfoVo cloudContrastInfoVo = ReplayHttpUtils.GetServerContrastByContrastId(contrastId);
            //if(cloudContrastInfoVo != null)
            //{
            //    sentenceMarkContrastDto.VideoContrast = JsonConvert.DeserializeObject<VideoContrast>(JsonConvert.SerializeObject(cloudContrastInfoVo), settings);
            //}

            //return sentenceMarkContrastDto;
        }

    }
}
