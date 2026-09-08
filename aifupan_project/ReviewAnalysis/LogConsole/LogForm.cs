using douyin.Utils;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using static System.Windows.Forms.VisualStyles.VisualStyleElement;

namespace ReviewAnalysis.LogConsole
{
    public partial class LogForm : Form
    {
        // 单例实例（避免多开导致线程冲突）
        private static LogForm _instance;
        public static LogForm Instance => _instance ?? (_instance = new LogForm());
        // 配置项：最大日志行数（默认 10000 行，约占用 5-10MB 内存）
        // 建议范围：5000-20000 行，根据机器性能调整
        private const int MAX_LOG_LINES = 10000;
        // 批量更新阈值：累计 N 条日志后批量追加到 UI（减少渲染次数）
        private const int BATCH_UPDATE_COUNT = 20;
        // 批量更新缓存（临时存储待追加的日志文本）
        private readonly List<string> _batchLogCache = new List<string>();
        // 线程安全锁（保护批量缓存）
        private readonly object _batchLock = new object();
        public LogForm()
        {
            InitializeComponent();
            // 必须在 UI 线程初始化日志服务（构造函数是 UI 线程）
            LogService.Instance.Init(UpdateLogUI);
            cboLogLevel.SelectedIndexChanged += CboLogLevel_SelectedIndexChanged;
            if (cboLogLevel.Items.Count > 0)
            {
                cboLogLevel.SelectedIndex = 0;
            }
            // 关键优化：禁用 RichTextBox 不必要特性，降低渲染压力
            OptimizeRichTextBoxSettings();
            // 恢复布局（批量更新前已 Suspend）
            rtbLog.ResumeLayout(false);
        }
        /// <summary>
        /// 优化 RichTextBox 设置（核心卡顿优化）
        /// </summary>
        private void OptimizeRichTextBoxSettings()
        {
            rtbLog.Font = new Font("Consolas", 12F, FontStyle.Regular, GraphicsUnit.Point, ((byte)(134)));
            rtbLog.ReadOnly = true;
            rtbLog.ScrollBars = RichTextBoxScrollBars.Both;
            rtbLog.WordWrap = false; // 禁用自动换行（大文本下换行渲染极耗资源）
            //rtbLog.DoubleBuffered = true; // 启用双缓冲，减少闪烁
            rtbLog.HideSelection = true; // 隐藏选中状态，降低渲染开销
                                         // 关闭自动刷新，批量更新后手动刷新
            rtbLog.SuspendLayout();
        }
        // 跨线程更新日志界面（核心：避免线程安全异常）
        /// <summary>
        /// 跨线程安全更新日志（核心修复）
        /// </summary>
        private void UpdateLogUI(LogModel log)
        {
            // 先判断窗体/控件是否已释放（避免窗体关闭后仍尝试更新）
            if (this.IsDisposed || rtbLog.IsDisposed)
                return;
            // 校验控件句柄是否已创建（核心修复！）
            if (!rtbLog.IsHandleCreated)
            {
                // 句柄未创建，重新存入缓存队列
                LogService.Instance.SubmitLog(log.Level, log.Message, log.Source);
                return;
            }
            // 判断是否需要跨线程调用（WinForms 控件只能主线程操作）
            if (rtbLog.InvokeRequired)
            {
                // 跨线程委托调用
                rtbLog.Invoke(new Action<LogModel>(UpdateLogUI), log);
                return;
            }
            // 强制使用异步委托（BeginInvoke），避免阻塞后台线程
            // 无论是否跨线程，都通过委托交给 UI 线程执行，彻底避免线程安全问题
            rtbLog.BeginInvoke(new Action(() =>
            {
                // 委托内部是 UI 线程，可安全操作控件
                if (rtbLog.IsDisposed) return;

                // 设置日志颜色
                switch (log.Level)
                {
                    case LogLevel.Info:
                        rtbLog.SelectionColor = Color.Black;
                        break;
                    case LogLevel.Warning:
                        rtbLog.SelectionColor = Color.Orange;
                        break;
                    case LogLevel.Error:
                        rtbLog.SelectionColor = Color.Red;
                        break;
                    case LogLevel.Recrd:
                        rtbLog.SelectionColor = Color.Green;
                        break;
                    case LogLevel.Analysis:
                        rtbLog.SelectionColor = Color.Blue;
                        break;
                }

                // 拼接日志内容
                string logText = $"[{log.Time:yyyy-MM-dd HH:mm:ss.fff}] " +
                                $"[{log.Level.ToString().PadRight(7)}] " +
                                $"[{log.Source.PadRight(15)}] " +
                                log.Message + Environment.NewLine;

                // 追加日志并自动滚动
                rtbLog.AppendText(logText);
                if (rtbLog.IsDisposed) return; // 追加文本后再次检查
                
                rtbLog.SelectionStart = rtbLog.TextLength;
                if (rtbLog.IsDisposed) return; // 设置选择位置后再次检查
                
                SafeScrollToCaret();

                // 更新日志计数显示
                UpdateLogCountDisplay();
                
                // 检查并限制日志行数，超过阈值自动清理旧日志
                LimitLogLines();

            }));
        }


