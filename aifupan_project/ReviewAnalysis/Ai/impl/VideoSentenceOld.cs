using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Ai.utols;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.trade;

namespace ReviewAnalysis.Ai.impl
{
    internal class VideoSentenceOld : AbstractSentence
    {
        public SentenceMarkDto dto = null;
        public string content = null;
        public string textParamsContent = null;
        public TradeVo trade = null;
        public string dataScreenshot = null;
        public int singleMaxNum = -1;

        public VideoSentenceOld(string sourceId, List<long> videoTimeOneList)
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
        /// 获取段落
        /// </summary>
        /// <param name="sourceId"></param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public override string GetContent()
        {
            if (dto == null) GetMarkDto();
            if (content != null) return content;

            string startTimeStr = dto?.videoInfo?.StartTime ?? "";
            if (string.IsNullOrEmpty(startTimeStr))
            {
                FileUtils.LogError(JsonConvert.SerializeObject(dto), "视频的开始时间获取失败");
                throw new CustomException("视频的开始时间获取失败", 7005);
            }
            long allStartTime = DateUtils.StringToTimestamp(startTimeStr);
            List<dynamic> list = new List<dynamic>();
            int onlineNumIndex = 0;
            string startStrTime = "", endStrTime = "";
            for (int i = 0; i < dto.audioaAlyses.Count; i++)
            {
                DateTime? startTime = null;
                DateTime? endTime = null;
                string renShu = null, textStart = null, textEnd = null, content = null;
                long startTempTime = 0;
                long endTempTime = 0;
                AudioaAlysis item = dto.audioaAlyses[i];
                if (!string.IsNullOrEmpty(item.DataJson))
                {
                    var res = JsonConvert.DeserializeObject<dynamic>(item.DataJson);
                    if (res.items != null && res.items.Count > 0)
                    {
                        content = res.content ?? "";
                        startTempTime = res.items[0]?.startTime ?? 0;
                        endTempTime = res.items[res.items.Count - 1]?.endTime ?? 0;
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
                list.Add(new
                {
                    startTempTime,
                    endTempTime,
                    startTime,
                    endTime,
                    textStart,
                    textEnd,
                    renShu,
                    content
                });
            }

            string str = "";
            if (list != null && list.Count > 0)
            {
                for (int i = 0; i < list.Count; i++)
                {
                    var item = list[i];
                    string temp = "";
                    string renShu = i == 0 ? item.renShu ?? "0" : item.renShu;
                    if (renShu == null) renShu = list[i - 1]?.renShu;
                    long startParagraphTime = item.startTempTime ?? 0;
                    long endParagraphTime = item.endTempTime ?? 0;
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

                    temp += i == 0 ? "" : "\n\n";
                    temp += $"本段开始时间：{item.textStart}" +
                        $"\n本段结束时间：{item.textEnd}";
                    temp += $"\n本段在线人数：{renShu}";
                    //if (item.startTime != null) temp += $"\n本段自然时间: {DateUtils.DateTimeToString(item.startTime, "HH:mm:ss")}";
                    int oldRenShu = i == 0 ? 0 : int.TryParse(list[i - 1].renShu, out int tempOldVlaue) ? tempOldVlaue : 0;
                    int currentValue = int.TryParse(renShu, out int tempRenShu) ? tempRenShu : 0;
                    string strName = currentValue >= oldRenShu ? "增加" : "减少";
                    int currentInt = i == 0 ? 0 : Math.Abs(currentValue - oldRenShu);
                    temp += $"\n本段在线人数{strName}：{currentInt}人";
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
        /// 获取段落说明
        /// </summary>
        /// <param name="sourceId"></param>
        /// <returns></returns>
        public override string GetTextParamsContent()
        {
            if (dto == null) GetMarkDto();
            if (textParamsContent != null) return textParamsContent;
            string str = "";
            if (videoTimeOneList != null)
            {
                long start = videoTimeOneList[0];
                long end = videoTimeOneList[1];

                string startStr = DateUtils.DateTimeUTCToString(start, "HH:mm:ss");
                string sendStr = DateUtils.DateTimeUTCToString(end, "HH:mm:ss");

                //string tempStartDate = "00:00:00";
                //int tempDuration = int.TryParse(dto.videoInfo.Duration, out int val) ? val : 0;
                //string tempEndDate = AiUtils.AddSecondsAndFormat(tempStartDate, tempDuration);

                str += $"\n文本内的参数说明：" +
                       $"\n本段开始时间：指的是转译成的文字段落开始时对应的视频播放时间。" +
                       $"\n本段结束时间：指的是转译成的文字段落结束时对应的视频播放时间。";
                str += $"\n本段在线人数：指的是转译成的文字段落开始时对应的直播间在线人数。";
                //if (isRenShu) str += $"\n本段自然开始时间：指的是转译成的文字段落开始时对应的北京时间。";
                str += $"\n从第二段开始直到最后一段，每一段的本段自然时间是上一段的自然结束时间。" +
                    $"\n本场直播的视频时间是{startStr}到{sendStr}，共{(end - start) / 1000}秒的直播 \n\n";
            }

            string anchorName = dto.anchorInfo.AnchorName;
            str += "以下是由#{platform}账号";
            if (!string.IsNullOrEmpty(anchorName)) str += $"昵称为{anchorName}的";
            str += $"直播间录屏成视频后转译成文字的直播全文脚本，从第一段到最后一段，每一段的内容都是连续的。";

            textParamsContent = str;
            return textParamsContent;
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
    }
}
