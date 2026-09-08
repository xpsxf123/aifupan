using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Global;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using ReviewAnalysis.vo.contrast;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Bll
{
    public class VideoContrastBll
    {
        /// <summary>
        /// 创建视频分析对比
        /// </summary>
        /// <param name="videoContrast"></param>
        public void Save(VideoContrast videoContrast)
        {
            videoContrast.ContrastTime = ServerTimeUtils.getCurrentTimeStr();
            videoContrast.UserId = ReplayHttpUtils.UserId;
            videoContrast.TenantId = ReplayHttpUtils.ActiveTenantId;
            videoContrast.ContrastId = Guid.NewGuid().ToString();
            videoContrast.IsShard = 0;
            videoContrast.Save();

            // 同步添加到服务器
            ReplayHttpUtils.SaveContrastToServer(videoContrast);
        }


        /// <summary>
        /// 分享对比数据
        /// </summary>
        /// <param name="contrastId">对比唯一标识</param>
        public string ShareAnalysis(string contrastId)
        {

            VideoContrast videoContrast = new VideoContrast();
            videoContrast.ContrastId = contrastId;
            videoContrast.IsShard = 1;
            videoContrast.ShareUrl = Constant.GetOnlineUrl() + "contrastOnlineAnalysis/" + videoContrast.ContrastId;
            videoContrast.DeleteStatus = -1;

            // 同步到服务器
            ReplayHttpUtils.UpdateContrastToServer(videoContrast);
            
            return videoContrast.ShareUrl;
        }


        /// <summary>
        /// 视频分析发生改变，同步对比数据的修改时间
        /// </summary>
        /// <param name="videoId"></param>
        public static void videoChangeUpdateContrast(string videoId)
        {
            try
            {
                VideoContrast videoContrast = new VideoContrast();
                videoContrast.UserId = ReplayHttpUtils.UserId;
                videoContrast.TenantId = ReplayHttpUtils.ActiveTenantId;
                List<VideoContrast> videoContrasts = videoContrast.GetList();
                if (videoContrasts != null && videoContrasts.Count > 0)
                {
                    List<VideoContrast> resultContrasts = new List<VideoContrast>();
                    foreach (var contrastItem in videoContrasts)
                    {
                        if (videoId.Equals(contrastItem.VideoOneId) || videoId.Equals(contrastItem.VideoTwoId))
                        {
                            resultContrasts.Add(contrastItem);
                        }
                    }
                    if (resultContrasts.Count > 0)
                    {
                        foreach (var item in resultContrasts)
                        {
                            ReplayHttpUtils.UpdateContrastToServer(item);
                        }
                    }
                }
            }catch(Exception e)
            {
                FileUtils.LogAnalysis($"视频分析发生改变，同步对比数据的修改时间错误：{e}");
            }
            
        }

        /// <summary>
        /// 查看云空间分析对比
        /// </summary>
        /// <param name="contrastId">对比id</param>
        public CloudContrastAnalysisVo lockCloudContrast(string contrastId)
        {
            // 从服务器获取视频信息
            CloudContrastInfoVo cloudContrastInfoVo = ReplayHttpUtils.GetServerContrastByContrastId(contrastId);
            if (cloudContrastInfoVo != null)
            {
                cloudContrastInfoVo.UpdateDate = cloudContrastInfoVo.UpdateDate.Replace(":", "").Replace("-", "").Replace(" ", "");

                // 查本地有没有zip
                string filePath = Path.GetFullPath($"analysisCloudData20\\contrast\\{contrastId}\\{contrastId}-{cloudContrastInfoVo.UpdateDate}.zip");

                if (!File.Exists(filePath))
                {
                    // 创建文件夹
                    string folderPath = Path.GetDirectoryName(filePath);
                    if (!Directory.Exists(folderPath))
                    {
                        Directory.CreateDirectory(folderPath);
                    }

                    // 从服务器拉取zip
                    string url = $"{ReplayHttpUtils.BaseUrl}/openapi/v2000/getOnlineContrastAnalysisZip?contrastId={contrastId}";

                    filePath = ReplayHttpUtils.DownloadCloudAnalysisFile(url, filePath);

                }

                if (File.Exists(filePath))
                {
                    // 将zip解压
                    string jsonCentent = ZipUtils.ReadFileFromZip(filePath);
                    if (!string.IsNullOrEmpty(jsonCentent))
                    {
                        CloudContrastAnalysisVo cloudAnalysisVo = JsonConvert.DeserializeObject<CloudContrastAnalysisVo>(jsonCentent);
                        return cloudAnalysisVo;
                    }
                }
            }

            throw new Exception("数据不存在，查看失败，可尝试重新分析");
        }
    }
}
