using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Policy;
using System.Text;
using System.Threading.Tasks;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Ai.Model;
using ReviewAnalysis.Asr;
using ReviewAnalysis.bo.diagnosis;
using ReviewAnalysis.entity.anchor;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using ReviewAnalysis.vo.ai;
using ReviewAnalysis.vo.diagnosis;
using ReviewAnalysis.vo.proxyIP;

namespace ReviewAnalysis.api
{
    public class DiagnosisApi
    {
        /// <summary>
        /// 保存ai问答记录数据
        /// </summary>
        /// <param name="list"></param>
        /// <returns></returns>
        public static async Task<List<ConversationDto>> saveConversationData(List<ConversationDto> list)
        {
            string dataStr = await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/ai/conversation/saveConversationData", list);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<List<ConversationDto>>(dataStr);
            }

            return new List<ConversationDto>();
        }

        public static List<ConversationDto> saveConversationDataNotAsync(List<ConversationDto> list)
        {
            string dataStr = HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/ai/conversation/saveConversationData", list);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<List<ConversationDto>>(dataStr);
            }

            return new List<ConversationDto>();
        }

        /// <summary>
        /// 更新ai诊断中提示词状态
        /// </summary>
        /// <param name="id"></param>
        /// <param name="status">状态 0未处理 1处理中 2处理完成 3处理失败</param>
        /// <param name="error">错误内容</param>
        /// <returns></returns>
        public static async Task updateDiagnosisCueStatus(long id, int status, string error = null)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("id", id);
            param.Add("qaStatus", status);
            param.Add("errorContent", error);

            await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/ai/diagnosis/updateDiagnosisCueStatus", param);
        }

        public static async Task<ModelConfigVo> aiModelBySourceIdAndType(string sourceId, int sourceTyoe, int diagnosisType)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("sourceId", sourceId);
            param.Add("sourceType", sourceTyoe);
            param.Add("diagnosisType", diagnosisType);
            string dataStr = await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/ai/diagnosis/aiModelBySourceIdAndType", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<ModelConfigVo>(dataStr);
            }

            return null;
        }

        /// <summary>
        /// 获取要自动提问的诊断问题
        /// </summary>
        /// <param name="videoId"></param>
        /// <returns></returns>
        public static async Task<List<DiagnosisCueVo>> getAutoDiagnosisQuestions(string videoId)
        {

            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            string dataStr = await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/ai/diagnosis/getAutoDiagnosisQuestions", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<List<DiagnosisCueVo>>(dataStr);
            }

            return null;

        }

        /// <summary>
        /// 查询视频是否生成诊断报告
        /// </summary>
        /// <param name="sourceId"></param>
        /// <returns></returns>
        public static bool isGenerateDiagnosisFile(AiAutoTimerDto dto)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("sourceId", dto.sourceId);
            param.Add("diagnosisType", dto.diagnosisType);

            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/ai/diagnosis/isGenerateDiagnosisFile", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return "yes".Equals(dataStr);
            }
            return false;
        }

        /// <summary>
        /// 诊断分析-提示词添加分析
        /// </summary>
        /// <param name="bo"></param>
        /// <returns></returns>
        public static List<DiagnosisCueVo> saveDiagnosis(SaveDiagnosisBo bo)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("sourceId", bo.videoId);
            param.Add("cueWordsIds", new List<long>() { bo.cueWordsId });
            var r = HttpUtils.SendServerPostR(ReplayHttpUtils.BaseUrl + "/ai/diagnosis/saveDiagnosis", param);
            if (r.success() && r.data != null)
            {
                return JsonConvert.DeserializeObject<List<DiagnosisCueVo>>(r.data);
            }
            else
            {
                throw new CustomException(r?.msg ?? "当前网络情况不佳，请稍后重试或联系管理员");
            }
        }

        /// <summary>
        /// 获取视频处理完成的诊断报告问题
        /// </summary>
        /// <param name="videoId"></param>
        /// <returns></returns>
        public static List<DiagnosisCueVo> getHandelSuccessDiagnosis(AiAutoTimerDto dto)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("sourceId", dto.sourceId);
            param.Add("diagnosisType", dto.diagnosisType);
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/ai/diagnosis/getHandelSuccessDiagnosis", param);
            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<List<DiagnosisCueVo>>(dataStr);
            }

            return new List<DiagnosisCueVo>() ;
        }

        /// <summary>
        /// 获取用户待分析和分析中的诊断报告
        /// </summary>
        /// <returns></returns>
        public static async Task<List<DiagnosisCueVo>> handleDiagnosisByUser()
        {
            string dataStr = await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/ai/diagnosis/handleDiagnosisByUser", null);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<List<DiagnosisCueVo>>(dataStr);
            }

            return null;
        }

        /// <summary>
        /// 获取诊断报告上传的预签名链接
        /// </summary>
        /// <param name="sourceId"></param>
        /// <param name="sourceType"></param>
        /// <returns></returns>
        public static SignUploadUrlVo getDiagnosisSignUploadUrl(string sourceId, int sourceType, int? uploadType)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("sourceId", sourceId);
            param.Add("sourceType", sourceType);
            param.Add("uploadType", uploadType);
            string dataStr = HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/ai/diagnosis/getDiagnosisSignUploadUrl", param);
            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<SignUploadUrlVo>(dataStr);
            }

            return null;
        }

        /// <summary>
        /// 获取诊断报告下载的预签名链接
        /// </summary>
        /// <param name="videoId"></param>
        /// <returns></returns>
        public static string getDiagnosisDownloadUrl(GenerateReportVo vo)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", vo.videoId);
            param.Add("sourceType", vo.sourceType);
            param.Add("uploadType", vo.uploadType);
            string url = ReplayHttpUtils.BaseUrl + "/ai/diagnosis/getDiagnosisDownloadUrl";
            string msg = "当前网络情况不佳，请稍后重试或联系管理员";
            var r = HttpUtils.SendServerGetR(url, param);
            if (r.success())
            {
                if (!string.IsNullOrEmpty(r.data))
                {
                    return r.data;
                }
            }
            else
            {
                msg = r.msg;
            }
            throw new CustomException(msg);
        }

        /// <summary>
        /// 更新视频是否上传诊断报告字段
        /// </summary>
        /// <param name="videoId"></param>
        /// <param name="hasDiagnosisReport"></param>
        /// <returns></returns>
        public static bool updateHasDiagnosisReport(string videoId, int hasDiagnosisReport, string finalFileName)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            param.Add("hasDiagnosisReport", hasDiagnosisReport);
            param.Add("diagnosisOssName", finalFileName);

            string dataStr = HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/words/anchorVideoDetail/updateHasDiagnosisReport", param);
            if (!string.IsNullOrEmpty(dataStr))
            {
                if (bool.TryParse(dataStr, out bool flag))
                {
                    return flag;
                }
            }

            return false;
        }
        
        /// <summary>
        /// 更新视频是否上传数据诊断报告字段
        /// </summary>
        /// <param name="videoId"></param>
        /// <param name="hasDataDiagnosisReport"></param>
        /// <returns></returns>
        public static bool updateHasDataDiagnosisReport(string videoId, int hasDataDiagnosisReport, string finalFileName)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            param.Add("hasDataDiagnosisReport", hasDataDiagnosisReport);
            param.Add("dataDiagnosisOssName", finalFileName);

            string dataStr = HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/words/anchorVideoDetail/updateHasDiagnosisReport", param);
            if (!string.IsNullOrEmpty(dataStr))
            {
                if (bool.TryParse(dataStr, out bool flag))
                {
                    return flag;
                }
            }

            return false;
        }
    }
}
