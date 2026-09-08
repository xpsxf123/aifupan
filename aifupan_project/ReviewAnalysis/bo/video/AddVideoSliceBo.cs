using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.video
{
    public class AddVideoSliceBo
    {
        /// <summary>
        /// 原视频id
        /// </summary>
        public string videoId { get; set; }
        /// <summary>
        /// 切片视频名称
        /// </summary>
        public string videoName { get; set; }
        /// <summary>
        /// 切片视频保存地址
        /// </summary>
        public string savePath { get; set; }
        /// <summary>
        /// 切片类型 0：复盘切片 1：短视频切片
        /// </summary>
        public int? sliceType { get; set; }
        /// <summary>
        /// 切片分类，取字典值
        /// </summary>
        public string sliceClass { get; set; }
        /// <summary>
        /// 截取的开始时间戳（毫秒）
        /// </summary>
        public long startTimeMs { get; set; }
        /// <summary>
        /// 截取的结束时间戳（毫秒）
        /// </summary>
        public long endTimeMs { get; set; }
        /// <summary>
        /// 是否自动上传云空间 0：否 1：是
        /// </summary>
        public int? isAutoUploadCloud { get; set; }
        /// <summary>
        /// 0：保存在原视频文件夹 1：自定义文件夹
        /// </summary>
        public int? savePathType { get; set; }
        /// <summary>
        /// 备注
        /// </summary>
        public string remarks { get; set; }
        /// <summary>
        /// 切片时间类型 0：视频时间 1：北京时间
        /// </summary>
        public int? sliceTimeType { get; set; }
    }
}
