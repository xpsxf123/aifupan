using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using System.Windows.Forms;
using MediaInfo;
using ReviewAnalysis.bo.shortVideo;
using ReviewAnalysis.vo.shortVideo;
using douyin.Utils;
using ReviewAnalysis.Utils;
using ReviewAnalysis.api;
using ReviewAnalysis.ShortVideo.Enums;
using ReviewAnalysis.ShortVideo;
using ReviewAnalysis.ShortVideo.Model;
using Newtonsoft.Json;
using ReviewAnalysis.ShortVideo.Servier;
using ReviewAnalysis.ShortVideo.DouYin;
using ReviewAnalysis.ShortVideo.DouYin.dto;

namespace ReviewAnalysis.Bll
{
    public class ShortVideoBll
    {

        /// <summary>
        /// 选择上传的本地视频
        /// </summary>
        /// <returns></returns>
        public LocalVideoInfoVo selectLocalVideo()
        {
            try
            {
                // 创建文件选择对话框
                OpenFileDialog openFileDialog = new OpenFileDialog
                {
                    Title = "选择视频文件",
                    Filter = "视频文件|*.mp4;*.avi;*.mov;*.wmv;*.flv;*.mkv;*.ts;*.m4v;*.3gp;*.webm|" +
                            "MP4文件|*.mp4|" +
                            "AVI文件|*.avi|" +
                            "MOV文件|*.mov|" +
                            "所有文件|*.*",
                    FilterIndex = 1,
                    CheckFileExists = true,
                    Multiselect = false // 只能选择一个文件
                };

                // 显示文件选择对话框
                if (openFileDialog.ShowDialog() == DialogResult.OK)
                {
                    string selectedFilePath = openFileDialog.FileName;
                    return GetVideoFileInfo(selectedFilePath);
                }
                else
                {
                    // 用户取消选择
                    return new LocalVideoInfoVo
                    {
                        success = false,
                        errorMessage = "用户取消选择文件"
                    };
                }
            }
            catch (Exception ex)
            {
                return new LocalVideoInfoVo
                {
                    success = false,
                    errorMessage = $"选择文件时发生错误: {ex.Message}"
                };
            }
        }

