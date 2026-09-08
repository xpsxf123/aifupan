using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.juliang.entity
{
    /// <summary>
    /// 巨量用户画像
    /// </summary>
    public class JuliangUserPortraitEntity
    {
        /// <summary>
        /// 年龄画像
        /// </summary>
        public List<JuliangUserPortraitItemEntity> agePortrait { get; set; } = new List<JuliangUserPortraitItemEntity>();
        /// <summary>
        /// 性别画像
        /// </summary>
        public List<JuliangUserPortraitItemEntity> genderPortrait { get; set; } = new List<JuliangUserPortraitItemEntity>();
        /// <summary>
        /// 城市画像
        /// </summary>
        public List<JuliangUserPortraitItemEntity> provincePortrait { get; set; } = new List<JuliangUserPortraitItemEntity>();
    }
}
