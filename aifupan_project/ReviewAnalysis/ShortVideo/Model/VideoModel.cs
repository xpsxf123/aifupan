using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using douyin.Utils;
using MediaInfo.Model;
using ReviewAnalysis.api;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.system;
using ReviewAnalysis.vo.video;

namespace ReviewAnalysis.ShortVideo.Model
{
    public class VideoModel
    {
        /// <summary>
        /// 主键ID（雪花ID）
        /// </summary>
        public long id { get; set; }

        /// <summary>
        /// 视频唯一标识
        /// </summary>
        public long? videoId { get; set; }

        /// <summary>
        /// 视频标题 
        /// </summary>
        public string videoTitle { get; set; }

        /// <summary>
        /// 租户ID 
        /// </summary>
        public long? tenantId { get; set; }

        /// <summary>
        /// 用户ID
        /// </summary>
        public long? userId { get; set; }

        /// <summary>
        /// 来源类型：1-短视频URL 2-本地上传 3-搜达人 4-搜爆款
        /// </summary>
        public int? sourceType { get; set; }

        /// <summary>
        /// 来源id（短视频：用户id；搜达人：达人id；搜爆款：爆款搜索id）
        /// </summary>
        public long? sourceId { get; set; }

        /// <summary>
        /// 文案提取状态：0-未提取 1-待处理 2-处理中 3-已完成 4-失败 
        /// </summary>
        public int extractStatus { get; set; }

        /// <summary>
        /// 本地文案提取状态： 0-待处理，1-处理中，2-处理完
        /// </summary>
        public int extractLocalStatus { get; set; }

        /// <summary>
        /// 文案提取时间 
        /// </summary>
        public string extractTime { get; set; }

        /// <summary>
        /// 提取失败原因 
        /// </summary>
        public string extractErrorReason { get; set; }

        /// <summary>
        /// AI分析时间
        /// </summary>
        public string analysisTime { get; set; }

        /// <summary>
        /// 创建时间 
        /// </summary>
        public string createdDate { get; set; }

        /// <summary>
        /// 更新时间 
        /// </summary>
        public string updateDate { get; set; }

        /// <summary>
        /// 视频提取文案
        /// </summary>
        public string extractContent { get; set; }

        /// <summary>
        /// 原文文案内容
        /// </summary>
        public string originalExtractContent { get; set; }


        /// <summary>
        /// 平台视频ID（各平台唯一标识）
        /// </summary>
        public string platformVideoId { get; set; }

        /// <summary>
        /// 平台类型
        /// </summary>
        public int? platformType { get; set; }

        /// <summary>
        /// 平台类型名称
        /// </summary>
        public string platformTypeName { get; set; }

        /// <summary>
        /// 来源名称
        /// </summary>
        public string sourceName { get; set; }

        /// <summary>
        /// 视频文件HASH值（用于关联MongoDB存储） 
        /// </summary>
        public string videoHash { get; set; }

        /// <summary>
        /// 封面图片URL（CDN地址）
        /// </summary>
        public string coverUrl { get; set; }

        /// <summary>
        /// 视频播放URL（支持MP4/HLS格式）
        /// </summary>
        public string videoUrl { get; set; }

        /// <summary>
        /// 本地文件地址
        /// </summary>
        public string localFileUrl { get; set; }

        /// <summary>
        /// 作者标识（user_id 或 influencer_id）
        /// </summary>
        public string authorId { get; set; }

        /// <summary>
        /// 作者昵称/名称 
        /// </summary>
        public string authorName { get; set; }

        /// <summary>
        /// 视频时长（单位：秒）
        /// </summary>
        public long duration { get; set; }


        /// <summary>
        /// 重写Equals方法，对比是否是同一个对象
        /// </summary>
        /// <param name="obj"></param>
        /// <returns></returns>
        public override bool Equals(object obj)
        {
            if (obj == null || GetType() != obj.GetType()) return false;
            return id == ((VideoModel)obj).id && videoHash == ((VideoModel)obj).videoHash;
        }


        public string getBaseVideoPath()
        {
            string result = getPlatformName();

            // 判断是否是本地上传，不是就加上主播名称
            if ((platformType??4) != 4)
            {
                result += "\\" + getAuthorName();
            }

            // 添加年月日
            result += "\\" + SplitCreateDate();

            // 创建多级嵌套文件夹
            Directory.CreateDirectory(result);

            return result;
        }

        /// <summary>
        /// 获取平台名称
        /// </summary>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public string getPlatformName()
        {
            if (!string.IsNullOrEmpty(this.platformTypeName))
            {
                return this.platformTypeName;
            }

            string name = ShortVideoUtils.getPlatformName(platformType);
            this.platformTypeName = name;
            return this.platformTypeName;
        }

        /// <summary>
        /// 获取主播名称
        /// </summary>
        /// <returns></returns>
        public string getAuthorName()
        {
            string name = "";

            if (!string.IsNullOrEmpty(authorName))
            {
                name = WindowsUtils.SanitizeForFolderName(authorName);
            }

            if (string.IsNullOrEmpty(name))
            {
                name = "未知主播";
            }
            return name;
        }

        /// <summary>
        /// 分割创建时间为yyyyMMdd格式
        /// </summary>
        /// <returns></returns>
        private string SplitCreateDate()
        {
            if (!string.IsNullOrEmpty(createdDate))
            {
                string[] parts = null;
                if (createdDate.Contains("T"))
                {
                    parts = createdDate.Split('T');
                }
                else
                {
                    parts = createdDate.Split(' ');
                }
                string datePart = parts[0];
                string[] dateParts = datePart.Split('-');
                string dateStr = string.Join("", dateParts);
                if (!string.IsNullOrEmpty(dateStr))
                {
                    return dateStr;
                }
            }
            return DateTime.Now.ToString("yyyyMMdd");
        }

        /// <summary>
        /// 获取下载的视频存储路径
        /// </summary>
        /// <returns></returns>
        public string getLocalDownloadDepositPath()
        {
            string result = "";


            if ((platformType?? 4) != 4 && !videoUrl.StartsWith("http"))
            {
                result = videoUrl;
            }
            else
            {
                result = getBaseVideoPath();
                result += "\\" + getMp4FileName();

                // 最后添加.mp4
                result += ".mp4";
                result = $"{ShortVideoHandle.savePath}\\{result}";
            }

            return result;
        }

        /// <summary>
        /// 获取MP4的文件名称
        /// </summary>
        /// <returns></returns>
        private string getMp4FileName()
        {
            // 添加title（处理文件名非法字符）
            string safeTitle = string.IsNullOrEmpty(videoTitle) ? $"video" : videoTitle;

            // 移除文件名中的非法字符
            safeTitle = WindowsUtils.SanitizeForFolderName(safeTitle);

            // 限制文件名长度
            if (safeTitle.Length > 50)
            {
                safeTitle = safeTitle.Substring(0, 50);
            }

            if (string.IsNullOrEmpty(safeTitle))
            {
                safeTitle = $"video";
            }

            return $"{safeTitle}_{id}";
        }

        /// <summary>
        /// 获取图片的地址
        /// </summary>
        /// <returns></returns>
        public string getLocalOneImgPath()
        {
            string result = getBaseVideoPath();

            result += $"\\{id}.png";

            return result;
        }

        /// <summary>
        /// 获取分割
        /// </summary>
        /// <returns></returns>
        public string getLocalSplitAudioPath()
        {
            string result = getBaseVideoPath();
            return result + "\\" + id + "_audio";
        }
    }
}
