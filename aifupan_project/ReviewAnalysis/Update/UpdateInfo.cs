using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Update
{
    public class UpdateInfo
    {

        /// <summary>
        /// 版本名称
        /// </summary>
        public string VersionName { get; set; }

        /// <summary>
        /// 更新信息
        /// </summary>
        public string Description { get; set; }

        /// <summary>
        ///更新方式  0手动更新  1强制更新
        /// </summary>
        public int UpdateType { get; set; }

        /// <summary>
        /// 是否更新前端 0否 1是  如果是，就要删除webview的文件夹，重新通过地址加载url  当更新的版本只有前端的时候才会选择
        /// </summary>
        public int UdateFront { get; set; }

        /// <summary>
        /// 版本值（版本编号）
        /// </summary>
        public double VersionNum { get; set; }

        /// <summary>
        /// 上传时间
        /// </summary>
        public string UpLoadTime { get; set; }

        /// <summary>
        /// 文件名称
        /// </summary>
        public string FileName { get;set; } 

        /// <summary>
        /// 0升级包   1最新版本 2 还原版本
        /// </summary>
        public int FileType {  get; set; }  
    }
}
