using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Threading.Tasks;
using System.Web;
using douyin.Utils;
using MediaInfo;
using ReviewAnalysis.api;
using ReviewAnalysis.ShortVideo.DouYin;
using ReviewAnalysis.upload;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using ReviewAnalysis.vo.system;

namespace ReviewAnalysis.ShortVideo
{
    public class ShortVideoUtils
    {

        // 存储服务器kv的值
        public static volatile Dictionary<string, string> dictValues = new Dictionary<string, string>();


        /// <summary>
        /// 在url中获取modal_id参数
        /// </summary>
        /// <param name="url"></param>
        /// <returns></returns>
        public static string getModalId(string url)
        {
            var uri = new Uri(url);
            var queryParams = HttpUtility.ParseQueryString(uri.Query);
            string modal_id = queryParams["modal_id"];

            if (!string.IsNullOrEmpty(modal_id))
            {
                return modal_id;
            }

            var patterns = new[]
            {
                @"(?:https?://)?(?:www\.)?douyin\.com/video/(\d+)"
            };

            string urlTemp = VideoUrlAnalysisUtils.Extract(patterns, url);

            if (string.IsNullOrEmpty(urlTemp))
            {
                return null;
            }

            if (urlTemp.EndsWith("/"))
            {
                urlTemp = urlTemp.Substring(0, urlTemp.Length - 1);
            }

            string[] split = urlTemp.Split('/');

            return split[split.Length - 1];

        }

        /// <summary>
        /// 通过key获取value
        /// 1、查询dictValues中有对应的key，有就返回
        /// 2、返回就查询服务器 SystemApi.getSystenKvByKey(key)，在把服务器返回的值设置到dictValues中
        /// </summary>
        /// <param name="key"></param>
        /// <returns></returns>
        public static string getKvByKey(string key)
        {
            SystemKvVo d = SystemApi.getSystenKvByKey(key);

            return "";
        }

        /// <summary>
        /// 判断
        /// </summary>
        /// <param name="currentTime">当前</param>
        /// <param name="objTime"></param>
        /// <param name="type">自动提取文案作品更新时间阈值: 0-全部 1-近一周, 2-近半个月, 3-近一个月, 4-近3个月 5-近6个月 </param>
        /// <returns></returns>
        public static bool isUpdateTimeCondition(long currentTime, long objTime, int type)
        {
            if (type == 0)
            {
                return true;
            }
            long now = (currentTime - objTime) / 1000;

            if (type == 1 && now <= (60 * 60 * 24 * 7))
            {
                return true;
            }
            else if (type == 2 && now <= (60 * 60 * 24 * 15))
            {
                return true;
            }
            else if (type == 3 && now <= (60 * 60 * 24 * 30))
            {
                return true;
            }
            else if (type == 4 && now <= (60 * 60 * 24 * 90))
            {
                return true;
            }
            else if (type == 5 && now <= (60 * 60 * 24 * 180))
            {
                return true;
            }
            return false;
        }

        /// <summary>
        /// 复制文件到目标文件夹，若目标文件夹中已存在同名文件，则生成唯一的文件名。
        /// </summary>
        /// <param name="sourcePath">源文件路径</param>
        /// <param name="destinationFolder">目标文件夹路径</param>
        /// <returns>返回复制是否成功的布尔值</returns>
        public static string CopyFileToFolder(string sourcePath, string destinationFolder)
        {
            try
            {
                // 检查源文件是否存在
                if (!File.Exists(sourcePath))
                {
                    throw new CustomException("源文件不存在！");
                }

                // 获取文件名和扩展名
                string fileName = Path.GetFileName(sourcePath);
                string destinationPath = Path.Combine(destinationFolder, fileName);

                // 如果目标文件夹中已存在文件，则调用 GetUniqueFilePath 生成唯一文件名
                destinationPath = GetUniqueFilePath(destinationPath);

                string directory = Path.GetDirectoryName(destinationPath);
                Directory.CreateDirectory(directory);

                // 复制文件
                File.Copy(sourcePath, destinationPath);

                FileUtils.log($"文件已成功从 {sourcePath} 复制到 {destinationPath}");
                return destinationPath;
            }
            catch (Exception ex)
            {
                // 捕获异常并返回失败
                FileUtils.LogError($"复制文件时发生异常: {ex.Message}", "复制文件时发生异常");
                return null;
            }
        }

