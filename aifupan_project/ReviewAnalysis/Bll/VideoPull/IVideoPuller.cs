using ReviewAnalysis.Bll.VideoPull.Models;
using System.Threading.Tasks;

namespace ReviewAnalysis.Bll.VideoPull
{
    /// <summary>
    /// 直播列表拉取接口。
    /// 各平台（巨量、来客等）实现各自的 Puller，统一通过此接口调用。
    /// </summary>
    public interface IVideoPuller
    {
        /// <summary>
        /// 检查主播是否已授权当前平台（Cookie 有效）
        /// </summary>
        /// <param name="secUid">主播 sec_uid</param>
        /// <returns>true=已授权，可拉取数据</returns>
        bool IsAuthorized(string secUid);

        /// <summary>
        /// 拉取主播最近 N 场直播记录
        /// </summary>
        /// <param name="secUid">主播 sec_uid</param>
        /// <param name="pullCount">拉取条数上限</param>
        /// <param name="pullDay">只拉取最近 N 天的数据</param>
        /// <returns>拉取结果，Success 标识成功/失败</returns>
        Task<VideoPullResult> GetRecentLiveSessionsAsync(string secUid, int pullCount, int pullDay);
        
        /// <summary>
        /// 查询当前在播的直播id
        /// </summary>
        /// <param name="secUid"></param>
        /// <returns></returns>
        Task<string> GetCurrentLiveRoomAsync(string secUid);
    }
}
