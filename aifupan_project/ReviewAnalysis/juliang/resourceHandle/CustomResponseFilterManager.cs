using CefSharp;
using douyin.Utils;
using juliang;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Web.UI.WebControls;

namespace ReviewAnalysis.juliang
{
    public class CustomResponseFilterManager
    {
        private static Dictionary<string, IResponseFilter> filterList = new Dictionary<string, IResponseFilter>();
        
        /// <summary>
        /// 添加过滤器到过滤器列表
        /// </summary>
        /// <param name="guid"></param>
        /// <returns></returns>
        public static void AddFilter(string guid, CustomResponseFilter filter)
        {
            try
            {
                lock (filterList)
                {
                    //filterList[guid] = filter;
                    filterList.Add(guid, filter);
                }
            }
            catch(Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"巨量添加过滤器到过滤器列表发生异常");
            }
            
        }

        /// <summary>
        /// 从过滤器列表获取过滤器
        /// </summary>
        /// <param name="guid"></param>
        /// <returns></returns>
        public static IResponseFilter GetFilter(string guid)
        {
            try
            {
                lock (filterList)
                {
                    if (filterList.ContainsKey(guid))
                    {
                        return filterList[guid];
                    }
                    else
                    {
                        return null;
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"巨量从过滤器列表获取过滤器发生异常");
            }
            return null;
        }
    }
}
