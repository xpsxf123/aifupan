using System;
using System.Collections;
using System.Collections.Generic;
using System.Dynamic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using ReviewAnalysis.api;
using ReviewAnalysis.bo.ai;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.ai;

namespace ReviewAnalysis.Ai.impl
{
    /// <summary>
    /// 弹幕数据
    /// </summary>
    public class BarrageSentence : VideoSentence
    {

        public BarrageSentence(string sourceId, int singleMaxNum, List<long> videoTimeOneList) : base(sourceId, videoTimeOneList)
        {
            this.singleMaxNum = singleMaxNum;

            base.GetMarkDto();
        }

        public override String GetContent()
        {
            if (content != null) return this.content;
            StringBuilder str = new StringBuilder();
            Dictionary<string, object> param = otherParams;
            param["videoId"] = sourceId;
            param["startTime"] = videoTimeOneList[0];
            param["endTime"] = videoTimeOneList[1];
            List<DanMuVo> danMuVos = AiRelatedApi.danMuCacheList(param);
            if (danMuVos == null || danMuVos.Count == 0)
            {
                throw new CustomException("弹幕数据为空不能发起ai提问", 7005);
            }

            int num = singleMaxNum;

            long startTime = DateUtils.StringToTimestamp(dto.videoInfo.StartTime);

            dynamic dataDisplay = otherParams.ContainsKey("dataDisplay") ? otherParams["dataDisplay"] : null;
            bool dateTime = (dataDisplay?.dateTime ?? 0) == 1;
            bool nickName = (dataDisplay?.nickName ?? 0) == 1;
            bool level = (dataDisplay?.level ?? 0) == 1;
            bool fansLevel = (dataDisplay?.fansLevel ?? 0) == 1;
            bool isNew = (dataDisplay?.isNew ?? 0) == 1;

            for (int i = 0; i < danMuVos.Count; i++)
            {
                DanMuVo danMu = danMuVos[i];

                long recordDate = danMu.recordDate ?? 0;

                if (recordDate > 0)
                {
                    string temp = "";

                    long time = recordDate - startTime;
                    if (dateTime)
                    {
                        temp += $"{DateUtils.DateTimeUTCToString(time, "HH:mm:ss")} ";
                    }
                    if (nickName)
                    {
                        temp += $"{danMu.nickName}";
                    }

                    if (level || fansLevel || isNew)
                    {
                        List<string> tempStr = new List<string>();
                        if (isNew)
                        {
                            string newStr = (danMu.isNew ?? false) && (danMu.fansLevelMin ?? 0) == 0 ? "新" : "";
                            if (!string.IsNullOrEmpty(newStr))
                            {
                                tempStr.Add(newStr);
                            }
                        }
                        if (level)
                        {
                            tempStr.Add($"UL:{danMu.level ?? 0}");
                        }
                        if (fansLevel)
                        {
                            tempStr.Add($"FL:{danMu.fansLevelCurrent ?? 0}");
                        }
                        if(tempStr.Count > 0)
                        {
                            temp += "(";
                            temp += string.Join("，", tempStr);
                            temp += ")";
                        }
                    }

                    if (!string.IsNullOrEmpty(temp))
                    {
                        temp += "：";
                    }
                    temp += $"{danMu.content}\n";
                    int tempNum = str.Length + temp.Length;
                    if (num != -1 && num - tempNum <= 0)
                    {
                        break;
                    }
                    str.Append(temp);
                }
            }
            this.content = str.ToString();
            return this.content;
        }

