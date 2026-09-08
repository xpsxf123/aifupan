using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.bo;

namespace ReviewAnalysis.vo.anchor
{
    public class AiAnchorInfoVo : BasicSettingsBo
    {
        /// <summary>
        /// 主播唯一标识
        /// </summary>
        public string secUid {  get; set; }

        /// <summary>
        /// 视频id
        /// </summary>
        public string videoId { get; set; }

        /// <summary>
        /// 是否更新
        /// </summary>
        public bool? isUpdate { get; set; }
        
        /// <summary>
        /// 来源id
        /// </summary>
        public string sourceId { get; set; }
        
        /// <summary>
        /// 来源类型 0：主播，1：视频，2：文件，
        /// </summary>
        public int? sourceType { get; set; }
    }
}