        // 日志级别筛选（下拉框变化时生效）
        private void CboLogLevel_SelectedIndexChanged(object sender, EventArgs e)
        {
            switch (cboLogLevel.SelectedItem.ToString())
            {
                case "所有":
                    LogService.Instance.FilterLevel = LogLevel.Info;
                    break;
                case "Warning":
                    LogService.Instance.FilterLevel = LogLevel.Warning;
                    break;
                case "Error":
                    LogService.Instance.FilterLevel = LogLevel.Error;
                    break;
                case "Recrd":
                    LogService.Instance.FilterLevel = LogLevel.Recrd;
                    break;
                case "Analysis":
                    LogService.Instance.FilterLevel = LogLevel.Analysis;
                    break;
            }
            // 筛选后清空现有日志，重新加载（可选：根据需求决定是否保留）
            rtbLog.Clear();
            UpdateLogCountDisplay();
        }

        // 清空日志按钮
        private void btnClear_Click(object sender, EventArgs e)
        {
            rtbLog.Clear();
            UpdateLogCountDisplay();
        }

        // 保存日志按钮（导出到文本文件）
        private void btnSave_Click(object sender, EventArgs e)
        {
            using (SaveFileDialog sfd = new SaveFileDialog())
            {
                sfd.Title = "保存日志";
                sfd.Filter = "文本文件 (*.txt)|*.txt|所有文件 (*.*)|*.*";
                sfd.FileName = $"程序日志_{DateTime.Now:yyyyMMddHHmmss}.txt";
                sfd.InitialDirectory = Environment.GetFolderPath(Environment.SpecialFolder.Desktop);

                if (sfd.ShowDialog() == DialogResult.OK)
                {
                    try
                    {
                        // 保存 RichTextBox 中的所有日志到文件
                        rtbLog.SaveFile(sfd.FileName, RichTextBoxStreamType.PlainText);
                        MessageBox.Show($"日志保存成功！\n路径：{sfd.FileName}", "成功", MessageBoxButtons.OK, MessageBoxIcon.Information);
                    }
                    catch (Exception ex)
                    {
                        MessageBox.Show($"日志保存失败：{ex.Message}", "错误", MessageBoxButtons.OK, MessageBoxIcon.Error);
                    }
                }
            }
        }

        // 复制日志按钮（复制到剪贴板）
        //private void btnCopy_Click(object sender, EventArgs e)
        //{
        //    if (string.IsNullOrEmpty(rtbLog.Text))
        //    {
        //        MessageBox.Show("当前无日志可复制！", "提示", MessageBoxButtons.OK, MessageBoxIcon.Information);
        //        return;
        //    }

        //    Clipboard.SetText(rtbLog.Text);
        //    MessageBox.Show("日志已复制到剪贴板！", "成功", MessageBoxButtons.OK, MessageBoxIcon.Information);
        //}

