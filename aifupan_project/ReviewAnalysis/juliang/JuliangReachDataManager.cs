using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.juliang
{
    public class JuliangReachDataManager
    {
        private JuliangReachData _instance;
        private readonly object _lock = new object();

        public static JuliangReachData GetInstance(JuliangReachDataConfig config = null)
        {
            //if (_instance == null)
            //{
            //    lock (_lock)
            //    {
            //        _instance = config == null ? new JuliangReachData() : new JuliangReachData(config);
            //    }
            //}
            //return _instance;
            return config == null ? new JuliangReachData() : new JuliangReachData(config);
        }

        //public static void ReleaseInstance()
        //{
        //    lock (_lock)
        //    {
        //        _instance?.Dispose();
        //        _instance = null;
        //    }
        //}
    }
}
