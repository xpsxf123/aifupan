using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.ai
{
    public class ModelConfigVo
    {
        /// <summary>
        /// ID
        /// </summary>
        public long id { get; set; }

        /// <summary>
        /// 模型编码
        /// </summary>
        public string modelCode { get; set; }

        /// <summary>
        /// 来源类型 0：豆包，1：通义
        /// </summary>
        public int? resourceType { get; set; }

        /// <summary>
        /// 使用方式 0分析内容，1数据截图的视觉理解
        /// </summary>
        public int? useType { get; set; }

        /// <summary>
        /// 模型名称
        /// </summary>
        public string modelName { get; set; }

        /// <summary>
        /// 接口apiKey
        /// </summary>
        public string apiKey { get; set; }

        /// <summary>
        /// 输出数据流大小(kb)
        /// </summary>
        public int? outSize { get; set; }

        /// <summary>
        /// 输入数据流大小(kb)
        /// </summary>
        public int? inputSize { get; set; }

        /// <summary>
        /// 缓存上下文大小(kb)
        /// </summary>
        public int? contextSize { get; set; }

        /// <summary>
        /// 限制使用字数数量
        /// </summary>
        public int? wordsNum { get; set; }

        /// <summary>
        /// 推荐输出字数
        /// </summary>
        public int? outWordNum { get; set; }

        /// <summary>
        /// 排序
        /// </summary>
        public int? sort { get; set; }

        /// <summary>
        /// 描述
        /// </summary>
        public string remarks { get; set; }

        /// <summary>
        /// 创建时间
        /// </summary>
        public DateTime createDate { get; set; }

        /// <summary>
        /// 最后修改时间
        /// </summary>
        public DateTime updateDate { get; set; }

        /// <summary>
        /// ai模型, 给客户端使用
        /// </summary>
        public int? aiModel { get; set; }
    }
}
