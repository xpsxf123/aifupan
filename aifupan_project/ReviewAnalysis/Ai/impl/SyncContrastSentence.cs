using System;
using System.Collections.Generic;
using System.Diagnostics.Contracts;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Web.UI.WebControls;
using ReviewAnalysis.Ai.utols;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.contrast;
using ReviewAnalysis.vo.system;
using ReviewAnalysis.vo.trade;

namespace ReviewAnalysis.Ai.impl
{
    public class SyncContrastSentence : AbstractSentence
    {

        public SentenceMarkDto dto = null;
        public List<SentenceMarkDto> dtoList = null;
        public VideoContrast videoContrast;
        public string content = null;
        public string textParamsContent = null;
        public TradeVo trade = null;
        public int singleMaxNum = -1;
        public SentenceMark sentenceMark1 = null;
        public SentenceMark sentenceMark2 = null;

        public SyncContrastSentence(string sourceId)
        {
            this.sourceId = sourceId;
            AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
            SentenceMarkContrastDto dto = anchorVideoBll.LockAnalysisContrast(sourceId);
            dtoList = new List<SentenceMarkDto>();
            dtoList.Add(dto.SentenceMark1);
            dtoList.Add(dto.SentenceMark2);
            videoContrast = dto.VideoContrast;
            this.sourceType = 2;
        }

        public SyncContrastSentence(string sourceId, int singleMaxNum, List<long> videoTimeOneList, List<long> videoTimeTwoList) : this(sourceId)
        {
            this.singleMaxNum = singleMaxNum;
            this.videoTimeOneList = videoTimeOneList;
            this.videoTimeTwoList = videoTimeTwoList;
            this.sourceType = 2;
        }
        
        public override void setOtherParams(Dictionary<string, object> otherParams)
        {
            if (otherParams != null)
            {
                foreach (var kv in otherParams)
                {
                    this.otherParams[kv.Key] = kv.Value;
                }
            }
            
            // 设置参数
            if (sentenceMark1 != null)
            {
                otherParams.TryGetValue("data1", out object data1);
                Dictionary<string, Object> otherParams1 = new Dictionary<string, Object>();
                otherParams1.Add("aiAssistantDisplay", data1);
                sentenceMark1.setOtherParams(otherParams1);
            }
            
            // 设置参数
            if (sentenceMark2 != null)
            {
                otherParams.TryGetValue("data2", out object data2);
                Dictionary<string, Object> otherParams2 = new Dictionary<string, Object>();
                otherParams2.Add("aiAssistantDisplay", data2);
                sentenceMark2.setOtherParams(otherParams2);
            }
        }

        public void setSentenceMarkToTwo()
        {
            if (sentenceMark1 != null && sentenceMark2 != null) return;
            int type = 0;
            object tempType = 0;
            otherParams.TryGetValue("type", out tempType);
            int.TryParse((tempType??"0").ToString(), out type);
            if (dtoList != null && dtoList.Count > 1)
            {
                SentenceMarkDto sentenceMarkDto1 = dtoList[0];
                SentenceMarkDto sentenceMarkDto2 = dtoList[1];
                if (!string.IsNullOrEmpty(videoContrast.VideoOneId))
                {
                    // 对比的是录制的视频
                    sentenceMark1 = AiFactory.GetSentenceMark(0, sentenceMarkDto1.videoInfo.VideoId, type, videoTimeOneList, videoTimeTwoList, this.singleMaxNum);
                    sentenceMark2 = AiFactory.GetSentenceMark(0, sentenceMarkDto2.videoInfo.VideoId, type, videoTimeOneList, videoTimeTwoList, this.singleMaxNum);
                }
                else
                {
                    // 对比的是上传的文件
                    sentenceMark1 = AiFactory.GetSentenceMark(1, sentenceMarkDto1.uploadFile.FileId, type, videoTimeOneList, videoTimeTwoList, this.singleMaxNum);
                    sentenceMark2 = AiFactory.GetSentenceMark(1, sentenceMarkDto2.uploadFile.FileId, type, videoTimeOneList, videoTimeTwoList, this.singleMaxNum);
                }
            }
        }

