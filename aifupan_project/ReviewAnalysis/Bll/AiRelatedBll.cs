using System;
using System.Collections.Generic;
using System.Data.Entity.Core.Common.CommandTrees.ExpressionBuilder;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Runtime.Remoting.Contexts;
using System.Runtime.Remoting.Messaging;
using System.Security.AccessControl;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using System.Web;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Ai;
using ReviewAnalysis.Ai.Model;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.bo;
using ReviewAnalysis.bo.ai;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Dto;
using ReviewAnalysis.enums;
using ReviewAnalysis.Global;
using ReviewAnalysis.Model;
using ReviewAnalysis.upload;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.ai;
using ReviewAnalysis.vo.trade;
using ReviewAnalysis.vo.user;
using ReviewAnalysis.Websocket;
using Swan.Parsers;

namespace ReviewAnalysis.Bll
{
    public class AiRelatedBll
    {

        /// <summary>
        /// 问答接口-流式
        /// </summary>
        /// <param name="dto"></param>
        /// <param name="request"></param>
        /// <param name="response"></param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public async Task AskStream(AskRequestDto dto, HttpListenerRequest request, HttpListenerResponse response)
        {
            //SentenceMark sentence = AiFactory.GetSentenceMark(dto.sourceType, dto.sourceId);
            // 获取全部的段落内容
            //string allContent = sentence.GetAllContent();

            //FileUtils.log(allContent.Length.ToString(), "当前全文长度");

            // 获取模型类型
            //int aiModel = ReplayHttpUtils.GetAiModel(allContent.Length);

            AiTempTokenDto token = AiRelatedApi.getAnalysisTempToken(dto.aiModel);

            if (token == null)
            {
                throw new CustomException("获取临时token失败", 7006);
            }

            dto.resourceType = token.resourceType;
            dto.useModelType = token.useModelWay;
            dto.token = token;

            Ask ask = AiFactory.GetAsk(dto.resourceType);
            await ask.AskStream(dto, request, response);
        }

        public AskResponseDto Ask(AskRequestDto dto)
        {
            //string redisId = null;
            //AskResponseDto result = new AskResponseDto();
            //// 判断用户的资产是否足够
            //UserPropertyEntity userProperty = UserPropertyHttpUtils.GetUserProperty(ReplayHttpUtils.Token).Result;
            //if (userProperty == null || userProperty.AiTokenNum <= 0)
            //{
            //    throw new CustomException("AI助手分析余量不足，请联系产品顾问进行套餐外购买", 7001);
            //}

            //if (dto.additionalList == null) dto.additionalList = new List<string>();

            //// 添加额外要求
            //dto.additionalList.Add("输出内容最多在2万字以内");

            //// 获取临时token
            //AiTempTokenDto token = AiRelatedCacheManager.GetTempToken();

            //// 获取上下文id
            //string contextId = AiUtils.getContextId(dto.sourceId, dto.sourceType, dto.type);
            //string askQuestion, allContent;
            //string tradeId = null;
            //// 获取全文和提问的字符串
            //if (dto.sourceType == 0)
            //{
            //    SentenceMarkDto sentenceMarkDto = lockAnalysisVideo(dto.sourceId);
            //    tradeId = sentenceMarkDto.anchorInfo.TradeId;
            //    askQuestion = getAskQuestion(dto, tradeId);
            //    allContent = getAllVideoContent(sentenceMarkDto);
            //}
            //else
            //{
            //    throw new CustomException("当前类型暂不支持", 7002);
            //}

            //// 设置问题多少个字符截断
            //if(askQuestion.Length > token.maxSendMessageLength) askQuestion = askQuestion.Substring(0, token.maxSendMessageLength);

            //// 预扣AI的token数量
            //string allContext = dto.identity ?? "";
            //if (string.IsNullOrEmpty(contextId))
            //{
            //    allContext += askQuestion + allContent;
            //}
            //else
            //{
            //    allContext += askQuestion;
            //}
            //// 预扣
            //int thisUseNum = allContext.Length * 2;
            //CompletionsDto completions = null;
            //try
            //{
            //    redisId = ReplayHttpUtils.isPropertyHaveAiToken(thisUseNum);
            //    // 如果上下文id为空，则创建会话
            //    if (contextId == null)
            //    {
            //        contextId = AiUtils.CreateContext(token, allContent);
            //    }
            //    ReplayHttpUtils.updateCosThumbsFile(dto.type, tradeId, ReplayHttpUtils.UserId, dto.sourceId, dto.sourceType, contextId, null);

            //    // 提问
            //    completions = AiUtils.chatCompletions(token, dto, contextId, askQuestion);
            //}
            //catch(Exception ex)
            //{
            //    FileUtils.LogError($"错误：{ex.Message},堆栈：{ex.StackTrace}", "调用ai问答API报错");
            //    if (!string.IsNullOrEmpty(redisId))
            //    {
            //        ReplayHttpUtils.removeTempUserProperty(redisId);
            //        redisId = null;
            //    }
            //}


            //// 实际扣减资产
            //if(!string.IsNullOrEmpty(redisId))UserPropertyHttpUtils.UpdateUserProperty("aiTokenNum", thisUseNum, redisId);

            //UserPropertyEntity userProperty2 = UserPropertyHttpUtils.GetUserProperty(ReplayHttpUtils.Token).Result;


            //// 添加ai会话缓存
            //AIRelatedCacheDto relatedCacheDto = new AIRelatedCacheDto();
            //relatedCacheDto.type = dto.type;
            //relatedCacheDto.sourceId = dto.sourceId;
            //relatedCacheDto.sourceType = dto.sourceType;
            //relatedCacheDto.contextId = contextId;
            //relatedCacheDto.contextId = contextId;
            //relatedCacheDto.giveStatuc = -1;
            //AiRelatedCacheManager.SetAiContextCache(relatedCacheDto);

            //string qaCode = Guid.NewGuid().ToString("N");
            //// 封装资产
            //result.property = new PropertyDto()
            //{
            //    currerntUseNum = thisUseNum,
            //    surplusNum = (int)userProperty2.AiTokenNum
            //};
            //string now = ServerTimeUtils.getCurrentTimeStr();
            //// 问的参数封装
            //result.problem = new ConversationDto()
            //{
            //    code = Guid.NewGuid().ToString("N"),
            //    qaCode = qaCode,
            //    contextId = contextId,
            //    type = "A",
            //    content = dto.content,
            //    realContent = dto.realContent,
            //    createDate = now,
            //};

            //// 回答的参数封装
            //result.answer = new ConversationDto()
            //{
            //    code = Guid.NewGuid().ToString("N"),
            //    completionId = completions?.id ?? null,
            //    qaCode = qaCode,
            //    contextId = contextId,
            //    type = "Q",
            //    content = completions?.choices?.message ?? AbstractAsk.errorReturnData,
            //    realContent = "",
            //    createDate = now,
            //};

            //// 保存会话文件
            //AiUtils.svaeConversationData(dto.type, dto.sourceId, dto.sourceType, contextId, new List<ConversationDto>() { result.problem, result.answer });

            //result.problem.code = AiUtils.GetStruacturePrefix(result.problem.contextId, result.problem.code);
            //result.answer.code = AiUtils.GetStruacturePrefix(result.answer.contextId, result.answer.code);

            //return result;
            return null;
        }

