using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.bo.anchor;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Dto;
using ReviewAnalysis.entity.anchor;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.anchor;
using ReviewAnalysis.vo.trade;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using static ReviewAnalysis.Utils.HttpUtils;

namespace ReviewAnalysis.api
{
    public class AnchorApi
    {

        /// <summary>
        /// 根据secUid获取主播信息-异步
        /// </summary>
        /// <param name="secUid">主播id</param>
        /// <returns></returns>
        public static async Task<AnchorEntity> GetAnchorBySecUid(string secUid)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("secUid", secUid);

            string dataStr = await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/anchorurl/getUserAnchorBySecUid", param);

            if (!string.IsNullOrEmpty(dataStr))
            {

                AnchorEntity anchorEntity = JsonConvert.DeserializeObject<AnchorEntity>(dataStr);
                return anchorEntity;
            }

            return null;
        }
        /// <summary>
        /// 根据secUid获取主播信息-同步
        /// </summary>
        /// <param name="secUid">主播id</param>
        /// <returns></returns>
        public static AnchorEntity GetAnchorBySecUidSync(string secUid)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("secUid", secUid);

            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/anchorurl/getUserAnchorBySecUid", param);

            if (!string.IsNullOrEmpty(dataStr))
            {

                AnchorEntity anchorEntity = JsonConvert.DeserializeObject<AnchorEntity>(dataStr);
                return anchorEntity;
            }