        public override string GetContent()
        {
            if (!string.IsNullOrEmpty(content)) return content;
            int maxNum = singleMaxNum;
            if (singleMaxNum != -1)
            {
                int length = GetTextParamsContent().Length;
                maxNum = (singleMaxNum - length) / 2;
                if (maxNum <= 0) return "";
            }

            // 设置对象
            setSentenceMarkToTwo();
            String str = "";
            if(sentenceMark1 != null)
            {
                sentenceMark1.setSingleMaxNum(maxNum);
                str += "视频1的全文:\n" + sentenceMark1.GetContent();
            }
            if(sentenceMark2 != null)
            {
                if (!string.IsNullOrEmpty(str)) str += "\n\n\n";
                sentenceMark2.setSingleMaxNum(maxNum);
                str += "视频2的全文:\n" + sentenceMark2.GetContent();
            }
            this.content = str;
            return str;
        }

        public override string GetTextParamsContent()
        {
            if (textParamsContent != null) return textParamsContent;
            string str = "";
            if ((videoTimeOneList != null && videoTimeOneList.Count > 0) || (videoTimeTwoList != null && videoTimeTwoList.Count > 0))
            {
                // 对比的是视频
                str += $"\n文本内的参数说明：";
                str += $"\n本段自然时间：指的是转译成的文字段落开始时对应的北京时间。";
                str += $"\n本段开始时间：指的是转译成的文字段落开始时对应的视频播放时间。";
                str += $"\n本段结束时间：指的是转译成的文字段落结束时对应的视频播放时间。";
                str += $"\n本段在线人数：指的是转译成的文字段落开始时对应的直播间在线人数。";
                str += $"\n本段语速：指的是1分钟内说的字数。";
                str += $"\n本段发弹幕人数：指的是转译成的文字段落开始时间到结束时间内直播间发送弹幕数量。";
                str += "\n本段成交数量：指的是转译成的文字段落开始时间到结束时间内商品的成交数量。";
                str += "\n本段互动率：指的是转译成的文字段落开始时间到结束时间内弹幕数量/在线人数得出的互动率。";
                str += "\n本段成交率：指的是转译成的文字段落开始时间到结束时间内成交数量/在线人数得出的成交率。";
                str += "\n本段销售额：指的是转译成的文字段落开始时间到结束时间内的销售额。";
                str += "\n本段UV价值：指的是转译成的文字段落开始时间到结束时间内销售额/在线人数得出的UV价值。";
                str += $"\n从第二段开始直到最后一段，每一段的本段自然时间是上一段的自然结束时间。";
                if (videoTimeOneList != null && videoTimeOneList.Count > 0)
                {
                    long start = videoTimeOneList[0];
                    long end = videoTimeOneList[1];
                    string startStr = DateUtils.DateTimeUTCToString(start, "HH:mm:ss");
                    string sendStr = DateUtils.DateTimeUTCToString(end, "HH:mm:ss");
                    str += $"\n视频1的时间是{startStr}到{sendStr}，共{(end - start) / 1000}秒的直播";
                }
                if(videoTimeTwoList != null && videoTimeTwoList.Count > 0)
                {
                    long start2 = videoTimeTwoList[0];
                    long end2 = videoTimeTwoList[1];
                    string startStr2 = DateUtils.DateTimeUTCToString(start2, "HH:mm:ss");
                    string sendStr2 = DateUtils.DateTimeUTCToString(end2, "HH:mm:ss");
                    str += $"\n视频2的时间是{startStr2}到{sendStr2}，共{(end2 - start2) / 1000}秒的直播";
                }
            }
            str += $"\n\n以下是由两个直播间录屏成视频后转译成文字的直播全文脚本，从第一段到最后一段，每一段的内容都是连续的。";
            textParamsContent = str;
            return textParamsContent;
        }

        public override string GetAllContent()
        {
            return $"{GetTextParamsContent()}\n\n{GetContent()}";
        }

        public override SentenceMarkDto GetMarkDto()
        {
            throw new NotImplementedException();
        }

        public override string GetTradeId()
        {
            throw new NotImplementedException();
        }

        public override string GetTradeName()
        {
            string str = "";

            // 设置对象
            setSentenceMarkToTwo();
            if (sentenceMark1 != null)
            {
                str += $"视频1行业为{sentenceMark1.GetTradeName() ?? "某行业"}";
            }
            if (sentenceMark2 != null)
            {
                if (!string.IsNullOrEmpty(str)) str += ",";
                str += $"视频2行业为{sentenceMark1.GetTradeName() ?? "某行业"}";
            }
            return str;
        }

