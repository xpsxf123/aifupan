
using douyin.Utils;
using MediaInfo;
using Newtonsoft.Json;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Attributes;
using ReviewAnalysis.Bll;
using ReviewAnalysis.bo.uploadFile;
using ReviewAnalysis.bo.video;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Dto;
using ReviewAnalysis.entity.uploadFile;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.trade;
using ReviewAnalysis.vo.uploadFile;
using Swan.Parsers;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Forms;

namespace ReviewAnalysis.Controller
{
    [RestController("配置相关的接口", "api/uploadfile")]
    public class UploadFileController
    {

        /// <summary>
        /// 压缩视频
        /// </summary>
        [HttpGet("压缩视频", "/compress")]
        public Dictionary<string, object> Compress(string fileId)
        {
            UploadFileBll uploadFileBll = new UploadFileBll();
            return uploadFileBll.Compress(fileId);

        }

        /// <summary>
        /// 确认消耗标注资源
        /// </summary>
        /// <param name="fileId">文件唯一标识</param>
        /// <param name="duration">消耗时长，单位：分钟</param>
        /// <param name="wordNum">消耗字数</param>
        [HttpGet("确认消耗标注资源", "/confirmusemark")]
        public void ConfirmUseMark(string fileId, int duration, int wordNum)
        {
            UploadFileBll uploadFileBll = new UploadFileBll();
            uploadFileBll.ConfirmUseMark(fileId, duration, wordNum);
        }

        /// <summary>
        /// 分享文件复盘
        /// </summary>
        /// <param name="fileId">文件唯一标识</param>
        [HttpGet("分享文件复盘", "/shareanalysis")]
        public string ShareAnalysis(string fileId, string onlineFileUrl)
        {
            UploadFileBll uploadFileBll = new UploadFileBll();
            return uploadFileBll.ShareAnalysis(fileId, onlineFileUrl);
        }

        /// <summary>
        /// 生成文件分析
        /// </summary>
        /// <param name="fileId">文件id</param>
        /// <param name="token">向服务器发请求的token</param>
        /// <param name="tradeId">行业id</param>
        [HttpGet("生成文件分析", "/createanalysis")]
        public string CreateAnalysis(string fileId, string token, string tradeId)
        {
            ServerTimeUtils.checkTimeAccurate();
            if (!ServerTimeUtils.timeAccurate)
            {
                throw new CustomException("本地电脑时间与北京时间不一致，请校准后再使用", 5601);
            }

            if(string.IsNullOrEmpty(tradeId))
            {
                throw new Exception("请选择行业后再分析");
            }

            if (AnchorVideoBll.isAnalysis)
            {
                // 有视频还没分析完，不给分析
                return null;
            }

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


            // 判断用户是否有足够的分析时长余额
            if (uploadFile.fileType == 0 || uploadFile.fileType == 1)
            {
                int minute = minute = Convert.ToInt32(uploadFile.fileDuration / 60) < 1 ? 1 : Convert.ToInt32(uploadFile.fileDuration / 60);

                int analysisStatus = UserPropertyHttpUtils.CheckAnalysisMinute(minute);
                if (analysisStatus == 2)
                {
                    AnchorVideoBll.isAnalysis = false;
                    throw new Exception("可用的分析资源不足，请前往购买");
                }
                else if (analysisStatus == 0)
                {
                    AnchorVideoBll.isAnalysis = false;
                    throw new Exception("网络不佳，请重试");
                }
            }

            // 修改视频分析状态
            UploadFileApi.UpdateFileAnalysisStatusSync(uploadFile.fileId, 1, "");

            AnchorVideoBll.isAnalysis = true;
            AnchorVideoBll.currentAnalysisId = fileId;

            // 异步线程，执行文件分析
            UploadFileBll uploadFileBll = new UploadFileBll();
            new Thread(() => uploadFileBll.Analysis(uploadFile, token, tradeId)).Start();

            return "success";

        }

