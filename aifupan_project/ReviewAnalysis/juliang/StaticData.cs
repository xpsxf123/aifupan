using CefSharp.WinForms;
using CefSharp;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace juliang
{
    public static class StaticData
    {
        public static Dictionary<string, string> dict_data_url = new Dictionary<string, string>();
        public static Dictionary<string, string> dict_data = new Dictionary<string, string>();
        public static uint url_collect_status = 0;
        public static uint data_collect_status = 0;
        public static uint loop_count = 0;

        public static void InitSettings()
        {
            CefSettings settings = new CefSettings();

            Cef.Initialize(settings);
        }
    }
}
