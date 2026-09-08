using douyin.Utils;
using MediaInfo;
using Newtonsoft.Json;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.bo.analysis;
using ReviewAnalysis.bo.sliceVideo;
using ReviewAnalysis.bo.uploadFile;
using ReviewAnalysis.bo.video;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Dto;
using ReviewAnalysis.entity.uploadFile;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.Global;
using ReviewAnalysis.HttpServer;
using ReviewAnalysis.Model;
using ReviewAnalysis.ShortVideo.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using ReviewAnalysis.vo.trade;
using ReviewAnalysis.vo.uploadFile;
using ReviewAnalysis.vo.video;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using ReviewAnalysis.vo.common;
using Ude;
using static System.Windows.Forms.VisualStyles.VisualStyleElement;

namespace ReviewAnalysis.Bll
{
    public class UploadFileBll
    {

        public async void Analysis(UploadFileEntity uploadFile, string token, string tradeId)
        {

            try
            {
                // 设置文件的行业
                await UploadFileApi.UpdateFileTrade(uploadFile.fileId, tradeId);
                uploadFile.tradeId = long.Parse(tradeId);

                // 分析
                if (uploadFile.fileType == 0 || uploadFile.fileType == 1)
                {
                    // 文件是视频或音频
                    await AnalysisVideoOrAudio(uploadFile, token, uploadFile.platformType, tradeId);
                }
                else if (uploadFile.fileType == 2)
                {
                    // 文件是文本
                    await AnalysisText(uploadFile, token, uploadFile.platformType, tradeId);
                }

            }
            catch (Exception e)
            {
                UploadFileApi.UpdateFileAnalysisStatusSync(uploadFile.fileId, 3, "网络不佳");
                FileUtils.LogAnalysis($"{e}", $"文件分析失败==={uploadFile.fileId}");
            }

            AnchorVideoBll.isAnalysis = false;
        }

        /// <summary>
        /// 保存选择上传的文件
        /// </summary>
        public void Save(CommitUploadFileBo commitUploadFileBo)
        {
           
            // 拷贝文件
            string filePath = CopeFile(commitUploadFileBo.filePath);
            BasicSettingsBaseDto dto = JsonConvert.DeserializeObject<BasicSettingsBaseDto>(JsonConvert.SerializeObject(commitUploadFileBo));
            // 保存文件信息
            SaveUploadFile(commitUploadFileBo.filePath, filePath, commitUploadFileBo.platformType,commitUploadFileBo.engSerViceType, dto);
        }


        /// <summary>
        /// 文件分析失败，清理数据
        /// </summary>
        /// <param name="uploadFile">文件信息</param>
        private void ClearFileData(UploadFileEntity uploadFile)
        {
            if(uploadFile != null)
            {

                // 删除关联的分析数据
                string filePath = Path.GetFullPath($"analysisData\\file\\{uploadFile.uploadTime.Substring(0, 10)}\\{uploadFile.fileId}.txt");
                if(File.Exists(filePath))
                {
                    File.Delete(filePath);
                }

                // 删除mp4文件
                string videoPath = uploadFile.nowPath.Substring(0, uploadFile.nowPath.LastIndexOf("\\"));
                videoPath += "\\mp4\\";
                videoPath += uploadFile.fileName + ".mp4";
                if (File.Exists(videoPath))
                {
                    File.Delete(videoPath);
                }

                // 删除音频文件
                string audioDirectoryPath = uploadFile.nowPath.Substring(0, uploadFile.nowPath.LastIndexOf("\\"));
                audioDirectoryPath += "\\audio\\";
                audioDirectoryPath += uploadFile.nowPath.Substring(uploadFile.nowPath.LastIndexOf("\\"));
                if (Directory.Exists(audioDirectoryPath))
                {
                    Directory.Delete(audioDirectoryPath, true);
                }
            }
            

        }

        /// <summary>
        /// 分析文本文件
        /// </summary>
        /// <param name="uploadFile">文件信息</param>
        /// <param name="token">向服务器请求的token</param>
        private async Task AnalysisText(UploadFileEntity uploadFile, string token, int platformType, string tradeId)
        {

            // 读取文本内容
            string content = File.ReadAllText(uploadFile.nowPath);

            // 将文本内容传到服务器，进行关键词/敏感词识别
            AnalysisResultVo analysisResultVo = await WordApi.WordsMarkByText(token, platformType, uploadFile.fileId, content, tradeId, 1);
            if (analysisResultVo != null)
            {
                // 将分析数据存到本地文件
                AnalysisUtils.saveFileLocalAnalysisData(analysisResultVo, tradeId, uploadFile);

                // 将文件分析状态改成分析完成
                UploadFileApi.UpdateFileAnalysisStatusSync(uploadFile.fileId, 2, "");

                // 消耗字数
                int wordNum = content.Length;
                UserPropertyHttpUtils.UpdateUserProperty("textTaggingWordCount", wordNum);
            }
            else
            {
                // 将文件分析状态改成失败，原因：关键词/敏感词识别失败
                UploadFileApi.UpdateFileAnalysisStatusSync(uploadFile.fileId, 3, "关键词识别失败");
            }
        }

