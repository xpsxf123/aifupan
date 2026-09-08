using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Net.WebSockets;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.Websocket.Entity
{
    /// <summary>
    /// 
    /// </summary>
    public class WebsocketEntity
    {
        /// <summary>
        /// 主播url
        /// </summary>
        public string url { get; set; }

        /// <summary>
        /// 直播场次号
        /// </summary>
        public string batchNumber { get; set; }

        /// <summary>
        /// 直播的唯一标识
        /// </summary>
        public string secUid { get; set; }

        /// <summary>
        /// 视频id
        /// </summary>
        public string videoId { get; set; }

        /// <summary>
        /// url类型，DouYinLive抖音直播，KuaiShouLive快手直播，RedeLightLive小红书直播
        /// </summary>
        public string type { get; set; }

        /// <summary>
        /// 链接websocket时的参数
        /// </summary>
        public string ttwid { get; set; }

        /// <summary>
        /// websocket地址
        /// </summary>
        public string wssUrl { get; set; }

        /// <summary>
        /// 状态 0待检查，1检测成功是开播的, 2已关闭
        /// </summary>
        public int status { get; set; } = 0;

        /// <summary>
        /// 获取websocket地址的方式，0服务器，1代码计算，2浏览器 -- 暂时无用
        /// </summary>
        public int socketAddressType { get; set; } = 2;

        /// <summary>
        /// websocket地址的方式  0 js，1浏览器
        /// </summary>
        public int gatherWay { get; set; } = 0;

        /// <summary>
        /// websocket地址获取的状态，0待开始，1websocket地址获取成功,2不需要获取
        /// </summary>
        public int webAddressStatus { get; set; } = 0;

        /// <summary>
        /// websocket链接状态, 0待开始，1链接中，2链接成功，3链接失败，4链接断开
        /// </summary>
        public int websocketLinkStatus { get; set; } = 0;

        /// <summary>
        /// websocket链接失败次数
        /// </summary>
        public int websocketLinkErroeNum { get; set; } = 0;

        /// <summary>
        /// Websocket没有获取到数据的次数
        /// </summary>
        public int websocketNoDataNum { get; set; } = 0;

        /// <summary>
        /// 是否使用代理
        /// </summary>
        public bool isProxy { get; set; } = false;

        /// <summary>
        /// 代理URL
        /// </summary>
        public string proxyHttpUrl { get; set; } = "";

        /// <summary>
        /// websocket运行的任务id，不一样后要停止当前task，
        /// </summary>
        public string websocketTaskId { get; set; }

        /// <summary>
        /// 是否保弹幕  0不保存，1保存
        /// </summary>
        public int hasDanMu { get; set; } = 0;

        /// <summary>
        /// 保存的弹幕数量
        /// </summary>
        public int danMuNum { get; set; } = -1;

        /// <summary>
        /// 最大的弹幕报错数量
        /// </summary>
        public int danMuMaxNum { get; set; } = 5000;

        /// <summary>
        /// websocket采集的数据存储位置
        /// </summary>
        public string fileSavePath { get; set; }

        /// <summary>
        /// 副程序在的process（使用ChildProcessInfo避免句柄继承问题）
        /// </summary>
        public WebSocketProcessUtils.ChildProcessInfo process;
    }

    /// <summary>
    /// 保存数据的实体类
    /// </summary>
    public class WebsocketDataEntity
    {
        /// <summary>
        /// 直播的url
        /// </summary>
        public string url { get; set; }
        /// <summary>
        /// 主播的唯一id
        /// </summary>
        public string secUid { get; set; }
        /// <summary>
        /// 视频场次id
        /// </summary>
        public string batchNumber { get; set; }
        /// <summary>
        /// 视频id
        /// </summary>
        public string videoId { get; set; }
        /// <summary>
        /// 时间
        /// </summary>
        public string pushDate { get; set; }
        /// <summary>
        /// websocket采集的数据存储位置
        /// </summary>
        public string fileSavePath { get; set; }
        /// <summary>
        /// 消息方式
        /// </summary>
        public string messageType { get; set; }
        /// <summary>
        /// 数据
        /// </summary>
        public WebsocketSaveEntity data { get; set; }
    }
}