        /// <summary>
        /// 获取一个不存在的文件路径，如果文件已存在，则在文件名中加上一个数字后缀（如: file_1, file_2）。
        /// </summary>
        /// <param name="filePath">原始文件路径</param>
        /// <returns>返回一个不存在的文件路径</returns>
        public static string GetUniqueFilePath(string filePath)
        {
            // 如果文件不存在，直接返回原路径
            if (!File.Exists(filePath))
            {
                return filePath;
            }

            // 获取文件的目录、文件名和扩展名
            string directory = Path.GetDirectoryName(filePath);
            string fileNameWithoutExtension = Path.GetFileNameWithoutExtension(filePath);
            string extension = Path.GetExtension(filePath);

            int counter = 1;
            string newFilePath;

            // 循环检查文件是否存在，如果存在，则给文件名添加后缀
            do
            {
                newFilePath = Path.Combine(directory, $"{fileNameWithoutExtension}({counter}){extension}");
                counter++;
            } while (File.Exists(newFilePath));

            return newFilePath;
        }

        /// <summary>
        /// 提取视频的首帧（同步操作）
        /// </summary>
        /// <param name="videoPath">视频文件路径</param>
        /// <param name="outputPath">输出图片路径（默认同目录下生成）</param>
        /// <returns>true表示成功，false表示失败</returns>
        public static bool ExtractFirstFrame(string ffmpegPath, string videoPath, string outputPath = null)
        {
            if (!File.Exists(videoPath))
                throw new FileNotFoundException("视频文件不存在", videoPath);

            // 如果图片存在就删除图片
            if (File.Exists(outputPath))
                File.Delete(outputPath);

            try
            {
                string directory = Path.GetDirectoryName(outputPath);
                Directory.CreateDirectory(directory);

                string arguments = $"-y -i \"{videoPath}\" -ss 00:00:00 -vframes 1 -q:v 2 \"{outputPath}\"";

                ProcessStartInfo startInfo = new ProcessStartInfo(ffmpegPath, arguments)
                {
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    CreateNoWindow = true
                };

                using (Process process = new Process())
                {
                    process.StartInfo = startInfo;

                    FileUtils.log($"开始执行FFmpeg提取首帧: {arguments}");

                    process.Start();

                    // 同步读取输出和错误流
                    Task<string> errorTask = process.StandardError.ReadToEndAsync();
                    process.WaitForExit(); // 等待FFmpeg进程结束

                    string error = errorTask.Result;

                    if (process.ExitCode == 0 && File.Exists(outputPath))
                    {
                        FileUtils.log($"FFmpeg提取首帧成功: {outputPath}");
                        return true;
                    }
                    else
                    {
                        FileUtils.LogError($"FFmpeg提取首帧失败，退出码: {process.ExitCode}, 错误信息: {error}", "ExtractFirstFrame");
                        return false;
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"FFmpeg提取首帧异常: {ex.Message}", "ExtractFirstFrame");
                return false;
            }
        }


        /// <summary>
        /// 封面图片上传到oss中
        /// </summary>
        /// <param name="imgPath"></param>
        /// <returns></returns>
        public static string getCoverImgPutUrl(string imgPath)
        {
            // 把图片上传到oss上
            try
            {
                // 获取不带点的扩展名
                string extension = Path.GetExtension(imgPath);
                string extensionWithoutDot = Path.GetExtension(imgPath).TrimStart('.');
                // 获取预上传url
                SignUploadUrlVo signUploadUrlVo = ShortVideoApi.getCoverImgPutUrl(extensionWithoutDot);

                if (signUploadUrlVo != null)
                {
                    bool uploadFlag = UploadUtils.UploadFileAsync(signUploadUrlVo.signedUrl, imgPath);
                    if (uploadFlag)
                    {
                        return signUploadUrlVo.ossKey;
                    }
                }
                else
                {
                    FileUtils.LogError($"imgPath = {imgPath}", "获取封面图片上传的预签名链接失败");
                }
            }
            catch (Exception e)
            {
                FileUtils.LogError($"{e.Message}, {e.StackTrace}", "封面图片上传报错");
            }
            finally
            {
                // 删除zip
                File.Delete(imgPath);
            }
            return null;
        }

        /// <summary>
        /// 获取视频的时长-校验视频超过10分钟就不提取
        /// </summary>
        /// <param name="model"></param>
        public static void checkDuration(long currentDuration)
        {
            int duration = ShortVideoUtils.getShortVideoDurationMax();
            if (duration > 0 && duration < currentDuration)
            {
                throw new CustomException($"视频播放时长超过{duration}秒");
            }
        }

        /// <summary>
        /// 下载图片到指定的文件路径（同步）
        /// </summary>
        /// <param name="imageUrl">图片的URL</param>
        /// <param name="savePath">下载后保存的文件路径</param>
        /// <returns>返回一个表示下载是否成功的布尔值</returns>
        public static bool DownloadImage(string imageUrl, string savePath)
        {
            try
            {
                // 创建HttpClient实例
                using (var client = new HttpClient())
                {
                    // 发送GET请求获取图片流
                    var response = client.GetAsync(imageUrl).Result; // 同步获取响应

                    if (response.IsSuccessStatusCode)
                    {
                        // 获取图片的字节流
                        var imageBytes = response.Content.ReadAsByteArrayAsync().Result; // 同步读取字节流

                        string directory = Path.GetDirectoryName(savePath);
                        Directory.CreateDirectory(directory);

                        // 将字节流写入到指定的文件路径
                        File.WriteAllBytes(savePath, imageBytes);

                        return true; // 下载成功
                    }
                    else
                    {
                        return false; // 请求失败
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError(ex.Message, "下载文件错误");
                return false; // 异常发生，返回失败
            }
        }

        /// <summary>
        /// 删除文件夹中的所有文件和子文件夹，但不删除本身文件夹
        /// </summary>
        /// <param name="path">目标文件夹路径</param>
        public static void DeletedDirectory(string path)
        {
            if (!Directory.Exists(path))
            {
                FileUtils.log($"文件夹不存在: {path}");
                return;
            }

            try
            {
                // 删除文件
                string[] files = Directory.GetFiles(path);
                foreach (string file in files)
                {
                    File.SetAttributes(file, FileAttributes.Normal); // 确保可删除
                    File.Delete(file);
                }

                // 删除子文件夹
                string[] directories = Directory.GetDirectories(path);
                foreach (string dir in directories)
                {
                    Directory.Delete(dir, true); // true = 递归删除子文件夹和文件
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"删除目录失败: {ex.Message}");
            }
        }

        /// <summary>
        /// 获取平台名称
        /// </summary>
        /// <param name="platformType"></param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public static string getPlatformName(int? platformType = 4)
        {
            // 平台类型: 1-抖音, 2-快手, 3-视频号, 4-本地上传
            if (platformType == null)
            {
                return "未知平台";
            }
            else if (platformType == 1)
            {
                return "抖音";
            }
            else if (platformType == 2)
            {
                return "快手";
            }
            else if (platformType == 3)
            {
                return "视频号";
            }
            else if (platformType == 4)
            {
                return "本地上传";
            }
            return "未知平台";
        }


        /// <summary>
        /// 服务器获取短视频最大可提取时长(单位：秒)
        /// </summary>
        /// <returns></returns>
        public static int getShortVideoDurationMax()
        {
            return KvHelper.GetIntKvByKey("short_video_duration_max", -1);
        }

        /// <summary>
        /// 格式化失败内容
        /// </summary>
        /// <param name="desc"></param>
        /// <returns></returns>
        public static string formatDesc(string desc)
        {
            if (string.IsNullOrEmpty(desc))
            {
                return "无";
            }

            if (desc.Length >= 495)
            {
                return desc.Substring(0, 495);
            }
            return desc;
        }

        /// <summary>
        /// 格式化视频时间
        /// </summary>
        /// <param name="duration"></param>
        /// <returns></returns>
        public static long formatDuration(long? duration)
        {

            // 如果输入为null或小于等于0，返回0，避免异常和无效计算。
            if (!duration.HasValue || duration.Value <= 0)
            {
                return 0L;
            }
            return (duration > ((long)(duration / 1000) * 1000)) ? (long)(duration / 1000) + 1 : (long)(duration / 1000);
        }


    }
}
