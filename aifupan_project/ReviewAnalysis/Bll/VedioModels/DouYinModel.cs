
using System.Collections.Generic;

namespace ReviewAnalysis.Bll.VedioModels
{
    public class DouYinModel
    {
        /// <summary>
        /// 主播Id
        /// </summary>
        public string AnchorId { get; set; }

        /// <summary>
        /// 主播在平台的唯一标识
        /// </summary>
        public string SecUid { get; set; }

        /// <summary>
        /// 主播名称
        /// </summary>
        public string AnchorName { get; set; }

        /// <summary>
        /// 主播头像缩略图
        /// </summary>
        public string AnchorThump { get; set; }

        /// <summary>
        /// 直播间Id
        /// </summary>
        public string RoomId { get; set; }

        /// <summary>
        /// 直播状态 2直播中，4未开播
        /// </summary>
        public int LiveStatus { get; set; }

        /// <summary>
        /// 直播标题
        /// </summary>
        public string RoomTitle { get; set; }

        /// <summary>
        /// 当前观看直播的简略人数  如5000+  是一个字符串类型
        /// </summary>
        public string BriefNumber { get; set; }

        /// <summary>
        /// 直播封面图片地址
        /// </summary>
        public string RoomConverUrl { get; set; }


        /// <summary>
        /// 直播间的访问总量（从开播到当前的总人数） 字符串类型如：10W+
        /// </summary>
        public string TotalVisits { get; set; }

        /// <summary>
        /// 直播间当前的访问人数（准确数字，如5364）
        /// </summary>
        public string CurrentNumber { get; set; }

        /// <summary>
        /// 录屏需要用到的Cookie信息
        /// </summary>
        public string Ttwid { get; set; }

        /// <summary>
        /// 录播需要用到的Cookie信息
        /// </summary>
        public string AcNonce { get; set; }

        /// <summary>
        /// 推流信息列表
        /// </summary>
        public List<StreamInfo> StreamInfos { get; set; }

        /// <summary>
        /// 直播拉流信息
        /// </summary>
        public class StreamInfo
        {

            /// <summary>
            /// 拉流地址
            /// </summary>
            public string StreaUrl { get; set; }

            /// <summary>
            /// 清晰度  0 标清  1高清 2超清 3蓝光
            /// </summary>
            public int Quality { get; set; }

            /// <summary>
            /// 拉流直播源类型 0 m3u8 1 flv
            /// </summary>
            public int LiveSource { get; set; }

            /// <summary>
            /// 是否为默认（原画）地址 0否 1是
            /// </summary>
            public int IsDefault { get; set; }
        }
    }
}