        /// <summary>
        /// 分析视频或音频文件
        /// </summary>
        /// <param name="uploadFile">文件信息</param>
        /// <param name="token">向服务器请求的token</param>
        /// <param name="platformType">平台类型 0：全平台 1：抖音 2：快手 3：视频号 4：小红书</param>
        /// <param name="tradeId">行业id</param>
        private async Task AnalysisVideoOrAudio(UploadFileEntity uploadFile, string token, int platformType, string tradeId)
        {

            string audioDirectoryPath = "";
            try
            {
                // 将视频转成Mp4
                string mp4Path = "";
                if (uploadFile.fileType == 0)
                {
                    mp4Path = VideoUtils.ConvertToMP4(uploadFile.nowPath, uploadFile.fileName);

                    // 检查ts文件和mp4文件时长是否一致（仅用于检测录制网络异常日志）
                    int checkDuration = VideoUtils.CkeckTsAndMp4Consistent(uploadFile.nowPath, mp4Path);
                    FileUtils.LogAnalysis($"{uploadFile.fileId}, checkDuration={checkDuration}", $"文件分析-检查时长一致性完成");
                }
                else if (uploadFile.fileType == 1)
                {
                    mp4Path = uploadFile.nowPath;
                }

                // 始终使用MP4文件的实际时长纠正文件时长
                try
                {
                    int actualDuration = VideoUtils.getVideoDuration(mp4Path);
                    if (actualDuration > 0)
                    {
                        uploadFile.fileDuration = actualDuration;
                        uploadFile.analysisStatus = 1;
                        await UploadFileApi.SaveOrUpdateFile(uploadFile);
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogAnalysis($"{ex}", $"文件分析-纠正文件时长发生异常，继续后续流程");
                }

                // 切割音频
                audioDirectoryPath = uploadFile.nowPath.Substring(0, uploadFile.nowPath.LastIndexOf("\\"));
                audioDirectoryPath += "\\audio\\";
                audioDirectoryPath += uploadFile.fileName;
                AudioUtils.SlicingAudio(mp4Path, audioDirectoryPath);
            }
            catch (Exception ex)
            {
                UploadFileApi.UpdateFileAnalysisStatusSync(uploadFile.fileId, 3, "音频文件异常");
                FileUtils.LogAnalysis($"{ex}", $"切割视频成音频发生异常");
                return;
            }

            // 保存音频信息到数据库
            //SaveAudio(audioDirectoryPath, uploadFile);

            try
            {
                // 将音频文件夹传入ai分析接口
                Dictionary<string, object> result = await AsrUtils.AsrByDirectoryPath(audioDirectoryPath, ReplayHttpUtils.Token, uploadFile.engSerViceType, AsrConsumerType.UserUpload);
                string asrError = await AnalysisUtils.checkAsrError(result);
                if (!string.IsNullOrEmpty(asrError))
                {
                    await UploadFileApi.UpdateFileAnalysisStatus(uploadFile.fileId, 3, asrError);
                    return;
                }

                List<ASRResultEntity> asrResultEntities = (List<ASRResultEntity>)result["data"];
                if(asrResultEntities == null || asrResultEntities.Count < 1)
                {
                    await UploadFileApi.UpdateFileAnalysisStatus(uploadFile.fileId, 3, "网络不佳");
                    return;
                }

                // 将分析结果传到服务器，进行关键词/敏感词识别
                AnalysisResultVo analysisResultVo = await WordApi.WordsMark(platformType, uploadFile.fileId, asrResultEntities, tradeId, 1);
                if (analysisResultVo == null)
                {
                    await UploadFileApi.UpdateFileAnalysisStatus(uploadFile.fileId, 3, "网络不佳");
                    return;
                }

                // 将分析数据存到本地文件
                AnalysisUtils.saveFileLocalAnalysisData(analysisResultVo, tradeId, uploadFile);

                // 扣除分析时长
                UserPropertyHttpUtils.UpdateUserProperty("aiAnalysisTime", Convert.ToInt32(uploadFile.fileDuration / 60 < 1 ? 1 : uploadFile.fileDuration / 60));

                // 将文件分析状态改成分析完成
                await UploadFileApi.UpdateFileAnalysisStatus(uploadFile.fileId, 2, "");
            }
            catch(Exception e)
            {
                await UploadFileApi.UpdateFileAnalysisStatus(uploadFile.fileId, 3, "网络不佳");
                FileUtils.LogAnalysis($"{e}", $"分析文件发生异常");
            }
            
        }

        /// <summary>
        /// 将词语分析结果存到数据库
        /// </summary>
        /// <param name="item">词语分析结果</param>
        /// <param name="status">识别状态  0：成功 1：失败</param>
        /// <param name="tradeId">行业id</param>
        public void SaveSentenceMarkToDb(SentenceMarkVo item, int status, string tradeId)
        {
            try
            {
                UploadFileAlysis uploadFileAlysis = new UploadFileAlysis();
                uploadFileAlysis.Paragraph = item.CurrentSort;
                uploadFileAlysis.FileId = item.VideoId;
                uploadFileAlysis.Status = status;
                uploadFileAlysis.DataJson = item == null ? "" : JsonConvert.SerializeObject(item);
                uploadFileAlysis.TradeId = tradeId;
                uploadFileAlysis.save();
            } catch (Exception ex)
            {
                FileUtils.LogAnalysis($"将词语分析结果存到数据库异常===={JsonConvert.SerializeObject(item)}====={status}");
                throw new Exception();
            }
        }

        /// <summary>
        /// 保存音频信息到数据库
        /// </summary>
        /// <param name="audioDirectoryPath">音频文件夹</param>
        /// <param name="uploadFile">文件信息</param>
        /// <returns></returns>
        private void SaveAudio(string audioDirectoryPath, UploadFile uploadFile)
        {
            try
            {
                string[] audioPaths = Directory.GetFiles(audioDirectoryPath);
                if (audioPaths != null && audioPaths.Length > 0)
                {
                    for (int i = 0; i < audioPaths.Length; i++)
                    {
                        UploadFileAudio audio = new UploadFileAudio();
                        audio.AudioName = audioPaths[i].Substring(audioPaths[i].LastIndexOf("\\") + 1);
                        audio.FileId = uploadFile.FileId;
                        audio.Paragraph = i + 1;
                        audio.AudioType = "mp3";
                        audio.save();
                    }
                }
                else
                {
                    // 将文件分析状态改成失败，原因：不存在音频数据
                    uploadFile.UpdateAnalysisStatus(uploadFile.FileId, 3, "不存在音频数据");
                    throw new Exception();
                }
            }
            catch (Exception ex)
            {
                // 将文件分析状态改成失败，原因：音频数据异常
                uploadFile.UpdateAnalysisStatus(uploadFile.FileId, 3, "音频数据异常");
                FileUtils.LogAnalysis($"保存音频信息到数据库异常, {audioDirectoryPath}, {JsonConvert.SerializeObject(uploadFile)}, {ex}");
                throw new Exception();
            }
        }

        /// <summary>
        /// 保存文件信息到数据库
        /// </summary>
        /// <param name="sourceFilePath">文件原路径</param>
        /// <param name="filePath">文件新保存的路径</param>
        /// <param name="platformType">平台类型 0：全平台 1：抖音 2：快手 3：视频号 4：小红书</param>
        /// <param name="engSerViceType">一句话识别引擎模型，如：16k_zh</param>
        /// <param name="basicSettings">基础配置</param>
        /// <returns></returns>
        private UploadFileEntity SaveUploadFile(string sourceFilePath, string filePath, int platformType, string engSerViceType, BasicSettingsBaseDto basicSettings)
        {
            UploadFileEntity uploadFile = new UploadFileEntity();

            uploadFile.fileName = filePath.Substring(filePath.LastIndexOf("\\") + 1);
            if (uploadFile.fileName.ToLower().EndsWith(".mp3") || uploadFile.fileName.ToLower().EndsWith(".acc"))
            {
                uploadFile.fileType = 1;
            }
            else if (uploadFile.fileName.ToLower().EndsWith(".txt")
                  || uploadFile.fileName.ToLower().EndsWith(".doc")
                  || uploadFile.fileName.ToLower().EndsWith(".docx"))
            {
                // Word 文件转换成文本并将路径替换成文本文件的路径。
                if (uploadFile.fileName.ToLower().EndsWith(".doc")
                 || uploadFile.fileName.ToLower().EndsWith(".docx"))
                {
                    filePath = ConvertToTextFile(filePath);
                }

                uploadFile.fileType = 2;
            }
            else
            {
                uploadFile.fileType = 0;
            }

            uploadFile.originalPath = sourceFilePath;
            uploadFile.nowPath = filePath;
            uploadFile.analysisStatus = 0;
            uploadFile.uploadTime = ServerTimeUtils.getCurrentTimeStr();
            FileInfo fileInfo = new FileInfo(filePath);
            uploadFile.fileSize = fileInfo.Length;
            uploadFile.platformType = platformType;
            uploadFile.fileId = Guid.NewGuid().ToString();
            
            if (uploadFile.fileType == 2)
            {
                // 文本，统计字数
                string content = File.ReadAllText(uploadFile.nowPath);
                int wordNum = Convert.ToInt32(content.Length);
                uploadFile.fileWordNum = wordNum;
            }
            else
            {
                // 音频或视频，获取时长
                int duration = 0;
                try
                {
                    duration = VideoUtils.getVideoDuration(uploadFile.nowPath);
                }
                catch (Exception e)
                {
                    FileUtils.LogAnalysis($"{e}", $"保存文件信息到数据库发生异常=={sourceFilePath}");
                }

                if (duration <= 0)
                {
                    throw new Exception("文件损坏或无法读取，请检查源文件");
                }

                int minute = Convert.ToInt32(duration);
                uploadFile.fileDuration = minute;
            }
            

            uploadFile.uploadStatus = 0;
            uploadFile.isMark = 0;
            uploadFile.engSerViceType = engSerViceType;
            
            uploadFile.copyProperties(basicSettings);

            // 保存到服务器
            UploadFileApi.SaveOrUpdateFileSync(uploadFile);

            return uploadFile;
        }

        /// <summary>
        /// 转换成文本文件
        /// </summary>
        /// <param name="wordFilePath">Word 文件路径</param>
        /// <returns>转换后的文本文件路径</returns>
        private string ConvertToTextFile(string wordFilePath)
        {
            var txtFilePath = wordFilePath.Substring(0, wordFilePath.LastIndexOf(".")) + ".txt";
            var wordText = WordDocumentBll.GetTextByWord(wordFilePath);

            File.WriteAllText(txtFilePath, wordText);
            return txtFilePath;
        }

        /// <summary>
        /// 检测文件编码
        /// </summary>
        /// <param name="filePath"></param>
        /// <returns></returns>
        private Encoding DetectFileEncoding(string filePath)
        {
            using (var stream = new FileStream(filePath, FileMode.Open, FileAccess.Read))
            {
                var detector = new CharsetDetector();
                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = stream.Read(buffer, 0, buffer.Length)) > 0)
                {
                    detector.Feed(buffer, 0, bytesRead);
                    if (detector.IsDone())
                        break;
                }

                detector.DataEnd();

                detector.DataEnd();

                if (!string.IsNullOrEmpty(detector.Charset))
                {
                    return Encoding.GetEncoding(detector.Charset);
                }

                return Encoding.UTF8; // 默认使用 UTF-8
            }
        }

