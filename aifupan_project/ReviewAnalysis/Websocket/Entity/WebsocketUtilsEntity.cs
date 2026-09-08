using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.Bll;

namespace ReviewAnalysis.Websocket.Entity
{
    public class WebsocketUtilsEntity
    {

        public class DataWebSocket
        {
            /// <summary>
            /// 视频开始录制时间
            /// </summary>
            public string videoStartTime { get; set; }

            /// <summary>
            /// 版本
            /// </summary>
            public string version { get; set; }

            /// <summary>
            /// 视频结束时间
            /// </summary>
            public string videoEndTime { get; set; }

            /// <summary>
            /// 主播id
            /// </summary>
            public string secUid { get; set; }

            /// <summary>
            /// 服务器开始时间
            /// </summary>
            public string serviceStartTime { get; set; }

            /// <summary>
            /// 服务器结束时间
            /// </summary>
            public string serviceEndTime { get; set; }

            /// <summary>
            /// 用户id
            /// </summary>
            public string userId { get; set; }

            /// <summary>
            /// 场次号
            /// </summary>
            public string batchNumber { get; set; }

            /// <summary>
            /// 视频id
            /// </summary>
            public string videoId { get; set; }

            /// <summary>
            /// 场观
            /// </summary>
            public string observationNum { get; set; }

            /// <summary>
            /// 累计最高观看人数
            /// </summary>
            public string totalOnlineNum { get; set; }

            /// <summary>
            /// 最大在线人数
            /// </summary>
            public string maxRenShu { get; set; }

            /// <summary>
            /// 最小在线人数
            /// </summary>
            public string minRenShu { get; set; }

            /// <summary>
            /// 统计的json数据
            /// </summary>
            public List<WebSocketEntity> datas { get; set; }
        }

        public class YourModel
        {
            public int id { get; set; }
            public long userId { get; set; }
            public string secUid { get; set; }
            public string batchNumber { get; set; }
            public string videoId { get; set; }
            public string startDate { get; set; }
            public string endDate { get; set; }

            // 注意：以下属性被声明为字符串，但根据实际业务逻辑，它们可能是数字类型
            public string totalOnlineNum { get; set; }
            public string totalFanGroupNum { get; set; }
            public string totalBulletChatNum { get; set; }
            public string totalThumbsUpNum { get; set; }
            public string totalGiftNum { get; set; }
            public string totalConcernNum { get; set; }

            //场观
            public string observationNum { get; set; }
            public string webSocketData { get; set; }
        }
        public class WebSocketEntity
        {
            /// <summary>
            /// 自然时间
            /// </summary>
            public string time { get; set; }

            /// <summary>
            /// 视频秒速 单前时间减开始时间
            /// </summary>
            public string videoTime { get; set; }

            /// <summary>
            /// 在线人数
            /// </summary>
            public string renshu { get; set; }

            /// <summary>
            /// 点赞范围统计条数，当前时间到下一个30秒的点赞统计
            /// </summary>
            public string dianzan { get; set; }

            /// <summary>
            /// 关注范围统计条数，当前时间到下一个30秒的关注统计
            /// </summary>
            public string guanzhu { get; set; }

            /// <summary>
            /// 粉丝团范围统计条数，单前时间到下一条30秒的粉丝团统计
            /// </summary>
            public string fensituan { get; set; }

            /// <summary>
            /// 累计观看人数
            /// </summary>
            public string leijiguankanrenshu { get; set; }
        }

        public class TimeWithData
        {
            public DateTime dateTime;
            public string data;
        }

        public class StatisticsJosnData
        {
            public int index { get; set; }

            public string value { get; set; }
        }

    }
}
