using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Remoting.Messaging;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using System.Windows;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Ai.Model;
using ReviewAnalysis.Ai.utols;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.ai;
using ReviewAnalysis.vo.barrage;
using ReviewAnalysis.vo.dataScreenshot;
using ReviewAnalysis.vo.system;
using ReviewAnalysis.vo.trade;
using ReviewAnalysis.vo.video;
using ReviewAnalysis.Websocket.Entity;

namespace ReviewAnalysis.Ai.impl
{
    public class FileSentence : AbstractSentence
    {
        public SentenceMarkDto dto = null;
        public string content = null;
        public string textParamsContent = null;
        public TradeVo trade = null;
        public int singleMaxNum = -1;

        public FileSentence(string sourceId, List<long> videoTimeOneList)
        {
            this.sourceId = sourceId;
            this.videoTimeOneList = videoTimeOneList;
            this.sourceType = 1;
        }

        /// <summary>
        /// 获取对象
        /// </summary>
        /// <param name="sourceId"></param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public override SentenceMarkDto GetMarkDto()
        {
            if (dto != null) return dto;
            UploadFileBll uploadFileBll = new UploadFileBll();
            dto = uploadFileBll.LockAnalysis(sourceId);
            if (dto?.fileAudioaAlyses == null) throw new CustomException("查询全文失败");
            return dto;
        }

        /// <summary>
        /// 获取分钟段落的内容
        /// </summary>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public string minuteParagraphContent()
        {
            List<AudioaAlysesStatisticsDto> list = new List<AudioaAlysesStatisticsDto>();
            int onlineNumIndex = 0;
            string startStrTime = "", endStrTime = "";
            dynamic dataDisplay = otherParams.ContainsKey("aiAssistantDisplay") ? otherParams["aiAssistantDisplay"] : null;
            bool startTimeFlag = (dataDisplay?.startTime ?? 1) == 1; // 开始时间
            bool natureTimeFlag = (dataDisplay?.natureTime ?? 1) == 1; // 自然时间-单前无用
            bool onlineNumFlag = (dataDisplay?.onlineNum ?? 1) == 1; // 在线人数
            bool barrageNumFlag = (dataDisplay?.barrageNum ?? 1) == 1; // 弹幕人数
            bool dealNumFlag = (dataDisplay?.dealNum ?? 1) == 1; // 成交数量-单前无用
            bool analysisCharFlag = (dataDisplay?.analysisChar ?? 1) == 1; // 语速
            for (int i = 0; i < dto.fileAudioaAlyses.Count; i++)
            {
                DateTime? startTime = null;
                DateTime? endTime = null;
                string renShu = null, textStart = null, textEnd = null, content = null;
                long startTempTime = 0, endTempTime = 0, endTempTimeTwo = 0;
                UploadFileAlysis item = dto.fileAudioaAlyses[i];
                if (!string.IsNullOrEmpty(item.DataJson))
                {
                    var res = JsonConvert.DeserializeObject<dynamic>(item.DataJson);
                    content = res.content ?? "";
                    if (res.items != null && res.items.Count > 0)
                    {
                        startTempTime = res.items[0]?.startTime ?? 0;
                        endTempTime = res.items[res.items.Count - 1]?.endTime ?? 0;
                        endTempTimeTwo = endTempTime;
                        if (i < dto.fileAudioaAlyses.Count - 1)
                        {
                            UploadFileAlysis item2 = dto.fileAudioaAlyses[i + 1];
                            if (!string.IsNullOrEmpty(item2.DataJson))
                            {
                                var res2 = JsonConvert.DeserializeObject<dynamic>(item2.DataJson);
                                if (res2.items != null && res2.items.Count > 0)
                                {
                                    endTempTime = res2.items[0]?.startTime ?? 0;
                                }
                            }
                        }
                        startTime = DateUtils.TimestampToDateTime(startTempTime);
                        endTime = DateUtils.TimestampToDateTime(endTempTime);
                        textStart = DateUtils.DateTimeToString(DateUtils.TimestampToDateTime(startTempTime, false), "HH:mm:ss");
                        textEnd = DateUtils.DateTimeToString(DateUtils.TimestampToDateTime(endTempTime, false), "HH:mm:ss");
                    }
                }

                if (i == 0) startStrTime = textStart;
                endStrTime = textEnd;
                list.Add(new AudioaAlysesStatisticsDto()
                {
                    startTempTime = startTempTime,
                    endTempTime = endTempTime,
                    endTempTimeTwo = endTempTimeTwo,
                    startTime = startTime,
                    endTime = endTime,
                    textStart = textStart,
                    textEnd = textEnd,
                    renShu = renShu,
                    content = content
                });
            }
            // 设置全段内容
            string str = "";
            if (list != null && list.Count > 0)
            {
                for (int i = 0; i < list.Count; i++)
                {
                    string temp = "";
                    var item = list[i];
                    string renShu = i == 0 ? item.renShu ?? "0" : item.renShu;
                    if (renShu == null) renShu = list[i - 1]?.renShu;

                    int tempDuration = dto.uploadFile.FileDuration;

                    long startParagraphTime = item.startTempTime;
                    long endParagraphTime = item.endTempTime;

                    if (tempDuration > 0)
                    {
                        if (videoTimeOneList != null && videoTimeOneList.Count > 1)
                        {
                            long start = videoTimeOneList[0];
                            long end = videoTimeOneList[1];
                            // 判断当前段落是否在指定的时间范围内
                            if (!(startParagraphTime >= start && endParagraphTime <= end))
                            {
                                continue;
                            }
                        }
                    }

                    temp += i == 0 ? "" : "\n\n";
                    
                    // 语数
                    if (analysisCharFlag)
                    {
                        long num = 0;
                        if (!string.IsNullOrEmpty(item.content))
                        {
                            string contentStr = Regex.Replace(item.content, @"[，。？、]", "");
                            long time = (item.endTempTimeTwo - startParagraphTime) / 1000;
                            if (time > 0)
                            {
                                num = contentStr.Length / time * 60;
                                if (analysisCharFlag) temp += $"\n本段语速：{((int)num)}";
                            }
                        }
                    }
                    if (startTimeFlag && !string.IsNullOrEmpty(item.textStart))
                    {
                        temp += $"\n本段开始时间：{item.textStart}";
                    }
                    if (startTimeFlag && !string.IsNullOrEmpty(item.textEnd))
                    {
                        temp += $"\n本段结束时间：{item.textEnd}";
                    }
                    temp += $"\n本段内容：";
                    string content = item.content;
                    if (singleMaxNum != -1 && singleMaxNum - str.Length - temp.Length - content.Length < 0)
                    {
                        int tempI = singleMaxNum - str.Length - temp.Length;
                        if (tempI > 0)
                        {
                            content = content.Substring(0, tempI);
                        }
                        else
                        {
                            break;
                        }
                    }
                    temp += $"{content}";
                    if (singleMaxNum != -1 && singleMaxNum - str.Length - temp.Length < 0)
                    {
                        break;
                    }
                    str += temp;
                }
            }
            this.content = str;
            return this.content;
        }

