using ReviewAnalysis.Model;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Dto
{
    public class VideoContrastDto
    {
        /// <summary>
        /// 文件一信息
        /// </summary>
        public UploadFile fileInfoOne { get; set; }
        /// <summary>
        /// 文件二信息
        /// </summary>
        public UploadFile fileInfoTwo { get; set; }
        /// <summary>
        /// 主播一信息
        /// </summary>
        public AnchorInfo anchorInfoOne { get; set; }
        /// <summary>
        /// 主播二信息
        /// </summary>
        public AnchorInfo anchorInfoTwo { get; set; }
        /// <summary>
        /// 视频一信息
        /// </summary>
        public AnchorVideo anchorVideoOne { get; set; }
        /// <summary>
        /// 视频二信息
        /// </summary>
        public AnchorVideo anchorVideoTwo { get; set; }
        /// <summary>
        /// 主键
        /// </summary>
        public int Id { get; set; }

        /// <summary>
        /// 对比唯一标识uuid
        /// </summary>
        public string ContrastId { get; set; }
        /// <summary>
        /// 是否已分享 0：否 1：是
        /// </summary>
        public int IsShard { get; set; }
        /// <summary>
        /// 在线对比复盘的地址
        /// </summary>
        public string ShareUrl { get; set; }

        /// <summary>
        /// 对比视频一Id
        /// </summary>
        public string VideoOneId { get; set; }

        /// <summary>
        /// 对比视频二Id
        /// </summary>
        public string VideoTwoId { get; set; }

        /// <summary>
        /// 对比主播一Id
        /// </summary>
        public string AnchorOneId { get; set; }

        /// <summary>
        /// 对比主播二Id
        /// </summary>
        public string AnchorTwoId { get; set; }

        /// <summary>
        /// 对比时间
        /// </summary>
        public string ContrastTime { get; set; }

        /// <summary>
        /// 对比文件一Id
        /// </summary>
        public string FileOneId { get; set; }

        /// <summary>
        /// 对比文件二Id
        /// </summary>
        public string FileTwoId { get; set; }
        /// <summary>
        /// 用户id
        /// </summary>
        public string UserId { get; set; }
        /// <summary>
        /// 删除状态 0：未删除 1：已从对比复盘列表删除 2：已从对比复盘列表和云空间删除
        /// </summary>
        public int DeleteStatus { get; set; }
    }
}