        /// <summary>
        /// 修改文件编码
        /// </summary>
        /// <param name="sourcePath"></param>
        /// <param name="destinationPath"></param>
        /// <param name="sourceEncoding"></param>
        private void ConvertFileToUtf8(string sourcePath, string destinationPath, Encoding sourceEncoding)
        {
            // 读取文件内容
            string content = File.ReadAllText(sourcePath, sourceEncoding);

            // 以 UTF-8 编码写入文件
            File.WriteAllText(destinationPath, content, Encoding.UTF8);
        }

        /// <summary>
        /// 拷贝文件到上传目录
        /// </summary>
        /// <param name="filePath">文件路径</param>
        private string CopeFile(string filePath)
        {
            //Config config = new Config();
            //config.GetModel();
            ConfigBll configBll = new ConfigBll();
            Config config = configBll.GetModel();

            string outPath = config.SavePath + "\\_本地上传\\";
            if(filePath.EndsWith(".mp3") || filePath.EndsWith(".aac"))
            {
                outPath += "音频文件";
            }else if(filePath.EndsWith(".txt")
                  || filePath.EndsWith(".doc")
                  || filePath.EndsWith(".docx"))
            {
                outPath += "文本文件";
            }else
            {
                outPath += "视频文件";
            }

            // 文件夹不存在，创建文件夹
            if (!Directory.Exists(outPath))
            {
                Directory.CreateDirectory(outPath);
            }

            
            outPath += filePath.Substring(filePath.LastIndexOf("\\"));

            if (File.Exists(outPath))
            {
                // 文件名重复，在文件名后面加新的标识
                while(File.Exists(outPath))
                {
                    string suffix = outPath.Substring(outPath.LastIndexOf("."));
                    outPath = outPath.Substring(0, outPath.LastIndexOf(".")) + "(1)";
                    outPath += suffix;
                }
            }
            if (!filePath.EndsWith(".txt"))
            {
                // 拷贝文件 除了txt文本之外的文件
                File.Copy(filePath, outPath, true);
            }
            else 
            {
                //txt文本需要先获取文件编码
                Encoding fileEncoding = DetectFileEncoding(filePath);
                // 转换文件编码为 UTF-8
                ConvertFileToUtf8(filePath, outPath, fileEncoding);
            }
            return outPath;
        }

        /// <summary>
        /// 分页获取上传文件列表
        /// </summary>
        /// <param name="pageIndex">当前页 不传默认为1</param>
        /// <param name="pageSize">分页大小 不穿默认为10</param>
        /// <param name="analysisStatus">分析状态</param>
        /// <param name="recordStartDate">上传时间范围-开始</param>
        /// <param name="recordEndDate">上传时间范围-结束</param>
        /// <param name="analysisStartDate">分析时间范围-开始</param>
        /// <param name="analysisEndDate">分析时间范围-结束</param>
        /// <returns></returns>
        public PageDto<UploadFileDto> GetPage(int pageIndex, int pageSize, int analysisStatus, string fileName, string uploadStartDate, string uploadEndDate, string analysisStartDate, string analysisEndDate)
        {
            UploadFilePageQueryDto uploadFilePageQueryDto = new UploadFilePageQueryDto();
            uploadFilePageQueryDto.PageIndex = pageIndex;
            uploadFilePageQueryDto.PageSize = pageSize;
            uploadFilePageQueryDto.AnalysisStatus = analysisStatus;
            uploadFilePageQueryDto.AnalysisStartDate = analysisStartDate;
            uploadFilePageQueryDto.AnalysisEndDate = analysisEndDate;
            uploadFilePageQueryDto.UpdateStartDate = uploadStartDate;
            uploadFilePageQueryDto.UpdateEndDate = uploadEndDate;
            uploadFilePageQueryDto.FileName = fileName;
            uploadFilePageQueryDto.UserId = ReplayHttpUtils.UserId;

            UploadFile uploadFile = new UploadFile();
            PageDto<UploadFileDto> pageDto = uploadFile.GetPage(uploadFilePageQueryDto);

            // 设置文件显示地址
            List<UploadFileDto> dataList = pageDto.DataList;
            if(dataList != null && dataList.Count > 0)
            {
                foreach (UploadFileDto uploadFileDto in dataList)
                {
                    if (uploadFileDto.FileType == 0)
                    {
                        // 获取视频播放地址
                        string videoPath = uploadFileDto.NowPath.Substring(0, uploadFileDto.NowPath.LastIndexOf("\\"));
                        videoPath += "\\mp4\\" + uploadFileDto.FileName + ".mp4";
                        videoPath = "http://localhost:45001/" + videoPath.Substring(videoPath.IndexOf("_本地上传"));
                        uploadFileDto.ShowUrl = videoPath;
                    }
                    else if (uploadFileDto.FileType == 1 || uploadFileDto.FileType == 2)
                    {
                        // 获取音频、文本地址
                        string path = "http://localhost:45001/" + uploadFileDto.NowPath.Substring(uploadFileDto.NowPath.IndexOf("_本地上传"));
                        uploadFileDto.ShowUrl = path;
                    }
                }
            }

            return pageDto;
        }

