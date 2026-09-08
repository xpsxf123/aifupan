using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ReviewAnalysis
{
    public partial class SplashScreen : Form
    {
        public SplashScreen()
        {
            InitializeComponent();
            // 启动窗体样式配置
            this.FormBorderStyle = FormBorderStyle.None; // 无边框
            this.StartPosition = FormStartPosition.CenterScreen; // 居中显示
            this.Size = new System.Drawing.Size(400, 200); // 固定尺寸
                                                           // 添加进度条

            // 添加加载提示标签
            Label lblLoading = new Label
            {
                Text = "程序加载中...\r\nReviewAnalysis 爱复盘",
                Font = new System.Drawing.Font("微软雅黑", 12),
                Dock = DockStyle.Fill,
                TextAlign = System.Drawing.ContentAlignment.MiddleCenter
            };
            this.Controls.Add(lblLoading);

            ProgressBar progressBar = new ProgressBar
            {
                Name = "progressBar",
                Dock = DockStyle.Bottom,
                Height = 20,
                Minimum = 0,
                Maximum = 100
            };
            this.Controls.Add(progressBar);
        }

        /// <summary>
        /// 更新启动加载提示（跨线程安全）
        /// </summary>
        /// <param name="message">提示文本</param>
        public void UpdateLoadingMessage(string message)
        {
            if (this.InvokeRequired)
            {
                this.BeginInvoke(new Action<string>(UpdateLoadingMessage), message);
                //this.Invoke(new Action<string>(UpdateLoadingMessage), message);
                return;
            }
            ((Label)this.Controls[0]).Text = message;
        }

    }
}
