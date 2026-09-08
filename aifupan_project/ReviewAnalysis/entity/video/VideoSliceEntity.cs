using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.entity.video
{
    public class VideoSliceEntity
    {

        /// <summary>
        /// ID
        /// </summary>
        public string id { get; set; }

        /// <summary>
        /// 用户id
        /// </summary>
        public string userId { get; set; }

        /// <summary>
        /// 租户id
        /// </summary>
        public long? tenantId { get; set; }

        /// <summary>
        /// 来源id，视频id/文件id
        /// </summary>
        public string sourceId { get; set; }

        /// <summary>
        /// 来源类型 0：视频 1：文件
        /// </summary>
        public int? sourceType { get; set; }

        /// <summary>
        /// 切片视频所属原视频id
        /// </summary>
        public string sourceParentId { get; set; }

        /// <summary>
        /// 切片类型 0：复盘切片 1：短视频切片
        /// </summary>
        public int? sliceType { get; set; }

        /// <summary>
        /// 切片分类，取字典值
        /// </summary>
        public string sliceClass { get; set; }

        /// <summary>
        /// 切片开始时间-毫秒
        /// </summary>
        public long? startMillisecond { get; set; }

        /// <summary>
        /// 切片开始时间 hh:mm:ss.fff
        /// </summary>
        public string startTime { get; set; }

        /// <summary>
        /// 切片结束时间-毫秒
        /// </summary>
        public long? endMillisecond { get; set; }

        /// <summary>
        /// 切片结束时间 mm:ss
        /// </summary>
        public string endTime { get; set; }

        /// <summary>
        /// 切片备注
        /// </summary>
        public string remarks { get; set; }
        /// <summary>
        /// 是否自动上传云空间 0：否 1：是
        /// </summary>
        public int? isAutoUploadCloud { get; set; }
        /// <summary>
        /// 路径保存类型 0：保存在原视频文件夹 1：自定义文件夹
        /// </summary>
        public int? savePathType { get; set; }
        /// <summary>
        /// 切片视频名称
        /// </summary>
        public string sliceVideoName { get; set; }
        /// <summary>
        /// 保存路径
        /// </summary>
        public string savePath { get; set; }
        /// <summary>
        /// 切片时间类型 0：视频时间 1：北京时间
        /// </summary>
        public int? sliceTimeType { get; set; }
    }
}