        /// <summary>
        /// 限制日志最大行数（核心：避免文本无限增长）
        /// </summary>
        private void LimitLogLines()
        {
            // 获取当前日志行数（通过换行符分割，高效计算）
            int currentLines = rtbLog.Lines.Length;
            if (currentLines <= MAX_LOG_LINES) return;

            // 计算需要删除的行数（保留最新 MAX_LOG_LINES 行，多删 100 行避免频繁触发）
            int linesToRemove = currentLines - MAX_LOG_LINES + 100;
            if (linesToRemove <= 0) return;

            // 暂停界面更新，提高删除效率
            rtbLog.SuspendLayout();
            
            try
            {
                // 找到删除的结束位置（第 linesToRemove 行的起始索引）
                int startIndex = 0;
                int lineCount = 0;
                string text = rtbLog.Text;
                int textLength = text.Length;
                
                for (int i = 0; i < textLength && lineCount < linesToRemove; i++)
                {
                    if (text[i] == '\n')
                    {
                        lineCount++;
                        startIndex = i + 1; // 跳过换行符，从下一行开始保留
                    }
                }

                // 删除旧日志（高效删除，避免重建整个文本）
                rtbLog.Select(0, startIndex);
                rtbLog.SelectedText = ""; // 清空选中的旧日志
                rtbLog.DeselectAll();

                // 记录清理日志（避免用户困惑）
                FileUtils.log($"日志自动清理：已删除 {linesToRemove} 条旧日志，保留最新 {MAX_LOG_LINES} 条", "LogForm.AutoClean");
            }
            finally
            {
                // 恢复界面更新
                rtbLog.ResumeLayout();
            }
            
            // 清理后更新计数显示
            UpdateLogCountDisplay();
        }
        
        /// <summary>
        /// 更新日志计数显示
        /// </summary>
        private void UpdateLogCountDisplay()
        {
            if (lblLogCount.IsDisposed) return;
            
            if (rtbLog.InvokeRequired)
            {
                rtbLog.BeginInvoke(new Action(() => UpdateLogCountDisplay()));
                return;
            }
            
            int currentLines = rtbLog.Lines.Length;
            lblLogCount.Text = $"日志: {currentLines} / {MAX_LOG_LINES}";
        }
        /// <summary>
        /// 清空日志（优化内存释放）
        /// </summary>
        private void ClearUiLog()
        {
            if (rtbLog.IsDisposed) return;

            // 彻底释放内存：先清空文本，再强制 GC（避免内存泄漏）
            rtbLog.Clear();
            rtbLog.Text = string.Empty; // 双重清空，确保释放
            GC.Collect(); // 触发垃圾回收，释放文本占用的内存
            GC.WaitForPendingFinalizers();

            FileUtils.log("界面日志已清空", "LogForm.Clear");
        }

        private void LogForm_FormClosing(object sender, FormClosingEventArgs e)
        {
            // 停止日志服务（终止后台线程）
            LogService.Instance.Stop();
            // 释放单例实例
            _instance = null;
            
            // 清空并安全释放资源
            if (!rtbLog.IsDisposed)
            {
                rtbLog.Clear();
                rtbLog.Dispose();
            }
            this.Dispose();
        }

        private void LogForm_Load(object sender, EventArgs e)
        {
            //try
            //{
            //    // 校验句柄是否已创建（确保安全）
            //    if (rtbLog.IsHandleCreated)
            //    {
            //        // 初始化日志服务+启动
            //        LogService.Instance.Init(UpdateLogUI);
            //        LogService.Instance.Start();
            //    }
            //    else
            //    {
            //        // 极端情况：Load 事件中句柄仍未创建，延迟100ms启动
            //        this.BeginInvoke(new Action(() =>
            //        {
            //            LogService.Instance.Init(UpdateLogUI);
            //            LogService.Instance.Start();
            //        }), 100);
            //    }
            //}
            //catch (Exception ex)
            //{
            //    FileUtils.LogError($"LogForm 启动日志服务异常：{ex.Message}", "LogForm 初始化异常");
            //}
        }

        /// <summary>
        /// 停止/恢复日志按钮点击事件（切换状态）
        /// </summary>
        private void btnPauseResume_Click(object sender, EventArgs e)
        {
            // 1. 获取当前暂停状态，切换为相反状态
            bool currentPaused = LogService.Instance.IsLogPaused;
            bool newPaused = !currentPaused;

            // 2. 调用 LogService 切换暂停状态（默认不停止文件写入，如需停止可改为 true）
            LogService.Instance.ToggleLogPause(newPaused, stopFileWrite: false);

            // 3. 更新按钮文本和提示
            btnPauseResume.Text = newPaused ? "恢复日志" : "停止日志";
            string tip = newPaused
                ? "日志界面更新已暂停（文件写入正常）\n可安心查看/复制日志内容"
                : "日志界面更新已恢复\n将批量显示暂停期间的缓存日志";
            //MessageBox.Show(tip, "操作成功", MessageBoxButtons.OK, MessageBoxIcon.Information);
        }