        /// <summary>
        /// 获取自然原文、优化原文
        /// </summary>
        /// <param name="type">1自然原文、2优化原文</param>
        /// <param name="name"></param>
        /// <returns></returns>
        public string textContent(int type, string name)
        {
            AnchorVideoFileAllVo anchorVideoFile = VideoContentApi.getVideoContent(sourceId, 1, type);
            StringBuilder str = new StringBuilder();
            str.Append("\n");
            if (anchorVideoFile != null && anchorVideoFile.contentStatus == 2 && anchorVideoFile.videoFileContentList != null && anchorVideoFile.videoFileContentList.Count > 0)
            {
                long i = 0;
                int num = singleMaxNum;
                long startI = videoTimeOneList[0];
                long endI = videoTimeOneList[1];
                for (int j = 0; j < anchorVideoFile.videoFileContentList.Count; j++)
                {
                    VideoContentVo item = anchorVideoFile.videoFileContentList[j];
                    if (item.contentList != null && item.contentList.Count > 0)
                    {
                        for (int index = 0; index < item.contentList.Count; index++)
                        {
                            if (i >= startI && i < endI)
                            {
                                String temp = item.contentList[index] + "\n"; ;
                                int tempNum = str.Length + temp.Length;
                                if (num != -1 && num - tempNum <= 0)
                                {
                                    break;
                                }
                                str.Append(temp);
                            }
                            i++;
                        }

                    }
                }
            }
            else
            {
                CustomException.create($"未生成{name}，请您先生成。");
            }
            this.content = str.ToString();
            return this.content;
        }

        /// <summary>
        /// 获取段落
        /// </summary>
        /// <param name="sourceId"></param>
        /// <returns></returns>
        public override string GetContent()
        {
            if(dto == null) GetMarkDto();
            if(content != null) return content;

            dynamic questionContentTemp = otherParams.ContainsKey("questionContent") ? otherParams["questionContent"] : 0;
            int questionContent = 0;
            int.TryParse(questionContentTemp.ToString(), out questionContent);

            if (questionContent == 0)
            {
                return minuteParagraphContent();
            }
            else if (questionContent == 1)
            {
                return textContent(1, "自然原文");
            }
            else if (questionContent == 2)
            {
                return textContent(2, "优化原文");
            }
            else
            {
                CustomException.create("questionContent未知");
            }

            return "";
        }

