using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Ai.impl
{
    /// <summary>
    /// 对比分析的数据-数据截图
    /// </summary>
    public class ScreenshotContrastSentence : SyncContrastSentence
    {
        public ScreenshotContrastSentence(string sourceId, int singleMaxNum, List<long> videoTimeOneList, List<long> videoTimeTwoList) : base(sourceId, singleMaxNum, videoTimeOneList, videoTimeTwoList)
        {
            this.singleMaxNum = singleMaxNum;
            this.videoTimeOneList = videoTimeOneList;
            this.videoTimeTwoList = videoTimeTwoList;
        }

        public override String GetTextParamsContent()
        {
            return "";
        }
        public override String GetAllContent()
        {
            return GetContent();
        }
    }
}
