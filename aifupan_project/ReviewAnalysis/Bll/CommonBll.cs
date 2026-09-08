using ReviewAnalysis.Model;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Bll
{
    public class CommonBll
    {
        /// <summary>
        /// 文件分析失败，清理数据
        /// </summary>
        /// <param name="uploadFile">文件信息</param>
        public void ClearFileData(AnchorVideo anchorVideo)
        {
            // 删除关联的音频和识别数据
            Audio audio = new Audio();
            audio.DeleteModelByVideoId(anchorVideo.VideoId);
            AudioaAlysis audioaAlysis = new AudioaAlysis();
            audioaAlysis.DeleteModelByVideoId(anchorVideo.VideoId);

            // 删除mp4文件
            string videoPath = anchorVideo.StoragePath.Substring(0, anchorVideo.StoragePath.LastIndexOf("\\"));
            videoPath += "\\mp4\\";
            videoPath += anchorVideo.VideoName + ".mp4";
            if (File.Exists(videoPath))
            {
                File.Delete(videoPath);
            }

            // 删除音频文件
            string audioDirectoryPath = anchorVideo.StoragePath.Substring(0, anchorVideo.StoragePath.LastIndexOf("\\"));
            audioDirectoryPath += "\\audio\\";
            audioDirectoryPath += anchorVideo.VideoName.Substring(0, anchorVideo.VideoName.LastIndexOf("."));
            if (Directory.Exists(audioDirectoryPath))
            {
                Directory.Delete(audioDirectoryPath, true);
            }
        }
    }
}
