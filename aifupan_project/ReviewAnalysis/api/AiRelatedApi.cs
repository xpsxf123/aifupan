using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Acornima;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Ai.Model;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll;
using ReviewAnalysis.bo.ai;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using ReviewAnalysis.vo.ai;
using static ReviewAnalysis.Utils.HttpUtils;

namespace ReviewAnalysis.api
{
    public class AiRelatedApi
    {

        /// <summary>
        /// 获取contextId
        /// </summary>
        /// <param name="dto"></param>
        /// <returns></returns>
        public static string getContextId(AskRequestDto dto)
        {
            return HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/aiRelated/getContextId", dto);
        }

        /// <summary>
        /// 获取contextId
        /// </summary>
        /// <param name="dto"></param>
        /// <returns></returns>
        public static void updateContextId(AskRequestDto dto)
        {
            HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/aiRelated/updateContextId", dto);
        }

        /// <summary>
        /// 查询视频的弹幕
        /// </summary>
        /// <param name="videoId"></param>
        /// <param name="startTime"></param>
        /// <param name="endTime"></param>
        /// <returns></returns>
        public static List<DanMuVo> danMuCacheList(Dictionary<string, object> param)
        {
            String str = HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/aiRelated/danMuCacheList", param);
            if (!string.IsNullOrEmpty(str))
            {
                return JsonConvert.DeserializeObject<List<DanMuVo>>(str);
            }
            else
            {
                return new List<DanMuVo>();
            }
        }

        /// <summary>
        /// 查询视频的看板数据
        /// </summary>
        /// <param name="videoId"></param>
        /// <param name="startTime"></param>
        /// <param name="endTime"></param>
        /// <returns></returns>
        public static VideoDataViewingConfuseVo videoDataViewingConfuseByVideoId(string videoId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            String str = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/aiRelated/videoDataViewingConfuseByVideoId", param);
            if (!string.IsNullOrEmpty(str))
            {
                return JsonConvert.DeserializeObject<VideoDataViewingConfuseVo>(str);
            }
            else
            {
                return null;
            }
        }

        /// <summary>
        /// 查询视频的看板数据-字符串
        /// </summary>
        /// <param name="videoId"></param>
        /// <returns></returns>
        public static String getVideoDataViewingStr(string videoId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            return HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/aiRelated/getVideoDataViewingStr", param);
        }

        /// <summary>
        /// 问答中全局额外要求
        /// </summary>
        /// <returns></returns>
        public static List<String> askRequireAdditional()
        {
            String result = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/aiRelated/askRequireAdditional", new Dictionary<string, object>());
            if (!string.IsNullOrEmpty(result))
            {
                return JsonConvert.DeserializeObject<List<string>>(result);
            }
            return new List<string>();
        }

        public static ConversationPage conversationPage(ConversationPageBo bo)
        {
            String result = HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/ai/conversation/conversationPage", bo);
            if (!string.IsNullOrEmpty(result))
            {
                return JsonConvert.DeserializeObject<ConversationPage>(result);
            }

            ConversationPage conversation = new ConversationPage();
            conversation.existPreviousPage = false;
            conversation.list = new List<ConversationVo>();
            return conversation;
        }

        /// <summary>
        /// 查询服务器是否有问答记录-默认有
        /// </summary>
        /// <param name="conversationVo"></param>
        /// <returns></returns>
        public static bool isExistConversation(ConversationVo conversationVo)
        {
            bool flag = true;
            String result = HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/ai/conversation/isExist", conversationVo);

            if (!string.IsNullOrEmpty(result))
            {
                bool.TryParse(result, out flag);
            }
            return flag;
        }

        public static AiTempTokenDto getAnalysisTempToken(int? aiModel, string code = null)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("aiModel", aiModel);
            param.Add("code", code);
            string result = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/third/aiModel/getAnalysisTempToken", param);
            if (!string.IsNullOrEmpty(result))
            {
                return JsonConvert.DeserializeObject<AiTempTokenDto>(result);
            }
            return null;
        }

        /// <summary>
        /// 获取登录用户主播列表的全部监控位
        /// </summary>
        /// <param name="sourceType"></param>
        /// <param name="sourceId"></param>
        /// <returns></returns>
        public static string getAiAnchorPrompt(int sourceType, string sourceId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("sourceType", sourceType);
            param.Add("sourceId", sourceId);
            R r = HttpUtils.SendServerGetR(ReplayHttpUtils.BaseUrl + "/words/aiWords/getAiAnchorPrompt", param);
            if (r != null && (r?.code ?? -1) == 0)
            {
                return r?.data ?? "";
            }
            else
            {
                FileUtils.LogError($"msg = {r?.msg ?? ""}", "getAiAnchorPrompt接口报错");
            }
            return "";
        }

