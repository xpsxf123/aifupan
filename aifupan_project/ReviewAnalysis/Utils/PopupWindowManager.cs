namespace ReviewAnalysis.Utils
{
    using System;
    using System.Collections.Concurrent;
    using System.Linq;
    using System.Threading.Tasks;
    using System.Windows.Forms;
    using CefSharp.WinForms;
    using douyin.Utils;

    public class PopupWindowManager
    {
        private static readonly Lazy<PopupWindowManager> lazy = new Lazy<PopupWindowManager>(() => new PopupWindowManager());
        public static PopupWindowManager Instance => lazy.Value;

        // 存储所有弹窗：Form -> Browser
        private readonly ConcurrentDictionary<Form, ChromiumWebBrowser> _popupWindows = new ConcurrentDictionary<Form, ChromiumWebBrowser>();

        private PopupWindowManager() { }

        public void RegisterPopup(Form form, ChromiumWebBrowser browser)
        {
            _popupWindows.TryAdd(form, browser);
            FileUtils.log($"[PopupManager] 注册弹窗，当前总数: {_popupWindows.Count}");
        }

        public void UnregisterPopup(Form form)
        {
            if (_popupWindows.TryRemove(form, out _))
            {
                FileUtils.log($"[PopupManager] 注销弹窗，当前总数: {_popupWindows.Count}");
            }
        }

        public bool HasActivePopups => _popupWindows.Count > 0;

        /// <summary>
        /// 请求关闭指定窗体（优雅关闭流程）
        /// </summary>
        public async Task RequestGracefulCloseAsync(Form form)
        {
            ChromiumWebBrowser browser = null;
            try
            {
                if (!_popupWindows.TryGetValue(form, out browser) || form.IsDisposed)
                    return;

                if (form.Visible)
                {
                    // 1. 隐藏窗体（视觉上“关闭”）
                    if (form.InvokeRequired)
                        form.Invoke((MethodInvoker)(() =>
                        {
                            form.Visible = false;
                        }));
                    else
                        form.Visible = false;
                }

                int waitCount = 0;
                while (!browser.IsDisposed && !browser.IsBrowserInitialized && waitCount < 50)
                {
                    FileUtils.log("cef还没有加载完成，等待100ms");
                    await Task.Delay(100); // 等待 0.1 秒
                    waitCount++;
                }

                // 2. 导航到 about:blank
                if (browser != null && !browser.IsDisposed && browser.IsBrowserInitialized)
                {
                    browser?.Load("about:blank");
                }

                // 3. 延迟释放（给 CEF 时间清理）
                await Task.Delay(5000); // 可根据需要调整
            }
            catch(Exception ex)
            {
                FileUtils.LogError(ex.Message, "请求关闭指定窗体报错");
            }


            // 4. 真正释放资源
            try
            {
                if (browser != null && !browser.IsDisposed)
                {
                    browser?.Dispose();
                }
            }
            catch (Exception ex)
            {
                FileUtils.log($"[PopupManager] Dispose 异常: {ex.Message}");
            }

            // 5. 从管理器移除
            UnregisterPopup(form);

            // 6. 真正关闭窗体（此时已无资源）
            if (!form.IsDisposed)
            {
                if (form.InvokeRequired)
                    form.Invoke((MethodInvoker)form.Close);
                else
                    form.Close();
            }

            FileUtils.log($"[PopupManager] 窗体已真正关闭，剩余弹窗: {_popupWindows.Count}");
        }


        /// <summary>
        /// 🔥 关闭所有已注册弹窗（顺序优雅关闭）
        /// </summary>
        public async Task CloseAllPopupsAsync()
        {
            if (_popupWindows.IsEmpty)
            {
                FileUtils.log("[PopupManager] 无需关闭，当前没有弹窗。");
                return;
            }

            FileUtils.log($"[PopupManager] 开始关闭全部弹窗，共 {_popupWindows.Count} 个。");

            // ✅ 先隐藏全部窗体（立即让界面消失）
            foreach (var form in _popupWindows.Keys.ToList())
            {
                try
                {
                    if (form != null && !form.IsDisposed)
                    {
                        if (form.InvokeRequired)
                            form.Invoke((MethodInvoker)(()=>
                            {
                                form.Visible = false;
                            }));
                        else
                            form.Visible = false;
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.log($"[PopupManager] 隐藏窗体异常: {ex.Message}");
                }
            }

            // 拷贝当前列表，避免在循环过程中被修改
            var forms = _popupWindows.Keys.ToList();

            foreach (var form in forms)
            {
                try
                {
                    await RequestGracefulCloseAsync(form);
                }
                catch (Exception ex)
                {
                    FileUtils.log($"[PopupManager] 关闭窗体异常: {ex.Message}");
                }
            }

            FileUtils.log("[PopupManager] 已完成关闭全部弹窗。");
        }
    }
}
