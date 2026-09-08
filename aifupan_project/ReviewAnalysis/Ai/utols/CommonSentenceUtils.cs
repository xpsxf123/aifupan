using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using ReviewAnalysis.bo.ai;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.ai;

namespace ReviewAnalysis.Ai.utols
{
    public class CommonSentenceUtils
    {
        /// <summary>
        /// 通用的设置提示词
        /// </summary>
        /// <param name="dto"></param>
        /// <param name="mark"></param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public static String setAskQuestion(AskRequestDto dto, SentenceMark mark)
        {
            string str = "";
            if (dto.useModelType == 0)
            {
                if (!string.IsNullOrEmpty(dto.paragraphCode) && string.IsNullOrEmpty(dto.paragraphContent))
                {

                    bool flag = true;
                    if (!string.IsNullOrEmpty(dto.paragraphCode) && !"0".Equals(dto.paragraphCode))
                    {
                        string content = AiUtils.GetHistoryContentByCode(dto.type, dto.sourceId, dto.sourceType, dto.paragraphCode);
                        HistoryParagraphVo history = JsonConvert.DeserializeObject<HistoryParagraphVo>(content);
                        str += history.content;
                        flag = false;
                    }
                    if (flag)
                    {
                        str += mark.GetContent();
                    }

                    if ("0".Equals(dto.paragraphCode))
                    {
                        str = "\n以上问题针对#{platform}直播脚本为全文。";
                    }
                    else
                    {
                        string content = AiUtils.GetHistoryContentByCode(dto.type, dto.sourceId, dto.sourceType, dto.paragraphCode);
                        HistoryParagraphVo history = JsonConvert.DeserializeObject<HistoryParagraphVo>(content);
                        str = $"\n本次回答内容不用考虑全文，除非提问中明确你需要涉及全文相关内容做分析，否则只需要根据以下内容进行回答：";
                        if (string.IsNullOrEmpty(content)) throw new CustomException("获取历史段落失败", 7005);
                        if (string.IsNullOrEmpty(history.content)) throw new CustomException("获取历史段落内容失败", 7005);
                        str += history.content;
                    }
                }
                else
                {
                    if (!string.IsNullOrEmpty(dto.paragraphContent))
                    {
                        str = "\n本次回答内容不用考虑全文，除非提问中明确你需要涉及全文相关内容做分析，否则只需要根据以下内容进行回答：";
                        str += dto.paragraphContent;
                    }
                    else
                    {
                        str = "\n以上问题针对#{platform}直播脚本为全文。";
                    }
                }
            }
            else if (dto.useModelType == 1)
            {
                if (string.IsNullOrEmpty(dto.paragraphContent))
                {
                    str = $"\n{mark.GetTextParamsContent()}\n\n针对本次问题回答的范围为以下内容：";
                    bool flag = true;
                    if (!string.IsNullOrEmpty(dto.paragraphCode) && !"0".Equals(dto.paragraphCode))
                    {
                        string content = AiUtils.GetHistoryContentByCode(dto.type, dto.sourceId, dto.sourceType, dto.paragraphCode);
                        HistoryParagraphVo history = JsonConvert.DeserializeObject<HistoryParagraphVo>(content);
                        str += history.content;
                        flag = false;
                    }
                    if (flag)
                    {
                        str += mark.GetContent();
                    }
                }
                else
                {
                    str = $"\n针对本次问题回答的范围为以下内容：\n{dto.paragraphContent}";
                }
            }
            else
            {

            }
            return str;
        }

        /// <summary>
        /// 设置提示词-文件的数据截图、视频的数据截图、视频的数据看板
        /// </summary>
        /// <param name="askRequestBo"></param>
        /// <param name="mark"></param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public static string setAskScreenshotQuestion(AskRequestDto askRequestBo, SentenceMark mark)
        {
            if (askRequestBo == null) return "";

            String str = "";
            if (askRequestBo.useModelType == 0)
            {
                // 有上下文缓存
                if (!string.IsNullOrEmpty(askRequestBo.paragraphCode) && string.IsNullOrEmpty(askRequestBo.paragraphContent))
                {
                    if (string.IsNullOrEmpty(askRequestBo.paragraphCode) || askRequestBo.paragraphCode == "0")
                    {
                        str = "\n以上问题针对#{platform}全场直播相关数据全文。";
                    }
                    else
                    {
                        string content = AiUtils.GetHistoryContentByCode(askRequestBo.type, askRequestBo.sourceId, askRequestBo.sourceType, askRequestBo.paragraphCode);
                        HistoryParagraphVo history = JsonConvert.DeserializeObject<HistoryParagraphVo>(content);
                        str = $"\n本次回答内容不用考虑全文，除非提问中明确你需要涉及全文相关内容做分析，否则只需要根据以下内容进行回答：";
                        if (string.IsNullOrEmpty(content)) throw new CustomException("获取历史段落失败", 7005);
                        if (string.IsNullOrEmpty(history.content)) throw new CustomException("获取历史段落内容失败", 7005);
                        str += $"\n本次回答内容不用考虑全文，除非提问中明确你需要涉及全文相关内容做分析，否则只需要根据以下内容进行回答：\n{history.content}";
                    }
                }
                else
                {
                    if (!string.IsNullOrEmpty(askRequestBo.paragraphContent))
                    {
                        str = $"\n本次回答内容不用考虑全文，除非提问中明确你需要涉及全文相关内容做分析，否则只需要根据以下内容进行回答：\n{askRequestBo.paragraphContent}";
                    }
                    else
                    {
                        str = "\n以上问题针对#{platform}全场直播相关数据全文。";
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
                        str = $"\n本次回答内容不用考虑全文，除非提问中明确你需要涉及全文相关内容做分析，否则只需要根据以下内容进行回答：";
                        if (string.IsNullOrEmpty(content)) throw new CustomException("获取历史段落失败", 7005);
                        if (string.IsNullOrEmpty(history.content)) throw new CustomException("获取历史段落内容失败", 7005);

                        str = $"\n针对本次问题回答的范围为以下内容：\n{history.content}";
                    }
                    else
                    {
                        str = $"\n针对本次问题回答的范围为以下内容：\n{mark.GetContent()}";
                    }
                }
                else
                {
                    str = $"\n针对本次问题回答的范围为以下内容：\n{askRequestBo.paragraphContent}";
                }
            }
            return str;
        }

    }
}
