using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Attributes;
using ReviewAnalysis.Bll;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Generic;
using ReviewAnalysis.Model;
using System.Threading;
using System.Linq;
using douyin.Utils;
using System.IO;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.Bll.VideoPull;
using ReviewAnalysis.api;
using ReviewAnalysis.bo.anchor;
using System.Web.UI.WebControls;
using ReviewAnalysis.vo.anchor;
using ReviewAnalysis.qianchuan;
using ReviewAnalysis.life;
using ReviewAnalysis.enterprise;
using System.Threading.Tasks;

namespace ReviewAnalysis.Controller
{
    [RestController("关于主播和录屏的控制器", "api/anchorinfo")]
    public class AnchorInfoController
    {

        /// <summary>
        /// 打开主播的巨量百应页面
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        [HttpGet("打开主播的巨量百应页面", "/openAnchorJuliang")]
        public void openAnchorJuliang(string secUid)
        {
            AnchorBll anchorBll = new AnchorBll();
            anchorBll.openAnchorJuliang(secUid);
        }

        /// <summary>
        /// 取消授权主播巨量百应
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        [HttpGet("取消授权主播巨量百应", "/cancelAuthorizeJuliang")]
        public void cancelAuthorizeJuliang(string secUid)
        {
            AnchorBll anchorBll = new AnchorBll();
            anchorBll.cancelAuthorizeJuliang(secUid);
        }

        /// <summary>
        /// 授权主播巨量百应
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <param name="authType">登录类型 0：登录主账号 1：登录子账号</param>
        [HttpGet("授权主播巨量百应", "/authorizeJuliang")]
        public void authorizeJuliang(string secUid, int authType)
        {
            AnchorBll anchorBll = new AnchorBll();
            anchorBll.AuthorizeJuliang(secUid, authType);
        }

        /// <summary>
        /// 授权主播千川
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <param name="authType">登录类型 0：登录主账号 1：登录子账号</param>
        [HttpGet("授权主播千川", "/authorizeQianchuan")]
        public async Task authorizeQianchuan(string secUid, int authType)
        {
            AnchorBll anchorBll = new AnchorBll();
            anchorBll.AuthorizeQianchuan(secUid, authType);
        }

        /// <summary>
        /// 取消授权主播千川
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        [HttpGet("取消授权主播千川", "/cancelAuthorizeQianchuan")]
        public void cancelAuthorizeQianchuan(string secUid)
        {
            AnchorBll anchorBll = new AnchorBll();
            anchorBll.CancelAuthorizeQianchuan(secUid);
        }

        /// <summary>
        /// 授权主播来客
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <param name="authType">登录类型 0：登录主账号 1：登录子账号</param>
        [HttpGet("授权主播来客", "/authorizeLife")]
        public void authorizeLife(string secUid, int authType)
        {
            AnchorBll anchorBll = new AnchorBll();
            anchorBll.AuthorizeLife(secUid, authType);
        }

        /// <summary>
        /// 取消授权主播来客
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        [HttpGet("取消授权主播来客", "/cancelAuthorizeLife")]
        public void cancelAuthorizeLife(string secUid)
        {
            AnchorBll anchorBll = new AnchorBll();
            anchorBll.CancelAuthorizeLife(secUid);
        }

        /// <summary>
        /// 授权主播企业号
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <param name="authType">登录类型 0：登录主账号 1：登录子账号</param>
        [HttpGet("授权主播企业号", "/authorizeEnterprise")]
        public void authorizeEnterprise(string secUid, int authType)
        {
            AnchorBll anchorBll = new AnchorBll();
            anchorBll.AuthorizeEnterprise(secUid, authType);
        }

        /// <summary>
        /// 取消授权主播企业号
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        [HttpGet("取消授权主播企业号", "/cancelAuthorizeEnterprise")]
        public void cancelAuthorizeEnterprise(string secUid)
        {
            AnchorBll anchorBll = new AnchorBll();
            anchorBll.CancelAuthorizeEnterprise(secUid);
        }

        /// <summary>
        /// 授权主播后台
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <param name="authType">登录类型 0：登录主账号 1：登录子账号</param>
        [HttpGet("授权主播后台", "/anchorLive")]
        public void authorizeAnchorLive(string secUid, int authType)
        {
            AnchorBll anchorBll = new AnchorBll();
            anchorBll.AuthorizeAnchorLive(secUid, authType);
        }

        /// <summary>
        /// 取消授权主播后台
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        [HttpGet("取消授权主播后台", "/cancelAuthorizeAnchorLive")]
        public void cancelAuthorizeAnchorLive(string secUid)
        {
            AnchorBll anchorBll = new AnchorBll();
            anchorBll.CancelAuthorizeAnchorLive(secUid);
        }

