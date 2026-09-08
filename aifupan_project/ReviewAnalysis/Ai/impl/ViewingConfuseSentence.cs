using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.Ai.utols;
using ReviewAnalysis.api;
using ReviewAnalysis.bo.ai;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.ai;

namespace ReviewAnalysis.Ai.impl
{
    /// <summary>
    /// 数据看板的数据来源-视频的数据看板
    /// </summary>
    public class ViewingConfuseSentence : VideoSentence
    {
        public ViewingConfuseSentence(string sourceId, List<long> videoTimeOneList) : base(sourceId, videoTimeOneList)
        {
            this.sourceId = sourceId;
            this.videoTimeOneList = videoTimeOneList;
        }

        public override String GetContent()
        {
            if (content != null) return this.content;
            string str = GetBoard() ;

            if (string.IsNullOrEmpty(str))
            {
                throw new CustomException("看板数据为空不能发起ai提问", 7005);
            }
            this.content = str;
            return this.content;
        }

        public override String GetTextParamsContent()
        {
            return "";
        }

        public override String GetAllContent()
        {
            return $"全场直播相关数据全文：\n{GetContent()}";
        }

        public override String setAskQuestion(AskRequestDto askRequest)
        {
            return CommonSentenceUtils.setAskScreenshotQuestion(askRequest, this);
        }
    }
}