        public string getAskQuestion(AskRequestDto dto, string tradeId)
        {
            string result = dto.realContent;
            if(string.IsNullOrEmpty(result)) throw new CustomException("问题不能为空", 7002);
            // 处理占位符
            // #{行业}、#{违规原因}
            // 判断是否有行业，有要查询行业
            string replace = "#{trade}";
            if (result.IndexOf(replace) != -1)
            {
                TradeVo trade = ReplayHttpUtils.GetTrade(tradeId);
                result = result.Replace(replace, trade?.name ?? "某行业");
            }
            replace = "#{reason}";
            if (result.IndexOf(replace) != -1)
            {
                if (string.IsNullOrEmpty(dto.reasonViolation))
                {
                    throw new CustomException("违规原因不能为空", 7002);
                }
                result = result.Replace(replace, dto.reasonViolation);
            }
            // 额外要求
            if (dto?.additionalList?.Count > 0)
            {
                result += "\n额外要求:";
                for (int i = 0; i < dto.additionalList.Count; i++)
                {
                    result += $"{i + 1}、{dto.additionalList[i]}";
                }
            }

            // 额外内容-文本内的参数说明
            if (dto?.paramsDescribe?.Length > 0)
            {
                result += "\n文本内的参数说明:";
                result += "\n";
            }

            // 判断是否传输文章
            string str = "";
            if (!string.IsNullOrEmpty(dto.paragraphCode) && string.IsNullOrEmpty(dto.paragraphContent))
            {
                if ("0".Equals(dto.paragraphCode))
                {
                    str = "\n以上问题针对#{platform}直播脚本为全文。";
                }
                else
                {
                    string content = AiUtils.GetHistoryContentByCode(dto.type, dto.sourceId, dto.sourceType, dto.paragraphCode);
                    HistoryParagraphVo history = JsonConvert.DeserializeObject<HistoryParagraphVo>(content);
                    str = $"\n以上问题针对的内容为：";
                    if (string.IsNullOrEmpty(content)) throw new CustomException("获取历史段落失败", 7005);
                    if (string.IsNullOrEmpty(history.content)) throw new CustomException("获取历史段落内容失败", 7005);
                    str += history.content;
                }
            }
            else
            {
                if (!string.IsNullOrEmpty(dto.paragraphContent))
                {
                    str = "\n以上问题针对的内容为：";
                    str += dto.paragraphContent;
                }
                else
                {
                    str = "\n#{platform}直播脚本为全文。";
                }
            }
            return result + str;
        }

