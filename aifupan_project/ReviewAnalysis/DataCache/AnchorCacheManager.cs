using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.bo.anchor;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Model;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using static COSXML.Model.Tag.ListBucketVersions;


namespace ReviewAnalysis.DataCache
{
    public class AnchorCacheManager
    {
        private static ConcurrentDictionary<string, AnchorInfo> AnchorCaches = new ConcurrentDictionary<string, AnchorInfo>();

        /// <summary>
        /// 清空AnchorCaches缓存
        /// </summary>
        public static void AnchorCachesClear()
        {
            AnchorCaches.Clear();
        }


        /// <summary>
        /// 设置主播信息到缓存
        /// </summary>
        /// <param name="anchorInfo"></param>
        public static void SetAnchorCache(AnchorInfo anchorInfo) 
        {
            string secuid = anchorInfo.SecUid;
            AnchorCaches.AddOrUpdate(secuid, anchorInfo, (oldKey, oldValue) => anchorInfo);
        }

        /// <summary>
        /// 获取全部主播列表
        /// </summary>
        /// <returns></returns>
        public static List<AnchorInfo> GetAllAnchors()
        {
            List<AnchorInfo> result = new List<AnchorInfo>();
            foreach (var item in AnchorCaches)
            {
                result.Add(item.Value);
            }
            return result;
        }

        /// <summary>
        /// 获取全部未从录制列表移除的主播列表
        /// </summary>
        /// <returns></returns>
        public static List<AnchorInfo> GetAllNotRemoveAnchors()
        {
            List<AnchorInfo> result = new List<AnchorInfo>();
            foreach (var item in AnchorCaches)
            {
                if(item.Value.IsRemoveRecord == 0)
                {
                    result.Add(item.Value);
                }
            }
            return result;
        }

        /// <summary>
        /// 分页获取主播列表
        /// </summary>
        /// <param name="anchorListBo">查询参数</param>
        /// <returns></returns>
        public static List<AnchorInfo> GetAnchorPage(AnchorListBo anchorListBo)
        {
            List<AnchorInfo> result = new List<AnchorInfo>();
            foreach (var item in AnchorCaches)
            {
                result.Add(item.Value);
            }

            if (result != null && result.Count > 0)
            {
                if(!string.IsNullOrEmpty(anchorListBo.anchorName))
                {
                    result.RemoveAll(p => !p.RemarksName.Contains(anchorListBo.anchorName));
                }
                if (anchorListBo.recordStatus != null)
                {
                    result.RemoveAll(p => p.RecordStatus != anchorListBo.recordStatus);
                }
                if (anchorListBo.isRemoveRecord != null)
                {
                    result.RemoveAll(p => p.IsRemoveRecord != anchorListBo.isRemoveRecord);
                }
                if (!string.IsNullOrEmpty(anchorListBo.tradeId))
                {
                    result.RemoveAll(p => !p.TradeId.Equals(anchorListBo.tradeId));
                }
                if (anchorListBo.accountType != null)
                {
                    result.RemoveAll(p => p.AccountType != anchorListBo.accountType);
                }
            }

            if (result != null && result.Count > 0) 
            {

                // 按添加时间排序 降序
                result = result.OrderByDescending(a => DateTime.Parse(a.AddTime)).ToList();

                //List<AnchorInfo> resultList = new List<AnchorInfo>();
                //// 筛选出置顶的
                //List<AnchorInfo> topList = result.Where(p => p.IsTop == 1).ToList();
                //List<AnchorInfo> notTopList = result.Where(p => p.IsTop == 0).ToList();
                //if(topList != null && topList.Count > 0)
                //{
                //    // 按置顶时间降序
                //    topList = topList.OrderByDescending(a => DateTime.Parse(a.AddTopTime)).ToList();
                //    resultList.AddRange(topList);
                //}
                //if(notTopList != null && notTopList.Count > 0)
                //{
                //    // 按最后录制时间降序
                //    notTopList = notTopList.OrderByDescending(a => DateTime.Parse(a.LastRecordTime)).ToList();
                //    resultList.AddRange(notTopList);
                //}

                //return resultList;

            }
            //FileUtils.log($"{JsonConvert.SerializeObject(result)}", "分页获取主播信息，排序");
            return result;
        }

