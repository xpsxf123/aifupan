using COSXML.Auth;
using ReviewAnalysis.Model;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using VodSDK;

using douyin.Utils;

namespace ReviewAnalysis.Asr
{
    public class TencentVodUtils
    {

        public static async Task UploadToVod(string videoPath)
        {
            // 获取临时凭证
            AudioTempTokenEntity tempTokenEntity = ReplayHttpUtils.GetVodTempToken();

            VodUploadClient client = new VodUploadClient(tempTokenEntity.TempSecretId, tempTokenEntity.TempSecretKey);
            VodUploadRequest request = new VodUploadRequest();
            request.MediaFilePath = videoPath; // 要上传的本地视频地址
            request.StorageRegion = "ap-shanghai"; // 指定存储地域
            try
            {
                VodUploadResponse response = await client.Upload("ap-shanghai", request);
                // 打印媒体 FileId
                FileUtils.log(response.FileId, "TencentVod上传");
            }
            catch (Exception e)
            {
                // 业务方进行异常处理
                FileUtils.LogError(e.ToString(), "TencentVod上传");
            }

        }
    }
}