        /// <summary>
        /// 检查是否有足够的资源分析文本文件
        /// </summary>
        /// <param name="fileId">文件id</param>
        [HttpGet("检查是否有足够的资源分析文本文件", "/checkTxtFileAnalysisProperty")]
        public CheckTxtFileAnalysisPropertyVo CheckTxtFileAnalysisProperty(string fileId)
        {
            UploadFileEntity uploadFile = UploadFileApi.GetFileByFileIdSync(fileId);

            if(uploadFile == null)
            {
                throw new CustomException("文件信息不存在", 6000);
            }

            if(uploadFile.fileType != 2)
            {
                throw new CustomException("当前不是文本文件", 6000);
            }

            if (!File.Exists(uploadFile.nowPath))
            {
                // 文件不存在
                AnchorVideoBll.isAnalysis = false;
                throw new Exception("文件不存在");
            }

            string content = File.ReadAllText(uploadFile.nowPath);
            int length = content.Length;
            long propertyWordNum = UserPropertyHttpUtils.GetTextWotdNum();

            CheckTxtFileAnalysisPropertyVo checkTxtFileAnalysisPropertyVo = new CheckTxtFileAnalysisPropertyVo();
            checkTxtFileAnalysisPropertyVo.propertyWordNum = propertyWordNum;
            checkTxtFileAnalysisPropertyVo.fileWordNum = length;

            if(length <= propertyWordNum)
            {
                checkTxtFileAnalysisPropertyVo.isSufficient = 1;
            }else
            {
                checkTxtFileAnalysisPropertyVo.isSufficient = 0;
            }

            return checkTxtFileAnalysisPropertyVo;
        }

        /// <summary>
        /// 选择上传的文件
        /// </summary>
        /// <param name="fileType">文件类型 0：音视频 1：文本文件</param>
        [HttpGet("选择上传的文件", "/checkUploadFile")]
        public Dictionary<string, object> CheckUploadFile(int fileType)
        {
            // 创建一个 OpenFileDialog 对象实例
            OpenFileDialog openFileDialog = new OpenFileDialog();

            // 设置文件筛选器
            if (fileType == 0)
            {
                openFileDialog.Filter = "音视频文件(*.ts, *.mp4, *.avi, *.flv, *.wmv, *.mov, *.mp3, *.aac)|*.ts;*.mp4;*.avi;*.flv;*.wmv;*.mov;*.mp3;*.aac";
            }
            else if (fileType == 1)
            {
                openFileDialog.Filter = "文本文件，Word 文件(*.txt, *.doc, *.docx)|*.txt;*.doc;*.docx";
            }

            // 设置是否检查文件是否存在，默认为 true
            openFileDialog.CheckFileExists = true;

            // 设置是否允许用户选择多个文件，默认为 false
            openFileDialog.Multiselect = false;

            // 显示文件选择对话框
            if (openFileDialog.ShowDialog() == DialogResult.OK)
            {

                // 所选文件的路径列表
                foreach (string filePath in openFileDialog.FileNames)
                {
                    // 返回文件信息
                    if(!string.IsNullOrEmpty(filePath))
                    {
                        FileInfo fileInfo = new FileInfo(filePath);
                        if(fileInfo.Exists)
                        {
                            Dictionary<string, object> result = new Dictionary<string, object>();
                            result.Add("fileName", filePath.Substring(filePath.LastIndexOf("\\") + 1));
                            result.Add("fileSize", fileInfo.Length);
                            result.Add("filePath", filePath);

                            return result;
                        }
                    }
                }
            }

            return null;
        }

        /// <summary>
        /// 上传文字
        /// </summary>
        /// <param name="uploadTxtFileByWordBo">参数</param>
        [HttpPost("上传文字", "/uploadTxtFileByWord")]
        public string uploadTxtFileByWord(UploadTxtFileByWordBo uploadTxtFileByWordBo)
        {
            if(string.IsNullOrEmpty(uploadTxtFileByWordBo.content))
            {
                throw new Exception("内容不能为空");
            }

            UploadFileBll uploadFileBll = new UploadFileBll();
            // 保存文件
            uploadFileBll.uploadTxtFileByWord(uploadTxtFileByWordBo);

            return "success";
        }

