using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.Ai.Model;
using ReviewAnalysis.api;
using ReviewAnalysis.Dto;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.ai;
using ReviewAnalysis.vo.cueWords;

namespace ReviewAnalysis.Ai.impl.diagnosis
{
    public abstract class AbstractDiagnosis : DiagnosisAskType
    {
        public AiAutoTimerDto dto = null;
        public CueWordsVo cueWordsVo = null;
        public VideoEntity video = null;
        public AskRequestDto result = null;

        public virtual async Task<AskRequestDto> getParams(AiAutoTimerDto dto)
        {
            this.dto = dto;
            result = new AskRequestDto();
            result.sourceId = dto.sourceId;
            result.sourceType = dto.sourceType;
            result.type = dto.cueType;
            result.paragraphCode = "0";

            // 获取视频相关的
            await setVideo();

            // 获取提示词
            await setProblem();

            // 获取模型
            await setModel();

            // 设置额外的参数
            await setAdditionalParams();

            return result;
        }

        public virtual async Task setModel()
        {
            int sourceType = dto.sourceType == 0 ? 1 : 0;

            ModelConfigVo modelConfig = await DiagnosisApi.aiModelBySourceIdAndType(dto.sourceId, sourceType, dto.diagnosisType);
            if(modelConfig == null)
            {
                CustomException.create("获取模型失败");
            }

            result.aiModel = modelConfig.aiModel ?? 0;
        }

        /// <summary>
        /// 获取视频相关的参数
        /// </summary>
        /// <returns></returns>
        public virtual async Task setVideo()
        {
            video = await VideoApi.GetVideoByVideoId(dto.sourceId);
            if(video == null)
            {
                CustomException.create("视频查询失败");
            }
            int duration = 0;
            int.TryParse(video.duration, out duration);

            // 分析全视频
            result.videoTimeOneList = new List<long>() { 0, duration * 1000};
        }

        /// <summary>
        /// 获取提示词相关的参数
        /// </summary>
        /// <returns></returns>
        public virtual async Task setProblem()
        {
            if (string.IsNullOrEmpty(dto.cueWordsId))
            {
                CustomException.create("问题id不能为空");
            }
            cueWordsVo = await CueWordsApi.info(dto.cueWordsId);
            if (cueWordsVo == null)
            {
                CustomException.create("问题获取失败");
            }
            result.cueWordsId = dto.cueWordsId;
            result.cueWordsType = 0;
            result.content = cueWordsVo.cueWord;
            result.realContent = cueWordsVo.problem;
        }

        /// <summary>
        /// 设置额外的参数
        /// </summary>
        /// <returns></returns>
        public abstract Task setAdditionalParams();
    }
}