        /// <summary>
        /// 复制日志按钮点击事件（优化版：暂停后复制，避免日志刷屏导致复制不完整）
        /// </summary>
        private void btnCopy_Click(object sender, EventArgs e)
        {
            // 记录原始暂停状态（用于复制后恢复）
            bool wasPaused = LogService.Instance.IsLogPaused;

            try
            {
                // 1. 若未暂停，先临时暂停日志更新（避免复制时日志变化）
                if (!wasPaused)
                {
                    LogService.Instance.ToggleLogPause(true);
                }

                // 2. 在 UI 线程执行复制操作（确保线程安全）
                if (rtbLog.InvokeRequired)
                {
                    rtbLog.Invoke(new Action(CopyLogToClipboard));
                }
                else
                {
                    CopyLogToClipboard();
                }
            }
            finally
            {
                // 3. 恢复原始暂停状态（如果之前未暂停，复制后自动恢复更新）
                if (!wasPaused)
                {
                    LogService.Instance.ToggleLogPause(false);
                }
            }
        }

        /// <summary>
        /// 核心复制逻辑（UI 线程执行）
        /// </summary>
        private void CopyLogToClipboard()
        {
            // 检查日志是否为空
            if (string.IsNullOrWhiteSpace(rtbLog.Text))
            {
                MessageBox.Show("当前日志为空，无内容可复制！", "提示", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }

            try
            {
                // 选中所有日志 → 复制到剪贴板 → 取消选中（避免界面高亮）
                rtbLog.SelectAll();
                rtbLog.Copy();
                rtbLog.DeselectAll(); // 取消选中，不影响后续查看

                MessageBox.Show("日志已成功复制到剪贴板！\n可直接粘贴到记事本、Excel 等工具", "复制成功", MessageBoxButtons.OK, MessageBoxIcon.Information);
            }
            catch (Exception ex)
            {
                // 复制失败时记录日志并提示
                FileUtils.LogError($"复制日志失败：{ex.Message}", "LogForm.Copy");
                MessageBox.Show($"复制日志失败：{ex.Message}", "错误", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }
        /// <summary>
        /// 复制选中的文本到剪贴板
        /// </summary>
        private void CopySelectedText()
        {
            try
            {
                // 检查是否有选中内容
                if (string.IsNullOrEmpty(rtbLog.SelectedText))
                {
                    MessageBox.Show("请先选中要复制的文本！", "提示", MessageBoxButtons.OK, MessageBoxIcon.Information);
                    return;
                }

                // 复制选中内容到剪贴板（两种方式任选）
                // 方式1：用RichTextBox自带的Copy方法（简洁）
                rtbLog.Copy();
                // 方式2：手动操作剪贴板（可自定义内容）
                // Clipboard.SetText(rtbLog.SelectedText);

                //MessageBox.Show("选中内容已复制到剪贴板！", "成功", MessageBoxButtons.OK, MessageBoxIcon.Information);
            }
            catch (Exception ex)
            {
                // 捕获剪贴板异常（如被占用、无权限）
                MessageBox.Show($"复制失败：{ex.Message}", "错误", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }

        private void rtbLog_KeyDown(object sender, KeyEventArgs e)
        {
            // 检测Ctrl+C组合键
            if (e.Control && e.KeyCode == Keys.C)
            {
                CopySelectedText();
                e.SuppressKeyPress = true; // 阻止系统默认行为（可选）
            }
        }

        /// <summary>
        /// 安全地滚动到光标位置（避免对象已释放异常）
        /// </summary>
        private void SafeScrollToCaret()
        {
            try
            {
                if (rtbLog != null && !rtbLog.IsDisposed && rtbLog.IsHandleCreated)
                {
                    rtbLog.ScrollToCaret();
                }
            }
            catch (ObjectDisposedException)
            {
                // 忽略已释放异常
            }
            catch (Exception)
            {
                // 忽略其他相关异常
            }
        }
    }
}