        public SentenceMarkDto lockAnalysisVideo(string videoId)
        {
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            SentenceMarkDto result = anchorVideoBll.LockAnalysis(videoId);
            if (result?.audioaAlyses == null) throw new CustomException("查询全文失败");
            return result;
        }


        public SentenceMarkDto lockAnalysisVideoFile(string fileId)
        {
            UploadFileBll uploadFileBll = new UploadFileBll();
            SentenceMarkDto result = uploadFileBll.LockAnalysis(fileId);
            if (result?.fileAudioaAlyses == null) throw new CustomException("查询全文失败");
            return result;
        }

        /// <summary>
        /// 获取文件的全文内容
        /// </summary>
        /// <param name="result"></param>
        /// <returns></returns>
        public string getAllFileContent(SentenceMarkDto result, AskRequestDto dto = null)
        {
            List<dynamic> list = new List<dynamic>();
            int onlineNumIndex = 0;
            string startStrTime = "", endStrTime = "";
            for (int i = 0; i < result.fileAudioaAlyses.Count; i++)
            {
                DateTime? startTime = null;
                DateTime? endTime = null;
                string renShu = null, textStart = null, textEnd = null, content = null;
                UploadFileAlysis item = result.fileAudioaAlyses[i];
                if (!string.IsNullOrEmpty(item.DataJson))
                {
                    var res = JsonConvert.DeserializeObject<dynamic>(item.DataJson);
                    if (res.items != null && res.items.Count > 0)
                    {
                        content = res.content ?? "";
                        long startTempTime = res.items[0]?.startTime ?? 0;
                        long endTempTime = res.items[res.items.Count - 1]?.endTime ?? 0;
                        if (i < result.audioaAlyses.Count - 1)
                        {
                            AudioaAlysis item2 = result.audioaAlyses[i + 1];
                            if (!string.IsNullOrEmpty(item2.DataJson))
                            {
                                var res2 = JsonConvert.DeserializeObject<dynamic>(item2.DataJson);
                                if (res2.items != null && res2.items.Count > 0)
                                {
                                    endTempTime = res2.items[0]?.startTime ?? 0;
                                }
                            }
                        }
                        textStart = DateUtils.DateTimeToString(DateUtils.TimestampToDateTime(startTempTime, false), "HH:mm:ss");
                        textEnd = DateUtils.DateTimeToString(DateUtils.TimestampToDateTime(endTempTime, false), "HH:mm:ss");
                    }
                }
                // 获取在线人数
                if (result.onlineNumList != null && result.onlineNumList.Count > 0)
                {
                    for (int j = onlineNumIndex; j < result.onlineNumList.Count; j++)
                    {
                        OnlineNum onlineNum = result.onlineNumList[j];
                        DateTime dateTime = DateUtils.StringToDateTime(onlineNum.RecordDate);
                        if (dateTime < startTime) continue;
                        if (dateTime > endTime) break;
                        if (dateTime >= startTime && dateTime <= endTime)
                        {
                            renShu = onlineNum.PeopleNum;
                        }
                        onlineNumIndex = j;
                    }
                }
                if (i == 0) startStrTime = textStart;
                endStrTime = textEnd;
                list.Add(new
                {
                    startTime,
                    endTime,
                    textStart,
                    textEnd,
                    renShu,
                    content
                });
            }
            return getAllContent(list, null, startStrTime, endStrTime, result.uploadFile.FileDuration.ToString(), false);
        }

