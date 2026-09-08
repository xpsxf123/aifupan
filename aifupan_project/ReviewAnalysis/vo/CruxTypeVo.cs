using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo
{
    public class CruxTypeVo
    {
        /// <summary>
        /// ID
        /// </summary>
        public string id { get; set; }

        /// <summary>
        /// 关键词类型名称
        /// </summary>
        public string name { get; set; }

        /// <summary>
        /// 层级，从1开始
        /// </summary>
        public int? level { get; set; }

        /// <summary>
        /// 排序
        /// </summary>
        public int? sort { get; set; }
        /// <summary>
        /// tab排序
        /// </summary>
        public int? tabSort { get; set; }

        /// <summary>
        /// 父id
        /// </summary>
        public string parentId { get; set; }

        /// <summary>
        /// 是否在数据罗盘展示 0：否 1：是
        /// </summary>
        public int? isShowCompass { get; set; }

        /// <summary>
        /// 是否统计到关键词总数 0：否 1：是
        /// </summary>
        public int? isCount { get; set; }

        /// <summary>
        /// 描述
        /// </summary>
        public string remarks { get; set; }

        /// <summary>
        /// 创建时间
        /// </summary>
        public string createDate { get; set; }

        /// <summary>
        /// 最后修改时间
        /// </summary>
        public string updateDate { get; set; }
    }
}
