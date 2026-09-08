using Swan.Formatters;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Websocket.Entity
{
    public class WebsocketSaveEntity
    {
        /// <summary>
        /// 内容
        /// </summary>
        public string content { get; set; }

        /// <summary>
        /// websocket数据类型
        /// </summary>
        public string type;

        /// <summary>
        /// 消息id
        /// </summary>
        public string msgId { get; set; }

        public string ToJsonString()
        {
            return Json.Serialize(this);
        }
    }

    /// <summary>
    /// 存在用户字段的websocke数据
    /// </summary>
    public class AbstractEntity: WebsocketSaveEntity
    {
        /// <summary>
        /// 用户昵称
        /// </summary>
        public string nickName { get; set; }

        /// <summary>
        /// 用户的账号等级
        /// </summary>
        public int level { get; set; }

        /// <summary>
        /// 粉丝团等级
        /// </summary>
        public int fansLevel { get; set; }
    }

    /// <summary>
    /// 用户进入
    /// </summary>
    public class JinRu : AbstractEntity
    {

    }

    /// <summary>
    /// 用户关注
    /// </summary>
    public class GuanZhu : AbstractEntity
    {

    }

    /// <summary>
    /// 弹幕
    /// </summary>
    public class DanMu: AbstractEntity
    {

        public int dataMaxNum = -1;
        public string ToJsonString()
        {
            return Json.Serialize(this);
        }
    }

    /// <summary>
    /// 点赞
    /// </summary>
    public class DianZan : AbstractEntity
    {

    }

    /// <summary>
    /// 粉丝团
    /// </summary>
    public class FensTuan : AbstractEntity
    {

    }

    /// <summary>
    /// 在线人数
    /// </summary>
    public class RenShu : WebsocketSaveEntity
    {

    }

    /// <summary>
    /// 累计观看人数
    /// </summary>
    public class ChangGuan : WebsocketSaveEntity
    {

    }

    /// <summary>
    /// 礼物
    /// </summary>
    public class LiWu : AbstractEntity
    {
        /// <summary>
        /// websocket推流id（有可能会推多次一样的数据过来，用这个做去重）
        /// </summary>
        public string traceId { get; set; }
    }

    public class CuoWu : WebsocketSaveEntity
    {

    }
}