        /// <summary>
        /// 获取视频的全文内容
        /// </summary>
        /// <param name="result"></param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public string getAllVideoContent(SentenceMarkDto result, AskRequestDto dto = null)
        {
            string startTimeStr = result?.videoInfo?.StartTime ?? "";
            if (string.IsNullOrEmpty(startTimeStr))
            {
                FileUtils.LogError(JsonConvert.SerializeObject(result),"视频的开始时间获取失败");
                throw new CustomException("视频的开始时间获取失败", 7005);
            }
            long allStartTime = DateUtils.StringToTimestamp(startTimeStr);
            List<dynamic> list = new List<dynamic>();
            int onlineNumIndex = 0;
            string startStrTime = "", endStrTime = "";
            for (int i = 0; i < result.audioaAlyses.Count; i++)
            {
                DateTime? startTime = null;
                DateTime? endTime = null;
                string renShu = null, textStart = null, textEnd = null, content = null;
                AudioaAlysis item = result.audioaAlyses[i];
                if (!string.IsNullOrEmpty(item.DataJson))
                {
                    var res = JsonConvert.DeserializeObject<dynamic>(item.DataJson);
                    if(res.items != null && res.items.Count > 0)
                    {
                        content = res.content ?? "";
                        long startTempTime = res.items[0]?.startTime ?? 0;
                        long endTempTime = res.items[res.items.Count - 1]?.endTime ?? 0;
                        if (i < result.audioaAlyses.Count - 1)
                        {
                            AudioaAlysis item2 = result.audioaAlyses[i + 1];
                            if (!string.IsNullOrEmpty(item2.DataJson))
                            {
                                var res2 = JsonConvert.DeserializeObject<dynamic>(item2.DataJson);
                                if (res2.items != null && res2.items.Count > 0)
                                {
                                    endTempTime = res2.items[0]?.startTime ?? 0;
                                }
                            }
                        }
                        startTime = DateUtils.TimestampToDateTime(allStartTime + startTempTime);
                        endTime = DateUtils.TimestampToDateTime(allStartTime + endTempTime);
                        textStart = DateUtils.DateTimeToString(DateUtils.TimestampToDateTime(startTempTime, false), "HH:mm:ss");
                        textEnd = DateUtils.DateTimeToString(DateUtils.TimestampToDateTime(endTempTime, false), "HH:mm:ss");
                    }
                }
                if (startTime == null || endTime == null) continue;
                // 获取在线人数
                if(result.onlineNumList != null && result.onlineNumList.Count > 0)
                {
                    for (int j = onlineNumIndex; j < result.onlineNumList.Count; j++)
                    {
                        OnlineNum onlineNum = result.onlineNumList[j];
                        DateTime dateTime = DateUtils.StringToDateTime(onlineNum.RecordDate);
                        if (dateTime < startTime) continue;
                        if (dateTime > endTime) break;
                        if(dateTime >= startTime && dateTime <= endTime)
                        {
                            renShu = onlineNum.PeopleNum;
                        }
                        onlineNumIndex = j;
                    }
                }
                if (i == 0) startStrTime = textStart;
                endStrTime = textEnd;
                list.Add(new
                {
                    startTime,
                    endTime,
                    textStart,
                    textEnd,
                    renShu,
                    content
                });
            }
            return getAllContent(list, result.anchorInfo.AnchorName, startStrTime, endStrTime, result.videoInfo.Duration);
        }

        public static string txtParamsDescribe(string anchorName, string startTime, string endTime, string duration, bool isRenShu = true)
        {
            string str = "";
            str += "以下是由#{platform}账号";
            if (!string.IsNullOrEmpty(anchorName)) str += $"昵称为{anchorName}的";
            str += $"直播间录屏成视频后转译成文字的直播全文脚本，从第一段到最后一段，每一段的内容都是连续的。";
            str += $"\n文本内的参数说明：" +
                   $"\n本段开始时间：指的是转译成的文字段落开始时对应的视频播放时间。" +
                   $"\n本段结束时间：指的是转译成的文字段落结束时对应的视频播放时间。";
            if (isRenShu) str += $"\n本段在线人数：指的是转译成的文字段落开始时对应的直播间在线人数。";
            //if (isRenShu) str += $"\n本段自然开始时间：指的是转译成的文字段落开始时对应的北京时间。";
            str += $"\n从第二段开始直到最后一段，每一段的本段自然时间是上一段的自然结束时间。" +
                $"\n本场直播的视频时间是{startTime}到{endTime}，共{duration}秒的直播 \n\n";
            return str;
        }