        /// <summary>
        /// 查看文件分析
        /// </summary>
        /// <param name="fileId">文件id</param>
        public SentenceMarkDto LockAnalysis(string fileId)
        {
            SentenceMarkDto sentenceMarkDto = new SentenceMarkDto();
            // 查询文件信息
            UploadFileEntity uploadFileEntity = UploadFileApi.GetFileByFileIdSync(fileId);

            if (uploadFileEntity != null)
            {
                var settings = new JsonSerializerSettings
                {
                    ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver(),
                    NullValueHandling = NullValueHandling.Ignore
                };
                uploadFileEntity.id = 0;
                UploadFile uploadFile = JsonConvert.DeserializeObject<UploadFile>(JsonConvert.SerializeObject(uploadFileEntity), settings);

                sentenceMarkDto.uploadFile = uploadFile;

                if (uploadFile.FileType == 0)
                {
                    if (File.Exists(uploadFile.NowPath))
                    {
                        VideoUtils.ConvertToMP4(uploadFile.NowPath, uploadFile.FileName);
                    }

                    // 获取视频播放地址
                    string mp4VideoPath = uploadFile.NowPath.Substring(0, uploadFile.NowPath.LastIndexOf("\\"));
                    mp4VideoPath += "\\mp4\\" + uploadFile.FileName + ".mp4";
                    if(File.Exists(mp4VideoPath))
                    {
                        string serverPath = uploadFile.NowPath.Substring(0, uploadFile.NowPath.IndexOf("_本地上传"));
                        HttpResourceFileServer httpResourceFileServer = new HttpResourceFileServer();
                        string port = httpResourceFileServer.GetPort(serverPath);

                        string videoPath = "http://localhost:" + port + "/" + mp4VideoPath.Substring(mp4VideoPath.IndexOf("_本地上传"));
                        sentenceMarkDto.playUrl = videoPath;
                    }
                }
                else if(uploadFile.FileType == 1)
                {
                    if (File.Exists(uploadFile.NowPath))
                    {
                        string serverPath = uploadFile.NowPath.Substring(0, uploadFile.NowPath.IndexOf("_本地上传"));
                        HttpResourceFileServer httpResourceFileServer = new HttpResourceFileServer();
                        string port = httpResourceFileServer.GetPort(serverPath);

                        // 获取音频播放地址
                        string audioPath = "http://localhost:" + port + "/" + uploadFile.NowPath.Substring(uploadFile.NowPath.IndexOf("_本地上传"));
                        sentenceMarkDto.playUrl = audioPath;
                    }
                }

                // 获取本地分析文件
                AnalysisResultTxtVo analysisResultTxtVo = AnalysisUtils.getFileLocalAnalysisData(uploadFileEntity);
                if (analysisResultTxtVo != null)
                {
                    sentenceMarkDto.fileAudioaAlyses = analysisResultTxtVo.fileAudioaAlyses;
                    sentenceMarkDto.cruxTypeList = analysisResultTxtVo.cruxTypeList;
                    sentenceMarkDto.wordsCollect = analysisResultTxtVo.wordsCollect;
                    sentenceMarkDto.wordsTabList = analysisResultTxtVo.wordsTabList;
                }
                else
                {
                    // 本地没有分析记录，从服务器同步
                    string zipPath = ReplayHttpUtils.DownloadAnalysisFile(null, fileId, Path.GetFullPath($"analysisTemp\\{fileId}.zip"));
                    if (!string.IsNullOrEmpty(zipPath))
                    {
                        // 将压缩包里面的内容转成字符串
                        string jsonCentent = ZipUtils.ReadFileFromZip(zipPath);
                        if (!string.IsNullOrEmpty(jsonCentent))
                        {
                            AnalysisResultVo analysisResultVo = JsonConvert.DeserializeObject<AnalysisResultVo>(jsonCentent, settings);

                            // 将分析数据存到本地文件
                            analysisResultTxtVo = AnalysisUtils.saveFileLocalAnalysisData(analysisResultVo, uploadFileEntity.tradeId + "", uploadFileEntity);

                            sentenceMarkDto.fileAudioaAlyses = analysisResultTxtVo.fileAudioaAlyses;
                            sentenceMarkDto.cruxTypeList = analysisResultTxtVo.cruxTypeList;
                            sentenceMarkDto.wordsCollect = analysisResultTxtVo.wordsCollect;
                            sentenceMarkDto.wordsTabList = analysisResultTxtVo.wordsTabList;
                        }
                        if (File.Exists(zipPath))
                        {
                            File.Delete(zipPath);
                        }
                    }
                }

                // 获取是否已推荐行业
                UploadFileDetailVo uploadFileDetail = UploadFileApi.infoDetailByFileId(fileId);
                sentenceMarkDto.suggestTrade = uploadFileDetail?.suggestTrade ?? 0;

                // 获取视频的行业信息
                TradeVo tradeVo = TradeApi.GetTradeById(uploadFileEntity.tradeId + "");
                sentenceMarkDto.tradeInfo = tradeVo;

                return sentenceMarkDto;
            }

            return null;
        }

        /// <summary>
        /// 异步查看文件分析,LockAnalysis方法改写，添加Async后缀（规范）
        /// </summary>
        /// <param name="fileId">文件id</param>
        /// <returns></returns>
        public async Task<SentenceMarkDto> LockFileAnalysisAsync(string fileId)
        {
            SentenceMarkDto sentenceMarkDto = new SentenceMarkDto();

            try
            {
                // 1. 异步查询文件信息（替换同步API）
                UploadFileEntity uploadFileEntity = await UploadFileApi.GetFileByFileIdAsync(fileId).ConfigureAwait(false);

                if (uploadFileEntity != null)
                {
                    var settings = new JsonSerializerSettings
                    {
                        ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver(),
                        NullValueHandling = NullValueHandling.Ignore
                    };
                    uploadFileEntity.id = 0;
                    UploadFile uploadFile = JsonConvert.DeserializeObject<UploadFile>(JsonConvert.SerializeObject(uploadFileEntity), settings);

                    sentenceMarkDto.uploadFile = uploadFile;

                    // 2. 异步转码视频（IO/CPU密集型操作异步化）
                    if (uploadFile.FileType == 0)
                    {
                        if (File.Exists(uploadFile.NowPath))
                        {
                            await Task.Run(() => VideoUtils.ConvertToMP4(uploadFile.NowPath, uploadFile.FileName)).ConfigureAwait(false);
                        }

                        // 获取视频播放地址
                        string mp4VideoPath = uploadFile.NowPath.Substring(0, uploadFile.NowPath.LastIndexOf("\\"));
                        mp4VideoPath += "\\mp4\\" + uploadFile.FileName + ".mp4";
                        if (File.Exists(mp4VideoPath))
                        {
                            string serverPath = uploadFile.NowPath.Substring(0, uploadFile.NowPath.IndexOf("_本地上传"));
                            HttpResourceFileServer httpResourceFileServer = new HttpResourceFileServer();
                            string port = await httpResourceFileServer.GetPortAsync(serverPath).ConfigureAwait(false);

                            string videoPath = "http://localhost:" + port + "/" + mp4VideoPath.Substring(mp4VideoPath.IndexOf("_本地上传"));
                            sentenceMarkDto.playUrl = videoPath;
                        }
                    }
                    else if (uploadFile.FileType == 1)
                    {
                        if (File.Exists(uploadFile.NowPath))
                        {
                            string serverPath = uploadFile.NowPath.Substring(0, uploadFile.NowPath.IndexOf("_本地上传"));
                            HttpResourceFileServer httpResourceFileServer = new HttpResourceFileServer();
                            string port = await httpResourceFileServer.GetPortAsync(serverPath).ConfigureAwait(false);

                            // 获取音频播放地址
                            string audioPath = $"http://localhost:{port}/{uploadFile.NowPath.Substring(uploadFile.NowPath.IndexOf("_本地上传"))}";
                            sentenceMarkDto.playUrl = audioPath;
                        }
                    }

                    // 3. 异步读取本地分析文件（IO操作异步化）
                    AnalysisResultTxtVo analysisResultTxtVo = AnalysisUtils.getFileLocalAnalysisData(uploadFileEntity);
                    if (analysisResultTxtVo != null)
                    {
                        sentenceMarkDto.fileAudioaAlyses = analysisResultTxtVo.fileAudioaAlyses;
                        sentenceMarkDto.cruxTypeList = analysisResultTxtVo.cruxTypeList;
                        sentenceMarkDto.wordsCollect = analysisResultTxtVo.wordsCollect;
                        sentenceMarkDto.wordsTabList = analysisResultTxtVo.wordsTabList;
                    }
                    else
                    {
                        // 4. 异步下载服务器分析文件（已异步，保留await）
                        string zipPath = Path.GetFullPath($"analysisTemp\\{fileId}.zip");
                        string downloadedZipPath = await ReplayHttpUtils.DownloadAnalysisFileAsync(null, fileId, zipPath).ConfigureAwait(false);

                        if (!string.IsNullOrEmpty(downloadedZipPath))
                        {
                            // 5. 异步读取压缩包内容（IO操作异步化）
                            string jsonCentent = await ZipUtils.ReadFileFromZipAsync(downloadedZipPath).ConfigureAwait(false);

                            if (!string.IsNullOrEmpty(jsonCentent))
                            {
                                AnalysisResultVo analysisResultVo = JsonConvert.DeserializeObject<AnalysisResultVo>(jsonCentent, settings);

                                // 将分析数据存到本地文件
                                analysisResultTxtVo = AnalysisUtils.saveFileLocalAnalysisData(analysisResultVo, uploadFileEntity.tradeId + "", uploadFileEntity);

                                sentenceMarkDto.fileAudioaAlyses = analysisResultTxtVo.fileAudioaAlyses;
                                sentenceMarkDto.cruxTypeList = analysisResultTxtVo.cruxTypeList;
                                sentenceMarkDto.wordsCollect = analysisResultTxtVo.wordsCollect;
                                sentenceMarkDto.wordsTabList = analysisResultTxtVo.wordsTabList;
                            }
                            if (File.Exists(downloadedZipPath))
                            {
                                File.Delete(downloadedZipPath);
                            }
                        }
                    }

                    // 6. 异步获取是否已推荐行业
                    UploadFileDetailVo uploadFileDetail = await UploadFileApi.infoDetailByFileIdAsync(fileId).ConfigureAwait(false);
                    sentenceMarkDto.suggestTrade = uploadFileDetail?.suggestTrade ?? 0;

                    return sentenceMarkDto;
                }

                return null;
            }
            catch (Exception ex)
            {
                await FileUtils.LogErrorAsync($"LockFileAnalysisAsync 异常: {ex.Message}", "UploadFileBll").ConfigureAwait(false);
                return null;
            }
        }

