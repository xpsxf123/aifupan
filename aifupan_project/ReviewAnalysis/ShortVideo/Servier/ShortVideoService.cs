using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.ShortVideo.DouYin.dto;

namespace ReviewAnalysis.ShortVideo.Model
{
    public interface ShortVideoService
    {

        DouYinVideoInfo GetVideoDetailAsync(string url);

    }
}
