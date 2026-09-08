using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;
using System.ComponentModel.DataAnnotations;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Asr
{
    public class VersionUpdateVo
    {
        /// <summary>
        /// 版本号
        /// </summary>
        public string VersionNum { get; set; }

        /// <summary>
        /// 更新时间
        /// </summary>
        public string UpdateTime { get; set; }

        /// <summary>
        /// 更新类型 0：爱复盘软件补丁包 1：更新软件补丁包
        /// </summary>
        public int IsFront { get; set; }

        /// <summary>
        /// 是否强制更新 0否 1是
        /// </summary>
        public int UpdateType { get; set; }

        /// <summary>
        /// 版本编号 数字越大，版本越新
        /// </summary>
        public double Version { get; set; }

        /// <summary>
        /// 文件的md5
        /// </summary>
        public string FileMd5 { get; set; }

        /// <summary>
        /// 更新文件的下载地址列表
        /// </summary>
        public List<string> FileDownLoadUrls { get; set; }

        /// <summary>
        /// 更新描述
        /// </summary>
        public string UpdateInfo { get; set; }
    }
}
