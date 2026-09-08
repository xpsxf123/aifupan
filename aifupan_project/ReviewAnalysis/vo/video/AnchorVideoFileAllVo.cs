using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.Dto;

namespace ReviewAnalysis.vo.video
{
    public class AnchorVideoFileAllVo
    {
        /// <summary>
        /// 来源id
        /// </summary>
        public string sourceId {  get; set; }

        /// <summary>
        /// 来源类型0视频、1文件、2对比分析
        /// </summary>
        public int sourceType { get; set; }
        /// <summary>
        /// 类型，1是自然原文，2是优化原文
        /// </summary>
        public int type { get; set; }
        /// <summary>
        /// 优化原文生成状态 0待生成，1生成中，2生成成功，3生成失败
        /// </summary>
        public int contentStatus { get; set; }
        /// <summary>
        /// 视频信息
        /// </summary>
        public VideoInfoVo videoInfo { get; set; }
        /// <summary>
        /// 文件信息
        /// </summary>
        public UploadFileDto uploadFile { get; set; }
        /// <summary>
        /// 视频或文件内容
        /// </summary>
        public List<VideoContentVo> videoFileContentList { get; set; }
    }
}