        /// <summary>
        /// 确认上传文件
        /// </summary>
        /// <param name="commitUploadFileBo">参数</param>
        [HttpPost("确认上传文件", "/commitUploadFile")]
        public async Task<string> CommitUploadFile(CommitUploadFileBo commitUploadFileBo)
        {

            if(string.IsNullOrEmpty(commitUploadFileBo.filePath) || !File.Exists(commitUploadFileBo.filePath))
            {
                throw new Exception("文件不存在");
            }

            if(commitUploadFileBo.filePath.Contains("%"))
            {
                throw new Exception("文件名含有特殊符号%，无法识别，请修改后上传。");
            }

            // 处理文件（在后台线程执行，避免阻塞 UI）
            UploadFileBll uploadFileBll = new UploadFileBll();
            
            // 使用 Task.Run 在后台线程执行文件操作
            await Task.Run(() =>
            {
                // 保存文件
                uploadFileBll.Save(commitUploadFileBo);
            }).ConfigureAwait(false);

            return "success";
        }

        /// <summary>
        /// 重新生成文件分析
        /// </summary>
        /// <param name="fileId">文件id</param>
        /// <param name="platformType">平台类型 0：全平台 1：抖音 2：快手 3：视频号 4：小红书</param>
        /// <param name="token">向服务器发请求的token</param>
        /// <param name="tradeId">行业id</param>
        [HttpGet("重新生成文件分析", "/reanalysis")]
        public string ReAnalysis(string fileId, int platformType, string token, string tradeId)
        {
            ServerTimeUtils.checkTimeAccurate();
            if (!ServerTimeUtils.timeAccurate)
            {
                throw new CustomException("本地电脑时间与北京时间不一致，请校准后再使用", 5601);
            }

            if (AnchorVideoBll.isAnalysis)
            {
                // 有视频还没分析完，不给分析
                return null;
            }

            
            UploadFileEntity uploadFileEntity = UploadFileApi.GetFileByFileIdSync(fileId);
            if(uploadFileEntity == null)
            {
                throw new Exception("文件信息不存在");
            }

            if (!File.Exists(uploadFileEntity.nowPath))
            {
                // 文件不存在
                throw new Exception("本地文件不存在");
            }

            var settings = new JsonSerializerSettings
            {
                ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver(),
                NullValueHandling = NullValueHandling.Ignore // 新增：忽略null值
            };
            uploadFileEntity.id = 0;
            UploadFile uploadFile = JsonConvert.DeserializeObject<UploadFile>(JsonConvert.SerializeObject(uploadFileEntity), settings);

            AnchorVideoBll.isAnalysis = true;
            AnchorVideoBll.currentAnalysisId = fileId;

            // 判断用户是否有足够的分析时长余额
            if (uploadFile.FileType == 0 || uploadFile.FileType == 1)
            {
                int minute = Convert.ToInt32(uploadFile.FileDuration / 60) < 1 ? 1 : Convert.ToInt32(uploadFile.FileDuration / 60);

                int analysisStatus = UserPropertyHttpUtils.CheckAnalysisMinute(minute);
                if (analysisStatus == 2)
                {
                    AnchorVideoBll.isAnalysis = false;
                    throw new Exception("可用的分析资源不足，请前往购买");
                }
                else if (analysisStatus == 0)
                {
                    AnchorVideoBll.isAnalysis = false;
                    throw new Exception("网络不佳，请重试");
                }
            }

            // 修改视频分析状态
            UploadFileApi.UpdateFileAnalysisStatusSync(uploadFileEntity.fileId, 1, "");

            UploadFileBll uploadFileBll = new UploadFileBll();
            new Thread(() => uploadFileBll.ReAnalysis(uploadFileEntity, platformType, token, tradeId)).Start();


            return "success";
        }

