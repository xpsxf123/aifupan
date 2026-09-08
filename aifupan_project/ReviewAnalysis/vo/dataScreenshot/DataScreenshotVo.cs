using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.dataScreenshot
{
    public class DataScreenshotVo
    {

        /// <summary>
        ///	id
        ///</summary>
        public long id { get; set; }
        /// <summary>
        ///	租户id
        ///</summary>
        public long tenantId { get; set; }
        /// <summary>
        ///	用户id
        ///</summary>
        public long userId { get; set; }
        /// <summary>
        ///	数据截图code
        ///</summary>
        public String screenshotCode { get; set; }
        /// <summary>
        ///	数据类型 0视频，1文件，2对比分析
        ///</summary>
        public int? sourceType { get; set; }
        /// <summary>
        ///	来源id
        ///</summary>
        public String sourceId { get; set; }
        /// <summary>
        ///	图片来源 0：腾讯cos
        ///</summary>
        public int? sourceImagesType { get; set; }
        /// <summary>
        ///	上传的图片地址
        ///</summary>
        public String sourceImagesAddress { get; set; }
        /// <summary>
        ///	ai识别的内容
        ///</summary>
        public String aiContent { get; set; }
        /// <summary>
        ///	状态；0:图片未上传，1：图片已上传，2：ai识别中，3：ai识别完成，4：ai识别失败
        ///</summary>
        public int? screenshotStatus { get; set; }
        /// <summary>
        ///	更新时间
        ///</summary>
        public string updateDate { get; set; }
        /// <summary>
        ///	创建时间
        ///</summary>
        public string createDate { get; set; }
        /// <summary>
        /// 标题
        /// </summary>
        public string title { get; set; }
        /// <summary>
        /// 示例图片地址
        /// </summary>
        public string exampleImgUrl { get; set; }
    }
}
