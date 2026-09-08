using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.uploadFile
{
    public class RenameFileBo
    {
        /// <summary>
        /// 文件Id
        /// </summary>
        public string fileId { get; set; }
        /// <summary>
        /// 新文件名称（不含扩展名）
        /// </summary>
        public string newFileName { get; set; }
    }
}