        /// <summary>
        /// 拼接全文
        /// </summary>
        /// <param name="list"></param>
        /// <param name="anchorName"></param>
        /// <param name="startTime"></param>
        /// <param name="endTime"></param>
        /// <param name="duration"></param>
        /// <returns></returns>
        public static string getAllContent(List<dynamic> list, string anchorName, string startTime, string endTime, string duration, bool isRenShu = true)
        {
            if (list.Count == 0) return "";
            string str = "";
            str += "以下是由#{platform}账号";
                if(!string.IsNullOrEmpty(anchorName)) str += $"昵称为{anchorName}的";
            str += $"直播间录屏成视频后转译成文字的直播全文脚本，从第一段到最后一段，每一段的内容都是连续的。";
            str += $"\n文本内的参数说明：" +
                   $"\n本段开始时间：指的是转译成的文字段落开始时对应的视频播放时间。" +
                   $"\n本段结束时间：指的是转译成的文字段落结束时对应的视频播放时间。";
            if (isRenShu) str += $"\n本段在线人数：指的是转译成的文字段落开始时对应的直播间在线人数。";
            //if (isRenShu) str += $"\n本段自然开始时间：指的是转译成的文字段落开始时对应的北京时间。";
            str += $"\n从第二段开始直到最后一段，每一段的本段自然时间是上一段的自然结束时间。" +
                $"\n本场直播的视频时间是{startTime}到{endTime}，共{duration}秒的直播 \n\n";
            for (int i = 0; i < list.Count; i++)
            {
                var item = list[i];
                string renShu = i == 0 ? item.renShu ?? "0" : item.renShu;
                if (renShu == null) renShu = list[i - 1]?.renShu;
                str += i == 0 ? "" : "\n\n";
                str += $"本段开始时间：{item.textStart}" +
                    $"\n本段结束时间：{item.textEnd}";
                if(isRenShu) str += $"\n本段在线人数：{renShu}";
                //if (item.startTime != null) str += $"\n本段自然时间: {DateUtils.DateTimeToString(item.startTime, "HH:mm:ss")}";
                int oldRenShu = i == 0 ? 0 : int.TryParse(list[i - 1].renShu, out int tempOldVlaue) ? tempOldVlaue : 0;
                int currentValue = int.TryParse(renShu, out int tempRenShu) ? tempRenShu : 0;
                string strName = currentValue >= oldRenShu ? "增加" : "减少";
                int currentInt = i == 0 ? 0 : Math.Abs(currentValue - oldRenShu);
                if (isRenShu) str += $"\n本段在线人数{strName}：{currentInt}人";
                str += $"\n本段内容：{item.content}";
            }
            return str;
        }

        /// <summary>
        /// 点赞
        /// </summary>
        /// <param name="likeBo"></param>
        public void likes(StructureUpdateBo likeBo)
        {
            if (!AiRelatedCacheManager.HasLikes())
            {

                AiUtils.UpdateContextGiveStatuc(likeBo.type, likeBo.sourceId, likeBo.sourceType, likeBo.code, likeBo.giveStatuc);
                AiRelatedCacheManager.setLikesKey();

                string[] split = likeBo.code.Split(new string[] { "_" }, StringSplitOptions.None);
                string contextId = split[1];
                string tempCode = split[2];
                AIRelatedCacheDto cacheDto = new AIRelatedCacheDto();
                cacheDto.type = likeBo.type;
                cacheDto.sourceId = likeBo.sourceId;
                cacheDto.sourceType = likeBo.sourceType;
                cacheDto.contextId = contextId;
                cacheDto.giveStatuc = likeBo.giveStatuc;
                AiRelatedCacheManager.SetAiContextCache(cacheDto, false);
            }
            else
            {
                throw new CustomException("点赞过为频繁", 7005);
            }
        }

        /// <summary>
        /// 新增历史段落
        /// </summary>
        /// <param name="history"></param>
        /// <returns></returns>
        /// <exception cref="NotImplementedException"></exception>
        public HistoryParagraphVo addHistoryParagraph(HistoryParagraphBo history)
        {
            if (string.IsNullOrEmpty(history.content)) throw new CustomException("段落内容不能为空", 7005);
            return AiRelatedApi.addHistoryParagraph(history);
        }

        /// <summary>
        /// 历史段落列表
        /// </summary>
        /// <param name="sourceId"></param>
        /// <param name="sourceType"></param>
        /// <param name="type"></param>
        /// <returns></returns>
        /// <exception cref="NotImplementedException"></exception>
        public List<HistoryParagraphVo> historyParagraphList(string sourceId, int sourceType, int type)
        {
            return AiRelatedApi.historyParagraphList(sourceId, sourceType, type);
        }

        /// <summary>
        /// 删除历史段落
        /// </summary>
        /// <param name="vo"></param>
        /// <exception cref="NotImplementedException"></exception>
        public void deleteHistoryParagraph(HistoryParagraphVo vo)
        {
            AiRelatedApi.deleteHistoryParagraph(vo);
        }

        /// <summary>
        /// 添加ai数据结构
        /// </summary>
        /// <param name="bo"></param>
        /// <exception cref="NotImplementedException"></exception>
        public void addStructure(StructureReqBo bo)
        {
            AiUtils.saveStructure(bo.type, bo.sourceId, bo.sourceType, bo.contentList.Select(v => $"{v.code}>>>{v.content}").ToList());
        }