        /// <summary>
        /// 立即提取文案
        /// </summary>
        /// <param name="localVideoBo"></param>
        /// <exception cref="NotImplementedException"></exception>
        public void immediatelyLocalVideo(ImmediatelyLocalVideoBo localVideoBo)
        {
            string videoPath = null;
            bool isDelete = false;
            VideoUserVideoVo resultVideo = null;
            string errorMessage = null;

            try
            {
                // 参数验证
                if (localVideoBo == null)
                {
                    throw new CustomException("参数不能为空");
                }

                if (string.IsNullOrWhiteSpace(localVideoBo.videoUrl))
                {
                    throw new CustomException("视频URL不能为空");
                }

                // 检查资产
                bool isCh = UserPropertyApi.checkUseProperty("shortVideoNum", -1);

                if (!isCh)
                {
                    throw new CustomException("提取文案条数不足！");
                }

                localVideoBo.platformType = 1;

                // 根据sourceType进行不同的验证
                VideoExtractBo temp = new VideoExtractBo();
                switch (localVideoBo.sourceType)
                {
                    case 1: // 短视频URL
                        //ValidateVideoUrl(localVideoBo.videoUrl);
                        localVideoBo.videoUrl = VideoUrlAnalysisUtils.VideoIdByUrl(localVideoBo.videoUrl);
                        temp = collectDouYinVideoData(localVideoBo);
                        break;
                    case 2: // 本地上传
                            // 把文件复制到下载的文件夹下

                        VideoModel videoModel = new VideoModel()
                        {
                            platformType = 4
                        };
                        string fileSave = $"{ShortVideoHandle.savePath}\\{videoModel.getBaseVideoPath()}";
                        // 移动文件
                        string patn = ShortVideoUtils.CopyFileToFolder(localVideoBo.videoUrl, fileSave);
                        if (string.IsNullOrEmpty(patn))
                        {
                            throw new CustomException("文件复制失败，请检查视频文件");
                        }
                        videoPath = patn;
                        localVideoBo.videoUrl = patn;
                        ValidateLocalVideoPath(localVideoBo.videoUrl);
                        temp = collectLocalVideoData(localVideoBo);
                        break;
                    default:
                        throw new CustomException($"不支持的来源类型: {localVideoBo.sourceType}");
                }

                // 判断视频时长
                ShortVideoUtils.checkDuration(temp.duration);


                VideoExtractBo bo = new VideoExtractBo()
                {
                    videoUrl = localVideoBo.videoUrl,
                    sourceType = localVideoBo.sourceType,
                    videoTitle = temp.videoTitle,
                    duration = temp.duration,
                    coverUrl = temp.coverUrl,
                    extractStatus = (int)ExtractStatus.Pending,
                };
                VideoUserVideoVo videoUserVideoVo = ShortVideoApi.urlOrLocal(bo);
                resultVideo = videoUserVideoVo;
                if (videoUserVideoVo == null)
                {
                    FileUtils.log($"手动提取-urlOrLocal返回null, videoUrl={localVideoBo.videoUrl}, sourceType={localVideoBo.sourceType}", "短视频提取-手动添加");
                }
                else
                {
                    FileUtils.log($"手动提取-urlOrLocal返回: id={videoUserVideoVo.id}, extractStatus={videoUserVideoVo.extractStatus}, videoTitle={videoUserVideoVo.videoTitle}", "短视频提取-手动添加");
                }
                // 判断是否是待提取
                if (videoUserVideoVo != null && (videoUserVideoVo.extractStatus == (int)ExtractStatus.Pending))
                {
                    VideoModel videoModel = JsonConvert.DeserializeObject<VideoModel>(JsonConvert.SerializeObject(videoUserVideoVo));
                    //添加加到队列中
                    ShortVideoHandle.addAll(new List<VideoModel>() { videoModel });
                    FileUtils.log($"手动提取-已加入队列: id={videoModel.id}, videoTitle={videoModel.videoTitle}", "短视频提取-手动添加");
                }
                else if (videoUserVideoVo != null)
                {
                    FileUtils.log($"手动提取-未加入队列(extractStatus={(videoUserVideoVo.extractStatus)}不是Pending): id={videoUserVideoVo.id}", "短视频提取-手动添加");
                }

            }
            catch (Exception ex)
            {
                isDelete = true;
                errorMessage = ex.Message;
                throw ex;
            }
            finally
            {
                if (isDelete && !string.IsNullOrEmpty(videoPath))
                {
                    File.Delete(videoPath);
                }
            }
        }

        /// <summary>
        /// 采集抖音短视频详情，
        /// </summary>
        /// <param name="localVideoBo"></param>
        /// <param name="isImgSave"></param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        private VideoExtractBo collectDouYinVideoData(ImmediatelyLocalVideoBo localVideoBo)
        {
            VideoExtractBo result = new VideoExtractBo();
            DouYinVideoInfo videoData = null;
            try
            {
                ShortVideoService shortVideoService = new ShortVideoDouYinService();
                videoData = shortVideoService.GetVideoDetailAsync(localVideoBo.videoUrl);
            }
            catch (Exception ex)
            {
                FileUtils.LogError(ex.Message, "爬取抖音视频失败");
            }
            if (videoData == null)
            {
                throw new CustomException("抖音短视频链接打开失败，请检查网络或者地址");
            }


            // 封面，先后顺序 dynamicCover -> cover 使用
            string cover = videoData?.video?.cover?.url_list?.FirstOrDefault() ?? "";
            if (string.IsNullOrEmpty(cover))
            {
                throw new CustomException("获取视频封面图片失败");
            }
            //string imgUrl = string.IsNullOrEmpty(dynamicCover) ? cover : dynamicCover;
            //string savePath = $"{ShortVideoHandle.savePath}\\{ShortVideoUtils.getSourceTypeName(localVideoBo.sourceType)}\\封面图片\\{Guid.NewGuid().ToString("N")}.png";
            //bool saveImgFlag = ShortVideoUtils.DownloadImage(imgUrl, savePath);
            //if (!saveImgFlag && File.Exists(savePath))
            //{
            //    throw new CustomException("下载视频封面图片失败，请检查网络");
            //}
            // 把图片上传到oss上
            //string ossKey = ShortVideoUtils.getCoverImgPutUrl(savePath);
            result.coverUrl = cover;


            // 视频时长
            result.duration = videoData.video?.duration ?? 0;

            // 标题
            result.videoTitle = videoData.desc;
            return result;
        }