        /// <summary>
        /// 授权抖音
        /// </summary>
        [HttpGet("授权抖音", "/authorizeDouyin")]
        public void authorizeDouyin()
        {
            AnchorBll anchorBll = new AnchorBll();
            anchorBll.AuthorizeDouyin();
        }

        /// <summary>
        /// 取消授权抖音
        /// </summary>
        [HttpGet("取消授权抖音", "/cancelAuthorizeDouyin")]
        public void cancelAuthorizeDouyin()
        {
            AnchorBll anchorBll = new AnchorBll();
            anchorBll.CancelAuthorizeDouyin();
        }

        /// <summary>
        /// 拉取巨量数据
        /// </summary>
        /// <param name="videoId">视频id</param>
        [HttpGet("拉取巨量数据", "/pullJuliang")]
        public async Task pullJuliang(string videoId)
        {
            AnchorBll anchorBll = new AnchorBll();
            await anchorBll.PullJuliangDataAsync(videoId);
        }


        /// <summary>
        /// 授权微信视频号
        /// </summary>
        /// <param name="redirectUrl">授权成功的返回地址</param>
        [HttpGet("授权微信视频号", "/authorizeWeChatChannels")]
        public void AuthorizeWeChatChannels(string redirectUrl)
        {
            // 判断是否还有微信视频号主播位
            bool flag = UserPropertyHttpUtils.CheckAddAnchor(1, 2);
            if (!flag)
            {
                throw new Exception("可添加的视频号主播数量不足,请购买资源");
            }

            AnchorBll anchorBll = new AnchorBll();
            anchorBll.AuthorizeWeChatChannels(redirectUrl);
        }

        /// <summary>
        /// 添加/修改主播
        /// </summary>
        /// <param name="anchorBo">主播信息</param>
        [HttpPost("添加主播", "/addOrUpdateAnchor")]
        public void AddOrUpdateAnchor(AddOrUpdateAnchorBo anchorBo)
        {

            if (string.IsNullOrEmpty(anchorBo.secUid))
            {
                // 判断是否还有主播位
                bool flag = UserPropertyHttpUtils.CheckAddAnchor(1, (int)anchorBo.platform);
                if (!flag)
                {
                    throw new Exception("可添加的主播数量不足,请购买资源");
                }
            }
            
            AnchorBll anchorBll = new AnchorBll();
            anchorBll.AddOrUpdateAnchor(anchorBo);
        }

        /// <summary>
        /// 从恢复列表添加主播
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        [HttpGet("添加主播", "/reAddAnchor")]
        public void reAddAnchor(string secUid)
        {

            AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
            if (anchorInfo == null)
            {
                throw new Exception("主播信息不存在");
            }

            // 判断是否还有主播位
            bool flag = UserPropertyHttpUtils.CheckAddAnchor(1, anchorInfo.platform);
            if (!flag)
            {
                throw new Exception("可添加的主播数量不足,请购买资源");
            }

            AnchorBll anchorBll = new AnchorBll();
            anchorBll.reAddAnchor(secUid);
        }


        /// <summary>
        /// 置顶主播
        /// </summary>
        /// <param name="secUid">主播secuid</param>
        /// <param name="action">动作 0：取消置顶 1：置顶</param>
        [HttpPost("置顶主播", "/topAnchor")]
        public void topAnchor(string secUid, int action)
        {
            OperationAnchorBll operationAnchorBll = FormUtils.GetOperationBll();
            operationAnchorBll.topAnchor(secUid, action);
        }


        /// <summary>
        /// 分页获取主播资料
        /// </summary>
        /// <param name="anchorListBo">查询参数</param>
        /// <returns></returns>
        [HttpPost("分页获取主播列表","/getpageanchor")]
        public async Task<AnchorPageDto<AnchorDto>> GetPageAnchor(AnchorListBo anchorListBo)
        {
            OperationAnchorBll anchorRecord = FormUtils.GetOperationBll();
            return await anchorRecord.GetPageAsync(anchorListBo).ConfigureAwait(false);
        }

        /// <summary>
        /// 获取单个直播信息
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        /// <returns></returns>
        [HttpGet("获取单个直播信息", "/getAnchorInfo")]
        public AnchorDto getAnchorInfo(string secUid)
        {
            AnchorBll anchorBll = new AnchorBll();
            return anchorBll.getAnchorInfo(secUid);
        }

        [HttpPost("更新ai页面的部分主播字段", "/updateAiPartial")]
        public void updateAiPartial(AiAnchorInfoVo vo)
        {
            AnchorBll anchorBll = new AnchorBll();
            anchorBll.updateAiPartial(vo);
        }