            return null;
        }
        /// <summary>
        /// 根据secUid获取主播信息-异步
        /// </summary>
        /// <param name="secUid">主播id</param>
        /// <returns></returns>
        public async static Task<AnchorEntity> GetAnchorBySecUidAsync(string secUid)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("secUid", secUid);

            string dataStr = await HttpAsyncUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/anchorurl/getUserAnchorBySecUid", param);

            if (!string.IsNullOrEmpty(dataStr))
            {

                AnchorEntity anchorEntity = JsonConvert.DeserializeObject<AnchorEntity>(dataStr);
                return anchorEntity;
            }

            return null;
        }


        /// <summary>
        /// 根据secUid获取当前用户主播信息-异步
        /// </summary>
        /// <param name="secUid">主播id</param>
        /// <returns></returns>
        public static async Task<AnchorEntity> GetCurrUserAnchorBySecUid(string secUid)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("secUid", secUid);

            string dataStr = await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/anchorurl/getCurrUserAnchorBySecUid", param);

            if (!string.IsNullOrEmpty(dataStr))
            {

                AnchorEntity anchorEntity = JsonConvert.DeserializeObject<AnchorEntity>(dataStr);
                return anchorEntity;
            }

            return null;
        }
        /// <summary>
        /// 根据secUid获取当前用户主播信息-同步
        /// </summary>
        /// <param name="secUid">主播id</param>
        /// <returns></returns>
        public static AnchorEntity GetCurrUserAnchorBySecUidSync(string secUid)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("secUid", secUid);

            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/anchorurl/getCurrUserAnchorBySecUid", param);

            if (!string.IsNullOrEmpty(dataStr))
            {

                AnchorEntity anchorEntity = JsonConvert.DeserializeObject<AnchorEntity>(dataStr);
                return anchorEntity;
            }

            return null;
        }
        /// <summary>
        /// 根据secUid获取当前用户主播信息-异步
        /// </summary>
        /// <param name="secUid">主播id</param>
        /// <returns></returns>
        public async static Task<AnchorEntity> GetCurrUserAnchorBySecUidAsync(string secUid)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("secUid", secUid);

            string dataStr = await HttpAsyncUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/anchorurl/getCurrUserAnchorBySecUid", param).ConfigureAwait(false);

            if (!string.IsNullOrEmpty(dataStr))
            {

                AnchorEntity anchorEntity = JsonConvert.DeserializeObject<AnchorEntity>(dataStr);
                return anchorEntity;
            }

            return null;
        }

        /// <summary>
        /// 客户端获取主播列表-同步
        /// </summary>
        /// <returns></returns>
        public static List<AnchorEntity> ClientAnchorList()
        {

            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/anchorurl/clientAnchorList", null);

            if (!string.IsNullOrEmpty(dataStr))
            {

                List<AnchorEntity> anchorEntityList = JsonConvert.DeserializeObject<List<AnchorEntity>>(dataStr);
                return anchorEntityList;
            }

            return null;
        }

        /// <summary>
        /// 客户端获取主播列表-异步
        /// </summary>
        /// <returns></returns>
        public static async Task<List<AnchorEntity>> ClientAnchorListAsync()
        {

            string dataStr = await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/anchorurl/clientAnchorList", null);

            if (!string.IsNullOrEmpty(dataStr))
            {

                List<AnchorEntity> anchorEntityList = JsonConvert.DeserializeObject<List<AnchorEntity>>(dataStr);
                return anchorEntityList;
            }

            return null;
        }

        /// <summary>
        /// 获取登录用户主播列表的全部监控位
        /// </summary>
        /// <returns></returns>
        public static async Task<List<CurrentMonitoringPositionVo>> getCurrentMonitoringPosition()
        {
            string dataStr = await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/words/anchorUrl/getCurrentMonitoringPosition", null);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<List<CurrentMonitoringPositionVo>>(dataStr);
            }

            return null;
        }

        /// <summary>
        /// 根据主播secuid集合获取昨日录制场次列表-同步
        /// </summary>
        /// <returns></returns>
        public static List<AnchorYesterdayRecordVo> ListAnchorYesterdayRecord(List<string> secUidList)
        {

            string dataStr = HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/anchorurl/listAnchorYesterdayRecord", secUidList);

            if (!string.IsNullOrEmpty(dataStr))
            {

                List<AnchorYesterdayRecordVo> anchorYesterdayRecordVos = JsonConvert.DeserializeObject<List<AnchorYesterdayRecordVo>>(dataStr);
                return anchorYesterdayRecordVos;
            }

            return null;
        }

        /// <summary>
        /// 根据主播secuid集合获取昨日录制场次列表-异步
        /// </summary>
        /// <returns></returns>
        public static async Task<List<AnchorYesterdayRecordVo>> ListAnchorYesterdayRecordAsync(List<string> secUidList)
        {

            string dataStr = await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/anchorurl/listAnchorYesterdayRecord", secUidList);

            if (!string.IsNullOrEmpty(dataStr))
            {

                List<AnchorYesterdayRecordVo> anchorYesterdayRecordVos = JsonConvert.DeserializeObject<List<AnchorYesterdayRecordVo>>(dataStr);
                return anchorYesterdayRecordVos;
            }

            return null;
        }

        /// <summary>
        /// 根据主播唯一标识(secUid、homeUrl、liveUrl)集合获取主播
        /// </summary>
        /// <param name="uniquesList">主播唯一标识集合</param>
        /// <returns></returns>
        public static async Task<List<AnchorUrlInfoVo>> ListByUniques(List<string> uniquesList)
        {

            string dataStr = await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/anchorurl/listByUniques", uniquesList);

            if (!string.IsNullOrEmpty(dataStr))
            {

                List<AnchorUrlInfoVo> anchorUrlInfoVos = JsonConvert.DeserializeObject<List<AnchorUrlInfoVo>>(dataStr);
                return anchorUrlInfoVos;
            }

            return null;
        }
        /// <summary>
        /// 根据主播唯一标识(secUid、homeUrl、liveUrl)集合获取主播-同步
        /// </summary>
        /// <param name="uniquesList">主播唯一标识集合</param>
        /// <returns></returns>
        public static List<AnchorUrlInfoVo> ListByUniquesSync(List<string> uniquesList)
        {

            string dataStr = HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/anchorurl/listByUniques", uniquesList);

            if (!string.IsNullOrEmpty(dataStr))
            {

                List<AnchorUrlInfoVo> anchorUrlInfoVos = JsonConvert.DeserializeObject<List<AnchorUrlInfoVo>>(dataStr);
                return anchorUrlInfoVos;
            }

            return null;
        }

        /// <summary>
        /// 绑定用户和主播的关联关系
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        /// <param name="tradeId">行业id</param>
        /// <returns></returns>
        public static async Task BindUserAnchor(string secUid, string tradeId)
        {

            Dictionary<string, string> requestData = new Dictionary<string, string>();
            requestData.Add("secUid", secUid);
            requestData.Add("tradeId", tradeId);

            await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/anchorurl/bindUserAnchor", requestData);

        }

        /// <summary>
        /// 修改用户主播信息-同步
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        /// <param name="tradeId">主播行业id</param>
        /// <param name="isAutoRecord">在线时,是否自动录制分析</param>
        /// <param name="isRemoveRecord">是否从录制列表移除了 0：否 1：是</param>
        /// <returns></returns>
        public static void UpdateUserAnchorSync(string secUid, string tradeId, int isAutoRecord, int isRemoveRecord)
        {
            Dictionary<string, object> dictionary = new Dictionary<string, object>();
            dictionary.Add("anchorUrlSecUid", secUid);
            if (!string.IsNullOrEmpty(tradeId))
            {
                dictionary.Add("tradeId", tradeId);
            }
            if (isAutoRecord != -1)
            {
                dictionary.Add("isAutoRecord", isAutoRecord + "");
            }
            if (isRemoveRecord != -1)
            {
                dictionary.Add("isRemoveRecord", isRemoveRecord + "");
            }

            HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/anchorurl/updateUserAnchor", dictionary);
        }

        /// <summary>
        /// 更新主播弹幕监控状态
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        /// <param name="isBarrageMonitoring">是否开启弹幕 0：否  1：是</param>
        /// <returns></returns>
        public static void UpdateBarrageMonitoringSync(string secUid, int isBarrageMonitoring)
        {

            Dictionary<string, object> dictionary = new Dictionary<string, object>();
            dictionary.Add("secUid", secUid);
            dictionary.Add("isBarrageMonitoring", isBarrageMonitoring);

            HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/openapi/v2100/updateBarrageMonitoring", dictionary);
        }

        /// <summary>
        /// 更新主播自动上传云空间状态
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        /// <param name="isAutoUploadCloud">是否自动上传云空间 0：否 1：是</param>
        public static void UpdateAutoUploadCloudSync(string secUid, int isAutoUploadCloud)
        {

            Dictionary<string, object> dictionary = new Dictionary<string, object>();
            dictionary.Add("secUid", secUid);
            dictionary.Add("isAutoUploadCloud", isAutoUploadCloud);

            HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/openapi/v2100/updateAutoUploadCloud", dictionary);
        }

        /// <summary>
        /// 添加或修改用户主播信息-异步
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        /// <returns></returns>
        public static async Task AddOrUpdateAnchor(AnchorInfo anchorInfo)
        {
            // 配置序列化忽略大小写，忽略null值
            var settings = new JsonSerializerSettings
            {
                ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver(),
                NullValueHandling = NullValueHandling.Ignore
            };
            AddOrUpdateAnchorBo addOrUpdateAnchorBo = JsonConvert.DeserializeObject<AddOrUpdateAnchorBo>(JsonConvert.SerializeObject(anchorInfo), settings);

            await HttpAsyncUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/anchorurl/addOrUpdateAnchor", addOrUpdateAnchorBo);

        }

        /// <summary>
        /// 新添加的主播后-自动打开监控位
        /// </summary>
        /// <param name="secUid">主播secuid</param>
        /// <returns></returns>
        public static async Task<OpenMonitoringPositionVo> openMonitoringPosition(string secUid)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("secUid", secUid);

            string dataStr = await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/anchorurl/openMonitoringPosition", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<OpenMonitoringPositionVo>(dataStr);
            }

            return new OpenMonitoringPositionVo() { isBarrageMonitoring = 0, isDataViewing = 0 };
        }
        /// <summary>
        /// 添加或修改用户主播信息-同步
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        /// <returns></returns>
        public static void AddOrUpdateAnchorSync(AnchorInfo anchorInfo)
        {
            // 配置序列化忽略大小写，忽略null值
            var settings = new JsonSerializerSettings
            {
                ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver(),
                NullValueHandling = NullValueHandling.Ignore
            };
            AddOrUpdateAnchorBo addOrUpdateAnchorBo = JsonConvert.DeserializeObject<AddOrUpdateAnchorBo>(JsonConvert.SerializeObject(anchorInfo), settings);

            HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/anchorurl/addOrUpdateAnchor", addOrUpdateAnchorBo);

        }

        /// <summary>
        /// 发送主播上下播通知短信-异步
        /// </summary>
        /// <param name="anchorUrlName">主播名称</param>
        /// <param name="type">类型：0上播，1下播</param>
        /// <returns></returns>
        public static async Task SendSwitchAnchorMsg(string anchorUrlName, int type)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("anchorUrlName", anchorUrlName);
            param.Add("type", type);

            try
            {
                await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/anchorurl/sendSwitchAnchorMsg", param);
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"发送主播上下播通知短信发生异常=={anchorUrlName} === {type}");
            }
            

        }

        /// <summary>
        /// 更新ai页面的部分主播字段
        /// </summary>
        /// <param name="vo"></param>
        /// <returns></returns>
        public static AiAnchorInfoVo updateAiPartial(AiAnchorInfoVo vo)
        {

            R r = HttpUtils.SendServerPostR(ReplayHttpUtils.BaseUrl + "/words/basicSettings/updateAiPartialNew", vo);

            if (r.success())
            {
                return JsonConvert.DeserializeObject<AiAnchorInfoVo>(r.data);
            }
            return null;
        }

        /// <summary>
        /// 更新主播基础信息-异步
        /// </summary>
        /// <param name="anchorBaseInfoDto">更新主播基础信息</param>
        /// <returns></returns>
        public static async Task updateAnchorBaseInfo(AnchorBaseInfoDto anchorBaseInfoDto)
        {

            await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/words/anchorUrl/updateAnchorBaseInfo", anchorBaseInfoDto);

        }

        /// <summary>
        /// 更新用户主播信息-异步
        /// </summary>
        /// <param name="anchorInfo">用户主播信息</param>
        /// <returns></returns>
        public static void UpdateAnchorUserInfo(UpdateAnchorUserDto updateAnchorUserDto)
        {

            HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/words/anchorUrl/updateUserAnchorInfo", updateAnchorUserDto);

        }

        /// <summary>
        /// 获取主播排班列表-异步
        /// </summary>
        /// <returns>排班列表</returns>
        public static async Task<List<AnchorSchedule>> GetAnchorSchedulesAsync()
        {
            try
            {
                string dataStr = await HttpUtils.SendServerGetAsync(ReplayHttpUtils.GovernanceBaseUrl + "/api/governance/client/live-room/future-plan", null);

                if (!string.IsNullOrEmpty(dataStr))
                {
                    List<AnchorSchedule> schedules = JsonConvert.DeserializeObject<List<AnchorSchedule>>(dataStr);
                    return schedules;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", "获取主播排班列表发生异常");
            }

            return null;
        }

        /// <summary>
        /// 从服务器同步排班数据到缓存
        /// </summary>
        /// <returns></returns>
        public static async Task SyncScheduleFromServer()
        {
            try
            {
                List<AnchorSchedule> schedules = await GetAnchorSchedulesAsync();

                // 添加日志：显示获取的排班信息
                if (schedules != null && schedules.Count > 0)
                {
                    FileUtils.LogRecrd($"从服务器获取到 {schedules.Count} 条排班记录", "同步排班数据");
                    
                    // 记录前几条排班详情用于调试（避免日志过多）
                    if (schedules.Count <= 10)
                    {
                        foreach (var schedule in schedules)
                        {
                            FileUtils.LogRecrd($"排班详情 - SecUid: {schedule.secUid}, 开始时间: {schedule.startTime}, 结束时间: {schedule.endTime}", "同步排班数据");
                        }
                    }
                    else
                    {
                        // 如果排班记录较多，只记录前3条
                        for (int i = 0; i < 3; i++)
                        {
                            var schedule = schedules[i];
                            FileUtils.LogRecrd($"排班示例[{i+1}] - SecUid: {schedule.secUid}, 开始时间: {schedule.startTime}, 结束时间: {schedule.endTime}", "同步排班数据");
                        }
                        FileUtils.LogRecrd($"... 及其他 {schedules.Count - 3} 条排班记录", "同步排班数据");
                    }
                }
                else
                {
                    FileUtils.LogRecrd("从服务器未获取到排班数据", "同步排班数据");
                }

                AnchorScheduleCacheManager.UpdateCache(schedules);
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", "同步排班数据到缓存发生异常");
            }
        }

        /// <summary>
        /// 获取授权用量信息
        /// </summary>
        /// <param name="authType">授权类型 1=巨量 / 2=千川 / 3=来客</param>
        /// <returns>授权用量信息（包含总量、已用量、剩余量），失败返回 null</returns>
        public static AuthUsageVo GetAuthUsage(int authType)
        {
            try
            {
                Dictionary<string, object> param = new Dictionary<string, object>();
                param.Add("authType", authType);

                string dataStr = HttpUtils.SendServerGet(
                    ReplayHttpUtils.BaseUrl + "/anchorurl/getAuthUsage", param);

                if (!string.IsNullOrEmpty(dataStr))
                {
                    return JsonConvert.DeserializeObject<AuthUsageVo>(dataStr);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"获取授权用量信息失败, authType={authType}");
            }

            return null;
        }

    }
}