        /// <summary>
        /// 采集本地视频的详情
        /// </summary>
        /// <param name="localVideoBo"></param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        private VideoExtractBo collectLocalVideoData(ImmediatelyLocalVideoBo localVideoBo)
        {
            VideoExtractBo result = new VideoExtractBo();

            LocalVideoInfoVo videoData = GetVideoFileInfo(localVideoBo.videoUrl);

            if(videoData == null || !videoData.success)
            {
                throw new CustomException(videoData.errorMessage);
            }

            string savePath = $"{ShortVideoHandle.savePath}\\封面图片\\{Guid.NewGuid().ToString("N")}.png";
            if (!ShortVideoUtils.ExtractFirstFrame(ShortVideoHandle.ffmpegPath, localVideoBo.videoUrl, savePath) && !File.Exists(savePath))
            {
                FileUtils.LogError($"异步提取视频首帧失败: localFileUrl = {localVideoBo.videoUrl}", "异步提取视频首帧失败");
                throw new CustomException("获取视频首帧图片失败");
            }
            // 把图片上传到oss上
            string ossKey = ShortVideoUtils.getCoverImgPutUrl(savePath);
            result.coverUrl = ossKey;

            // 视频时长
            int duration = ((int?)videoData.duration) ?? -1;
            if (duration == -1)
            {
                throw new CustomException("获取视频时长失败");
            }
            result.duration = duration;

            // 标题
            result.videoTitle = videoData.fileName;
            return result;
        }