        /// <summary>
        /// 获取段落说明
        /// </summary>
        /// <param name="sourceId"></param>
        /// <returns></returns>
        public override string GetTextParamsContent()
        {
            if (dto == null) GetMarkDto();
            if (textParamsContent != null) return textParamsContent;

            string str = "";
            dynamic questionContentTemp = otherParams.ContainsKey("questionContent") ? otherParams["questionContent"] : 0;
            int questionContent = 0;
            int.TryParse(questionContentTemp.ToString(), out questionContent);
            if (videoTimeOneList != null && questionContent == 0)
            {
                dynamic dataDisplay = otherParams.ContainsKey("aiAssistantDisplay") ? otherParams["aiAssistantDisplay"] : null;
                bool startTimeFlag = (dataDisplay?.startTime ?? 1) == 1; // 开始时间
                bool natureTimeFlag = (dataDisplay?.natureTime ?? 1) == 1; // 自然时间-单前无用
                bool onlineNumFlag = (dataDisplay?.onlineNum ?? 1) == 1; // 在线人数
                bool barrageNumFlag = (dataDisplay?.barrageNum ?? 1) == 1; // 弹幕人数
                bool dealNumFlag = (dataDisplay?.dealNum ?? 1) == 1; // 成交数量-单前无用
                string tempStartDate = "00:00:00";
                int tempDuration = dto.uploadFile.FileDuration;
                long start = videoTimeOneList[0];
                long end = videoTimeOneList[1];

                string startStr = DateUtils.DateTimeUTCToString(start, "HH:mm:ss");
                string sendStr = DateUtils.DateTimeUTCToString(end, "HH:mm:ss");
                string tempEndDate = AiUtils.AddSecondsAndFormat(tempStartDate, tempDuration);
                str += $"\n文本内的参数说明：";
                str += $"\n本段语速：指的是1分钟内说的字数。";
                if (startTimeFlag) str += $"\n本段开始时间：指的是转译成的文字段落开始时对应的视频播放时间。";
                if(startTimeFlag) str += $"\n本段结束时间：指的是转译成的文字段落结束时对应的视频播放时间。";
                if(startTimeFlag) str += $"\n从第二段开始直到最后一段，每一段的本段自然时间是上一段的自然结束时间。";
                if(startTimeFlag) str += $"\n本场直播的视频时间是{startStr}到{sendStr}，共{(end - start) / 1000}秒的直播 \n\n";
            }
            str += "以下是#{platform}平台某直播账号直播间录屏转写的完整文字脚本，内容为从第一段到最后一段的连续全文。";

            textParamsContent = str;
            return textParamsContent;
        }

        /// <summary>
        /// 获取全部的内容
        /// </summary>
        /// <param name="sourceId"></param>
        /// <returns></returns>
        public override string GetAllContent()
        {
            return $"{GetTextParamsContent()}\n\n{GetContent()}";
        }

        /// <summary>
        /// 获取行业id
        /// </summary>
        /// <param name="sourceId"></param>
        /// <returns></returns>
        /// <exception cref="NotImplementedException"></exception>
        public override string GetTradeId()
        {
            if (dto == null) GetMarkDto();
            return dto.uploadFile.TradeId;
        }

        public override string GetTradeName()
        {
            if (dto == null) GetMarkDto();
            if (trade != null) return trade.name;
            trade = ReplayHttpUtils.GetTrade(GetTradeId());
            if (trade == null) return "";
            return trade.name;
        }

        public override string getPlatformValue()
        {
            return dto?.uploadFile?.PlatformType ?? null; ;
        }


        public override String setAskQuestion(AskRequestDto dto)
        {
            return CommonSentenceUtils.setAskQuestion(dto, this);
        }

        public override void setSingleMaxNum(int num)
        {
            this.singleMaxNum = num;
        }
        
        public override int? getSpeed()
        {
            if (dto == null) GetMarkDto();
            
            // 检查必要数据是否为空
            if (dto?.fileAudioaAlyses == null || dto.fileAudioaAlyses.Count == 0 || 
                dto?.uploadFile == null || dto.uploadFile.FileDuration == 0)
            {
                return null;
            }
            
            // 解析所有段落的 SentenceMarkVo
            var sentenceMarkVos = new List<SentenceMarkVo>();
            foreach (var audio in dto.fileAudioaAlyses)
            {
                if (!string.IsNullOrEmpty(audio.DataJson))
                {
                    try
                    {
                        var sentenceMarkVo = JsonConvert.DeserializeObject<SentenceMarkVo>(audio.DataJson);
                        if (sentenceMarkVo != null)
                        {
                            sentenceMarkVos.Add(sentenceMarkVo);
                        }
                    }
                    catch { }
                }
            }
            
            if (sentenceMarkVos.Count == 0)
            {
                return null;
            }
            
            // 计算所有句子内容的字符总数（去掉标点）
            int contentSum = sentenceMarkVos.Sum(item =>
            {
                if (string.IsNullOrEmpty(item.Content))
                {
                    return 0;
                }
                // 去掉标点的内容
                return removePunctuation(item.Content).Length;
            });
            
            if (contentSum == 0)
            {
                return 0;
            }
            
            // 计算所有句子的时间跨度总和
            long time = sentenceMarkVos.Sum(item =>
            {
                if (item.Items == null || item.Items.Count == 0)
                {
                    return 0L;
                }
                
                var times = item.Items.SelectMany(jtem => new[] { jtem.StartTime, jtem.EndTime }).ToArray();
                long min = times.Min();
                long max = times.Max();
                return max - min;
            });
            
            if (time == 0)
            {
                return 0;
            }
            
            // 返回语速：字符数 / (时间/1000) * 60
            return (int)(contentSum / (time / 1000.0) * 60.0);
        }
    }
}