        /// <summary>
        /// 预览文件
        /// </summary>
        /// <param name="videoId">文件id</param>
        public string Preview(string fileId)
        {
            // 查询文件信息
            UploadFileEntity uploadFile = UploadFileApi.GetFileByFileIdSync(fileId);

            if (uploadFile == null)
            {
                throw new Exception("文件信息不存在");
            }

            if (!File.Exists(uploadFile.nowPath))
            {
                // 文件不存在
                throw new Exception("本地文件不存在");
            }

            VideoUtils.ConvertToMP4(uploadFile.nowPath, uploadFile.fileName);

            string serverPath = uploadFile.nowPath.Substring(0, uploadFile.nowPath.IndexOf("_本地上传"));
            //HttpResourceFileServer httpResourceFileServer = new HttpResourceFileServer(serverPath);
            //httpResourceFileServer.StartResourceFileServer();
            HttpResourceFileServer httpResourceFileServer = new HttpResourceFileServer();
            string port = httpResourceFileServer.GetPort(serverPath);

            string showUrl = "";

            if (uploadFile.fileType == 0)
            {
                // 获取视频播放地址
                string videoPath = uploadFile.nowPath.Substring(0, uploadFile.nowPath.LastIndexOf("\\"));
                videoPath += "\\mp4\\" + uploadFile.fileName + ".mp4";
                videoPath = "http://localhost:"+ port + "/" + videoPath.Substring(videoPath.IndexOf("_本地上传"));
                showUrl = videoPath;
            }
            else if (uploadFile.fileType == 1 || uploadFile.fileType == 2)
            {
                // 获取音频、文本地址
                string path = "http://localhost:"+ port + "/" + uploadFile.nowPath.Substring(uploadFile.nowPath.IndexOf("_本地上传"));
                showUrl = path;
            }

            return showUrl;
        }

        /// <summary>
        /// 重新选择行业分析
        /// </summary>
        /// <param name="fileId">文件id</param>
        /// <param name="tradeId">行业id</param>
        /// <param name="platformType">平台类型 0：全平台 1：...</param>
        /// <param name="duration">消耗时长，单位：分钟</param>
        /// <param name="wordNum">消耗字数</param>
        public SentenceMarkDto ReAnalysisByTrade(string fileId, string tradeId, string platformType, int duration, int wordNum)
        {
            UploadFileEntity uploadFile = UploadFileApi.GetFileByFileIdSync(fileId);

            if (uploadFile == null)
            {
                throw new Exception("文件信息不存在");
            }

            // 修改文件的行业
            UploadFileApi.UpdateFileTradeSync(fileId, tradeId);

            // 视频或音频
            AnalysisResultVo analysisResultVo = WordApi.WordsMarkSync(platformType, uploadFile.fileId, tradeId, 1);

            if (analysisResultVo != null)
            {
                // 将分析数据存到本地文件
                AnalysisUtils.saveFileLocalAnalysisData(analysisResultVo, tradeId, uploadFile);
            }
            else
            {
                throw new Exception("分析失败，请检查网络后再重试");
            }

            // 使用标注资源
            if (wordNum != 0)
            {
                // 消耗字数
                UserPropertyHttpUtils.UpdateUserProperty("textTaggingWordCount", wordNum);
            }

            // 获取新的分析数据
            return LockAnalysis(fileId);
        }

        /// <summary>
        /// 重新生成文件分析
        /// </summary>
        /// <param name="uploadFileEntity">文件信息</param>
        /// <param name="platformType">平台类型 0：全平台 1：抖音 2：快手 3：视频号 4：小红书</param>
        /// <param name="token">向服务器发请求的token</param>
        /// <param name="tradeId">行业id</param>
        public async Task ReAnalysis(UploadFileEntity uploadFileEntity, int platformType, string token, string tradeId)
        {
            
            try
            {
                // 清理旧数据
                ClearFileData(uploadFileEntity);
                // 设置文件的行业
                await UploadFileApi.UpdateFileTrade(uploadFileEntity.fileId, tradeId);
                uploadFileEntity.tradeId = long.Parse(tradeId);

                // 分析
                if (uploadFileEntity.fileType == 0 || uploadFileEntity.fileType == 1)
                {
                    // 文件是视频或音频
                    await AnalysisVideoOrAudio(uploadFileEntity, token, platformType, tradeId);
                }
                else if (uploadFileEntity.fileType == 2)
                {
                    // 文件是文本
                    await AnalysisText(uploadFileEntity, token, platformType, tradeId);
                }
                
            }
            catch (Exception ex)
            {
                UploadFileApi.UpdateFileAnalysisStatusSync(uploadFileEntity.fileId, 3, "网络不佳");
                FileUtils.LogAnalysis($"{ex}", $"文件分析失败==={uploadFileEntity.fileId}");
            }

            AnchorVideoBll.isAnalysis = false;

        }

        /// <summary>
        /// 根据文件id集合删除文件
        /// <param name="ids">文件file_id集合</param>
        /// </summary>
        public void Delete(List<string> ids)
        {
            if (ids != null || ids.Count > 0)
            {
                // 获取文件列表
                List<UploadFileEntity> uploadFileEntities = UploadFileApi.ListFileByFileIdsSync(ids);

                if (uploadFileEntities != null && uploadFileEntities.Count > 0)
                {
                    // 批量删除服务器文件
                    UploadFileApi.DelFileByIdsSync(ids);

                    foreach (var uploadFile in uploadFileEntities)
                    {
                        try
                        {
                            // 删除文件
                            if (File.Exists(uploadFile.nowPath))
                            {
                                File.Delete(uploadFile.nowPath);
                            }

                            ClearFileData(uploadFile);
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogError($"{ex}", $"删除本地文件发生异常");
                        }
                        
                    }
                    
                    
                }
            }
        }