        /// <summary>
        /// 获取视频文件的详细信息
        /// </summary>
        /// <param name="filePath">视频文件路径</param>
        /// <returns>视频文件信息</returns>
        private LocalVideoInfoVo GetVideoFileInfo(string filePath)
        {
            try
            {
                FileInfo fileInfo = new FileInfo(filePath);

                if (!fileInfo.Exists)
                {
                    return new LocalVideoInfoVo
                    {
                        success = false,
                        errorMessage = "文件不存在"
                    };
                }

                var videoInfo = new LocalVideoInfoVo
                {
                    success = true,
                    fileName = fileInfo.Name,
                    filePath = filePath,
                    fileSize = fileInfo.Length,
                    fileSizeFormatted = FormatFileSize(fileInfo.Length),
                    fileExtension = fileInfo.Extension.ToLower(),
                    creationTime = fileInfo.CreationTime,
                    lastWriteTime = fileInfo.LastWriteTime
                };

                // 使用MediaInfo获取视频详细信息（使用超时保护）
                try
                {
                    var mediaInfoTask = Task.Run(() =>
                    {
                        var result = new Dictionary<string, object>();
                        try
                        {
                            using (var mediaInfo = new MediaInfo.MediaInfo())
                            {
                                mediaInfo.Open(filePath);

                            // 获取视频时长
                            string durationString = mediaInfo.Get(StreamKind.General, 0, "Duration");
                            if (!string.IsNullOrEmpty(durationString) && double.TryParse(durationString, out double durationMs))
                            {
                                result["duration"] = durationMs / 1000; // 转换为秒
                            }

                            // 获取视频宽度
                            string widthString = mediaInfo.Get(StreamKind.Video, 0, "Width");
                            if (!string.IsNullOrEmpty(widthString) && int.TryParse(widthString, out int width))
                            {
                                result["width"] = width;
                            }

                            // 获取视频高度
                            string heightString = mediaInfo.Get(StreamKind.Video, 0, "Height");
                            if (!string.IsNullOrEmpty(heightString) && int.TryParse(heightString, out int height))
                            {
                                result["height"] = height;
                            }

                            // 获取帧率
                            string frameRateString = mediaInfo.Get(StreamKind.Video, 0, "FrameRate");
                            if (!string.IsNullOrEmpty(frameRateString) && double.TryParse(frameRateString, out double frameRate))
                            {
                                result["frameRate"] = frameRate;
                            }

                            // 获取比特率
                            string bitRateString = mediaInfo.Get(StreamKind.General, 0, "OverallBitRate");
                            if (!string.IsNullOrEmpty(bitRateString) && long.TryParse(bitRateString, out long bitRate))
                            {
                                result["bitRate"] = bitRate;
                            }

                            // 获取视频编码格式
                            string videoCodec = mediaInfo.Get(StreamKind.Video, 0, "Format");
                            if (!string.IsNullOrEmpty(videoCodec))
                            {
                                result["videoCodec"] = videoCodec;
                            }

                            // 获取音频编码格式
                            string audioCodec = mediaInfo.Get(StreamKind.Audio, 0, "Format");
                            if (!string.IsNullOrEmpty(audioCodec))
                            {
                                result["audioCodec"] = audioCodec;
                            }
                            }
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogAnalysis($"{ex}", $"ShortVideoBll-MediaInfo获取视频信息异常");
                        }
                        return result;
                    });

                    // 设置60秒超时
                    if (mediaInfoTask.Wait(TimeSpan.FromSeconds(60)))
                    {
                        var mediaResult = mediaInfoTask.Result;
                        if (mediaResult.ContainsKey("duration"))
                        {
                            videoInfo.duration = (double)mediaResult["duration"];
                            videoInfo.durationFormatted = FormatDuration(videoInfo.duration.Value);
                        }
                        if (mediaResult.ContainsKey("width"))
                            videoInfo.width = (int)mediaResult["width"];
                        if (mediaResult.ContainsKey("height"))
                            videoInfo.height = (int)mediaResult["height"];
                        if (videoInfo.width.HasValue && videoInfo.height.HasValue)
                            videoInfo.resolution = $"{videoInfo.width}x{videoInfo.height}";
                        if (mediaResult.ContainsKey("frameRate"))
                            videoInfo.frameRate = (double)mediaResult["frameRate"];
                        if (mediaResult.ContainsKey("bitRate"))
                            videoInfo.bitRate = (long)mediaResult["bitRate"];
                        if (mediaResult.ContainsKey("videoCodec"))
                            videoInfo.videoCodec = (string)mediaResult["videoCodec"];
                        if (mediaResult.ContainsKey("audioCodec"))
                            videoInfo.audioCodec = (string)mediaResult["audioCodec"];
                    }
                    else
                    {
                        videoInfo.errorMessage = "MediaInfo打开文件超时";
                    }
                }
                catch (Exception ex)
                {
                    // MediaInfo获取失败，但基本文件信息仍然有效
                    videoInfo.errorMessage = $"获取视频详细信息时发生错误: {ex.Message}";
                }

                return videoInfo;
            }
            catch (Exception ex)
            {
                return new LocalVideoInfoVo
                {
                    success = false,
                    errorMessage = $"获取文件信息时发生错误: {ex.Message}"
                };
            }
        }

        /// <summary>
        /// 格式化文件大小
        /// </summary>
        /// <param name="bytes">字节数</param>
        /// <returns>格式化后的文件大小</returns>
        private string FormatFileSize(long bytes)
        {
            string[] sizes = { "B", "KB", "MB", "GB", "TB" };
            double len = bytes;
            int order = 0;
            while (len >= 1024 && order < sizes.Length - 1)
            {
                order++;
                len = len / 1024;
            }
            return $"{len:0.##} {sizes[order]}";
        }

        /// <summary>
        /// 格式化时长
        /// </summary>
        /// <param name="seconds">秒数</param>
        /// <returns>格式化后的时长 (HH:mm:ss)</returns>
        private string FormatDuration(double seconds)
        {
            TimeSpan timeSpan = TimeSpan.FromSeconds(seconds);
            if (timeSpan.TotalHours >= 1)
            {
                return timeSpan.ToString(@"hh\:mm\:ss");
            }
            else
            {
                return timeSpan.ToString(@"mm\:ss");
            }
        }
        

        /// <summary>
        /// 验证视频URL是否正常
        /// </summary>
        /// <param name="videoUrl">视频URL</param>
        private void ValidateVideoUrl(string videoUrl)
        {
            try
            {
                // 验证URL格式
                if (!Uri.TryCreate(videoUrl, UriKind.Absolute, out Uri uri))
                {
                    throw new CustomException("URL格式不正确");
                }

                // 验证协议
                if (uri.Scheme != "http" && uri.Scheme != "https")
                {
                    throw new CustomException("URL必须是http或https协议");
                }

                FileUtils.log($"URL验证通过: {videoUrl}", "视频url验证", true);
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"URL验证失败: {videoUrl}, 错误: {ex.Message}", "ValidateVideoUrl");
                throw new CustomException($"URL验证失败: {ex.Message}");
            }
        }