        /// <summary>
        /// 检测录制
        /// </summary>
        /// <param name="VideoType">视频类型 video_type 0 ts,1 flv,2 mp4</param>
        /// <param name="definition">清晰度 definition 0 标清 1高清 2超清 3 蓝光</param>
        [HttpGet("检测录制", "/decector")]
        public void Decector(int VideoType, int Definition) 
        {
            ServerTimeUtils.checkTimeAccurate();
            if (!ServerTimeUtils.timeAccurate)
            {
                throw new CustomException("本地电脑时间与北京时间不一致，请校准后再使用", 5601);
            }

            // 检测磁盘空间是否小于10G，小于不给录制
            ConfigBll configBll = new ConfigBll();
            Config config = configBll.GetModel();
            string savePath = config.SavePath;
            string driveLetter = Path.GetPathRoot(savePath).Substring(0, 1);
            Dictionary<string, long> dictionary = FileUtils.GetDiskSize(driveLetter);
            long driveAvailablepace = 0;
            try
            {
                driveAvailablepace = dictionary["driveAvailablepace"];
            }
            catch (Exception ex)
            {
                throw new Exception("找不到存储盘");
            }
            if (driveAvailablepace < 10)
            {
                throw new CustomException("磁盘剩余空间少于10G，请清理磁盘空间或更换存储位置后再尝试", 5701);
            }
            //线程安全检查：避免重复启动

            try
            {
                // 启动后台长时间任务（异步等待，不阻塞UI）
                //Task.Run(() => DetectionBackgroundManager.LongRunningTask(cancellationToken, VideoType, Definition), cancellationToken);
                AnchorBll.StartDetectionAll(VideoType, Definition);
                // await longRunningTask;
            }
            catch (OperationCanceledException)
            {
                // 捕获任务取消异常（正常停止）

            }
            catch (Exception ex)
            {
                // 捕获其他异常

            }
            finally
            {
                //// 清理资源+重置状态
                //lock (DetectionBackgroundManager._lockObj)
                //{
                //    DetectionBackgroundManager._isRunning = false;
                //}
                //DetectionBackgroundManager._cts?.Dispose();
                //DetectionBackgroundManager._cts = null;

            }

           //AnchorBll.StartDetectionAll(VideoType, Definition);
        }

        /// <summary>
        /// 打开当前存储视频的目录
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        /// <returns></returns>
        [HttpGet("打开指定的目录", "/openfolder")]
        public bool OpenFolder(string secUid)
        {
            //AnchorRecordBll anchorRecord = new AnchorRecordBll();
            OperationAnchorBll operationAnchorBll = FormUtils.GetOperationBll();
            return operationAnchorBll.OpenFolder(secUid);
        }

        /// <summary>
        /// 停止所有的录制
        /// </summary>
        [HttpGet("停止所有的录制", "/stopdecector")]
        public async Task StopDecector() 
        {
            await AnchorBll.StopDecectorAll();
        }

        /// <summary>
        /// 预览录制的视频
        /// </summary>
        /// <param name="secUid">主播的secUid</param>
        [HttpGet("视频预览", "/previewvideo")]
        public void PreviewVideo(string secUid) 
        {
            OperationAnchorBll anchorRecord = FormUtils.GetOperationBll();
            anchorRecord.PreviewVideo(secUid);
        }

        /// <summary>
        /// 停止单个主播录制
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        [HttpGet("停止单个主播录制","/stoprecord")]
        public async Task StopRecord(string secUid) 
        {
            await AnchorBll.StopRecord(secUid);
        }

        /// <summary>
        /// 开启单个主播录制
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        [HttpGet("开启单个主播录制", "/StartRecord")]
        public void StartRecord(string secUid)
        {
            ServerTimeUtils.checkTimeAccurate();
            if (!ServerTimeUtils.timeAccurate)
            {
                throw new CustomException("本地电脑时间与北京时间不一致，请校准后再使用", 5601);
            }

            AnchorBll.StartRecord(secUid);


        }

        /// <summary>
        /// 将视频转换为mp4
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        [HttpGet("将视频转换为mp4", "/converttomp4")]
        public void ConvertToMp4(string secUid) 
        {
           
        }

        ///<summary>
        /// 开启/关闭自动录制
        /// </summary>
        /// <param name="secUid">主播Id</param>
        /// <param name="isAuto">是否开启自动录制 0否，1是</param>
        [HttpGet("开启/关闭自动录制", "/openorcloseautorecord")]
        public void OpenOrCloseAutoRecord(string secUid, int isAuto) 
        {
            ServerTimeUtils.checkTimeAccurate();
            if (!ServerTimeUtils.timeAccurate)
            {
                throw new CustomException("本地电脑时间与北京时间不一致，请校准后再使用", 5601);
            }
            AnchorBll.OpenOrCloseAutoRecord(secUid, isAuto);

            
        }

