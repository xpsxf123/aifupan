using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Global
{
    public class ServerConfig
    {
        /// <summary>
        /// 超过多少秒的视频会自动生成看盘数据
        /// </summary>
        public static volatile int autoCreateDataViewingSecond = 3000;

    }
}
