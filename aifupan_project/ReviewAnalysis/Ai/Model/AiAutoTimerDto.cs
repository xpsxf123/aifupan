using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using static COSXML.Model.Tag.ListBucketVersions;
using System.Xml.Linq;

namespace ReviewAnalysis.Ai.Model
{
    public class AiAutoTimerDto
    {
        /// <summary>
        /// id
        /// </summary>
        public long id { get; set; }

        /// <summary>
        /// 来源id
        /// </summary>
        public string sourceId {  get; set; }

        /// <summary>
        /// 来源类型 0视频，1文件
        /// </summary>
        public int sourceType { get; set; }

        /// <summary>
        /// 问题id
        /// </summary>
        public string cueWordsId { get; set; }

        /// <summary>
        /// 助手类型
        /// </summary>
        public int cueType { get; set; }

        /// <summary>
        /// 是否自动生成诊断报告
        /// </summary>
        public bool isAuto {  get; set; }

        /// <summary>
        /// 生成状态 0待生成，1生成中，2生成完成，3生成失败
        /// </summary>
        public int status { get; set; }

        /// <summary>
        /// 诊断报告类型 0：内容诊断，1：数据诊断
        /// </summary>
        public int diagnosisType { get; set; } = 0;
        
        /// <summary>
        /// 是否上传数据截图(默认为1)：0不传，1传
        /// </summary>
        public int? uploadScreenshot { get; set; } = 1;

        /// <summary>
        /// 是否上传数据截图(默认为1)：0不传，1传
        /// </summary>
        public int? uploadBoard { get; set; } = 1;

        /// <summary>
        /// 重写Equals方法，对比是否是同一个对象
        /// </summary>
        /// <param name="obj"></param>
        /// <returns></returns>
        public override bool Equals(object obj)
        {
            if (obj == null || GetType() != obj.GetType())return false;
            return id == ((AiAutoTimerDto)obj).id;
        }

    }
}
