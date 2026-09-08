using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Policy;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Input;
using COSXML.Network;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.bo.shortVideo;
using ReviewAnalysis.ShortVideo.Enums;
using ReviewAnalysis.ShortVideo.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using ReviewAnalysis.vo.proxyIP;
using ReviewAnalysis.vo.shortVideo;
using static ReviewAnalysis.Utils.HttpUtils;

namespace ReviewAnalysis.api
{
    public class ShortVideoApi
    {

        /// <summary>
        /// 视频文案提取（状态机接口）
        ///                     **核心状态机接口**：支持URL链接和本地上传两种方式的视频文案提取，通过状态参数控制不同的处理阶段。
        ///
        ///                     ## 🔄 状态转换流程
        ///
        ///                     ### 1️⃣ PENDING (1) - 待处理
        ///                     **首次提交**：创建提取任务
        ///                     - 必填：videoTitle（视频标题）、videoUrl（视频URL）
        ///                     - 创建视频基础信息和用户关联记录
        ///                     - 状态：PENDING → 返回任务ID
        ///
        ///                     ### 2️⃣ PROCESSING (2) - 处理中
        ///                     **上传完成**：提交视频文件信息
        ///                     - 必填：id（任务ID）、videoHash（文件哈希）、coverUrl（封面URL）
        ///                     - 检查是否已有相同文件的提取结果（基于hash）
        ///                     - 如有现成结果：直接返回并标记COMPLETED
        ///                     - 如无现成结果：标记PROCESSING，等待客户端处理
        ///
        ///                     ### 3️⃣ COMPLETED (3) - 已完成
        ///                     **提取完成**：提交文案内容
        ///                     - 必填：id（任务ID）、extractContent（文案内容）
        ///                     - 保存文案到MongoDB
        ///                     - 状态：PROCESSING → COMPLETED
        ///                     - 扣减用户套餐资产
        ///
        ///                     ### 4️⃣ FAILED (4) - 失败
        ///                     **处理失败**：标记失败原因
        ///                     - 必填：id（任务ID）、extractErrorReason（失败原因）
        ///                     - 状态：PROCESSING → FAILED
        ///
        ///                     ## 🔒 权限控制
        ///                     - 租户隔离：只能操作自己租户下的数据
        ///                     - 用户权限：子账号只能操作自己的记录
        ///
        ///                     ## ⚡ 性能优化
        ///                     - 文件去重：相同hash的文件直接复用已有结果
        ///                     - 事务保证：确保数据一致性
        /// </summary>
        /// <param name="bo"></param>
        /// <returns></returns>
        public static VideoUserVideoVo urlOrLocal(VideoExtractBo bo)
        {
            string dataStr = "";
            string respnseBody = HttpUtils.SendPost(ReplayHttpUtils.BaseUrl + "/video/extract/urlOrLocal", bo);
            if (!string.IsNullOrEmpty(respnseBody))
            {
                var resultObj = JsonConvert.DeserializeObject<dynamic>(respnseBody);
                if (resultObj != null && resultObj.code == 0)
                {
                    if (resultObj.data != null)
                    {
                        dataStr = resultObj.data.ToString();
                    }
                }
                else if (!string.IsNullOrEmpty(resultObj?.msg?.ToString() ?? ""))
                {
                    FileUtils.LogError($"{respnseBody}", $"往服务端发送请求-urlOrLocal，发生异常");
                    throw new CustomException(resultObj?.msg?.ToString() ?? "");
                }
            }

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<VideoUserVideoVo>(dataStr);
            }

            return null;
        }

        /// <summary>
        /// 重新视频提取
        /// </summary>
        /// <param name="id"></param>
        /// <returns></returns>
        public static bool extractReuse(long id)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("userVideoId", id);
            string dataStr = "";
            string respnseBody = HttpUtils.SendGet(ReplayHttpUtils.BaseUrl + "/video/extract/reuse", param);
            if (!string.IsNullOrEmpty(respnseBody))
            {
                var resultObj = JsonConvert.DeserializeObject<dynamic>(respnseBody);
                if (resultObj != null && resultObj.code == 0)
                {
                    if (resultObj.data != null)
                    {
                        dataStr = resultObj.data.ToString();
                    }
                }
                else if (!string.IsNullOrEmpty(resultObj?.msg?.ToString() ?? ""))
                {
                    FileUtils.LogError($"{respnseBody}", $"往服务端发送请求-urlOrLocal，发生异常");
                    throw new CustomException(resultObj?.msg?.ToString() ?? "当前网络情况不佳，请稍后重试或联系管理员");
                }
            }


