using CefSharp;
using CefSharp.Handler;
using juliang;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.juliang
{
    public class CustomResourceHandler : RequestHandler
    {

        private JuliangForm juliangForm;

        public CustomResourceHandler(JuliangForm juliangForm)
        {
            this.juliangForm = juliangForm;
        }

        /// <summary>
        /// 为浏览器每个资源请求（如请求接口、HTML、CSS、JavaScript、图片等文件）创建专属过滤器，请求资源时会触发这个过滤器
        /// </summary>
        protected override IResourceRequestHandler GetResourceRequestHandler(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, bool isNavigation, bool isDownload, string requestInitiator, ref bool disableDefaultHandling)
        {
            // 精细化控制单个资源请求
            return new CustomResourceRequestHandler(this.juliangForm);
        }
    }
}