        public override String GetTextParamsContent()
        {
            if (textParamsContent != null) return textParamsContent;
            dynamic dataDisplay = otherParams.ContainsKey("dataDisplay") ? otherParams["dataDisplay"] : null;
            bool dateTime = (dataDisplay?.dateTime ?? 0) == 1;
            bool nickName = (dataDisplay?.nickName ?? 0) == 1;
            bool level = (dataDisplay?.level ?? 0) == 1;
            bool fansLevel = (dataDisplay?.fansLevel ?? 0) == 1;
            bool isNew = (dataDisplay?.isNew ?? 0) == 1;


            String str = "";
            str += $"\n文本内的参数说明：";
            str += $"\n一条完整的弹幕为一行，数据格式分为：";
            List<string> temps = new List<string>();
            if (dateTime) temps.Add("弹幕发送时间(HH:mm:ss)");
            if (nickName) temps.Add("用户昵称");
            if (isNew) temps.Add("新");
            if (level) temps.Add("UL");
            if (fansLevel) temps.Add("FL");
            temps.Add("弹幕内容"); 
            str += string.Join("、", temps);
            if (level || fansLevel)
            {
                str += $"\n";
                temps = new List<string>();
                if (level) temps.Add("UL是用户等级的简称");
                if (fansLevel) temps.Add("FL是用户粉丝团的简称");
                str += string.Join(",", temps);
            }
            if (isNew) str += $"\n弹幕格式中如果有“新”字则是新用户，没有就是老用户";
            if (nickName) str += $"\n用户昵称是否隐藏判断：金***、建***、青***这种类型的昵称都是隐藏的，其他格式的都是不隐藏的";
            if (nickName && level) str += $"\n判断是否为用一个用户的方式：1、昵称没有隐藏就以\"用户昵称\"区分。2、昵称已经隐藏就以\"用户昵称+UL\"区分";
            textParamsContent = str;
            return textParamsContent;
        }

        public override String setAskQuestion(AskRequestDto askRequestBo)
        {
            if (askRequestBo == null) return "";

            string str = "";
            if (askRequestBo.useModelType == 0)
            {
                // 有上下文缓存
                if (!string.IsNullOrEmpty(askRequestBo.paragraphCode) && askRequestBo.paragraphContent == null)
                {
                    if (string.IsNullOrEmpty(askRequestBo.paragraphCode) || askRequestBo.paragraphCode == "0")
                    {
                        str = "\n以上问题针对直播的全部弹幕。";
                    }
                    else
                    {
                        string content = AiUtils.GetHistoryContentByCode(askRequestBo.type, askRequestBo.sourceId, askRequestBo.sourceType, askRequestBo.paragraphCode);
                        HistoryParagraphVo history = JsonConvert.DeserializeObject<HistoryParagraphVo>(content);
                        if (string.IsNullOrEmpty(content)) throw new CustomException("获取历史段落失败", 7005);
                        if (string.IsNullOrEmpty(history.content)) throw new CustomException("获取历史段落内容失败", 7005);
                        str = $"\n本次回答内容不用考虑全部弹幕，除非提问中明确需要涉及全部弹幕相关内容做分析，否则只需要根据以下的弹幕内容进行回答：\n{history.content}";
                    }
                }
                else
                {
                    if (!string.IsNullOrEmpty(askRequestBo.paragraphContent))
                    {
                        str = $"\n本次回答内容不用考虑全部弹幕，除非提问中明确需要涉及全部弹幕相关内容做分析，否则只需要根据以下的弹幕内容进行回答：\n{askRequestBo.paragraphContent}";
                    }
                    else
                    {
                        str = "\n以上问题针对直播的全部弹幕。";
                    }
                }
            }
            else if (askRequestBo.useModelType == 1)
            {
                // 没有上下文缓存
                if (string.IsNullOrEmpty(askRequestBo.paragraphContent))
                {
                    if (!string.IsNullOrEmpty(askRequestBo.paragraphCode) && askRequestBo.paragraphCode != "0")
                    {
                        string content = AiUtils.GetHistoryContentByCode(askRequestBo.type, askRequestBo.sourceId, askRequestBo.sourceType, askRequestBo.paragraphCode);
                        HistoryParagraphVo history = JsonConvert.DeserializeObject<HistoryParagraphVo>(content);
                        if (string.IsNullOrEmpty(content)) throw new CustomException("获取历史段落失败", 7005);
                        if (string.IsNullOrEmpty(history.content)) throw new CustomException("获取历史段落内容失败", 7005);
                        str = $"\n针对本次问题回答的范围为以下弹幕内容：\n{history.content}";
                    }
                    else
                    {
                        str = string.Format("\n{0}\n\n针对本次问题回答的范围为以下弹幕内容：\n{1}", this.GetTextParamsContent(), this.GetContent());
                    }
                }
                else
                {
                    str = string.Format("\n针对本次问题回答的范围为以下弹幕内容：\n{0}", askRequestBo.paragraphContent);
                }
            }

            return str;
        }
    }
}