        /// <summary>
        /// ai数据结构分页查询
        /// </summary>
        /// <param name="type"></param>
        /// <param name="sourceId"></param>
        /// <param name="sourceType"></param>
        /// <param name="pageIndex"></param>
        /// <param name="pageSize"></param>
        /// <returns></returns>
        public StructurePageVo structurePage(int type, string sourceId, int sourceType, int? pageIndex, int? pageSize)
        {
            StructurePageVo result = new StructurePageVo();
            string path = AiUtils.GetStructureFilePath(type, sourceId, sourceType);

            PagedResult page = AiUtils.ReadFileReversePaged(path, (int)pageIndex, (int)pageSize);

            result.existPreviousPage = page.HasPreviousPage;
            if (result.list == null) result.list = new List<dynamic>();

            List<String> codeList = new List<string>();

            if(page.Lines != null && page.Lines.Count > 0)
            {
                page.Lines.ForEach(line =>
                {
                    if(!string.IsNullOrEmpty(line))
                    {
                        string[] split = line.Split(new string[] { ">>>" }, StringSplitOptions.None);
                        if(split.Length > 1)
                        {
                            string tempCode = split[0];
                            if (tempCode.StartsWith("ai_"))
                            {
                                codeList.Add(tempCode.Replace("ai_", ""));
                            }
                            if (!string.IsNullOrEmpty(split[1])) result.list.Add(JsonConvert.DeserializeObject<dynamic>(split[1]));
                        }
                    }
                });
            }
            result.rawObj = new Dictionary<string, ConversationDto>();
            // 判断是否需要获取
            if (codeList != null && codeList.Count > 0)
            {
                Dictionary<string, List<string>> map = new Dictionary<string, List<string>>();
                foreach (string code in codeList)
                {
                    string[] split = code.Split(new string[] { "_" }, StringSplitOptions.None);
                    if (split.Length > 1)
                    {
                        if(map.TryGetValue(split[0], out List<string> tempList))
                        {
                            tempList.Add(split[1]);
                        }
                        else
                        {
                            map.Add(split[0], new List<string> { split[1] });
                        }
                    }
                }
                if(map.Count > 0)
                {
                    result.rawObj = AiUtils.GetStructureDataByCode(type, sourceId, sourceType, map);
                }
            }

            return result;
        }

        /// <summary>
        /// 获取使用ai的模型
        /// </summary>
        /// <param name="vo"></param>
        /// <returns></returns>
        /// <exception cref="NotImplementedException"></exception>
        public int GetAiModel(AiModelVo vo)
        {
            string allContent = string.Empty;
            if (vo.sourceType == 0)
            {
                SentenceMarkDto sentenceMarkDto = lockAnalysisVideo(vo.sourceId);
                allContent = getAllVideoContent(sentenceMarkDto);
            }
            else if (vo.sourceType == 1)
            {
                SentenceMarkDto sentenceMarkDto = lockAnalysisVideoFile(vo.sourceId);
                allContent = getAllFileContent(sentenceMarkDto);
            }
            else
            {
                throw new CustomException("当前类型暂不支持", 7005);
            }

            return ReplayHttpUtils.GetAiModel(allContent.Length);
        }

        public dynamic conversationPage(ConversationPageBo bo)
        {

            // 先判断服务器是否有ai问答纪录
            ConversationVo vo = new ConversationVo();
            vo.sourceId = bo.sourceId;
            vo.sourceType = bo.sourceType;
            vo.askType = bo.type;

            if (!AiRelatedApi.isExistConversation(vo))
            {
                // 服务器没有问答记录，查询本地是否存在，存在就上传到服务器上
                string path = AiUtils.GetAiProblemPath(bo.type, bo.sourceId, bo.sourceType);
                // 判断是否有这个文件件
                if (Directory.Exists(path))
                {
                    string[] strings = Directory.GetFiles(path, "*.txt");
                    if(strings.Length > 0)
                    {
                        List<string> filePaths = strings.Where(item => !item.EndsWith("structure.txt")).ToList();
                        List<ConversationDto> list = new List<ConversationDto>();
                        if (filePaths != null && filePaths.Count > 0)
                        {
                            foreach (string filePath in filePaths)
                            {
                                string[] fileContents = File.ReadAllLines(filePath);
                                if (fileContents != null && fileContents.Length > 0)
                                {
                                    foreach (string fileContent in fileContents)
                                    {

                                        try
                                        {
                                            string[] strings1 = fileContent.Split(new string[] { ">>>" }, StringSplitOptions.None);
                                            if (strings1 != null && strings1.Length < 2) { continue; }
                                            ConversationDto item = JsonConvert.DeserializeObject<ConversationDto>(strings1[1]);
                                            item.sourceId = bo.sourceId;
                                            item.sourceType = bo.sourceType;
                                            item.userId = ReplayHttpUtils.UserId;
                                            item.tenantId = ReplayHttpUtils.ActiveTenantId.ToString();
                                            item.askType = bo.type;
                                            list.Add(item);
                                        }
                                        catch (Exception ex)
                                        {
                                            FileUtils.LogError($"fileContent = {fileContent},msg={ex.Message}", "同步ai问答的记录到服务器时json转对象报错");
                                        }

                                    }
                                }
                            }
                        }

                        if (list.Count > 0)
                        {

                            List<ConversationDto> resList = DiagnosisApi.saveConversationDataNotAsync(list);
                        }
                    }
                    
                }
            }

            ConversationPage page = AiRelatedApi.conversationPage(bo);

            if (page == null || page.list == null || page.list.Count == 0)
            {
                return page;
            }

            // 下载html的文件和跟换html的域名
            getHtmlFile(page.list);

            return page;
        }

