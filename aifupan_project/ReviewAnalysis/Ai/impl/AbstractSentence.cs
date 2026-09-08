using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using System.Web.UI.WebControls;
using Newtonsoft.Json;
using PdfSharp.Charting;
using ReviewAnalysis.Ai.Model;
using ReviewAnalysis.api;
using ReviewAnalysis.Bll;
using ReviewAnalysis.Dto;
using ReviewAnalysis.juliang;
using ReviewAnalysis.juliang.entity;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.dataScreenshot;
using ReviewAnalysis.vo.system;
using douyin.Utils;

namespace ReviewAnalysis.Ai.impl
{
    public abstract class AbstractSentence : SentenceMark
    {
        public Dictionary<string, object> otherParams = new Dictionary<string, object>();
        public string sourceId = null;
        public int sourceType = 0;
        public List<long> videoTimeOneList;
        public List<long> videoTimeTwoList;
        public string dataScreenshot = null;
        public string board = null;

        public abstract string GetAllContent();
        public abstract string GetContent();
        public virtual string GetDataScreenshot()
        {
            if (!string.IsNullOrEmpty(dataScreenshot)) return dataScreenshot;
            string str = "";
            List<DataScreenshotVo> list = DataScreenshotApi.getExistDataScreenshotList(sourceType, sourceId);
            if (list == null && list.Count == 0) return str;
            for (int i = 0; i < list.Count; i++)
            {
                var item = list[i];
                str += $"数据{i + 1}、\n{item.aiContent}\n";
            }
            this.dataScreenshot = str;
            return str;
        }

        public virtual string GetBoard()
        {
            if (board != null) return this.board;
            this.board = AiRelatedApi.getVideoDataViewingStr(sourceId);
            return this.board;
        }
        public abstract SentenceMarkDto GetMarkDto();
        public abstract string GetTextParamsContent();
        public abstract string GetTradeId();
        public abstract string GetTradeName();

        public virtual string GetPlatform()
        {
            string platformType = getPlatformValue();
            string platformTypeStr = "抖音";
            if (!string.IsNullOrEmpty(platformType))
            {
                DictDataListVo dictDataListVo = SystemApi.dictDataByValue("replay_platform_type", platformType);
                if (dictDataListVo != null)
                {
                    platformTypeStr = dictDataListVo.label;
                }
            }
            return platformTypeStr;
        }

        public virtual string getPlatformValue()
        {
            return "1";
        }

        public abstract string setAskQuestion(AskRequestDto dto);

        public virtual void setOtherParams(Dictionary<string, object> otherParams)
        {
            if (otherParams != null)
            {
                foreach (var kv in otherParams)
                {
                    this.otherParams[kv.Key] = kv.Value;
                }
            }
        }

        public abstract void setSingleMaxNum(int num);

        public virtual int? getSpeed()
        {
            return null;
        }

        public virtual void setSpeed(AskRequestDto dto)
        {
            int? speed = getSpeed();
            if (speed == null) return;
            
            if (dto.otherObj != null)
            {
                if (dto.otherObj.TryGetValue("backgroundConfigOne", out dynamic backgroundConfigOne))
                {
                    // 设置语速
                    backgroundConfigOne.speechRate = speed;
                }
            }
        }

        /// <summary>
        /// 获取去掉标点的内容
        /// </summary>
        /// <param name="content"></param>
        /// <returns></returns>
        public string removePunctuation(string content)
        {
            if (content == null) return "";
            return Regex.Replace(content, @"[，。？、]", "");
        }

        /// <summary>
        /// 根据时间获取巨量对应的数据
        /// </summary>
        /// <param name="videoId"></param>
        /// <param name="list"></param>
        /// <returns></returns>
        public Dictionary<long, JuliangStatisticsDto> getJuliangData(string platform, string videoId, List<AudioaAlysesStatisticsDto> list)
        {
            Dictionary<long, JuliangStatisticsDto> res = new Dictionary<long, JuliangStatisticsDto>();
            if (string.IsNullOrEmpty(videoId) || list == null || list.Count == 0) return res;

            // 获取巨量的实时数据
            List<JuliangRealTimeDataEntity> juliangList = DashboardBll.GetDashboardRealTimeDataList(platform, videoId);
            if (juliangList == null || juliangList.Count == 0) return res;

            // 排序 + 修复非单调字段
            juliangList.Sort((a, b) => a.gatherTimeStamp.CompareTo(b.gatherTimeStamp));
            juliangList = FixNonMonotonic(juliangList);

            long prevEnd = list[0].startVideoTime;
            List<JuliangStatisticsDto> result = new List<JuliangStatisticsDto>();

            for (int i = 0; i < list.Count; i++)
            {
                var audioaAlysesDto = list[i];
                long windowStart = audioaAlysesDto.startVideoTime;
                long windowEnd = audioaAlysesDto.endVideoTime;

                List<JuliangRealTimeDataEntity> bucket = new List<JuliangRealTimeDataEntity>();
                var baseline = FindLastNotAfter(juliangList, prevEnd);
                if (baseline != null) bucket.Add(baseline);

                foreach (var item in juliangList)
                {
                    if (item.gatherTimeStamp > prevEnd && item.gatherTimeStamp <= windowEnd)
                        bucket.Add(item);
                }

                result.Add(BuildStatisticsDto(bucket, windowStart));
                prevEnd = windowEnd;
            }

            if (result.Count > 0)
            {
                return result.GroupBy(x => x.startVideoTime, x => x)
                .ToDictionary(x => x.Key, x => x.First());
            }
            return res;
        }

