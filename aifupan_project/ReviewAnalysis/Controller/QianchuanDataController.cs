using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.Model;
using ReviewAnalysis.qianchuan;
using ReviewAnalysis.Utils;
using System;
using System.Threading.Tasks;

namespace ReviewAnalysis.Controller
{
    /// <summary>
    /// 千川数据控制器
    /// </summary>
    [RestController("千川数据接口", "api/qianchuan")]
    public class QianchuanDataController
    {
        /// <summary>
        /// 拉取千川数据
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <param name="roomId">直播间ID</param>
        /// <param name="aavid">千川账户ID</param>
        /// <param name="anchorId">主播ID</param>
        [HttpGet("拉取千川数据", "/pullQianchuan")]
        public void PullQianchuan(string secUid, string roomId, string aavid, string anchorId)
        {
            try
            {
                if (string.IsNullOrEmpty(secUid))
                {
                    throw new Exception("主播SecUid不能为空");
                }

                if (string.IsNullOrEmpty(roomId))
                {
                    throw new Exception("直播间ID不能为空");
                }

                if (string.IsNullOrEmpty(aavid))
                {
                    throw new Exception("千川账户ID不能为空");
                }

                if (string.IsNullOrEmpty(anchorId))
                {
                    throw new Exception("主播ID不能为空");
                }

                AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
                if (anchorInfo == null)
                {
                    throw new Exception("主播信息不存在");
                }

                if (anchorInfo.qianchuanAuthStatus != 2)
                {
                    throw new Exception("主播未授权千川，请先授权");
                }

                _ = QianchuanDataHandle.PullQianchuanData(anchorInfo, roomId, aavid, anchorId);
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"拉取千川数据异常: {ex.Message}", "QianchuanDataController.PullQianchuan");
                throw;
            }
        }

        /// <summary>
        /// 获取千川账户列表
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <param name="roomId">直播间ID</param>
        [HttpGet("获取千川账户列表", "/getQianchuanAccountList")]
        public async Task<object> GetQianchuanAccountList(string secUid, string roomId)
        {
            try
            {
                if (string.IsNullOrEmpty(secUid))
                {
                    throw new Exception("主播SecUid不能为空");
                }

                if (string.IsNullOrEmpty(roomId))
                {
                    throw new Exception("直播间ID不能为空");
                }

                AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
                if (anchorInfo == null)
                {
                    throw new Exception("主播信息不存在");
                }

                if (anchorInfo.qianchuanAuthStatus != 2)
                {
                    throw new Exception("主播未授权千川，请先授权");
                }

                var cookies = QianchuanDataHandle.GetCookiesFromLocal(secUid);
                if (cookies.Count == 0)
                {
                    throw new Exception("本地无千川Cookie，无法获取账户列表");
                }

                var accountList = await QianchuanDataApi.GetAccountUserList(roomId, cookies, secUid);
                return new
                {
                    code = 0,
                    status = 200,
                    data = accountList
                };
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"获取千川账户列表异常: {ex.Message}", "QianchuanDataController.GetQianchuanAccountList");
                return new
                {
                    code = -1,
                    status = 500,
                    message = ex.Message
                };
            }
        }
    }
}