        /// <summary>
        /// 同步服务器的文件列表到本地
        /// </summary>
        public void syncServerFile()
        {

            // 获取服务器的文件
            List<UploadFile> serverFileList = ReplayHttpUtils.GetFileList();
            if (serverFileList != null && serverFileList.Count > 0)
            {
                // 获取本地所有文件
                UploadFile uploadFile = new UploadFile();
                uploadFile.UserId = ReplayHttpUtils.UserId;
                List<UploadFile> uploadFiles = uploadFile.GetList();

                if(uploadFiles != null && uploadFiles.Count > 0)
                {
                    // 本地有文件，只同步不存在的
                    foreach (var serverFile in serverFileList)
                    {
                        bool exist = false;
                        foreach (var item in uploadFiles)
                        {
                            if(item.FileId == serverFile.FileId)
                            {
                                exist = true;
                                break;
                            }
                        }
                        if(!exist)
                        {
                            serverFile.UserId = ReplayHttpUtils.UserId;
                            serverFile.UploadStatus = 1;
                            if (string.IsNullOrEmpty(serverFile.PlatformType))
                            {
                                serverFile.PlatformType = "1";
                            }
                            serverFile.Save(false);
                        }
                    }

                }
                else
                {
                    // 本地没有文件，同步所有
                    foreach (var item in serverFileList)
                    {
                        item.UserId = ReplayHttpUtils.UserId;
                        item.UploadStatus = 1;
                        if (string.IsNullOrEmpty(item.PlatformType))
                        {
                            item.PlatformType = "1";
                        }
                        item.Save(false);
                    }
                }
            }

                


            //// 删除本地文件
            //UploadFile uploadFile = new UploadFile();
            //uploadFile.DeleteAll();
            //// 删除本地文件音频记录
            //UploadFileAudio uploadFileAudio = new UploadFileAudio();
            //uploadFileAudio.DeleteAll();
            //// 删除本地文件分析记录
            //UploadFileAlysis uploadFileAlysis = new UploadFileAlysis();
            //uploadFileAlysis.DeleteAll();

            //// 将服务器文件同步到本地
            //List<UploadFile> uploadFiles = ReplayHttpUtils.GetFileList();
            //if(uploadFiles != null && uploadFiles.Count > 0)
            //{

            //    foreach (var item in uploadFiles)
            //    {
            //        if (string.IsNullOrEmpty(item.PlatformType))
            //        {
            //            item.PlatformType = "1";
            //        }
            //        item.Save(false);
            //    }
            //}
        }

        /// <summary>
        /// 分享文件复盘
        /// </summary>
        /// <param name="fileId">文件唯一标识</param>
        public string ShareAnalysis(string fileId, string onlineFileUrl)
        {
            UploadFileEntity uploadFile = UploadFileApi.GetFileByFileIdSync(fileId);

            if(uploadFile == null)
            {
                throw new Exception("文件信息不存在");
            }

            uploadFile.uploadStatus = 1;
            uploadFile.shareUrl = Constant.GetOnlineUrl() + "onlineAnalysis/1/" + uploadFile.fileId;
            uploadFile.playUrl = onlineFileUrl;
            
            UploadFileApi.SaveOrUpdateFileSync(uploadFile);

            return uploadFile.shareUrl;
        }

        /// <summary>
        /// 确认消耗标注资源
        /// </summary>
        /// <param name="fileId">文件唯一标识</param>
        /// <param name="duration">消耗时长，单位：分钟</param>
        /// <param name="wordNum">消耗字数</param>
        public void ConfirmUseMark(string fileId, int duration, int wordNum)
        {
            UploadFileEntity uploadFile = UploadFileApi.GetFileByFileIdSync(fileId);

            if (uploadFile == null)
            {
                throw new Exception("文件信息不存在");
            }

            // 使用资源
            if (duration != 0)
            {
                // 消耗时长
                UserPropertyHttpUtils.UpdateUserProperty("videoTaggingTime", duration);
            }else
            {
                // 消耗字数
                UserPropertyHttpUtils.UpdateUserProperty("textTaggingWordCount", wordNum);
            }

        }

        /// <summary>
        /// 压缩视频
        /// </summary>
        public Dictionary<string, object> Compress(string fileId)
        {
            UploadFileEntity uploadFile = UploadFileApi.GetFileByFileIdSync(fileId);

            if(uploadFile == null)
            {

                throw new Exception("文件信息不存在");
            }

            string videoPath = uploadFile.nowPath.Substring(0, uploadFile.nowPath.LastIndexOf("\\"));
            videoPath += "\\mp4\\";
            videoPath += uploadFile.fileName + ".mp4";

            if (!File.Exists(videoPath))
            {
                throw new Exception("文件不存在");
            }

            Dictionary<string, object> dictionary = VideoUtils.Compress(videoPath);

            object fileSize = 0;
            dictionary.TryGetValue("fileSize", out fileSize);
            uploadFile.cloudStore = (int)fileSize;

            UploadFileApi.SaveOrUpdateFileSync(uploadFile);

            return dictionary;

        }

        /// <summary>
        /// 上传文字
        /// </summary>
        /// <param name="uploadTxtFileByWordBo">参数</param>
        internal void uploadTxtFileByWord(UploadTxtFileByWordBo uploadTxtFileByWordBo)
        {
            ConfigBll configBll = new ConfigBll();
            Config config = configBll.GetModel();

            string outPath = config.SavePath + "\\_本地上传\\";

            // 文件夹不存在，创建文件夹
            if (!Directory.Exists(outPath))
            {
                Directory.CreateDirectory(outPath);
            }

            outPath = $"{outPath}\\{ServerTimeUtils.getCurrentTime()}.txt";
            // 写入文本到文件
            File.WriteAllText(outPath, uploadTxtFileByWordBo.content);

            BasicSettingsBaseDto dto = JsonConvert.DeserializeObject<BasicSettingsBaseDto>(JsonConvert.SerializeObject(uploadTxtFileByWordBo));
            // 保存文件信息
            SaveUploadFile("文字拷贝", outPath, uploadTxtFileByWordBo.platformType, null, dto);

        }