            bool result = false;
            if (!string.IsNullOrEmpty(dataStr))
            {
                bool.TryParse(dataStr, out result);
            }

            return result;
        }

        /// <summary>
        /// 客户端-批量创建提取文案任务
        /// </summary>
        /// <param name="param"></param>
        /// <returns></returns>
        public static List<VideoUserVideoVo> batchCreateExtract(Dictionary<string, object> param)
        {
            string dataStr = HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/video/extract/batchCreateExtract", param);
            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<List<VideoUserVideoVo>>(dataStr);
            }

            return new List<VideoUserVideoVo>();
        }

        /// <summary>
        /// 查询待处理队列数据
        /// </summary>
        /// <returns></returns>
        public static List<VideoUserVideoVo> getPendingQueue()
        {
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/video/extract/pendingQueue", null);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<List<VideoUserVideoVo>>(dataStr);
            }

            return null;
        }

        /// <summary>
        /// 获取封面图片上传的预签名链接
        /// </summary>
        /// <param name="suffix">图片的后缀</param>
        /// <returns></returns>
        public static SignUploadUrlVo getCoverImgPutUrl(string suffix)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("suffix", suffix);
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/video/extract/getCoverImgPutUrl", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<SignUploadUrlVo>(dataStr);
            }

            return null;
        }

        /// <summary>
        /// 客户端-保存搜索达人数据
        /// 根据关键词搜索达人信息保存
        /// </summary>
        /// <param name="bo">达人搜索业务对象</param>
        /// <returns>返回保存的记录ID</returns>
        public static long? saveSearchInfluencers(InfluencerSearchBo bo)
        {
            R res = HttpUtils.SendServerPostR(ReplayHttpUtils.BaseUrl + "/video/influencer/saveSearch", bo);

            if (res?.code == 0 && !string.IsNullOrEmpty(res?.data?.ToString() ?? ""))
            {
                if(long.TryParse(res?.data, out long result))
                {
                    return result;
                }
            }
            else
            {
                throw new CustomException(res?.msg ?? "");
            }
            return null;
        }

        /// <summary>
        /// 判断达人是否需要同步视频
        /// </summary>
        /// <param name="platformUserId"></param>
        /// <param name="platformType"></param>
        /// <returns></returns>
        public static bool hasInfluencerSyncVideos(string platformUserId, int platformType = 1)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("platformUserId", platformUserId);
            param.Add("platformType", platformType);

            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/video/influencer/hasInfluencerSyncVideos", param);

            bool result = false;
            if (!string.IsNullOrEmpty(dataStr))
            {
                bool.TryParse(dataStr, out result);
            }

            return result;
        }

        /// <summary>
        /// 客户端-同步达人数据（包含短视频信息）
        /// </summary>
        /// <param name="list"></param>
        /// <returns></returns>
        public static bool syncInfluencerInfo(SyncInfluencerInfoBo bo)
        {

            string dataStr = HttpUtils.SendServerPut(ReplayHttpUtils.BaseUrl + "/video/influencer/syncInfluencerInfo", bo);

            bool result = false;
            if (!string.IsNullOrEmpty(dataStr))
            {
                bool.TryParse(dataStr, out result);
            }

            return result;
        }

        /// <summary>
        /// 添加达人订阅
        /// </summary>
        /// <param name="influencerBo"></param>
        /// <returns></returns>
        public static bool subscribe(VideoInfluencerSubscribeBo influencerBo)
        {
            R res = HttpUtils.SendServerPostR(ReplayHttpUtils.BaseUrl + "/video/influencer/subscribe", influencerBo);

            bool result = false;
            if (res.success())
            {
                if (!string.IsNullOrEmpty(res.data))
                {
                    bool.TryParse(res.data, out result);
                }
            }
            else
            {
                throw new CustomException(res.msg ?? "当前网络情况不佳，请稍后重试或联系管理员");
            }
            return result;
        }

        /// <summary>
        /// 获取订阅的达人
        /// </summary>
        /// <returns></returns>
        public static List<UserInfluencerSubscriptionVo> influencerSubscriptionList(long? influencerId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("influencerId", influencerId);
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/video/influencer/subscription/list", param);
            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<List<UserInfluencerSubscriptionVo>>(dataStr);
            }

            return new List<UserInfluencerSubscriptionVo>();
        }

        /// <summary>
        /// 获取订阅的爆款
        /// </summary>
        /// <returns></returns>
        public static List<VideoUserHotSubscriptionAllVo> hotSearchSubscriptionList()
        {
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/video/hotSearch/subscription/list", null);
            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<List<VideoUserHotSubscriptionAllVo>>(dataStr);
            }

            return new List<VideoUserHotSubscriptionAllVo>();
        }

        /// <summary>
        /// 查询服务器该关键词是否已经存在
        /// </summary>
        /// <param name="keyword"></param>
        /// <returns></returns>
        public static List<VideoHotSearchVideoInfoVo> syncHutSearchKeywords(string keyword, int platformType)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("keyword", keyword);
            param.Add("platformType", platformType);

            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/video/hotSearch/videoList", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<List<VideoHotSearchVideoInfoVo>>(dataStr);
            }

            return new List<VideoHotSearchVideoInfoVo>();
        }

        /// <summary>
        /// 客户端-保存搜索爆款数据
        /// </summary>
        /// <param name="bo"></param>
        /// <returns></returns>
        public static long? saveSearchHotVideos(VideoHotSearchBo bo)
        {
            R res = HttpUtils.SendServerPostR(ReplayHttpUtils.BaseUrl + "/video/hotSearch/saveSearch", bo);

            long? result = null;
            if (res.success() && !string.IsNullOrEmpty(res.data))
            {
                if (long.TryParse(res.data, out long tempResult))
                {
                    result = tempResult;
                }
            }

            return result;
        }

        /// <summary>
        /// WEB端-添加爆款订阅
        /// </summary>
        /// <param name="bo"></param>
        /// <returns></returns>
        public static VideoUserHotSubscriptionVo addSubscription(VideoHotSubscriptionAddBo bo)
        {
            R r = HttpUtils.SendServerPostR(ReplayHttpUtils.BaseUrl + "/video/hotSearch/subscription", bo);

            if ((r?.code ?? -1) == 0 && !string.IsNullOrEmpty(r?.data?.ToString() ?? ""))
            {
                return JsonConvert.DeserializeObject<VideoUserHotSubscriptionVo>(r?.data?.ToString() ?? "");
            }
            else
            {
                throw new CustomException((r?.msg?.ToString() ?? "当前网络情况不佳，请稍后重试或联系管理员"));
            }

            return null;
        }


        /// <summary>
        /// 客户端-同步爆款订阅数据
        /// </summary>
        /// <param name="bo"></param>
        /// <returns></returns>
        public static bool syncVideoHotSearchData(VideoHotSearchSyncDataBo bo)
        {
            string dataStr = HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/video/hotSearch/syncVideoHotSearchData", bo);

            bool result = false;
            if (!string.IsNullOrEmpty(dataStr))
            {
                bool.TryParse(dataStr, out result);
            }

            return result;
        }

        /// <summary>
        /// 获取视频详情
        /// </summary>
        /// <param name="videoId"></param>
        /// <returns></returns>
        public static VideoInfoVo getVideoInfoByVideoId(long? videoId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);

            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/video/extract/getVideoInfoByVideoId", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<VideoInfoVo>(dataStr);
            }

            return null;
        }

        /// <summary>
        /// 重新统计用户的短视频资产
        /// </summary>
        /// <returns></returns>
        public static bool syncUserShortVideoProperty()
        {
            R res = HttpUtils.SendServerGetR(ReplayHttpUtils.BaseUrl + "/video/extract/syncUserShortVideoProperty", null);
            bool result = false;
            if (res.success() && !string.IsNullOrEmpty(res.data))
            {
                bool.TryParse(res.data, out result);
            }

            return result;
        }
    }
}