        /// <summary>
        /// 获取登录用户主播列表的全部监控位
        /// </summary>
        /// <param name="sourceType"></param>
        /// <param name="sourceId"></param>
        /// <returns></returns>
        public static string getAiAnchorPrompt(AskRequestDto dto)
        {
            AskRequestDto newData = JsonConvert.DeserializeObject<AskRequestDto>(JsonConvert.SerializeObject(dto));
            newData.realContent = null;
            newData.content = null;
            R r = HttpUtils.SendServerPostR(ReplayHttpUtils.BaseUrl + "/words/aiWords/getAiAnchorPrompt2", newData);
            if (r != null && (r?.code ?? -1) == 0)
            {
                return r?.data ?? "";
            }
            else
            {
                FileUtils.LogError($"msg = {r?.msg ?? ""}", "getAiAnchorPrompt接口报错");
            }
            return "";
        }

        /// <summary>
        /// 获取推荐的行业提示词
        /// </summary>
        /// <param name="sourceType"></param>
        /// <param name="sourceId"></param>
        /// <returns></returns>
        public static AiAnalyzeTradePromptVo aiAnalyzeTradePrompt(int sourceType, string sourceId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("sourceType", sourceType);
            param.Add("sourceId", sourceId);
            R result = HttpUtils.SendServerGetR(ReplayHttpUtils.BaseUrl + "/words/aiWords/aiAnalyzeTradePrompt", param);
            if (result?.code == 0 && !string.IsNullOrEmpty(result.data))
            {
                return JsonConvert.DeserializeObject<AiAnalyzeTradePromptVo>(result.data);
            }
            else
            {
                throw new CustomException(result.msg);
            }
        }


        /// <summary>
        /// 获取提取文案后优化的提示词
        /// </summary>
        /// <returns></returns>
        public static ExtractPromptVo getExtractPrompt()
        {
            R result = HttpUtils.SendServerGetR(ReplayHttpUtils.BaseUrl + "/video/extract/getExtractPrompt", null);
            if (result?.code == 0 && !string.IsNullOrEmpty(result.data))
            {
                return JsonConvert.DeserializeObject<ExtractPromptVo>(result.data);
            }
            else
            {
                throw new CustomException(result.msg);
            }
        }

