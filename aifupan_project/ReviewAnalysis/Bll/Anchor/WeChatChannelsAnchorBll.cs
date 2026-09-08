using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Bll.Anchor.Entity;
using ReviewAnalysis.Model;
using ReviewAnalysis.WeChatChannels.Services;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using TencentCloud.Asr.V20190614.Models;

namespace ReviewAnalysis.Bll.Anchor
{
    public static class WeChatChannelsAnchorBll
    {
        /// <summary>
        /// 获得主播在线状态
        /// </summary>        
        /// <param name="exportId">直播 Id</param>
        /// <returns>0：检测失败 1：在线 2：离线</returns>
        public static async Task<int> GetOnlineStatus(AnchorInfo anchorInfo)
        {
            try
            {
                // 用户已获得直播 Id 时使用心跳接口查询在线状态，心跳接口更准确延迟更低。
                if (!string.IsNullOrEmpty(anchorInfo.BatchNumber))
                {
                    var model = await WeChatChannelsSerivce.QueryLiveHeartbeat(anchorInfo.BatchNumber);

                    if (model.Finish)
                    {
                        // 直播已结束，将直播 Id 设置为 null 以避免再次调用心跳接口。
                        anchorInfo.BatchNumber = null;
                        return 2;
                    }

                    return 1;
                }
                else
                {
                    var liveInfo = await WeChatChannelsSerivce.QueryLiveInfo(int.Parse(anchorInfo.SecUid));

                    // OpenLive 仅判断是否有直播流地址，并非准确开播状态。开播状态需要使用 LiveStatus 判断。
                    if (liveInfo.OpenLive && liveInfo.LiveStatus != 0 && liveInfo.LiveStatus != 4)
                    {
                        return 1;
                    }

                    return 2;
                }
            }
            catch(Exception ex)
            {
                FileUtils.LogError(ex.ToString(), "微信视频号获得主播在线状态异常！");
                return 0;
            }
        }

        public static async Task<DouYinAnchorInfoEntity> GetLiveAnchorInfo(string secUid)
        {
            var liveInfo = await WeChatChannelsSerivce.QueryLiveInfo(int.Parse(secUid));

            var result = new DouYinAnchorInfoEntity();
            result.SecUid = secUid;
            result.AnchorId = secUid;
            result.AnchorName = liveInfo.Live.Nickname;
            result.AnchorThump = liveInfo.Live.HeadUrl;
            result.RoomConverUrl = liveInfo.Live.CoverUrl;
            result.RoomTitle = liveInfo.Live.Description;
            result.RoomId = liveInfo.RecentlyExportId;

            result.StreamInfos = new List<DouYinAnchorInfoEntity.StreamInfo>
            {
                new DouYinAnchorInfoEntity.StreamInfo
                {
                    LiveSource = 0, // 微信视频号默认为 flv，flv 格式部分直播流不能录制。改成 m3u8 后正常，格式会在后面被转换成 m3u8。
                    Quality = 0,    // 微信视频号只有一个直播流地址，也只有一种清晰度。
                    StreaUrl = liveInfo.Live.PullStream
                }
            };

            return result;
        }

        public static AnchorInfo GetLatelyAuthorizationAnchorInfo()
        {
            var result = new AnchorInfo();
            var authorizationResult = WeChatChannelsSerivce.GetLatelyAuthorizationResult();
            if (authorizationResult == null)
            {
                throw new Exception("未获得当前主播的授权数据，请重新授权！");
            }

            result.SecUid = authorizationResult.AuthorizerInfoId.ToString();
            result.AnchorUserId = authorizationResult.AuthorizerInfoId.ToString();
            result.anchorNumber = authorizationResult.AuthorizerInfoId.ToString();
            result.AnchorName = authorizationResult.AccountBody.NickName;
            result.AnchorAvatar = authorizationResult.AccountBody.HeadImg;
            result.AnchorPlatform = "WeChatChannelsLive";
            result.HomeUrl = $"https://weixin.qq.com/?{authorizationResult.AuthorizerInfoId}";  // 微信视频号没有直播网站，但需要默认值以避免出现值为 null 抛出异常
            result.LiveUrl = $"https://weixin.qq.com/?{authorizationResult.AuthorizerInfoId}";  // 微信视频号没有直播网站，但需要默认值以避免出现值为 null 抛出异常
            result.platform = 2;
            result.WeChatChannelsAuthStatus = 1;
            result.IsAutoUploadCloud = 1;

            return result;
        }
    }
}