using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Model
{
    public class Message
    {

        /// <summary>
        /// 主键
        /// </summary>
        public int Id { get; set; }

        /// <summary>
        /// 是否推送  0否  1是
        /// </summary>
        public int IsPush { get; set; }


        /// <summary>
        /// 开播/下播事件  0下播，1开播
        /// </summary>
        public int LiveEvent { get; set; }

        /// <summary>
        /// 录制开始事件  0未开始录制，1开始录制
        /// </summary>
        public int RecordStartEvent { get; set; }

        /// <summary>
        /// 录制完成事件  0 未完成，1已经完成
        /// </summary>
        public int RecordEndEvent { get; set; }

        /// <summary>
        /// webhook地址  
        /// </summary>
        public string WebHookUrl { get; set; }

        /// <summary>
        /// 消息接收者  
        /// </summary>
        public string MessageReceive { get; set; }
    }
}