        private void downloadHtmlFile(List<ConversationVo> list)
        {
            if (list == null || list.Count == 0)
            {
                return;
            }

            // 检查Constant.htmlProxy是否有值
            if (string.IsNullOrEmpty(Constant.htmlProxy))
            {
                FileUtils.LogError("Constant.htmlProxy 为空，无法下载HTML文件", "下载HTML文件");
                return;
            }

            // 获取需要下载的项目
            var itemsToDownload = list.Where(item =>
            {
                if (string.IsNullOrEmpty(item.htmlSavePath))
                {
                    return false;
                }
                return !File.Exists(WebsocketDataHandle.savePath + "/" + item.htmlSavePath);
            }).ToList();

            if (itemsToDownload.Count == 0)
            {
                return;
            }

            // 使用Parallel.ForEach进行多线程下载
            Parallel.ForEach(itemsToDownload, item =>
            {
                string fullPath = WebsocketDataHandle.savePath + "/" + item.htmlSavePath;
                
                // 创建目录（如果不存在）
                string directoryPath = Path.GetDirectoryName(fullPath);
                if (!string.IsNullOrEmpty(directoryPath) && !Directory.Exists(directoryPath))
                {
                    Directory.CreateDirectory(directoryPath);
                }
                
                // 构建完整的下载URL，使用Constant.htmlProxy作为域名-
                string fullUrl = item.htmlDomainName + "/" + item.htmlSavePath;
                
                // 下载文件
                bool downloadSuccess = OssUtils.DownloadFileFromFullUrl(fullUrl, fullPath);
                
                if (!downloadSuccess)
                {
                    // 下载失败，将htmlSavePath设置为null，并打印错误日志
                    FileUtils.LogError($"下载HTML文件失败: {item.htmlSavePath}", "下载HTML文件");
                    item.htmlSavePath = null;
                }
            });
        }

