using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Ai.impl
{
    /// <summary>
    /// 对比分析的数据-数据看板
    /// </summary>
    internal class ViewingContrastSentence : SyncContrastSentence
    {
        public ViewingContrastSentence(string sourceId, int singleMaxNum, List<long> videoTimeOneList, List<long> videoTimeTwoList) : base(sourceId, singleMaxNum, videoTimeOneList, videoTimeTwoList)
        {
            this.singleMaxNum = singleMaxNum;
            this.videoTimeOneList = videoTimeOneList;
            this.videoTimeTwoList = videoTimeTwoList;
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
            base.setSentenceMarkToTwo();
            String str = "";
            if (sentenceMark1 != null)
            {
                sentenceMark1.setSingleMaxNum(maxNum);
                str += "\n直播间1的" + sentenceMark1.GetContent();
            }
            if (sentenceMark2 != null)
            {
                if (!string.IsNullOrEmpty(str)) str += "\n\n\n";
                sentenceMark2.setSingleMaxNum(maxNum);
                str += "\n直播间2的" + sentenceMark2.GetContent();
            }
            this.content = str;
            return str;
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