        /// <summary>
        /// 分页获取上传文件列表
        /// </summary>
        /// <param name="pageIndex">当前页 不传默认为1</param>
        /// <param name="pageSize">分页大小 不穿默认为10</param>
        /// <param name="analysisStatus">分析状态</param>
        /// <param name="fileName">文件名</param>
        /// <param name="recordStartDate">上传时间范围-开始</param>
        /// <param name="recordEndDate">上传时间范围-结束</param>
        /// <param name="analysisStartDate">分析时间范围-开始</param>
        /// <param name="analysisEndDate">分析时间范围-结束</param>
        /// <returns></returns>
        [HttpGet("分页获取上传文件列表", "/getpage")]
        public PageDto<UploadFileDto> GetPage(int pageIndex, int pageSize, int analysisStatus, string fileName,
            string uploadStartDate, string uploadEndDate, string analysisStartDate, string analysisEndDate)
        {
            UploadFileBll uploadFileBll = new UploadFileBll();
            return uploadFileBll.GetPage(pageIndex, pageSize, analysisStatus, fileName, uploadStartDate, uploadEndDate, analysisStartDate, analysisEndDate);
        }

        /// <summary>
        /// 查看文件分析
        /// </summary>
        /// <param name="fileId">文件id</param>
        [HttpGet("查看视频分析", "/lockanalysis")]
        public async Task<SentenceMarkDto> LockAnalysis(string fileId)
        {
            UploadFileBll uploadFileBll = new UploadFileBll();
            SentenceMarkDto result = await uploadFileBll.LockFileAnalysisAsync(fileId).ConfigureAwait(false);
            return result ?? new SentenceMarkDto();
        }

        /// <summary>
        /// 预览文件
        /// </summary>
        /// <param name="videoId">文件id</param>
        [HttpGet("预览文件", "/preview")]
        public string Preview(string fileId)
        {
            UploadFileBll uploadFileBll = new UploadFileBll();
            return uploadFileBll.Preview(fileId);
        }

        /// <summary>
        /// 重新选择行业分析
        /// </summary>
        /// <param name="fileId">文件id</param>
        /// <param name="tradeId">行业id</param>
        /// <param name="platformType">平台类型 0：全平台 1：...</param>
        /// <param name="duration">消耗时长，单位：分钟</param>
        /// <param name="wordNum">消耗字数</param>
        [HttpGet("重新选择行业分析", "/reanalysisbytrade")]
        public SentenceMarkDto ReAnalysisByTrade(string fileId, string tradeId, string platformType, int duration, int wordNum)
        {
            UploadFileBll uploadFileBll = new UploadFileBll();

            return uploadFileBll.ReAnalysisByTrade(fileId, tradeId, platformType, duration, wordNum);
        }

        /// <summary>
        /// 根据文件id集合删除文件
        /// <param name="ids">文件file_id集合</param>
        /// </summary>
        [HttpPost("根据文件id集合删除文件", "/deletebyids")]
        public string DeleteByIds(List<string> ids)
        {
            if(ids.Contains(AnchorVideoBll.currentAnalysisId) && AnchorVideoBll.isAnalysis)
            {
                return null;
            }
            UploadFileBll uploadFileBll = new UploadFileBll();
            uploadFileBll.Delete(ids);

            return "success";
        }

        /// <summary>
        /// 新增文件切片
        /// </summary>
        /// <param name="addVideoSliceBo">新增文件切片数据</param>
        /// <returns></returns>
        [HttpPost("新增视频切片", "/addFileSlice")]
        public void addFileSlice(AddFileSliceBo addFileSliceBo)
        {
            UploadFileBll uploadFileBll = new UploadFileBll();
            uploadFileBll.addFileSlice(addFileSliceBo);

        }

        /// <summary>
        /// 修改文件名称
        /// </summary>
        /// <param name="renameFileBo">修改文件名称参数</param>
        /// <returns></returns>
        [HttpPost("修改文件名称", "/renameFile")]
        public void RenameFile(RenameFileBo renameFileBo)
        {
            UploadFileBll uploadFileBll = new UploadFileBll();
            uploadFileBll.RenameFile(renameFileBo.fileId, renameFileBo.newFileName);
        }

    }


}
