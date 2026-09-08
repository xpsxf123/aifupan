using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.Ai.utols;
using ReviewAnalysis.bo.ai;
using ReviewAnalysis.Dto;

namespace ReviewAnalysis.Ai.impl
{
    /// <summary>
    /// 数据截图的数据来源-文件的数据截图
    /// </summary>
    public class ScreenshotFileSentence : FileSentence
    {
        public ScreenshotFileSentence(string sourceId, List<long> videoTimeOneList) : base(sourceId, videoTimeOneList)
        {
            this.sourceId = sourceId;
            this.videoTimeOneList = videoTimeOneList;
        }

        public override String GetContent()
        {
            if (content != null) return this.content;
            this.content = GetDataScreenshot();
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

        public override string setAskQuestion(AskRequestDto askRequestBo)
        {
            return CommonSentenceUtils.setAskScreenshotQuestion(askRequestBo, this);
        }
    }
}