        /// <summary>
        /// 验证本地视频文件路径是否正确
        /// </summary>
        /// <param name="localPath">本地文件路径</param>
        private void ValidateLocalVideoPath(string localPath)
        {
            try
            {
                // 检查路径格式
                if (!Path.IsPathRooted(localPath))
                {
                    throw new CustomException("必须提供完整的文件路径");
                }

                // 检查文件是否存在
                if (!File.Exists(localPath))
                {
                    throw new CustomException("文件不存在");
                }

                // 检查文件扩展名
                string extension = Path.GetExtension(localPath).ToLower();
                string[] supportedExtensions = { ".mp4", ".avi", ".mov", ".wmv", ".flv", ".mkv", ".ts", ".m4v", ".3gp", ".webm" };

                if (!supportedExtensions.Contains(extension))
                {
                    throw new CustomException($"不支持的视频格式: {extension}");
                }

                // 检查文件大小
                FileInfo fileInfo = new FileInfo(localPath);
                if (fileInfo.Length == 0)
                {
                    throw new CustomException("文件大小为0，可能是损坏的文件");
                }

                // 可选：检查文件是否可读
                try
                {
                    using (FileStream fs = File.OpenRead(localPath))
                    {
                        // 尝试读取文件头，验证文件是否可访问
                        byte[] buffer = new byte[1024];
                        fs.Read(buffer, 0, buffer.Length);
                    }
                }
                catch (Exception ex)
                {
                    throw new CustomException($"文件无法读取: {ex.Message}");
                }

                FileUtils.log($"本地文件验证通过: {localPath}");
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"本地文件验证失败: {localPath}, 错误: {ex.Message}", "ValidateLocalVideoPath");
                throw new CustomException($"本地文件验证失败: {ex.Message}");
            }
        }

        /// <summary>
        /// 搜达人
        /// </summary>
        /// <param name="bo"></param>
        /// <returns></returns>
        public long? captureInfluencerInfo(InfluencerSearchBo bo)
        {

            bool isCh = UserPropertyApi.checkUseProperty("searchInfluencerNum", -1);

            if (!isCh)
            {
                throw new CustomException("搜达人资源不足！");
            }

            bo.influencerList = captureInfluencer(bo);
            // 上传服务器
            return ShortVideoApi.saveSearchInfluencers(bo);
        }

        /// <summary>
        /// 搜达人
        /// </summary>
        /// <param name="bo"></param>
        /// <returns></returns>
        private List<InfluencerSearchInfoBo> captureInfluencer(InfluencerSearchBo bo)
        {
            // 抓取达人
            DouyinSpider douyinSpider = new DouyinSpider();
            List<JuLiangUserInfoDto> userList = douyinSpider.JuLiangSearchDaren(bo.searchKeyword, 2);

            if (userList == null || userList.Count == 0)
            {
                return null;
            }

            // 判断关键词是否是等于抖音号
            List<JuLiangUserInfoDto> douYinUserInfoDtos = userList.Where(item => bo.searchKeyword.Equals(item.aweme_id)).ToList();

            if (douYinUserInfoDtos != null && douYinUserInfoDtos.Count == 0)
            {
                return null;
            }

            return douYinUserInfoDtos.Select(item =>
            {
                // 采集抖音的主播详情信息
                DouyinUser douYinUser = douyinSpider.UserProfile(item.secUid);
                if (douYinUser == null)
                {
                    throw new CustomException("链接错误，请检查URL链接");
                }
                //bo.searchKeyword = douYinUser.unique_id;
                InfluencerSearchInfoBo temp = new InfluencerSearchInfoBo()
                {
                    platformType = bo.platformType,
                    platformUserId = douYinUser.sec_uid,
                    platformAccount = item.aweme_id,
                    nickname = douYinUser.nickname,
                    avatar = douYinUser?.avatar_thumb?.url_list?[0] ?? null,
                    description = douYinUser.signature,
                    followersCount = douYinUser.follower_count,
                    followingCount = douYinUser.following_count,
                    videoCount = douYinUser.aweme_count,
                    likeCount = douYinUser.total_favorited,
                    verificationStatus = douYinUser.verificationStatus,
                    verificationInfo = douYinUser.verificationInfo
                };
                return temp;
            }).ToList();

        }

        /// <summary>
        /// 获取一个达人的数据
        /// </summary>
        /// <param name="bo"></param>
        /// <returns></returns>
        private InfluencerSearchInfoBo captureInfluencerOne(InfluencerSearchBo bo)
        {
            List<InfluencerSearchInfoBo> result = captureInfluencer(bo);

            if (result == null || result.Count == 0)
            {
                return null;
            }
            return result[0];
        }

        /// <summary>
        /// 同步达人的视频
        /// </summary>
        /// <param name="bo"></param>
        public void syncInfluencerVideo(SyncInfluencerVideoBo bo)
        {
            bo.platformType = 1;

            InfluencerModel model = new InfluencerModel()
            {
                platformUserId = bo.platformUserId,
                platformAccount = bo.platformAccount,
                platformType = bo.platformType ?? 0,
                isEnabled = 0,
                influencerId = bo.influencerId ?? 0
            };

            // 如果actionType是2，就要查询达人是否要自动提取视频
            if (bo.actionType == 2 && bo.influencerId != null)
            {
                List<UserInfluencerSubscriptionVo>  list = ShortVideoApi.influencerSubscriptionList(bo.influencerId);
                if (list != null && list.Count > 0)
                {
                    list = list.Where(item => (item?.videoInfluencerInfoVo?.platformUserId ?? "-1").Equals(bo.platformUserId)).ToList();

                    if (list != null && list.Count > 0)
                    {
                        model.isEnabled = list[0].isEnabled??0;
                        model.likeCountThreshold = list[0].likeCountThreshold;
                        model.updateTimeCondition = list[0].updateTimeCondition??0;
                    }
                }
            }

            InfluencerHandle.syncAndextract(model);
        }

        /// <summary>
        /// 添加订阅达人
        /// </summary>
        /// <param name="bo"></param>
        /// <exception cref="CustomException"></exception>
        public void addInfluencerInfo(CaptureInfluencerInfoBo bo)
        {
            bool isCh = UserPropertyApi.checkUseProperty("subscribeInfluencerNum", -1);

            if (!isCh)
            {
                throw new CustomException("订阅达人数量资源不足！");
            }

            bo.platformType = 1;
            if (bo.tradeId == null)
            {
                throw new CustomException("行业不能为空");
            }
            // 获取资产，判断是否够

            // 抓取达人
            InfluencerSearchBo influencerTemp = new InfluencerSearchBo()
            {
                platformType = bo.platformType ?? 1,
                searchKeyword = bo.searchKeyword
            };
            InfluencerSearchInfoBo influencer = captureInfluencerOne(influencerTemp);
            if (influencer == null)
            {
                throw new CustomException("抖音号未知，请重新输入");
            }
            VideoInfluencerSubscribeBo influencerBo = JsonConvert.DeserializeObject<VideoInfluencerSubscribeBo>(JsonConvert.SerializeObject(influencer));
            influencerBo.industryId = bo.tradeId;
            influencerBo.groupId = bo.groupId;
            bool f = ShortVideoApi.subscribe(influencerBo);

            // 启动一个多线程获取达人对应视频
            Task.Run(()=>
            {
                try
                {
                    InfluencerModel model = new InfluencerModel()
                    {
                        platformUserId = influencerBo.platformUserId,
                        platformAccount = influencerBo.platformAccount,
                        platformType = bo.platformType ?? 0,
                        isEnabled = 0,
                        influencerId = 0
                    };
                    InfluencerHandle.syncAndextract(model);
                }
                catch (Exception e)
                {
                    FileUtils.LogError(e.Message, "同步达人时报错");
                }
            });

        }

        /// <summary>
        /// 搜爆款
        /// </summary>
        /// <param name="temp"></param>
        /// <returns></returns>
        public long? captureHotSearch(InfluencerSearchBo temp)
        {
            if (string.IsNullOrEmpty(temp.searchKeyword))
            {
                throw new CustomException("关键词不能为空");
            }

            bool isCh = UserPropertyApi.checkUseProperty("searchHotVideoNum", -1);

            if (!isCh)
            {
                throw new CustomException("搜爆款资源不足！");
            }

            VideoHotSearchBo param = new VideoHotSearchBo()
            {
                searchKeyword = temp.searchKeyword,
                platformType = temp.platformType
            };
            List<VideoHotSearchInfoBo> videoList = new List<VideoHotSearchInfoBo>();
            // 查询服务器是否有
            List<VideoHotSearchVideoInfoVo> tempVideoList = ShortVideoApi.syncHutSearchKeywords(temp.searchKeyword, temp.platformType);
            if (tempVideoList != null && tempVideoList.Count > 0)
            {
                param.snatchDataType = 1;
                videoList = tempVideoList.Select(item =>
                {
                    return new VideoHotSearchInfoBo()
                    {
                        // 平台类型: 1-抖音, 2-快手, 3-视频号 
                        platformType = item.platformType,
                        // 平台视频ID
                        platformVideoId = item.platformVideoId,
                        // 平台用户ID 
                        platformUserId = item.influencerPlatformUserId,
                        // 视频标题 
                        title = item.title,
                        // 视频文件hash值
                        videoHash = item.videoHash,
                        // 视频描述
                        description = item.description,
                        // 视频封面URL
                        coverUrl = item.coverUrl,
                        // 视频播放URL 
                        videoUrl = item.videoUrl,
                        // 作者ID 
                        authorId = item.authorId,
                        // 作者名称
                        authorName = item.authorName,
                        // 达人头像URL 
                        influencerAvatar = item.influencerAvatar,
                        // 达人粉丝数
                        influencerFollowersCount = item.influencerFollowersCount,
                        // 视频时长(秒)
                        duration = item.duration,
                        // 发布时间 
                        publishTime = item.publishTime,
                        // 点赞数
                        likeCount = item.likeCount,
                        // 评论数
                        commentCount = item.commentCount,
                        // 分享数
                        shareCount = item.shareCount,
                        // 收藏数
                        collectCount = item.collectCount
                    };
                }).ToList();
            }
            else
            {
                param.snatchDataType = 2;
                // 搜爆款
                List<DouYinVideoInfo> list = SearchVideo3.SearchDouyinVideo(temp.searchKeyword);

                if (list != null && list.Count > 0)
                {
                    videoList = list.Select(item =>
                    {
                        return new VideoHotSearchInfoBo()
                        {
                            // 平台类型: 1-抖音, 2-快手, 3-视频号 
                            platformType = temp.platformType,
                            // 平台视频ID
                            platformVideoId = item?.aweme_id ?? "",
                            // 平台用户ID 
                            platformUserId = item?.author?.sec_uid ?? "",
                            // 视频标题 
                            title = item?.desc ?? "",
                            // 视频文件hash值
                            videoHash = item?.video?.play_addr?.file_hash ?? "",
                            // 视频描述
                            description = item?.desc ?? "",
                            // 视频封面URL
                            coverUrl = item?.video?.cover?.url_list?[0] ?? "",
                            // 视频播放URL 
                            videoUrl = $"https://www.douyin.com/video/{item?.aweme_id ?? "-1"}",
                            // 作者ID 
                            authorId = item?.author?.sec_uid ?? "",
                            // 作者名称
                            authorName = item?.author?.nickname ?? "",
                            // 达人头像URL 
                            influencerAvatar = item?.author?.avatar_thumb?.url_list?[0] ?? "",
                            // 达人粉丝数
                            influencerFollowersCount = item?.author?.follower_count ?? 0,
                            // 视频时长(秒)
                            duration = item?.video?.duration ?? 0,
                            // 发布时间 
                            publishTime = (item?.create_time ?? null) != null ? ServerTimeUtils.getTimeStrByTime((long)(item.create_time * 1000)) : null,
                            // 点赞数
                            likeCount = item?.statistics?.digg_count ?? 0,
                            // 评论数
                            commentCount = item?.statistics?.comment_count ?? 0,
                            // 分享数
                            shareCount = item?.statistics?.share_count ?? 0,
                            // 收藏数
                            collectCount = item?.statistics?.collect_count ?? 0
                        };
                    }).ToList();
                }
            }

            // 设置视频列表
            param.videoList = videoList;
            return ShortVideoApi.saveSearchHotVideos(param);
        }

        /// <summary>
        /// 添加订阅爆款
        /// </summary>
        /// <param name="bo"></param>
        /// <exception cref="NotImplementedException"></exception>
        public VideoUserHotSubscriptionVo addHotSearch(VideoHotSubscriptionAddBo bo)
        {
            bool isCh = UserPropertyApi.checkUseProperty("subscribeHotVideoNum", -1);

            if (!isCh)
            {
                throw new CustomException("订阅爆款数量资源不足！");
            }
            VideoUserHotSubscriptionVo vo = ShortVideoApi.addSubscription(bo);
            if (vo != null && vo.id != null)
            {
                // 判断这个提示词下是否有视频，如果没有就同步，有就不用管
                List<VideoHotSearchVideoInfoVo> tempVideoList = ShortVideoApi.syncHutSearchKeywords(vo.keyword, vo.platformType ?? 1);
                if (tempVideoList?.Count > 0)
                {
                    return vo;
                }

                List<HotSearchModel> influencerModels = new List<HotSearchModel>()
                {
                    new HotSearchModel()
                    {
                        hotSearchId = vo.id ?? 0,
                        searchKeyword = bo.keyword,
                        platformType = bo.platformType ?? 1,
                        isEnabled = bo.autoSyncEnabled,
                        likeCountThreshold = bo.likeCountThreshold,
                        updateTimeCondition = bo.updateTimeCondition,
                        status = 0
                    }
                };
                FileUtils.log($"获取要同步的爆款 size = {influencerModels.Count}", "获取要同步的爆款");
                HotSearchHandle.dataQueue.AddAll(influencerModels);
            }
            return vo;
        }

        /// <summary>
        /// 手动更新订阅爆款数据
        /// </summary>
        /// <param name="bo"></param>
        /// <exception cref="NotImplementedException"></exception>
        public async Task updateHotSearchData(VideoHotSubscriptionAddBo bo)
        {
            // 判断这个提示词下是否有视频，如果没有就同步，有就不用管
            List<VideoHotSearchVideoInfoVo> tempVideoList = ShortVideoApi.syncHutSearchKeywords(bo.keyword, bo.platformType ?? 1);
            if (tempVideoList?.Count > 0)
            {
                return;
            }

            HotSearchModel dto = new HotSearchModel()
            {
                searchKeyword = bo.keyword,
                platformType = bo.platformType ?? 1,
                isEnabled = bo.autoSyncEnabled,
                likeCountThreshold = bo.likeCountThreshold,
                updateTimeCondition = bo.updateTimeCondition
            };
           await  HotSearchHandle.syncAndextract(dto);
        }

        /// <summary>
        /// 手动更新订阅爆款数据-异步
        /// </summary>
        /// <param name="bo"></param>
        /// <exception cref="NotImplementedException"></exception>
        public async Task UpdateHotSearchDataAsync(VideoHotSubscriptionAddBo bo)
        {
            // 判断这个提示词下是否有视频，如果没有就同步，有就不用管
            List<VideoHotSearchVideoInfoVo> tempVideoList = ShortVideoApi.syncHutSearchKeywords(bo.keyword, bo.platformType ?? 1);
            if (tempVideoList?.Count > 0)
            {
                return;
            }

            HotSearchModel dto = new HotSearchModel()
            {
                searchKeyword = bo.keyword,
                platformType = bo.platformType ?? 1,
                isEnabled = bo.autoSyncEnabled,
                likeCountThreshold = bo.likeCountThreshold,
                updateTimeCondition = bo.updateTimeCondition
            };
            await HotSearchHandle.SyncAndextractAsync(dto);
        }

        /// <summary>
        /// 重新提取文案
        /// </summary>
        /// <param name="bo"></param>
        /// <exception cref="NotImplementedException"></exception>
        public void reExtract(ReExtractBo bo)
        {
            if (bo.id == null)
            {
                throw new CustomException("视频id不能为空");
            } 
            ShortVideoApi.extractReuse((long)bo.id);

            Task.Run(()=>
            {
                ShortVideoHandle.startProjectInit();
            });
        }
    }
}