        public override string GetDataScreenshot()
        {
            string str = "";

            // 设置对象
            setSentenceMarkToTwo();
            if (sentenceMark1 != null)
            {
                string str1 = sentenceMark1.GetDataScreenshot();
                if (!string.IsNullOrEmpty(str1))
                {
                    str += $"视频1直播相关的数据：\n{str1}";
                }
            }
            if (sentenceMark2 != null)
            {
                string str2 = sentenceMark2.GetDataScreenshot();
                if (!string.IsNullOrEmpty(str2))
                {
                    if (!string.IsNullOrEmpty(str)) str += "\n\n";
                    str += $"视频2直播相关的数据：\n{str2}";
                }
            }
            return str;
        }

        public override string GetBoard()
        {
            string str = "";

            // 设置对象
            setSentenceMarkToTwo();
            if (sentenceMark1 != null)
            {
                string str1 = sentenceMark1.GetBoard();
                if (!string.IsNullOrEmpty(str1))
                {
                    str += $"视频1直播相关的数据：\n{str1}";
                }
            }
            if (sentenceMark2 != null)
            {
                string str2 = sentenceMark2.GetBoard();
                if (!string.IsNullOrEmpty(str2))
                {
                    if (!string.IsNullOrEmpty(str)) str += "\n\n";
                    str += $"视频2直播相关的数据：\n{str2}";
                }
            }
            return str;
        }

        public override String setAskQuestion(AskRequestDto dto)
        {
            return CommonSentenceUtils.setAskQuestion(dto, this);
        }

        public override void setSingleMaxNum(int num)
        {
            this.singleMaxNum = num;
        }

        public override string GetPlatform()
        {
            string str1 = "抖音", str2 = "抖音";
            // 设置对象
            setSentenceMarkToTwo();
            if (sentenceMark1 != null)
            {
                str1 = sentenceMark1.GetPlatform();
            }
            if (sentenceMark2 != null)
            {
                str2 = sentenceMark2.GetPlatform();
            }
            if (str1.Equals(str2))
            {
                return "两个视频的平台都是" + str1;
            }
            else
            {
                return $"视频1的平台为{str1},视频2的平台为{str2}";
            }
        }
        
        public override void setSpeed(AskRequestDto dto)
        {
            if (dto.otherObj == null || dto.otherObj.Count == 0)
            {
                return;
            }
            
            // 设置对象
            setSentenceMarkToTwo();
            
            // 设置 sentenceMark1 的语速
            if (sentenceMark1 != null)
            {
                int? speed = sentenceMark1.getSpeed();
                dto.otherObj.TryGetValue("backgroundConfigOne", out object backgroundConfig);
                // 判断是否是 Dictionary<string, object> 类型
                if (backgroundConfig is Dictionary<string, object> backgroundConfigMap)
                {
                    backgroundConfigMap["speechRate"] = speed;
                }
                else if (backgroundConfig is Newtonsoft.Json.Linq.JObject jObj)
                {
                    jObj["speechRate"] = speed;
                }
            }
            
            // 设置 sentenceMark2 的语速
            if (sentenceMark2 != null)
            {
                int? speed = sentenceMark2.getSpeed();
                dto.otherObj.TryGetValue("backgroundConfigTwo", out object backgroundConfig);
                
                // 如果 backgroundConfigTwo 不存在，则从 backgroundConfigOne 复制
                if (backgroundConfig == null)
                {
                    dto.otherObj.TryGetValue("backgroundConfigOne", out object backgroundConfigOne);
                    if (backgroundConfigOne != null)
                    {
                        if (backgroundConfigOne is Dictionary<string, object> dictConfig)
                        {
                            // 深拷贝 Dictionary
                            string json = Newtonsoft.Json.JsonConvert.SerializeObject(dictConfig);
                            backgroundConfig = Newtonsoft.Json.JsonConvert.DeserializeObject<Dictionary<string, object>>(json);
                            dto.otherObj["backgroundConfigTwo"] = backgroundConfig;
                        }
                        else if (backgroundConfigOne is Newtonsoft.Json.Linq.JObject jObjConfig)
                        {
                            // JObject 使用 DeepClone 深拷贝
                            backgroundConfig = jObjConfig.DeepClone();
                            dto.otherObj["backgroundConfigTwo"] = backgroundConfig;
                        }
                    }
                }
                
                // 重新获取 backgroundConfigTwo
                dto.otherObj.TryGetValue("backgroundConfigTwo", out backgroundConfig);
                // 判断是否是 Dictionary<string, object> 类型
                if (backgroundConfig is Dictionary<string, object> backgroundConfigMap)
                {
                    backgroundConfigMap["speechRate"] = speed;
                }
                else if (backgroundConfig is Newtonsoft.Json.Linq.JObject jObj)
                {
                    jObj["speechRate"] = speed;
                }
            }
        }
    }
}