        /// <summary>
        /// 新增文件切片
        /// </summary>
        /// <param name="addVideoSliceBo">新增文件切片数据</param>
        /// <returns></returns>
        public void addFileSlice(AddFileSliceBo addFileSliceBo)
        {

            lock (AnchorVideoBll._lockObject)
            {
                if (AnchorVideoBll.sliceVideoing)
                {
                    throw new Exception("请等待上一个视频切片完成再继续");
                }
                AnchorVideoBll.sliceVideoing = true;
            }

            try
            {

                if (string.IsNullOrEmpty(addFileSliceBo.videoName))
                {
                    throw new Exception("切片视频名称不能为空");
                }
                addFileSliceBo.videoName = WindowsUtils.SanitizeForFolderName(addFileSliceBo.videoName);

                if (addFileSliceBo.endTimeMs - addFileSliceBo.startTimeMs < 1000)
                {
                    throw new Exception("切片视频时长不能低于1秒");
                }
                // 获取原视频信息
                UploadFileEntity fileEntity = UploadFileApi.GetFileByFileIdSync(addFileSliceBo.fileId);
                if (fileEntity == null)
                {
                    throw new Exception("文件信息不存在");
                }
                if (!File.Exists(fileEntity.nowPath))
                {
                    throw new Exception("视频文件不存在");
                }
                if (fileEntity.fileType != 0)
                {
                    throw new Exception("仅支持视频文件切片");
                }

                // 设置切片保存地址
                if (addFileSliceBo.savePathType == 0)
                {
                    addFileSliceBo.savePath = fileEntity.nowPath.Substring(0, fileEntity.nowPath.LastIndexOf("\\")) + "\\";
                }
                else
                {
                    addFileSliceBo.savePath = addFileSliceBo.savePath + "\\_本地上传\\";
                }
                addFileSliceBo.videoName = addFileSliceBo.videoName + ".mp4";
                string sliceVideoPath = addFileSliceBo.savePath + addFileSliceBo.videoName;
                if (File.Exists(sliceVideoPath))
                {
                    throw new Exception("切片视频已存在，请检查路径和视频名称");
                }

                // 取出本地分析段落文字
                AnalysisResultTxtVo analysisResultTxtVo = AnalysisUtils.getFileLocalAnalysisData(fileEntity);
                if (analysisResultTxtVo == null || analysisResultTxtVo.fileAudioaAlyses == null || analysisResultTxtVo.fileAudioaAlyses.Count < 1)
                {
                    throw new Exception("当前视频分析数据丢失，请尝试重新分析");
                }

                // 异步做剩下任务
                Task.Run(async () =>
                {
                    UploadFileEntity sliceFile = null;
                    try
                    {
                        // 将原视频转成MP4
                        VideoUtils.ConvertToMP4(fileEntity.nowPath, fileEntity.fileName, fileEntity.platformType);
                        string mp4Path = AnalysisUtils.getVideoMp4Path(fileEntity.nowPath, fileEntity.fileName);

                        // 保存切片视频到服务器
                        sliceFile = await saveSliceFileToServer(fileEntity, addFileSliceBo, sliceVideoPath);

                        // 切割视频
                        long durationMs = addFileSliceBo.endTimeMs - addFileSliceBo.startTimeMs;
                        VideoUtils.SliceVideo(mp4Path, sliceVideoPath, addFileSliceBo.startTimeMs, durationMs);

                        FileInfo fileInfo = new FileInfo(sliceVideoPath);
                        sliceFile.fileSize = fileInfo.Length;
                        await UploadFileApi.SaveOrUpdateFile(sliceFile);

                        // 封装段落词语
                        List<WordsMarkParagraphBo> wordsMarkParagraphBos = packageParagraphWord(addFileSliceBo, analysisResultTxtVo, sliceFile);

                        // 敏感词/关键词识别
                        AnalysisResultVo analysisResultVo = await WordApi.SliceWordsMark(wordsMarkParagraphBos);

                        // 将分析结果存到本地文件
                        AnalysisUtils.saveFileLocalAnalysisData(analysisResultVo, sliceFile.tradeId + "", sliceFile);
                        FileUtils.LogAnalysis($"{sliceFile?.fileName}", $"文件切片分析数据成功");

                        // 将视频分析状态改成分析完成
                        await UploadFileApi.UpdateFileAnalysisStatus(sliceFile.fileId, 2, "");
                        FileUtils.LogAnalysis($"{JsonConvert.SerializeObject(sliceFile)}", $"文件切片分析成功");

                        // 转成MP4
                        VideoUtils.ConvertToMP4(sliceFile.nowPath, sliceFile.fileName, sliceFile.platformType);

                        // 通知前端
                        try
                        {
                            FrontNotice frontNotice = new FrontNotice();
                            var requestDataObj = new Dictionary<string, object>();
                            requestDataObj["code"] = 0;
                            requestDataObj["status"] = 200;
                            requestDataObj["action"] = "sliceSuccess";
                            Dictionary<string, object> data = new Dictionary<string, object>();
                            data.Add("sliceName", addFileSliceBo.videoName);
                            data.Add("fileId", sliceFile.fileId);
                            data.Add("sliceType", addFileSliceBo.sliceType);
                            requestDataObj["data"] = data;
                            frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                        }
                        catch (Exception e)
                        {
                            FileUtils.LogError($"{e}", $"通知前端短视频切割完成发生异常");
                        }
                        AnchorVideoBll.sliceVideoing = false;
                    }
                    catch (Exception ex)
                    {
                        AnchorVideoBll.sliceVideoing = false;
                        FileUtils.LogAnalysis($"{ex}", $"文件切片视频任务发生异常==={JsonConvert.SerializeObject(addFileSliceBo)}");
                        if (sliceFile != null)
                        {
                            // 从服务器删除文件
                            UploadFileApi.DelFileById(sliceFile.fileId);
                        }
                        if (File.Exists(sliceVideoPath))
                        {
                            File.Delete(sliceVideoPath);
                            ClearFileData(sliceFile);
                        }
                    }
                });
            }
            catch (Exception e)
            {
                AnchorVideoBll.sliceVideoing = false;
                FileUtils.LogAnalysis($"{e}", $"文件切片任务异常==={JsonConvert.SerializeObject(addFileSliceBo)}");
                throw new Exception(e.Message);
            }

            
        }

        /// <summary>
        /// 封装视频切片的段落词语
        /// </summary>
        /// <param name="addVideoSliceBo">切片信息</param>
        /// <param name="analysisResultTxtVo">原视频段落词语信息</param>
        /// <param name="uploadFile">切片视频信息</param>
        /// <returns></returns>
        private List<WordsMarkParagraphBo> packageParagraphWord(AddFileSliceBo addFileSliceBo, AnalysisResultTxtVo analysisResultTxtVo, UploadFileEntity uploadFile)
        {
            try
            {
                // 取出符合的词语
                List<WordListItemVo> allWordList = new List<WordListItemVo>();
                foreach (var audioaAlysis in analysisResultTxtVo.fileAudioaAlyses)
                {
                    SentenceMarkVo sentenceMarkVo = JsonConvert.DeserializeObject<SentenceMarkVo>(audioaAlysis.DataJson);
                    List<WordListItemVo> wordItems = sentenceMarkVo.Items;
                    if (wordItems != null && wordItems.Count > 0)
                    {
                        // 不在切片时间范围内的段落直接跳过
                        if (wordItems[0].StartTime > addFileSliceBo.endTimeMs || wordItems[wordItems.Count - 1].EndTime < addFileSliceBo.startTimeMs)
                        {
                            continue;
                        }

                        foreach (var wordItem in wordItems)
                        {
                            // 获取在切片时间范围内的词语
                            if (wordItem.StartTime >= addFileSliceBo.startTimeMs && wordItem.EndTime <= addFileSliceBo.endTimeMs)
                            {
                                allWordList.Add(wordItem);
                            }
                        }
                    }
                }

                // 计算一共有多少段
                long sliceVideoDuration = addFileSliceBo.endTimeMs - addFileSliceBo.startTimeMs;
                int paragraphCount = (int)Math.Ceiling(sliceVideoDuration / 59000.0);
                // 计算切片起始时间戳在所属段落的开始时间戳
                long paragraphStartTimeRemainderMs = addFileSliceBo.startTimeMs % 59000;

                List<WordsMarkParagraphBo> wordsMarkParagraphBos = new List<WordsMarkParagraphBo>();
                for (int i = 0; i < paragraphCount; i++)
                {
                    // 整理段落数据
                    WordsMarkParagraphBo wordsMarkParagraphBo = new WordsMarkParagraphBo();
                    wordsMarkParagraphBo.type = 1;
                    wordsMarkParagraphBo.videoId = uploadFile.fileId;
                    wordsMarkParagraphBo.tradeId = uploadFile.tradeId + "";
                    wordsMarkParagraphBo.platformType = uploadFile.platformType;
                    wordsMarkParagraphBo.currentSort = i + 1;
                    wordsMarkParagraphBo.isLast = paragraphCount == (i + 1) ? 1 : 0;

                    string content = "";
                    List<WordListItemBo> paragraphWordList = new List<WordListItemBo>();
                    long paragraphStartTimeMs = i * 59000;
                    long paragraphEndTimeMs = paragraphStartTimeMs + 59000;
                    foreach (var wordItem in allWordList)
                    {
                        // 将符合段落的词语添加进列表
                        if (wordItem.StartTime - addFileSliceBo.startTimeMs >= paragraphStartTimeMs && wordItem.EndTime - addFileSliceBo.startTimeMs <= paragraphEndTimeMs)
                        {
                            WordListItemBo wordListItemBo = new WordListItemBo();
                            // 段落词语时间 = 原词语时间 - 切片开始时间 - 段落起始时间
                            wordListItemBo.StartTime = wordItem.StartTime - addFileSliceBo.startTimeMs - paragraphStartTimeMs;
                            wordListItemBo.EndTime = wordItem.EndTime - addFileSliceBo.startTimeMs - paragraphStartTimeMs;
                            wordListItemBo.Word = wordItem.Word;
                            paragraphWordList.Add(wordListItemBo);
                            content += wordListItemBo.Word;
                        }
                    }

                    if (paragraphWordList.Count < 1)
                    {
                        WordListItemBo wordListItemBo = new WordListItemBo();
                        wordListItemBo.StartTime = 0;
                        wordListItemBo.EndTime = 20;
                        wordListItemBo.Word = "-";
                        paragraphWordList.Add(wordListItemBo);
                        content += "-";
                    }

                    wordsMarkParagraphBo.content = content;
                    wordsMarkParagraphBo.items = paragraphWordList;
                    wordsMarkParagraphBos.Add(wordsMarkParagraphBo);

                }
                return wordsMarkParagraphBos;
            }
            catch (Exception e)
            {
                FileUtils.LogAnalysis($"{e}", $"封装视频切片的段落词语发生异常==={JsonConvert.SerializeObject(addFileSliceBo)}");
            }
            return null;

        }

