using CefSharp;
using CefSharp.Handler;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using System.Text;
using System.Threading.Tasks;
using System.Timers;
using douyin.Utils;

namespace ReviewAnalysis.socketAddress.browser
{


    public class ProxyRequestHandler : IRequestHandler
    {
        public ProxyRequestHandler() { }

        public string username;
        public string password;
        public ProxyRequestHandler(string username, string password)
        {
            this.username = username;
            this.password = password;
        }

        public bool GetAuthCredentials(IWebBrowser chromiumWebBrowser, IBrowser browser, string originUrl, bool isProxy, string host, int port, string realm, string scheme, IAuthCallback callback)
        {
            if (isProxy && !string.IsNullOrEmpty(username) && !string.IsNullOrEmpty(password))
            {
                callback.Continue(username, password);
                return true;
            }
            return false;
        }

        public IResourceRequestHandler GetResourceRequestHandler(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, bool isNavigation, bool isDownload, string requestInitiator, ref bool disableDefaultHandling)
        {
            return null;
        }

        public bool OnBeforeBrowse(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, bool userGesture, bool isRedirect)
        {
            //Debug.WriteLine(request.Url);
            return false;
        }

        public bool OnCertificateError(IWebBrowser chromiumWebBrowser, IBrowser browser, CefErrorCode errorCode, string requestUrl, ISslInfo sslInfo, IRequestCallback callback)
        {
            callback.Continue(true); //Callback will Dispose it's self once exeucted
            return true;
        }

        public void OnDocumentAvailableInMainFrame(IWebBrowser chromiumWebBrowser, IBrowser browser)
        {
            return;
        }

        public bool OnOpenUrlFromTab(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, string targetUrl, WindowOpenDisposition targetDisposition, bool userGesture)
        {
            return false;
        }

        public bool OnQuotaRequest(IWebBrowser chromiumWebBrowser, IBrowser browser, string originUrl, long newSize, IRequestCallback callback)
        {
            return true;
        }

        public void OnRenderProcessTerminated(IWebBrowser chromiumWebBrowser, IBrowser browser, CefTerminationStatus status, int errorCode, string errorMessage)
        {

        }

        public void OnRenderProcessTerminated(IWebBrowser chromiumWebBrowser, IBrowser browser, CefTerminationStatus status)
        {
            //throw new NotImplementedException();
        }

        public void OnRenderViewReady(IWebBrowser chromiumWebBrowser, IBrowser browser)
        {

        }

        // 其他必须实现的方法...

        public CefReturnValue OnResourceResponse(IWebBrowser browserControl, IBrowser browser, IFrame frame, IRequest request, IResponse response)
        {
            var contentType = response.Headers["Content-Type"];
            FileUtils.log(contentType);
            if (contentType != null && (contentType.Contains("image/") || contentType.Contains("image/")))
            {
                // 如果是图片，返回CefReturnValue.Cancel，停止加载
                return CefReturnValue.Cancel;
            }

            // 如果不是图片，返回CefReturnValue.Continue允许加载
            return CefReturnValue.Continue;
        }

        public bool OnSelectClientCertificate(IWebBrowser chromiumWebBrowser, IBrowser browser, bool isProxy, string host, int port, X509Certificate2Collection certificates, ISelectClientCertificateCallback callback)
        {
            return false;
        }
    }

    public class ICustomRequestHandler : IRequestHandler
    {
        public bool GetAuthCredentials(IWebBrowser chromiumWebBrowser, IBrowser browser, string originUrl, bool isProxy, string host, int port, string realm, string scheme, IAuthCallback callback)
        {
            return false;
        }

        public IResourceRequestHandler GetResourceRequestHandler(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, bool isNavigation, bool isDownload, string requestInitiator, ref bool disableDefaultHandling)
        {
            // 返回自定义的资源请求处理器，用于拦截媒体资源
            return new MuteResourceRequestHandler();
        }

        public bool OnBeforeBrowse(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, bool userGesture, bool isRedirect)
        {
            //Debug.WriteLine(request.Url);
            return false;
        }

        public bool OnCertificateError(IWebBrowser chromiumWebBrowser, IBrowser browser, CefErrorCode errorCode, string requestUrl, ISslInfo sslInfo, IRequestCallback callback)
        {
            callback.Continue(true); //Callback will Dispose it's self once exeucted
            return true;
        }