        private List<JuliangRealTimeDataEntity> FixNonMonotonic(List<JuliangRealTimeDataEntity> sorted)
        {
            if (sorted.Count <= 1) return sorted;
            var fixed_ = new List<JuliangRealTimeDataEntity> { sorted[0] };
            for (int i = 1; i < sorted.Count; i++)
            {
                var cur = sorted[i];
                var prev = fixed_[i - 1];
                fixed_.Add(new JuliangRealTimeDataEntity
                {
                    gatherTimeStamp = cur.gatherTimeStamp,
                    gatherDateTime = cur.gatherDateTime,
                    payComboCnt = FixField(cur.payComboCnt, prev.payComboCnt),
                    payAmt = FixField(cur.payAmt, prev.payAmt),
                    fansClubJoinUcnt = FixField(cur.fansClubJoinUcnt, prev.fansClubJoinUcnt),
                    followAnchorUcnt = FixField(cur.followAnchorUcnt, prev.followAnchorUcnt),
                    watchNum = FixField(cur.watchNum, prev.watchNum),
                    qianchuanCost = FixFieldDecimal(cur.qianchuanCost, prev.qianchuanCost),
                    refundAmt = FixFieldDecimal(cur.refundAmt, prev.refundAmt),
                    totalRoi = FixFieldDecimal(cur.totalRoi, prev.totalRoi)
                });
            }
            return fixed_;
        }

        private int FixField(int cur, int prev) => cur < prev ? prev : cur;
        private decimal FixFieldDecimal(decimal cur, decimal prev) => cur < prev ? prev : cur;

        private JuliangRealTimeDataEntity FindLastNotAfter(List<JuliangRealTimeDataEntity> sorted, long target)
        {
            JuliangRealTimeDataEntity last = null;
            foreach (var item in sorted)
            {
                if (item.gatherTimeStamp <= target)
                    last = item;
                else
                    break;
            }
            return last;
        }

        private JuliangStatisticsDto BuildStatisticsDto(List<JuliangRealTimeDataEntity> bucket, long windowStart)
        {
            var dto = new JuliangStatisticsDto { startVideoTime = windowStart };
            if (bucket.Count == 0) return dto;

            dto.payComboCntMin = bucket.Min(x => x.payComboCnt);
            dto.payComboCntMax = bucket.Max(x => x.payComboCnt);
            dto.rangePayComboCnt = Math.Max(0, dto.payComboCntMax - dto.payComboCntMin);

            dto.payAmtMin = bucket.Min(x => x.payAmt);
            dto.payAmtMax = bucket.Max(x => x.payAmt);
            dto.rangePayAmt = Math.Max(0, dto.payAmtMax - dto.payAmtMin);

            dto.fansClubJoinUcntMin = bucket.Min(x => x.fansClubJoinUcnt);
            dto.fansClubJoinUcntMax = bucket.Max(x => x.fansClubJoinUcnt);
            dto.rangeFansClubJoinUcnt = Math.Max(0, dto.fansClubJoinUcntMax - dto.fansClubJoinUcntMin);

            dto.followAnchorUcntMin = bucket.Min(x => x.followAnchorUcnt);
            dto.followAnchorUcntMax = bucket.Max(x => x.followAnchorUcnt);
            dto.rangeFollowAnchorUcnt = Math.Max(0, dto.followAnchorUcntMax - dto.followAnchorUcntMin);

            dto.watchNumMin = bucket.Min(x => x.watchNum);
            dto.watchNumMax = bucket.Max(x => x.watchNum);
            dto.rangeWatchNum = Math.Max(0, dto.watchNumMax - dto.watchNumMin);

            dto.qianchuanCostMin = bucket.Min(x => x.qianchuanCost);
            dto.qianchuanCostMax = bucket.Max(x => x.qianchuanCost);
            dto.rangeQianchuanCost = dto.qianchuanCostMax - dto.qianchuanCostMin;
            if (dto.rangeQianchuanCost < 0) dto.rangeQianchuanCost = 0;

            dto.refundAmtMin = bucket.Min(x => x.refundAmt);
            dto.refundAmtMax = bucket.Max(x => x.refundAmt);
            dto.rangeRefundAmt = dto.refundAmtMax - dto.refundAmtMin;
            if (dto.rangeRefundAmt < 0) dto.rangeRefundAmt = 0;

            dto.totalRoiMin = bucket.Min(x => x.totalRoi);
            dto.totalRoiMax = bucket.Max(x => x.totalRoi);
            return dto;
        }
    }
}