        /// <summary>
        /// 保存切片文件到服务器
        /// </summary>
        /// <param name="uploadFileEntity">原文件信息</param>
        /// <param name="addFileSliceBo">切片文件信息</param>
        /// <param name="sliceVideoPath">切片保存地址</param>
        /// <returns></returns>
        private async Task<UploadFileEntity> saveSliceFileToServer(UploadFileEntity uploadFileEntity, AddFileSliceBo addFileSliceBo, string sliceVideoPath)
        {
            UploadFileEntity sliceFile = new UploadFileEntity();

            // 文件信息
            long durationMs = addFileSliceBo.endTimeMs - addFileSliceBo.startTimeMs;
            sliceFile.fileDuration = durationMs / 1000;
            sliceFile.fileSize = 0;

            sliceFile.fileType = 0;
            sliceFile.originalPath = uploadFileEntity.nowPath;
            sliceFile.nowPath = sliceVideoPath;
            sliceFile.analysisStatus = 5;
            sliceFile.uploadTime = ServerTimeUtils.getCurrentTimeStr();
            //FileInfo fileInfo = new FileInfo(sliceVideoPath);
            //sliceFile.fileSize = fileInfo.Length;
            sliceFile.platformType = uploadFileEntity.platformType;
            sliceFile.fileId = Guid.NewGuid().ToString();
            sliceFile.tradeId = uploadFileEntity.tradeId;
            sliceFile.fileName = addFileSliceBo.videoName;
            sliceFile.engSerViceType = uploadFileEntity.engSerViceType;
            //using (var mediaInfo = new MediaInfo.MediaInfo())
            //{
            //    mediaInfo.Open(sliceFile.nowPath); // 打开视频文件

            //    // 获取视频时长
            //    string durationString = mediaInfo.Get(StreamKind.General, 0, "Duration");
            //    if (string.IsNullOrEmpty(durationString))
            //    {
            //        throw new Exception("文件损坏，请检查源文件");
            //    }
            //    double duration = double.Parse(durationString) / 1000; // 将毫秒转换为秒
            //    int minute = Convert.ToInt32(duration);
            //    sliceFile.fileDuration = minute;
            //    mediaInfo.Close(); // 关闭文件
            //}
            sliceFile.uploadStatus = 0;
            sliceFile.isMark = 0;
            sliceFile.fileSliceType = addFileSliceBo.sliceType + 1;

            // 切片信息
            VideoSliceEntity videoSliceEntity = new VideoSliceEntity();
            videoSliceEntity.userId = sliceFile.userId + "";
            videoSliceEntity.tenantId = sliceFile.tenantId;
            videoSliceEntity.sourceId = sliceFile.fileId;
            videoSliceEntity.sourceType = 1;
            videoSliceEntity.sourceParentId = uploadFileEntity.fileId;
            videoSliceEntity.sliceType = addFileSliceBo.sliceType;
            videoSliceEntity.sliceClass = addFileSliceBo.sliceClass;
            videoSliceEntity.startMillisecond = addFileSliceBo.startTimeMs;
            videoSliceEntity.startTime = TimeUtils.millisecondsFormat(addFileSliceBo.startTimeMs);
            videoSliceEntity.endMillisecond = addFileSliceBo.endTimeMs;
            videoSliceEntity.endTime = TimeUtils.millisecondsFormat(addFileSliceBo.endTimeMs);
            videoSliceEntity.remarks = addFileSliceBo.remarks;
            videoSliceEntity.isAutoUploadCloud = addFileSliceBo.isAutoUploadCloud;
            videoSliceEntity.savePathType = addFileSliceBo.savePathType;
            videoSliceEntity.savePath = sliceFile.nowPath;
            videoSliceEntity.sliceVideoName = addFileSliceBo.videoName;
            videoSliceEntity.sliceTimeType = addFileSliceBo.sliceTimeType;


            // 保存到服务器
            await UploadFileApi.SaveOrUpdateFile(sliceFile);
            await VideoSliceApi.SaveVideoSliceAsync(videoSliceEntity);

            return sliceFile;
        }

        /// <summary>
        /// 修改文件名称
        /// </summary>
        /// <param name="fileId">文件Id</param>
        /// <param name="newFileName">新文件名称（不含扩展名）</param>
        public void RenameFile(string fileId, string newFileName)
        {
            if (string.IsNullOrEmpty(fileId))
            {
                throw new Exception("文件Id不能为空");
            }

            if (string.IsNullOrEmpty(newFileName))
            {
                throw new Exception("新文件名称不能为空");
            }

            // 1. 通过fileId获取文件信息
            UploadFileEntity uploadFile = UploadFileApi.GetFileByFileIdSync(fileId);
            if (uploadFile == null)
            {
                throw new Exception("文件信息不存在");
            }

            if (File.Exists(uploadFile.nowPath))
            {
                try
                {
                    using (var fs = File.Open(uploadFile.nowPath, FileMode.Open, FileAccess.ReadWrite, FileShare.None))
                    {
                        // 尝试是否能打开
                    }
                }
                catch (IOException ex)
                {
                    throw new Exception($"{uploadFile.fileName}文件正在被占用，删除失败，请明天再试");
                }
            }

            string oldNowPath = uploadFile.nowPath;
            string oldFileName = uploadFile.fileName;

            // 记录需要回滚的文件路径映射 (新路径 -> 旧路径)
            Dictionary<string, string> renamedFiles = new Dictionary<string, string>();

            try
            {
                // 2. 获取文件目录和扩展名
                string directory = Path.GetDirectoryName(oldNowPath);
                string oldExtension = Path.GetExtension(oldNowPath);

                // 检查并重命名文件
                string newFilePath = Path.Combine(directory, newFileName + oldExtension);
                if (File.Exists(oldNowPath))
                {
                    File.Move(oldNowPath, newFilePath);
                    renamedFiles.Add(newFilePath, oldNowPath);
                }

                if (uploadFile.fileType == 0)
                {
                    // 检查并重命名mp4文件
                    string oldMp4Path = Path.Combine(directory, "mp4", oldFileName + ".mp4");
                    string newMp4Path = Path.Combine(directory, "mp4", newFileName + oldExtension + ".mp4");
                    if (File.Exists(oldMp4Path))
                    {
                        File.Move(oldMp4Path, newMp4Path);
                        renamedFiles.Add(newMp4Path, oldMp4Path);
                    }
                }

                // 更新文件实体信息
                string newNowPath = Path.Combine(directory, newFileName + oldExtension);
                uploadFile.nowPath = newNowPath;
                uploadFile.fileName = newFileName + oldExtension;
                uploadFile.videoRename = newFileName + oldExtension;

                // 3. 同步到服务器
                UploadFileApi.SaveOrUpdateFileSync(uploadFile);
            }
            catch (Exception ex)
            {
                // 4. 同步服务器失败，回滚本地文件名
                foreach (var kvp in renamedFiles)
                {
                    try
                    {
                        if (File.Exists(kvp.Key))
                        {
                            File.Move(kvp.Key, kvp.Value);
                        }
                    }
                    catch
                    {
                        // 回滚失败时忽略，避免影响异常抛出
                        FileUtils.LogError($"{ex}", $"同步服务器失败，回滚本地文件名发生异常==={kvp.Key}==={kvp.Value}");
                    }
                }

                throw new Exception("修改文件名称失败：" + ex.Message);
            }
        }
    }
}