        public void OnDocumentAvailableInMainFrame(IWebBrowser chromiumWebBrowser, IBrowser browser)
        {
            // 在文档可用时立即设置静音
            try
            {
                browser.GetHost().SetAudioMuted(true);
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"OnDocumentAvailableInMainFrame静音失败: {ex.Message}", "ICustomRequestHandler");
            }
            return;
        }

        public bool OnOpenUrlFromTab(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, string targetUrl, WindowOpenDisposition targetDisposition, bool userGesture)
        {
            return false;
        }

        public bool OnQuotaRequest(IWebBrowser chromiumWebBrowser, IBrowser browser, string originUrl, long newSize, IRequestCallback callback)
        {
            return true;
        }

        public void OnRenderProcessTerminated(IWebBrowser chromiumWebBrowser, IBrowser browser, CefTerminationStatus status, int errorCode, string errorMessage)
        {

        }

        public void OnRenderProcessTerminated(IWebBrowser chromiumWebBrowser, IBrowser browser, CefTerminationStatus status)
        {
            //throw new NotImplementedException();
        }

        public void OnRenderViewReady(IWebBrowser chromiumWebBrowser, IBrowser browser)
        {
            // 在渲染进程就绪时立即设置静音（最早的时机）
            try
            {
                browser.GetHost().SetAudioMuted(true);
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"OnRenderViewReady静音失败: {ex.Message}", "ICustomRequestHandler");
            }
        }

        // 其他必须实现的方法...

        public CefReturnValue OnResourceResponse(IWebBrowser browserControl, IBrowser browser, IFrame frame, IRequest request, IResponse response)
        {
            var contentType = response.Headers["Content-Type"];
            FileUtils.log(contentType);
            if (contentType != null && (contentType.Contains("image/") || contentType.Contains("image/")))
            {
                // 如果是图片，返回CefReturnValue.Cancel，停止加载
                return CefReturnValue.Cancel;
            }

            // 如果不是图片，返回CefReturnValue.Continue允许加载
            return CefReturnValue.Continue;
        }

        public bool OnSelectClientCertificate(IWebBrowser chromiumWebBrowser, IBrowser browser, bool isProxy, string host, int port, X509Certificate2Collection certificates, ISelectClientCertificateCallback callback)
        {
            return false;
        }

        // 其他IRequestHandler成员...

    }

    /// <summary>
    /// 自定义资源请求处理器，用于在资源加载前设置静音
    /// </summary>
    public class MuteResourceRequestHandler : IResourceRequestHandler
    {
        private static bool isMuted = false;
        private static readonly object muteLock = new object();

        public void Dispose()
        {
        }

        public ICookieAccessFilter GetCookieAccessFilter(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request)
        {
            return null;
        }

        public IResourceHandler GetResourceHandler(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request)
        {
            return null;
        }

        public bool GetAuthCredentials(IWebBrowser chromiumWebBrowser, IBrowser browser, string originUrl, bool isProxy, string host, int port, string realm, string scheme, IAuthCallback callback)
        {
            return false;
        }

        public IResponseFilter GetResourceResponseFilter(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, IResponse response)
        {
            return null;
        }

        public CefReturnValue OnBeforeResourceLoad(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, IRequestCallback callback)
        {
            // 在每次资源加载前确保静音（线程安全）
            lock (muteLock)
            {
                if (!isMuted)
                {
                    try
                    {
                        browser.GetHost().SetAudioMuted(true);
                        isMuted = true;
                    }
                    catch
                    {
                        // 忽略错误
                    }
                }
            }
            return CefReturnValue.Continue;
        }

        public bool OnProtocolExecution(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request)
        {
            return false;
        }

        public void OnResourceRedirect(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, IResponse response, ref string newUrl)
        {
        }

        public void OnResourceLoadComplete(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, IResponse response, UrlRequestStatus status, long receivedContentLength)
        {
        }

        public bool OnResourceResponse(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, IResponse response)
        {
            return false;
        }
    }



    public class CustomRequestHandler : RequestHandler
    {
        public static readonly string VersionNumberString = String.Format("Chromium: {0}, CEF: {1}, CefSharp: {2}",
        Cef.ChromiumVersion, Cef.CefVersion, Cef.CefSharpVersion);

        protected override bool OnBeforeBrowse(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, bool userGesture, bool isRedirect)
        {
            //FileUtils.log(     request.GetHeaderByName("cookie"));
            FileUtils.log(request.Url);
            return false;
        }

        protected override bool OnOpenUrlFromTab(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, string targetUrl, WindowOpenDisposition targetDisposition, bool userGesture)
        {
            return false;
        }

        protected override bool OnCertificateError(IWebBrowser chromiumWebBrowser, IBrowser browser, CefErrorCode errorCode, string requestUrl, ISslInfo sslInfo, IRequestCallback callback)
        {
            //NOTE: We also suggest you wrap callback in a using statement or explicitly execute callback.Dispose as callback wraps an unmanaged resource.

            //Example #1
            //Return true and call IRequestCallback.Continue() at a later time to continue or cancel the request.
            //In this instance we'll use a Task, typically you'd invoke a call to the UI Thread and display a Dialog to the user
            //You can cast the IWebBrowser param to ChromiumWebBrowser to easily access
            //control, from there you can invoke onto the UI thread, should be in an async fashion
            Task.Run(() =>
            {
                //NOTE: When executing the callback in an async fashion need to check to see if it's disposed
                if (!callback.IsDisposed)
                {
                    using (callback)
                    {
                        //We'll allow the expired certificate from badssl.com
                        if (requestUrl.ToLower().Contains("https://expired.badssl.com/"))
                        {
                            callback.Continue(true);
                        }
                        else
                        {
                            callback.Continue(false);
                        }
                    }
                }
            });

            return true;

            //Example #2
            //Execute the callback and return true to immediately allow the invalid certificate
            //callback.Continue(true); //Callback will Dispose it's self once exeucted
            //return true;

            //Example #3
            //Return false for the default behaviour (cancel request immediately)
            //callback.Dispose(); //Dispose of callback
            //return false;
        }

        protected override bool GetAuthCredentials(IWebBrowser chromiumWebBrowser, IBrowser browser, string originUrl, bool isProxy, string host, int port, string realm, string scheme, IAuthCallback callback)
        {
            //NOTE: We also suggest you explicitly Dispose of the callback as it wraps an unmanaged resource.

            //Example #1
            //Spawn a Task to execute our callback and return true;
            //Typical usage would see you invoke onto the UI thread to open a username/password dialog
            //Then execute the callback with the response username/password
            //You can cast the IWebBrowser param to ChromiumWebBrowser to easily access
            //control, from there you can invoke onto the UI thread, should be in an async fashion
            //Load https://httpbin.org/basic-auth/cefsharp/passwd in the browser to test
            Task.Run(() =>
            {
                using (callback)
                {
                    if (originUrl.Contains("https://httpbin.org/basic-auth/"))
                    {
                        var parts = originUrl.Split('/');
                        var username = parts[parts.Length - 2];
                        var password = parts[parts.Length - 1];
                        callback.Continue(username, password);
                    }
                }
            });

            return true;

            //Example #2
            //Return false to cancel the request
            //callback.Dispose();
            //return false;
        }

        //protected override void OnRenderProcessTerminated(IWebBrowser chromiumWebBrowser, IBrowser browser, CefTerminationStatus status, int errorCode, string errorMessage)
        //{
        // TODO: Add your own code here for handling scenarios where the Render Process terminated for one reason or another.
        //chromiumWebBrowser.Load(CefExample.RenderProcessCrashedUrl);
        //}

        protected override IResourceRequestHandler GetResourceRequestHandler(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, bool isNavigation, bool isDownload, string requestInitiator, ref bool disableDefaultHandling)
        {
            //NOTE: In most cases you examine the request.Url and only handle requests you are interested in
            /*if (request.Url.ToLower().StartsWith("https://cefsharp.example")
                || request.Url.ToLower().StartsWith(CefSharpSchemeHandlerFactory.SchemeName)
                || request.Url.ToLower().StartsWith("mailto:")
                || request.Url.ToLower().StartsWith("https://googlechrome.github.io/samples/service-worker/"))
            {
                return new ExampleResourceRequestHandler();
            }*/

            return null;
        }
    }
    public class CookieVisitor : ICookieVisitor
    {
        public event Action<CefSharp.Cookie> SendCookie;
        /// <summary>
        /// 遍历完成事件
        /// </summary>
        public event Action OnComplete;
        private bool _completed = false;
        private int _total = -1;

        public bool Visit(CefSharp.Cookie cookie, int count, int total, ref bool deleteCookie)
        {
            deleteCookie = false;
            _total = total;
            if (SendCookie != null)
            {
                SendCookie(cookie);
            }
            // 判断是否遍历完成（count从0开始，所以最后一个是 count == total - 1）
            if (count == total - 1)
            {
                TriggerComplete();
            }
            return true;
        }

        private void TriggerComplete()
        {
            if (!_completed)
            {
                _completed = true;
                OnComplete?.Invoke();
            }
        }

        public void Dispose()
        {
            // 如果没有cookie（_total仍为-1表示Visit从未被调用），触发完成事件
            if (_total == -1)
            {
                TriggerComplete();
            }
        }
    }

    //加解密函数
    public class Cryptic
    {
        /// <summary>
        /// 有密码的AES加密 
        /// </summary>
        /// <param name="text">加密字符</param>
        /// <param name="password">加密的密码</param>
        /// <param name="iv">密钥</param>
        /// <returns></returns>
        public static string AESEncrypt(string text, string password, string iv)
        {
            RijndaelManaged rijndaelCipher = new RijndaelManaged();

            rijndaelCipher.Mode = CipherMode.CBC;

            rijndaelCipher.Padding = PaddingMode.PKCS7;

            rijndaelCipher.KeySize = 128;

            rijndaelCipher.BlockSize = 128;

            byte[] pwdBytes = System.Text.Encoding.UTF8.GetBytes(password);

            byte[] keyBytes = new byte[16];

            int len = pwdBytes.Length;

            if (len > keyBytes.Length) len = keyBytes.Length;

            System.Array.Copy(pwdBytes, keyBytes, len);

            rijndaelCipher.Key = keyBytes;


            byte[] ivBytes = System.Text.Encoding.UTF8.GetBytes(iv);
            rijndaelCipher.IV = ivBytes;

            ICryptoTransform transform = rijndaelCipher.CreateEncryptor();

            byte[] plainText = Encoding.UTF8.GetBytes(text);

            byte[] cipherBytes = transform.TransformFinalBlock(plainText, 0, plainText.Length);

            return Convert.ToBase64String(cipherBytes);

        }

        /// <summary>
        /// 随机生成密钥
        /// </summary>
        /// <returns></returns>
        public static string GetIv(int n)
        {
            char[] arrChar = new char[]{
           'a','b','d','c','e','f','g','h','i','j','k','l','m','n','p','r','q','s','t','u','v','w','z','y','x',
           '0','1','2','3','4','5','6','7','8','9',
           'A','B','C','D','E','F','G','H','I','J','K','L','M','N','Q','P','R','T','S','V','U','W','X','Y','Z'
          };

            StringBuilder num = new StringBuilder();

            Random rnd = new Random(DateTime.Now.Millisecond);
            for (int i = 0; i < n; i++)
            {
                num.Append(arrChar[rnd.Next(0, arrChar.Length)].ToString());

            }

            return num.ToString();
        }

        /// <summary>
        /// AES解密
        /// </summary>
        /// <param name="text"></param>
        /// <param name="password"></param>
        /// <param name="iv"></param>
        /// <returns></returns>
        public static string AESDecrypt(string text, string password, string iv)
        {
            RijndaelManaged rijndaelCipher = new RijndaelManaged();

            rijndaelCipher.Mode = CipherMode.CBC;

            rijndaelCipher.Padding = PaddingMode.PKCS7;

            rijndaelCipher.KeySize = 128;

            rijndaelCipher.BlockSize = 128;

            byte[] encryptedData = Convert.FromBase64String(text);

            byte[] pwdBytes = System.Text.Encoding.UTF8.GetBytes(password);

            byte[] keyBytes = new byte[16];

            int len = pwdBytes.Length;

            if (len > keyBytes.Length) len = keyBytes.Length;

            System.Array.Copy(pwdBytes, keyBytes, len);

            rijndaelCipher.Key = keyBytes;

            byte[] ivBytes = System.Text.Encoding.UTF8.GetBytes(iv);
            rijndaelCipher.IV = ivBytes;

            ICryptoTransform transform = rijndaelCipher.CreateDecryptor();

            byte[] plainText = transform.TransformFinalBlock(encryptedData, 0, encryptedData.Length);

            return Encoding.UTF8.GetString(plainText);

        }
    }
}
