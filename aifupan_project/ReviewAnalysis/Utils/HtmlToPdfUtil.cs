using System;
using System.Diagnostics;
using System.IO;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Timers;
using System.Windows.Forms;
using CefSharp;
using CefSharp.WinForms;
using douyin.Utils;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Global;
using Timer = System.Timers.Timer;

namespace ReviewAnalysis.Utils
{
    public class HtmlToPdfUtil
    {
        
        public static Task<bool> Print(string html, string pdfPath, Action<bool> generateFilePost)
        {
            var tcs = new TaskCompletionSource<bool>();
            Thread thread = new Thread(() =>
            {
                Application.Run(new HtmlPrintForm(html, pdfPath, tcs, generateFilePost));
            });
            thread.IsBackground = true;
            thread.SetApartmentState(ApartmentState.STA);
            thread.Start();
            return tcs.Task;
        }
    }

    public class CefSharpMsg
    {
        HtmlPrintForm _form;
        
        public CefSharpMsg (HtmlPrintForm form)
        {
            _form = form;
        }
        
        public void print()
        {
            _form.Print();
        }
    }
    
    public class HtmlPrintForm : Form
    {
        private readonly string _html;
        private readonly string _pdfPath;
        private readonly TaskCompletionSource<bool> _tcs;
        private readonly Action<bool> _generateFilePost;
        private ChromiumWebBrowser _browser;
        private readonly Timer _timeoutTimer;
        private readonly string fileName;
        private readonly string filePath = Path.GetFullPath("dataCollect");
        private string filePathName = null;

        public HtmlPrintForm(string html, string pdfPath, TaskCompletionSource<bool> tcs, Action<bool> generateFilePost)
        {
            _html = html;
            _pdfPath = pdfPath;
            _tcs = tcs;
            _generateFilePost = generateFilePost;
            fileName = "temp/" + Guid.NewGuid().ToString("N") + ".html";
            FileUtils.createDirectory(filePath+"/temp");
            // 设置60秒超时自动关闭
            _timeoutTimer = new Timer(40000);
            _timeoutTimer.Elapsed += TimeoutClosed;
            _timeoutTimer.AutoReset = false;
            _timeoutTimer.Enabled = true;
            
            InitForm();
            
            // 不在这里 InitBrowser！
            Load += (s, e) => 
            {
                // 此时 Handle 已创建，可以安全使用 Invoke
                InitBrowser();
            };
        }

        private void InitForm()
        {
            Width = 1200;     // ≥ A4 宽度
            Height = 1600;    // ≥ A4 高度
            Opacity = 0;      // 不可见
            ShowInTaskbar = false;
            FormBorderStyle = FormBorderStyle.None;
        }

        private void InitBrowser()
        {
            try
            {
                // 把html内容存到文件中
                Directory.CreateDirectory(filePath);
                filePathName = filePath + "/" + fileName;
                var encoding = new UTF8Encoding(encoderShouldEmitUTF8Identifier: true); // 带 BOM
                
                File.WriteAllText(filePathName, _html, encoding);
                
                string url = new Uri(filePathName).AbsoluteUri;
                
                _browser = new ChromiumWebBrowser(url)
                {
                    Dock = DockStyle.Fill
                };
                _browser.JavascriptObjectRepository.Settings.LegacyBindingEnabled = true;

                _browser.JavascriptObjectRepository.Register(
                    "client",
                    new CefSharpMsg(this),
                    isAsync: true
                );
                if (ReplayHttpUtils.developmentMode == 0 && Constant.VERSION.Contains("test"))
                {
                    _browser.IsBrowserInitializedChanged += (sender, args) =>
                    {
                        if (_browser.IsBrowserInitialized)
                        {
                            _browser?.ShowDevTools();
                        }
                    };
                    
                    Opacity = 1;      // 可见
                    ShowInTaskbar = true;
                    FormBorderStyle = FormBorderStyle.FixedSingle;
                }
                Controls.Add(_browser);
            }
            catch (Exception ex)
            {
                _tcs.TrySetException(ex);
                myClose(false, ex.Message);
            }
        }

        public async void Print()
        {
            var settings = new PdfPrintSettings
            {
                BackgroundsEnabled = true,

                // A4：单位是 inch（英寸）
                PaperWidth = 8.27,
                PaperHeight = 11.69,

                // 边距（英寸）
                MarginTop = 0.5,
                MarginBottom = 0.5,
                MarginLeft = 0.5,
                MarginRight = 0.5,

                Landscape = false
            };

            // 直接返回，不等打印了
            try
            {
                _tcs.TrySetResult(true);
            }
            catch (Exception ex)
            {
                _tcs.TrySetException(ex);
                myClose(false, ex.Message);
            }
            try
            {
                await _browser.PrintToPdfAsync(_pdfPath, settings);
                myClose(true, "");
            }
            catch (Exception ex)
            {
                myClose(false, ex.Message);
            }
        }

        private void myClose(bool success, string error)
        {
            // 删除临时文件
            if (!string.IsNullOrEmpty(filePathName))
            {
                try
                {
                    File.Delete(filePathName);
                }
                catch (Exception e)
                {
                    FileUtils.LogError(e.Message, "删除临时文件报错");
                }
            }
            if (!success)
            {
                FileUtils.LogError(error, "打印pdf报错");
            }
            try
            {
                _timeoutTimer?.Dispose();
                _generateFilePost?.Invoke(success);
            }
            catch (Exception e)
            {
            }
            Invoke(new Action(() =>
            {
                try
                {
                    _browser?.Dispose();
                }
                finally
                {
                    Close();
                    Application.ExitThread();
                }
            }));
        }

        /// <summary>
        /// 定时器超时后，直接打印窗口
        /// </summary>
        /// <param name="source"></param>
        /// <param name="e"></param>
        public void TimeoutClosed(object source, ElapsedEventArgs e)
        {
            _tcs.SetResult(true);
            Print();
        }
    }
}