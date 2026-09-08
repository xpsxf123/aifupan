using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ReviewAnalysis
{
    // 自定义应用上下文
    public class AfpAppContext : ApplicationContext
    {
        private SplashScreen _splash;
        private FormMain _mainForm;

        public AfpAppContext()
        {
            // 显示加载窗体
            _splash = new SplashScreen();
            _splash.Show();
            _splash.UpdateLoadingMessage("爱复盘启动中...");

            // 启动后台加载
            Task.Run(LoadResourcesAsync);
        }

        private async Task LoadResourcesAsync()
        {
            try
            {
                _splash.UpdateLoadingMessage("初始化日志模块...");
                UpdateProgress(1);
                await Task.Delay(500); // 模拟耗时操作

                _splash.UpdateLoadingMessage("启动HTTP服务...");
                UpdateProgress(2);
                await Task.Delay(800);

                _splash.UpdateLoadingMessage("加载配置文件...");
                UpdateProgress(3);

                await Task.Delay(800); // 模拟异步加载

                // 模拟资源加载流程
                await LoadStep("初始化日志模块...", 4);
                await LoadStep("启动HTTP服务...", 30);
                await LoadStep("加载配置文件...", 40);
                await LoadStep("准备用户界面...", 50);
                await LoadStep("完成系统初始化", 100);

                // 切换到主窗体（在UI线程）
                if (_splash.InvokeRequired)
                {
                    _splash.Invoke(new Action(SwitchToMainForm));
                }
                else
                {
                    SwitchToMainForm();
                }
            }
            catch(Exception ex) 
            {
                // 处理加载错误
                _splash.Invoke(new Action(() =>
                {
                    _splash.UpdateLoadingMessage($"加载失败: {ex.Message}");
                    _splash.Close();
                    MessageBox.Show($"系统初始化失败: {ex.Message}", "错误",
                        MessageBoxButtons.OK, MessageBoxIcon.Error);
                    ExitThread();
                })) ;
            }


        }

        private async Task LoadStep(string message, int progress)
        {
            _splash.UpdateLoadingMessage(message);
            UpdateProgress(progress);
            await Task.Delay(500); // 模拟每个步骤的耗时
        }

        private void UpdateProgress(int progress)
        {
            // 安全更新进度条（确保在UI线程）
            _splash?.Invoke(new Action(() =>
            {
                if (_splash.Controls.ContainsKey("progressBar"))
                {
                    ((ProgressBar)_splash.Controls["progressBar"]).Value = progress;
                }
            }));
        }

        private void SwitchToMainForm()
        {
            // 关闭加载窗体
            _splash?.Close();
            _splash = null;

            // 创建主窗体
            _mainForm = new FormMain();

            // 监听主窗体关闭事件
            _mainForm.FormClosed += OnMainFormClosed;

            // 显示主窗体
            _mainForm.Show();
        }

        private void OnMainFormClosed(object sender, FormClosedEventArgs e)
        {
            // 主窗体关闭 → 退出整个应用程序
            ExitThread(); // 通知 ApplicationContext 退出
        }
    }
}