        /// <summary>
        /// 获取ai推荐的行业
        /// </summary>
        /// <param name="bo"></param>
        /// <returns></returns>
        public AiRecommendTradeVo getAiRecommendTrade(AiRecommendTradeBo bo)
        {

            if(bo.sourceType == 0)
            {
            }

            AiAnalyzeTradePromptVo aiAnalyzeTradePromptVo = AiRelatedApi.aiAnalyzeTradePrompt(bo.sourceType, bo.sourceId);

            AiRecommendTradeVo res = new AiRecommendTradeVo()
            {
                tradeId = aiAnalyzeTradePromptVo?.defTradeId ?? 1,
                tradeName = aiAnalyzeTradePromptVo?.defTradeName ?? "全行业"
            };


            if (string.IsNullOrEmpty(aiAnalyzeTradePromptVo?.cueWord ?? null))
            {
                return res;
            }

            AiTempTokenDto token = AiRelatedApi.getAnalysisTempToken(aiAnalyzeTradePromptVo.aiModel??0);
            AskRequestDto dto = new AskRequestDto()
            {
                identity = aiAnalyzeTradePromptVo.aiIdentity,
                thinkingType = "disabled"
            };
            var service = AiFactory.GetAiChatService(token.resourceType);
            CompletionsDto completions =  service.ChatCompletion(token, dto, aiAnalyzeTradePromptVo.cueWord);

            
            if (completions.status == 0)
            {
                // 保存到aiToken使用记录中
                AiTokenUseRecordVo recordVo = new AiTokenUseRecordVo();
                recordVo.useSourceType = bo.sourceType;
                recordVo.useSourceId = bo.sourceId;
                recordVo.assistantType = (int)CueType.推荐行业;
                recordVo.modelName = token.modelCode;
                recordVo.requestId = completions.choices.requestId;
                recordVo.finishReason = completions.choices.finishReason;
                recordVo.remarks = "推荐行业";
                recordVo.promptTokens = completions.usage.promptTokens;
                recordVo.completionTokens = completions.usage.completionTokens;
                recordVo.cachedTokens = completions.usage.promptTokensDetails?.cached_tokens ?? 0;
                recordVo.totalTokens = completions.usage.totalTokens;
                AiTokenUseRecordVo result = AiTokenUseRecordApi.saveAiTokenUseRecord(recordVo);

                if (!string.IsNullOrEmpty(completions?.choices?.onlyMessage ?? "") && aiAnalyzeTradePromptVo.tradeList != null && aiAnalyzeTradePromptVo.tradeList.Count > 0)
                {
                    try
                    {
                        Regex regex = new Regex(@"\d+(\.\d+)+");

                        // 执行匹配
                        Match match = regex.Match(completions.choices.onlyMessage);

                        string val = match.Value;

                        if (aiAnalyzeTradePromptVo.tradeList.TryGetValue(val, out TradeTemp temp) && temp != null)
                        {
                            res.tradeId = temp.tradeId;
                            res.tradeName = temp.tradeName;
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError($"{ex.Message}", "匹配ai输出的行业报错");
                    }
                    
                }
                // 把行业同步到服务器
                AiRelatedApi.updateSuggestTradeId(bo.sourceType, bo.sourceId, res.tradeId);
            }

            return res;
        }

        /// <summary>
        /// 导出ai配置
        /// </summary>
        public void exportAiConfig(string qaCodes)
        {
            // 调用接口 replay/openapi/v2100/queryDanMuExport, post， 参数为：bo，下载弹幕数据的导出文件到savnPath处
            string url = ReplayHttpUtils.BaseUrl + "/ai/conversation/exportByQaCode?qaCodes=" + qaCodes;

            HttpClient client = HttpUtils.getClient();
            // 构建请求头

            var httpRequestMessage = new HttpRequestMessage(HttpMethod.Get, url)
            {
                Headers = { { "token", ReplayHttpUtils.Token } }
            };

            SignatureHeaders.AddSignature(httpRequestMessage);
            using (HttpResponseMessage res = client.SendAsync(httpRequestMessage).Result)
            {
                string fileName = "";
                if (res.IsSuccessStatusCode)
                {
                    // 获取 Content-Type 头信息
                    string contentType = res.Content.Headers.ContentType?.MediaType;

                    if (contentType != "application/octet-stream")
                    {
                        string respnseBody = res.Content.ReadAsStringAsync().Result;
                        dynamic resultObj = JsonConvert.DeserializeObject<dynamic>(respnseBody);
                        string msg = resultObj.msg == null ? "导出失败" : resultObj.msg.ToString();
                        throw new CustomException(msg, 3001);
                    }
                    else
                    {
                        string path = UploadUtils.uploadsFilePath;
                        // 确保目录存在
                        Directory.CreateDirectory(path);

                        // 获取文件名称
                        IEnumerable<string> contentDispositionValues;
                        if (res.Content.Headers.TryGetValues("Content-Disposition", out contentDispositionValues))
                        {
                            foreach (string value in contentDispositionValues)
                            {
                                if (value.StartsWith("attachment"))
                                {
                                    int index = value.IndexOf("filename=", StringComparison.OrdinalIgnoreCase);
                                    if (index > -1)
                                    {
                                        fileName = value.Substring(index + 9).Trim('"');
                                        break;
                                    }
                                }
                            }
                        }
                        fileName = HttpUtility.UrlDecode(fileName);

                        // 在设置文件名前调用 getCurrentFileName 方法，避免文件覆盖
                        string fileNameWithoutExtension = Path.GetFileNameWithoutExtension(fileName);
                        string suffixName = Path.GetExtension(fileName);
                        fileName = UploadUtils.getCurrentFileName(fileNameWithoutExtension, suffixName);

                        path += $"\\{fileName}";
                        using (Stream stream = res.Content.ReadAsStreamAsync().Result)
                        using (FileStream fs = new FileStream(path, FileMode.Create, FileAccess.Write))
                        {
                            stream.CopyTo(fs);
                        }

                        // 下载完成后
                        FileUtils.openFile(path);
                    }
                }
                else
                {
                    // 记录非成功状态码的日志
                    FileUtils.LogError($"请求失败，状态码: {res.StatusCode}", $"AI问答数据导出请求发生异常");
                    throw new CustomException($"导出失败，状态码: {res.StatusCode}", 3001);
                }
            }
        }
        
        /// <summary>
        /// 获取html状态
        /// </summary>
        public List<ConversationVo> getHtmlStatus(List<string> ids)
        {
            List<ConversationVo> resList = AiRelatedApi.getHtmlStatus(ids);
            // 下载html的文件和跟换html的域名
            getHtmlFile(resList);

            return resList;
        }
        
        private void getHtmlFile(List<ConversationVo> resList)
        {
            // 下载html的文件和跟换html的域名
            downloadHtmlFile(resList);

            foreach (var item in resList)
            {
                if (!string.IsNullOrEmpty(item.htmlSavePath))
                {
                    item.htmlDomainName = Constant.htmlProxy;
                }
            }
        }
    }
}