        /// <summary>
        /// 根据主播secuid获取主播信息
        /// </summary>
        /// <param name="secuid">主播secuid</param>
        /// <returns></returns>
        public static AnchorInfo GetAnchorByIdFromCache(string secuid)
        {
            if (AnchorCaches.TryGetValue(secuid, out AnchorInfo result))
            {
                return result;
            }
            return null;
        }
        /// 从缓存异步获取主播信息（兼容后续异步IO扩展）
        /// </summary>
        /// <param name="secuid">主播唯一标识</param>
        /// <returns>主播信息</returns>
        public async static Task<AnchorInfo> GetAnchorByIdFromCacheAsync(string secuid)
        {
            // 参数校验
            if (string.IsNullOrWhiteSpace(secuid))
            {
                throw new ArgumentNullException(nameof(secuid), "主播secuid不能为空或空白");
            }
            AnchorCaches.TryGetValue(secuid, out var result);
            // 用 await Task.CompletedTask 满足 async 方法的 await 要求（无性能损耗）
            await Task.CompletedTask;
            return result;
        }

        /// <summary>
        /// 根据平台获取主播列表
        /// </summary>
        /// <param name="platform">DouYinLive/DouYinHomeLive（抖音直播/抖音主页）</param>
        /// <returns></returns>
        public static List<AnchorInfo> GetAnchorInfosByPlatForm(string platform)
        {
            List<AnchorInfo> result = new List<AnchorInfo>();
            foreach (var item in AnchorCaches)
            {
                AnchorInfo anchor = item.Value;
                if (anchor.AnchorPlatform.Equals(platform))
                {
                    result.Add(anchor);
                }
            }
            return result;
        }

        /// <summary>
        /// 根据平台获取主播列表
        /// </summary>
        /// <param name="platform">平台类型 0：抖音 1：快手 2：视频号</param>
        /// <returns></returns>
        public static List<AnchorInfo> GetAnchorListByPlatForm(int platform)
        {
            List<AnchorInfo> result = new List<AnchorInfo>();
            foreach (var item in AnchorCaches)
            {
                AnchorInfo anchor = item.Value;
                if (anchor.platform == platform)
                {
                    result.Add(anchor);
                }
            }
            return result;
        }

        /// <summary>
        /// 根据平台获取主播列表(未从录制列表删除)
        /// </summary>
        /// <param name="platform">平台类型 0：抖音 1：快手 2：视频号</param>
        /// <returns></returns>
        public static List<AnchorInfo> GetNotRemoveAnchorListByPlatForm(int platform)
        {
            List<AnchorInfo> result = new List<AnchorInfo>();
            foreach (var item in AnchorCaches)
            {
                AnchorInfo anchor = item.Value;
                if (anchor.platform == platform && anchor.IsRemoveRecord == 0)
                {
                    result.Add(anchor);
                }
            }
            return result;
        }

        /// <summary>
        /// 根据url获取 主播信息
        /// </summary>
        /// <param name="url"></param>
        /// <returns></returns>
        public static AnchorInfo GetAnchorInfoByUrl(string url) 
        {
            foreach (var item in AnchorCaches)
            {
                AnchorInfo anchor = item.Value;
                if (anchor.HomeUrl != null &&
                    anchor.HomeUrl.Equals(url.Trim()))
                {
                    return anchor;
                }

                if (anchor.LiveUrl != null &&
                    anchor.LiveUrl.Equals(url.Trim()))
                {
                    return anchor;
                }
            }

            return null;
        }

        /// <summary>
        /// 从缓存更新/添加数据到数据库
        /// </summary>
        public static void SetAnchorToDataBaseFromCache()
        {
            
            AnchorInfo anchorData = new AnchorInfo();
            foreach (var item in AnchorCaches)
            {
                string json = JsonConvert.SerializeObject(item.Value);
                AnchorInfo anchor = JsonConvert.DeserializeObject<AnchorInfo>(json);
                if (anchor.Id > 0)
                {
                    anchor.update();
                }
                else
                {
                    anchor.GetModelBySecuid();
                    if (anchor.Id == 0)
                    {
                        anchor.save();
                        //FileUtils.log($"插入数据库:{JsonConvert.SerializeObject(anchor)}");
                        //重新填充实体
                        anchor.GetModelBySecuid();
                        AnchorCaches.AddOrUpdate(anchor.SecUid, anchor, (oldKey, oldValue) => anchor);
                    }
                }
            }
        }


    }
}