        /// <summary>
        /// 修改推荐行业的id
        /// </summary>
        /// <param name="sourceType"></param>
        /// <param name="sourceId"></param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public static bool updateSuggestTradeId(int sourceType, string sourceId, long tradeId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("sourceType", sourceType);
            param.Add("sourceId", sourceId);
            param.Add("tradeId", tradeId);
            R result = HttpUtils.SendServerGetR(ReplayHttpUtils.BaseUrl + "/words/aiWords/updateSuggestTradeId", param);
            if (result?.code == 0 && !string.IsNullOrEmpty(result.data))
            {
                if (bool.TryParse(result.data, out bool res))
                {
                    return res;
                }
                return res;
            }
            else
            {
                throw new CustomException(result.msg);
            }
        }

        /// <summary>
        /// 修改推荐的行业为已推荐
        /// </summary>
        /// <param name="sourceType"></param>
        /// <param name="sourceId"></param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public static bool updateSuggestTrade(int sourceType, string sourceId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("sourceType", sourceType);
            param.Add("sourceId", sourceId);
            R result = HttpUtils.SendServerGetR(ReplayHttpUtils.BaseUrl + "/words/aiWords/updateSuggestTrade", param);
            if (result?.code == 0 && !string.IsNullOrEmpty(result.data))
            {
                if (bool.TryParse(result.data, out bool res))
                {
                    return res;
                }
                return res;
            }
            else
            {
                throw new CustomException(result.msg);
            }
        }

        /// <summary>
        /// 获取提示词
        /// </summary>
        /// <param name="cueWordsId">提示词id</param>
        /// <param name="cueWordsType">提示词类型 0系统提示词，1用户提示词</param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public static (string, string, int?) getPromptWord(string cueWordsId, int cueWordsType, string lastConversationId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("cueWordsId", cueWordsId);
            param.Add("cueWordsType", cueWordsType);
            param.Add("lastConversationId", lastConversationId);
            R result = HttpUtils.SendServerGetR(ReplayHttpUtils.BaseUrl + "/ai/aiRelated/getAiPromptWord2", param);
            if (result?.code == 0)
            {
                if (result.data == null ||  string.IsNullOrEmpty(result.data))
                {
                    return (null, null, 0);
                }
                // 转成json
                var json = JsonConvert.DeserializeObject<dynamic>(result.data);
                return (AESHelper.Decrypt(json?.prompt?.ToString() ?? ""), json?.reason?.ToString()??null, json?.optimizeActions ?? 0);
            }
            else
            {
                throw new CustomException(result.msg);
            }
        }

        /// <summary>
        /// 获取html生成前的参数
        /// </summary>
        /// <param name="id">id</param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public static ConversationVo htmlProGenerateParams(string id)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("id", id);
            R result = HttpUtils.SendServerGetR(ReplayHttpUtils.BaseUrl + "/ai/conversation/htmlProGenerateParams", param);
            if (result?.code == 0 && !string.IsNullOrEmpty(result.data))
            {
                return JsonConvert.DeserializeObject<ConversationVo>(result.data);
            }
            else
            {
                throw new CustomException(result.msg);
            }
        }

        /// <summary>
        /// 获取html的模型配置
        /// </summary>
        /// <returns></returns>
        public static AiTempTokenDto getHtmlTempToken()
        {
            string result = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/ai/conversation/getHtmlTempToken", null);
            if (!string.IsNullOrEmpty(result))
            {
                return JsonConvert.DeserializeObject<AiTempTokenDto>(result);
            }
            return null;
        }

        /// <summary>
        /// 获取html上传的签名url
        /// </summary>
        /// <returns></returns>
        public static SignUploadUrlVo getHtmlSignUploadUrl()
        {
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/ai/conversation/getHtmlSignUploadUrl", null);
            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<SignUploadUrlVo>(dataStr);
            }

            return null;
        }

        /// <summary>
        /// 更新html状态
        /// </summary>
        public static void updateHtmlStatus(string id, int status, string htmlSavePath, string error)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("id", id);
            param.Add("htmlStatus", status);
            param.Add("htmlSavePath", htmlSavePath);
            param.Add("htmlCreateError", error);
            HttpUtils.SendServerPostR(ReplayHttpUtils.BaseUrl + "/ai/conversation/updateHtmlStatus", param);
        }

        /// <summary>
        /// 获取html状态
        /// </summary>
        public static List<ConversationVo> getHtmlStatus(List<string> ids)
        {
            
            var result = HttpUtils.SendServerPostR(ReplayHttpUtils.BaseUrl + "/ai/conversation/getHtmlStatus", ids);
            
            if (result?.code == 0 && !string.IsNullOrEmpty(result.data))
            {
                return JsonConvert.DeserializeObject<List<ConversationVo>>(result.data);
            }
            else
            {
                throw new CustomException(result?.msg ?? "");
            }
        }

        /// <summary>
        /// 构建额外的上一次对话的输出字符串
        /// </summary>
        /// <param name="dto">参数</param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public static string extraBuildLastOutString(AskRequestDto dto)
        {
            AskRequestDto newData = JsonConvert.DeserializeObject<AskRequestDto>(JsonConvert.SerializeObject(dto));
            newData.realContent = null;
            newData.content = null;
            var result = HttpUtils.SendServerPostR(ReplayHttpUtils.BaseUrl + "/aiRelated/extraBuildLastOutString", newData);
            
            if (result.success())
            {
                return result.data;
            }
            else
            {
                throw new CustomException(result?.msg ?? "");
            }
        }

        // ==================== AI纠正内容相关接口 ====================

        /// <summary>
        /// 获取AI纠正前的参数
        /// </summary>
        /// <param name="id">id</param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public static ConversationVo correctProGenerateParams(string id)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("id", id);
            R result = HttpUtils.SendServerGetR(ReplayHttpUtils.BaseUrl + "/ai/conversation/correctProGenerateParams", param);
            if (result?.code == 0 && !string.IsNullOrEmpty(result.data))
            {
                return JsonConvert.DeserializeObject<ConversationVo>(result.data);
            }
            else
            {
                throw new CustomException(result.msg);
            }
        }

        /// <summary>
        /// 更新AI纠正状态
        /// </summary>
        /// <param name="id">数据 id</param>
        /// <param name="status">ai纠错状态 0：正常，1：纠错中，2：纠错完成，3：纠错失败</param>
        /// <param name="content">纠正后的内容</param>
        /// <param name="error">错误原因</param>
        public static void updateCorrectStatus(string id, int status, string content, string error)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("id", id);
            param.Add("aiCorrectStatus", status);
            param.Add("content", content);
            param.Add("aiCorrectError", error);
            HttpUtils.SendServerPostR(ReplayHttpUtils.BaseUrl + "/ai/conversation/updateCorrectStatus", param);
        }

        /// <summary>
        /// 获取AI纠正状态
        /// </summary>
        public static List<ConversationVo> getCorrectStatus(List<string> ids)
        {
            var result = HttpUtils.SendServerPostR(ReplayHttpUtils.BaseUrl + "/ai/conversation/getCorrectStatus", ids);
            
            if (result?.code == 0 && !string.IsNullOrEmpty(result.data))
            {
                return JsonConvert.DeserializeObject<List<ConversationVo>>(result.data);
            }
            else
            {
                throw new CustomException(result?.msg ?? "");
            }
        }

        /// <summary>
        /// 调用服务端组装AI提示词
        /// </summary>
        public static PromptAssemblyResultDto assemblePrompt(AskRequestDto dto)
        {
            var result = HttpUtils.SendServerPostR(ReplayHttpUtils.BaseUrl + "/aiRelated/assemblePrompt", dto);
            if (result?.code == 0 && !string.IsNullOrEmpty(result.data))
            {
                return JsonConvert.DeserializeObject<PromptAssemblyResultDto>(result.data);
            }
            return null;
        }

        /// <summary>
        /// 新增历史段落（服务端存储）
        /// </summary>
        public static HistoryParagraphVo addHistoryParagraph(HistoryParagraphBo bo)
        {
            R r = HttpUtils.SendServerPostR(ReplayHttpUtils.BaseUrl + "/aiRelated/addHistoryParagraph", bo);
            if (r.success() && !string.IsNullOrEmpty(r.data))
            {
                return JsonConvert.DeserializeObject<HistoryParagraphVo>(r.data);
            }
            throw new CustomException(r?.msg ?? "保存历史段落失败");
        }

        /// <summary>
        /// 历史段落列表（服务端查询）
        /// </summary>
        public static List<HistoryParagraphVo> historyParagraphList(string sourceId, int sourceType, int type)
        {
            var param = new Dictionary<string, object>();
            param.Add("sourceId", sourceId);
            param.Add("sourceType", sourceType);
            param.Add("type", type);
            R r = HttpUtils.SendServerPostR(ReplayHttpUtils.BaseUrl + "/aiRelated/historyParagraphList", param);
            if (r.success() && !string.IsNullOrEmpty(r.data))
            {
                return JsonConvert.DeserializeObject<List<HistoryParagraphVo>>(r.data);
            }
            return new List<HistoryParagraphVo>();
        }

        /// <summary>
        /// 删除历史段落（服务端删除）
        /// </summary>
        public static void deleteHistoryParagraph(HistoryParagraphVo vo)
        {
            var param = new Dictionary<string, object>();
            param.Add("type", vo.type);
            param.Add("sourceId", vo.sourceId);
            param.Add("sourceType", vo.sourceType);
            param.Add("code", vo.code);
            HttpUtils.SendServerPostR(ReplayHttpUtils.BaseUrl + "/aiRelated/deleteHistoryParagraph", param);
        }

        /// <summary>
        /// 按code获取历史段落内容（服务端查询）
        /// </summary>
        public static string getHistoryContentByCode(int type, string sourceId, int sourceType, string code)
        {
            var param = new Dictionary<string, object>();
            param.Add("type", type);
            param.Add("sourceId", sourceId);
            param.Add("sourceType", sourceType);
            param.Add("code", code);
            R r = HttpUtils.SendServerPostR(ReplayHttpUtils.BaseUrl + "/aiRelated/getHistoryContentByCode", param);
            if (r.success() && !string.IsNullOrEmpty(r.data))
            {
                var vo = JsonConvert.DeserializeObject<HistoryParagraphVo>(r.data);
                return vo?.content ?? "";
            }
            return "";
        }
    }
}
