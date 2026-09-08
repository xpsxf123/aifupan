using System;
using System.Collections.Generic;
using System.Dynamic;
using System.Linq;
using System.Runtime.Remoting.Messaging;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using System.Web.UI.WebControls;
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
using ReviewAnalysis.vo.trade;
using ReviewAnalysis.vo.video;
using ReviewAnalysis.Websocket.Entity;

namespace ReviewAnalysis.Ai.impl
{
    public class VideoSentence : AbstractSentence
    {
        public SentenceMarkDto dto = null;
        public string content = null;
        public string textParamsContent = null;
        public TradeVo trade = null;
        public string dataScreenshot = null;
        public int singleMaxNum = -1;
        public BarrageDataVo barrageData = null;

        public VideoSentence(string sourceId, List<long> videoTimeOneList)
        {
            this.sourceId = sourceId;
            this.videoTimeOneList = videoTimeOneList;
            this.sourceType = 0;
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
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            dto = anchorVideoBll.LockAnalysis(sourceId);
            if (dto?.audioaAlyses == null) throw new CustomException("查询全文失败");
            return dto;
        }

        /// <summary>
        /// 获取分钟段落的内容
        /// </summary>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public string minuteParagraphContent()
        {
            string startTimeStr = dto?.videoInfo?.StartTime ?? "";
            if (string.IsNullOrEmpty(startTimeStr))
            {
                FileUtils.LogError(JsonConvert.SerializeObject(dto), "视频的开始时间获取失败");
                throw new CustomException("视频的开始时间获取失败", 7005);
            }
            long allStartTime = DateUtils.StringToTimestamp(startTimeStr);
            List<AudioaAlysesStatisticsDto> list = new List<AudioaAlysesStatisticsDto>();
            int onlineNumIndex = 0;
            string startStrTime = "", endStrTime = "";
            dynamic dataDisplay = otherParams.ContainsKey("aiAssistantDisplay") ? otherParams["aiAssistantDisplay"] : null;
            bool startTimeFlag = (dataDisplay?.startTime ?? 1) == 1; // 开始时间
            bool natureTimeFlag = (dataDisplay?.natureTime ?? 1) == 1; // 自然时间
            bool onlineNumFlag = (dataDisplay?.onlineNum ?? 1) == 1; // 在线人数
            bool barrageNumFlag = (dataDisplay?.barrageNum ?? 0) == 1; // 弹幕人数
            bool dealNumFlag = (dataDisplay?.dealNum ?? 0) == 1; // 成交数量
            bool interactionRateFlag = (dataDisplay?.interactionRate ?? 0) == 1; // 互动率
            bool dealRateFlag = (dataDisplay?.dealRate ?? 0) == 1; // 成交率
            bool analysisCharFlag = (dataDisplay?.analysisChar ?? 1) == 1; // 语速
            bool salesFlag = (dataDisplay?.sales ?? 0) == 1; // 销售额
            bool uvFlag = (dataDisplay?.uv ?? 0) == 1; // uv价值
            for (int i = 0; i < dto.audioaAlyses.Count; i++)
            {
                DateTime? startTime = null;
                DateTime? endTime = null;
                string renShu = null, textStart = null, textEnd = null, content = null;
                long startTempTime = 0, endTempTime = 0, startVideoTime = 0, endVideoTime = 0, endTempTimeTwo = 0;
                AudioaAlysis item = dto.audioaAlyses[i];
                if (!string.IsNullOrEmpty(item.DataJson))
                {
                    var res = JsonConvert.DeserializeObject<dynamic>(item.DataJson);
                    if (res.items != null && res.items.Count > 0)
                    {
                        content = res.content ?? "";
                        startTempTime = res.items[0]?.startTime ?? 0;
                        endTempTime = res.items[res.items.Count - 1]?.endTime ?? 0;
                        endTempTimeTwo = endTempTime;
                        if (i < dto.audioaAlyses.Count - 1)
                        {
                            AudioaAlysis item2 = dto.audioaAlyses[i + 1];
                            if (!string.IsNullOrEmpty(item2.DataJson))
                            {
                                var res2 = JsonConvert.DeserializeObject<dynamic>(item2.DataJson);
                                if (res2.items != null && res2.items.Count > 0)
                                {
                                    endTempTime = res2.items[0]?.startTime ?? 0;
                                }
                            }
                        }
                        startVideoTime = allStartTime + startTempTime;
                        endVideoTime = allStartTime + endTempTime;
                        startTime = DateUtils.TimestampToDateTime(allStartTime + startTempTime);
                        endTime = DateUtils.TimestampToDateTime(allStartTime + endTempTime);
                        textStart = DateUtils.DateTimeToString(DateUtils.TimestampToDateTime(startTempTime, false), "HH:mm:ss");
                        textEnd = DateUtils.DateTimeToString(DateUtils.TimestampToDateTime(endTempTime, false), "HH:mm:ss");
                    }
                }
                if (startTime == null || endTime == null) continue;
                // 获取在线人数
                if (dto.onlineNumList != null && dto.onlineNumList.Count > 0)
                {
                    for (int j = onlineNumIndex; j < dto.onlineNumList.Count; j++)
                    {
                        OnlineNum onlineNum = dto.onlineNumList[j];
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
                list.Add(new AudioaAlysesStatisticsDto()
                {
                    startTempTime = startTempTime,
                    endTempTime = endTempTime,
                    endTempTimeTwo = endTempTimeTwo,
                    startVideoTime = startVideoTime,
                    endVideoTime = endVideoTime,
                    startTime = startTime,
                    endTime = endTime,
                    textStart = textStart,
                    textEnd = textEnd,
                    renShu = renShu,
                    content = content
                });
            }

            string str = "";
            if (list != null && list.Count > 0)
            {
                Dictionary<string, int> barrageDataMap = new Dictionary<string, int>();
                if (barrageNumFlag || interactionRateFlag)
                {
                    BarrageDataVo barrageDataVo = getBarrageList();
                    if (barrageDataVo.barrageDataList != null && barrageDataVo.barrageDataList.Count > 0)
                    {
                        barrageDataMap = barrageDataVo.barrageDataList.GroupBy(x => x.date, x => x)
                            .ToDictionary(g => g.Key, g => g.First().barrageNum);
                    }
                }
                Dictionary<long, JuliangStatisticsDto> juliangDataMap = new Dictionary<long, JuliangStatisticsDto>();
                if (dealNumFlag || dealRateFlag)
                {
                    juliangDataMap = getJuliangData(dto.videoInfo.PlatformType, sourceId, list);
                }

                for (int i = 0; i < list.Count; i++)
                {
                    var item = list[i];
                    string temp = "";
                    string renShu = i == 0 ? item.renShu ?? "0" : item.renShu;
                    if (renShu == null) renShu = list[i - 1]?.renShu;
                    long startParagraphTime = item.startTempTime;
                    long endParagraphTime = item.endTempTime;
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

                    temp += i == 0 ? "" : "\n";
                    int barrageNum = 0;
                    if (item.startTime != null)
                    {
                        string dateTimeStr = DateUtils.DateTimeToString(item.startTime ?? new DateTime());
                        barrageDataMap.TryGetValue(dateTimeStr, out barrageNum);
                    }
                    JuliangStatisticsDto tempJuliang = null;
                    juliangDataMap.TryGetValue(item.startVideoTime, out tempJuliang);

                    if (natureTimeFlag && item.startTime != null)
                    {
                        temp += $"\n本段自然时间: {DateUtils.DateTimeToString(item.startTime ?? new DateTime(), "HH:mm:ss")}";
                    }
                    if (startTimeFlag) temp += $"\n本段开始时间：{item.textStart}";
                    if (startTimeFlag) temp += $"\n本段结束时间：{item.textEnd}";
                    int oldRenShu = i == 0 ? 0 : int.TryParse(list[i - 1].renShu, out int tempOldVlaue) ? tempOldVlaue : 0;
                    int currentValue = int.TryParse(renShu, out int tempRenShu) ? tempRenShu : 0;
                    string strName = currentValue >= oldRenShu ? "增加" : "减少";
                    int currentInt = i == 0 ? 0 : Math.Abs(currentValue - oldRenShu);
                    if (onlineNumFlag) temp += $"\n本段在线人数：{renShu}";
                    if (onlineNumFlag) temp += $"\n本段在线人数{strName}：{currentInt}人";
                    // 语数
                    if (analysisCharFlag)
                    {
                        double num = 0.0;
                        if (!string.IsNullOrEmpty(item.content))
                        {
                            string contentStr = removePunctuation(item.content);
                            double time = (item.endTempTimeTwo - startParagraphTime) / 1000.0;
                            if (time > 0)
                            {
                                num = (1.0 * contentStr.Length / time * 60.0);
                            }
                        }
                        if (analysisCharFlag) temp += $"\n本段语速：{((int)num)}";
                    }
                    if (barrageNumFlag) // 弹幕条数
                    {
                        temp += $"\n本段发弹幕条数：{barrageNum}条";
                    }
                    if (dealNumFlag) // 成交人数
                    {
                        temp += $"\n本段成交人数：{(tempJuliang == null ? 0 : tempJuliang.rangePayComboCnt)}";
                    }
                    if (interactionRateFlag) // 互动率
                    {
                        int rs = 0;
                        int.TryParse(renShu, out rs);
                        double rela = 0;
                        if(!(barrageNum == 0 || rs == 0))
                        {
                            rela = Math.Round((double)barrageNum / rs * 100, 2);
                        }
                        temp += $"\n本段互动率：{rela}%";
                    }
                    if (dealRateFlag) // 成交率
                    {
                        int rs = 0;
                        int.TryParse(renShu, out rs);
                        double rela = 0;
                        if (!(tempJuliang == null || tempJuliang.rangePayComboCnt == 0))
                        {
                            rela = Math.Round((double)tempJuliang.rangePayComboCnt / rs * 100, 2);
                        }
                        temp += $"\n本段成交率：{rela}%";
                    }
                    if (salesFlag) // 销售额
                    {
                        if (!(tempJuliang == null || tempJuliang.rangePayAmt == 0))
                        {
                            temp += $"\n本段销售额：{(int)(tempJuliang.rangePayAmt / 100)}元";
                        }
                    }
                    if (uvFlag) // uv价值
                    {
                        int rs = 0;
                        int.TryParse(renShu, out rs);
                        double rela = 0;
                        if (!(tempJuliang == null || tempJuliang.rangePayAmt == 0))
                        {
                            rela = Math.Round((double)tempJuliang.rangePayAmt / rs / 100, 2);
                        }
                        temp += $"\n本段UV价值：{rela}";
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
            AnchorVideoFileAllVo anchorVideoFile = VideoContentApi.getVideoContent(sourceId, 0, type);
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
        /// <exception cref="CustomException"></exception>
        public override string GetContent()
        {
            if (dto == null) GetMarkDto();
            if (content != null) return content;
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
                bool natureTimeFlag = (dataDisplay?.natureTime ?? 1) == 1; // 自然时间
                bool onlineNumFlag = (dataDisplay?.onlineNum ?? 1) == 1; // 在线人数
                bool barrageNumFlag = (dataDisplay?.barrageNum ?? 0) == 1; // 弹幕人数
                bool dealNumFlag = (dataDisplay?.dealNum ?? 0) == 1; // 成交数量
                bool interactionRateFlag = (dataDisplay?.interactionRate ?? 0) == 1; // 互动率
                bool dealRateFlag = (dataDisplay?.dealRate ?? 0) == 1; // 成交率
                bool analysisCharFlag = (dataDisplay?.analysisChar ?? 1) == 1; // 语速
                bool salesFlag = (dataDisplay?.sales ?? 0) == 1; // 销售额
                bool uvFlag = (dataDisplay?.uv ?? 0) == 1; // uv价值

                long start = videoTimeOneList[0];
                long end = videoTimeOneList[1];

                string startStr = DateUtils.DateTimeUTCToString(start, "HH:mm:ss");
                string sendStr = DateUtils.DateTimeUTCToString(end, "HH:mm:ss");

                //string tempStartDate = "00:00:00";
                //int tempDuration = int.TryParse(dto.videoInfo.Duration, out int val) ? val : 0;
                //string tempEndDate = AiUtils.AddSecondsAndFormat(tempStartDate, tempDuration);
                string tempStr = "";
                if (natureTimeFlag) tempStr += $"\n本段自然时间：指的是转译成的文字段落开始时对应的北京时间。";
                if (startTimeFlag) tempStr += $"\n本段开始时间：指的是转译成的文字段落开始时对应的视频播放时间。";
                if (startTimeFlag) tempStr += $"\n本段结束时间：指的是转译成的文字段落结束时对应的视频播放时间。";
                if (onlineNumFlag) tempStr += $"\n本段在线人数：指的是转译成的文字段落开始时对应的直播间在线人数。";
                if (analysisCharFlag) tempStr += $"\n本段语速：指的是1分钟内说的字数。";
                if (barrageNumFlag) tempStr += $"\n本段发弹幕人数：指的是转译成的文字段落开始时间到结束时间内直播间发送弹幕数量。";
                if (dealNumFlag) tempStr += "\n本段成交数量：指的是转译成的文字段落开始时间到结束时间内商品的成交数量。";
                if (interactionRateFlag) tempStr += "\n本段互动率：指的是转译成的文字段落开始时间到结束时间内弹幕数量/在线人数得出的互动率。";
                if (dealRateFlag) tempStr += "\n本段成交率：指的是转译成的文字段落开始时间到结束时间内成交数量/在线人数得出的成交率。";
                if (salesFlag) tempStr += "\n本段销售额：指的是转译成的文字段落开始时间到结束时间内的销售额。";
                if (uvFlag) tempStr += "\n本段UV价值：指的是转译成的文字段落开始时间到结束时间内销售额/在线人数得出的UV价值。";
                if (!string.IsNullOrEmpty(tempStr))
                {
                    str += $"\n文本内的参数说明：{tempStr}";
                }

                //if (isRenShu) str += $"\n本段自然开始时间：指的是转译成的文字段落开始时对应的北京时间。";
                //str += $"\n从第二段开始直到最后一段，每一段的本段自然时间是上一段的自然结束时间。";
                str += $"\n本场直播的视频时间是{startStr}到{sendStr}，共{(end - start) / 1000}秒的直播 \n\n";
            }

            string anchorName = dto.anchorInfo.AnchorName;
            // 核心优化：无昵称时跳过“直播账号”，避免重复生硬
            if (!string.IsNullOrEmpty(anchorName))
                str += $"以下是#{{platform}}平台主播「{anchorName}」直播间录屏转写的完整文字脚本，内容为从第一段到最后一段的连续全文。";
            else
                str += "以下是#{platform}平台某直播账号直播间录屏转写的完整文字脚本，内容为从第一段到最后一段的连续全文。";

            textParamsContent = str;
            return textParamsContent;
        }

        /// <summary>
        /// 获取弹幕数量列表
        /// </summary>
        /// <returns></returns>
        public BarrageDataVo getBarrageList()
        {
            if (this.barrageData == null)
            {
                // 弹幕标注
                this.barrageData = BarrageApi.videoBarrageData(this.sourceId);
            }
            return this.barrageData;
        }

        /// <summary>
        /// 获取全文的字符串
        /// </summary>
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
            return dto.videoInfo.TradeId;
        }

        /// <summary>
        /// 获取行业名称
        /// </summary>
        /// <returns></returns>
        public override string GetTradeName()
        {
            if (dto == null) GetMarkDto();
            if (trade != null) return trade.name;
            trade = ReplayHttpUtils.GetTrade(GetTradeId());
            if (trade == null) return "";
            return trade.name;
        }

        public override string setAskQuestion(AskRequestDto dto)
        {
            return CommonSentenceUtils.setAskQuestion(dto, this);
        }

        public override void setSingleMaxNum(int num)
        {
            this.singleMaxNum = num;
        }

        public override string getPlatformValue()
        {
            return dto?.videoInfo?.PlatformType ?? null;
        }
        
        public override int? getSpeed()
        {
            if (dto == null) GetMarkDto();
            
            // 检查必要数据是否为空
            if (dto?.audioaAlyses == null || dto.audioaAlyses.Count == 0 || 
                dto?.videoInfo == null || string.IsNullOrEmpty(dto.videoInfo.Duration))
            {
                return null;
            }
            
            int duration = 0;
            if (!int.TryParse(dto.videoInfo.Duration, out duration) || duration == 0)
            {
                return null;
            }
            
            // 解析所有段落的 SentenceMarkVo
            List<SentenceMarkVo> sentenceMarkVos = new List<SentenceMarkVo>();
            foreach (var audio in dto.audioaAlyses)
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
