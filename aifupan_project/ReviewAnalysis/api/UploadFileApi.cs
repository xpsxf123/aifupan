using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.entity.uploadFile;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.uploadFile;
using ReviewAnalysis.vo.video;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.api
{
    public class UploadFileApi
    {
        /// <summary>
        /// 根据文件id获取文件信息-同步
        /// </summary>
        /// <param name="fileId">文件id</param>
        /// <returns></returns>
        public static UploadFileEntity GetFileByFileIdSync(string fileId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("fileId", fileId);
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/UploadFile/clientGetFileByFileId", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                UploadFileEntity uploadFileEntity = JsonConvert.DeserializeObject<UploadFileEntity>(dataStr);
                return uploadFileEntity;
            }

            return null;
        }

        /// <summary>
        /// 根据文件id获取文件信息-异步
        /// </summary>
        /// <param name="fileId">文件id</param>
        /// <returns></returns>
        public static async Task<UploadFileEntity> GetFileByFileIdAsync(string fileId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("fileId", fileId);
            string dataStr = await HttpAsyncUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/UploadFile/clientGetFileByFileId", param).ConfigureAwait(false);

            if (!string.IsNullOrEmpty(dataStr))
            {
                UploadFileEntity uploadFileEntity = JsonConvert.DeserializeObject<UploadFileEntity>(dataStr);
                return uploadFileEntity;
            }

            return null;
        }

        /// <summary>
        /// 保存或修改文件信息-同步
        /// </summary>
        /// <param name="uploadFileEntity">文件信息</param>
        /// <returns></returns>
        public static void SaveOrUpdateFileSync(UploadFileEntity uploadFileEntity)
        {

            HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/UploadFile/saveOrUpdateFile", uploadFileEntity);

        }
        /// <summary>
        /// 保存或修改文件信息-异步
        /// </summary>
        /// <param name="uploadFileEntity">文件信息</param>
        /// <returns></returns>
        public static async Task SaveOrUpdateFile(UploadFileEntity uploadFileEntity)
        {

            await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/UploadFile/saveOrUpdateFile", uploadFileEntity);

        }

        /// <summary>
        /// 修改文件的分析状态-同步
        /// </summary>
        /// <param name="fileId">文件id</param>
        /// <param name="analysisStatus">分析状态 0：未分析 1：分析中 2：分析完成 3：分析失败</param>
        /// <param name="errorReason">分析失败原因</param>
        /// <returns></returns>
        public static void UpdateFileAnalysisStatusSync(string fileId, int analysisStatus, string errorReason)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("fileId", fileId);
            param.Add("analysisStatus", analysisStatus);
            param.Add("errorReason", errorReason);

            HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/UploadFile/updateFileAnalysisStatus", param);
        }
        /// <summary>
        /// 修改文件的分析状态-异步
        /// </summary>
        /// <param name="fileId">文件id</param>
        /// <param name="analysisStatus">分析状态 0：未分析 1：分析中 2：分析完成 3：分析失败</param>
        /// <param name="errorReason">分析失败原因</param>
        /// <returns></returns>
        public static async Task UpdateFileAnalysisStatus(string fileId, int analysisStatus, string errorReason)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("fileId", fileId);
            param.Add("analysisStatus", analysisStatus);
            param.Add("errorReason", errorReason);

            await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/UploadFile/updateFileAnalysisStatus", param);
        }

        /// <summary>
        /// 修改文件的行业-异步
        /// </summary>
        /// <param name="fileId">文件id</param>
        /// <param name="tradeId">行业id</param>
        /// <returns></returns>
        public static async Task UpdateFileTrade(string fileId, string tradeId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("fileId", fileId);
            param.Add("tradeId", tradeId);

            await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/UploadFile/updateFileTrade", param);
        }
        /// <summary>
        /// 修改文件的行业-同步
        /// </summary>
        /// <param name="fileId">文件id</param>
        /// <param name="tradeId">行业id</param>
        /// <returns></returns>
        public static void UpdateFileTradeSync(string fileId, string tradeId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("fileId", fileId);
            param.Add("tradeId", tradeId);

            HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/UploadFile/updateFileTrade", param);
        }

        /// <summary>
        /// 根据文件id集合获取文件列表-同步
        /// </summary>
        /// <param name="fileIds">文件id集合</param>
        /// <returns></returns>
        public static List<UploadFileEntity> ListFileByFileIdsSync(List<string> fileIds)
        {

            string dataStr = HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/UploadFile/clientListFileByFileIds", fileIds);

            if (!string.IsNullOrEmpty(dataStr))
            {
                List<UploadFileEntity> fileList = JsonConvert.DeserializeObject<List<UploadFileEntity>>(dataStr);
                return fileList;
            }

            return null;
        }

        /// <summary>
        /// 批量删除文件-同步
        /// </summary>
        /// <param name="fileIdList">文件id集合</param>
        /// <returns></returns>
        public static void DelFileByIdsSync(List<string> fileIdList)
        {

            HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/UploadFile/clientDeleteFile", fileIdList);
        }

        /// <summary>
        /// 批量删除文件-异步
        /// </summary>
        /// <param name="fileIdList">文件id集合</param>
        /// <returns></returns>
        public static async Task DelFileByIds(List<string> fileIdList)
        {

            await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/UploadFile/clientDeleteFile", fileIdList);
        }

        /// <summary>
        /// 删除文件-异步
        /// </summary>
        /// <param name="fileId">文件id</param>
        /// <returns></returns>
        public static async Task DelFileById(string fileId)
        {
            List<string> fileIdList = new List<string>();
            fileIdList.Add(fileId);
            await DelFileByIds(fileIdList);
        }

        /// <summary>
        /// 获取视频详情
        /// </summary>
        /// <param name="fileId"></param>
        /// <returns></returns>
        public static UploadFileDetailVo infoDetailByFileId(string fileId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("fileId", fileId);
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/words/uploadFileDetail/infoByFileId", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<UploadFileDetailVo>(dataStr);
            }

            return null;
        }

        /// <summary>
        /// 获取视频详情-异步
        /// </summary>
        /// <param name="fileId"></param>
        /// <returns></returns>
        public static async Task<UploadFileDetailVo> infoDetailByFileIdAsync(string fileId)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("fileId", fileId);
            string dataStr = await HttpAsyncUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/words/uploadFileDetail/infoByFileId", param).ConfigureAwait(false);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<UploadFileDetailVo>(dataStr);
            }

            return null;
        }
    }
}