        /// <summary>
        /// 开启/关闭弹幕监控
        /// </summary>
        /// <param name="secUid">主播id</param>
        /// <param name="IsBarrageMonitoring">弹幕监控 0否 1是</param>
        [HttpGet("开启/关闭弹幕监控", "/updateBarrageMonitoring")]
        public void UpdateBarrageMonitoring(string secUid, int isBarrageMonitoring)
        {
            OperationAnchorBll operationAnchorBll = FormUtils.GetOperationBll();
            operationAnchorBll.UpdateBarrageMonitoring(secUid, isBarrageMonitoring);
        }

        /// <summary>
        /// 开启/关闭自动上传云空间
        /// </summary>
        /// <param name="secUid">主播id</param>
        /// <param name="isAutoUploadCloud">自动上传云空间 0否 1是</param>
        [HttpGet("开启/关闭自动上传云空间", "/openOrCloseAutoUploadCloud")]
        public void OpenOrCloseAutoUploadCloud(string secUid, int isAutoUploadCloud)
        {
            OperationAnchorBll operationAnchorBll = FormUtils.GetOperationBll();
            operationAnchorBll.OpenOrCloseAutoUploadCloud(secUid, isAutoUploadCloud);
        }

        /// <summary>
        /// 开启/关闭主播的数据看板
        /// </summary>
        /// <param name="secUid">主播id</param>
        /// <param name="isDataViewing">是否开启数据看板 0：否 1：是</param>
        [HttpGet("开启/关闭主播的数据看板", "/openOrCloseDataViewing")]
        public void OpenOrCloseDataViewing(string secUid, int isDataViewing)
        {
            OperationAnchorBll operationAnchorBll = FormUtils.GetOperationBll();
            operationAnchorBll.OpenOrCloseDataViewing(secUid, isDataViewing);
        }

        
        /// <summary>
        /// 修改主播的纯录制版是否获取在线人数状态
        /// </summary>
        /// <param name="secUid">主播id</param>
        /// <param name="pureRecordOnlineNum">纯录制版是否获取在线人数 0：否 1：是</param>
        [HttpGet("修改主播的纯录制版是否获取在线人数状态", "/updatePureRecordOnlineNum")]
        public void updatePureRecordOnlineNum(string secUid, int pureRecordOnlineNum)
        {
            OperationAnchorBll operationAnchorBll = FormUtils.GetOperationBll();
            operationAnchorBll.updatePureRecordOnlineNum(secUid, pureRecordOnlineNum);
        }

        /// <summary>
        /// 移除主播
        /// </summary>
        /// <param name="secUid"></param>
        [HttpGet("移除主播", "/removeanchor")]
        public void RemoveAnchor(string secUid) 
        {

            AnchorBll.RemoveAnchor(secUid);
		}

        /// <summary>
        /// 从恢复列表移除主播
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        [HttpGet("移除主播", "/fullRemoveAnchor")]
        public void fullRemoveAnchor(string secUid)
        {

            AnchorBll.fullRemoveAnchor(secUid);
        }

        /// <summary>
        /// 修改主播的行业
        /// </summary>
        /// <param name="anchorId"></param>
        [HttpGet("修改主播的行业", "/updateanchortrade")]
        public void UpdateAnchorTrade(string secUid, string tradeId)
        {
            AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
            if(anchorInfo == null)
            {
                throw new CustomException("主播信息不存在");
            }

            AnchorApi.UpdateUserAnchorSync(secUid, tradeId, -1, -1);

            anchorInfo.TradeId = tradeId;
            AnchorCacheManager.SetAnchorCache(anchorInfo);

        }

        /// <summary>
        /// 判断是否存在主播
        /// </summary>
        [HttpGet("判断是否存在主播", "/existAnchor")]
        public bool existAnchor()
        {
            List<AnchorInfo> anchorInfos = AnchorCacheManager.GetAllAnchors();
            return anchorInfos != null && anchorInfos.Count > 0;
        }

        /// <summary>
        /// 拉取主播直播记录并保存
        /// </summary>
        /// <param name="request">请求参数</param>
        [HttpPost("拉取直播记录", "/pullVideoData")]
        public async Task PullVideoData(PullVideoDataRequest request)
        {
            var manager = new VideoPullManager();
            await manager.PullAndSave(request.secUid, request.platform ?? "juliang");
        }


    }
}
