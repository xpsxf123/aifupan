using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using CefSharp;
using ReviewAnalysis.Ai.impl;
using ReviewAnalysis.Ai.impl.diagnosis;
using ReviewAnalysis.Dto;
using ReviewAnalysis.enums;
using ReviewAnalysis.Utils;
using ReviewAnalysis.Ai.Model;

namespace ReviewAnalysis.Ai
{
    public class AiFactory
    {

        /// <summary>
        /// 获取文章的对象
        /// </summary>
        /// <param name="sourceType">0视频，1文件</param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public static SentenceMark GetSentenceMark(int sourceType, string sourceId, int type, List<long> videoTimeOneList, List<long> videoTimeTwoList, int maxSendNum)
        {
            if (sourceType == 0)
            {
                if (type == 2)
                {
                    return new BarrageSentence(sourceId, maxSendNum, videoTimeOneList);
                }
                else if (type == 3)
                {
                    return new ScreenshotVideoSentence(sourceId, videoTimeOneList);
                }
                else if (type == 4)
                {
                    return new ViewingConfuseSentence(sourceId, videoTimeOneList);
                }
                else if(type == 6)
                {
                    return new VideoSentence(sourceId, videoTimeOneList);
                }
                else
                {
                    return new VideoSentence(sourceId, videoTimeOneList);
                }
            }
            else if(sourceType == 1)
            {
                if (type == 3)
                {
                    return new ScreenshotFileSentence(sourceId, videoTimeOneList);
                }
                else
                {
                    return new FileSentence(sourceId, videoTimeOneList);
                }
            }
            else if(sourceType == 2)
            {
                if (type == 3)
                {
                    return new ScreenshotContrastSentence(sourceId, maxSendNum, videoTimeOneList, videoTimeTwoList);
                }
                else if (type == 4)
                {
                    return new ViewingContrastSentence(sourceId, maxSendNum, videoTimeOneList, videoTimeTwoList);
                }
                else
                {
                    return new SyncContrastSentence(sourceId, maxSendNum, videoTimeOneList, videoTimeTwoList);
                }
            }
            else
            {
                throw new CustomException("来源类型未知", 7006);
            }
        }

        /// <summary>
        /// 获取模型的对象（上下文/简单模式由 Ask 内部根据 token.useModelWay 自行分发）
        /// </summary>
        /// <param name="resourceType">模型厂商 0豆包火山，1通义千问，2DeepSeek</param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public static Ask GetAsk(int resourceType)
        {
            if (resourceType == 0)
            {
                return new ChatCompletionsAsk();
            }
            else if (resourceType == 2)
            {
                return new DeepSeekAsk();
            }
            else
            {
                throw new CustomException("来源类型未知", 7006);
            }
        }

        /// <summary>
        /// 获取AI问答服务实现
        /// </summary>
        /// <param name="resourceType">模型厂商 0豆包火山，2DeepSeek</param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public static IAiChatService GetAiChatService(int resourceType)
        {
            if (resourceType == 0)
            {
                return new DoubaoChatService();
            }
            else if (resourceType == 2)
            {
                return new DeepSeekChatService();
            }
            else
            {
                throw new CustomException("未知厂商类型", 7006);
            }
        }

        /// <summary>
        /// 获取诊断报告助手对应的参数实现
        /// </summary>
        /// <param name="cueType"></param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public static AbstractDiagnosis GetDiagnosis(int cueType)
        {
            AbstractDiagnosis temp = null;
            if (cueType == (int) CueType.运营)
            {
                return new OperationDiagnosis();
            }
            else if(cueType == (int) CueType.弹幕)
            {
                return new BarrageDiagnosis();
            }
            else if (cueType == (int) CueType.数据诊断报告)
            {
                return new OperationDiagnosis();
            }
            else
            {
                throw new CustomException("来源类型未知", 7006);
            }
        }

    }
}
