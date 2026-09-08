using CefSharp;
using ReviewAnalysis.Global;
using ReviewAnalysis.Utils;
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Windows.Forms;

namespace ReviewAnalysis
{
    partial class FormMain
    {

        private enum HitTestType { None, Left = 10, Right = 11, Top = 12, Bottom = 13, TopLeft = 14, TopRight = 15, BottomLeft = 16, BottomRight = 17 }
        private const int _edgeSize = 5;

        /// <summary>
        /// 必需的设计器变量。
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// 清理所有正在使用的资源。
        /// </summary>
        /// <param name="disposing">如果应释放托管资源，为 true；否则为 false。</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows 窗体设计器生成的代码

        private System.Windows.Forms.NotifyIcon notifyIcon;
        /// <summary>
        /// 设计器支持所需的方法 - 不要修改
        /// 使用代码编辑器修改此方法的内容。
        /// </summary>
        private void InitializeComponent()
        {
            System.Drawing.Rectangle screenBounds = System.Windows.Forms.Screen.PrimaryScreen.Bounds;
            int screenWidth = screenBounds.Width;      // 屏幕宽度
            int screenHeight = screenBounds.Height;    // 屏幕高度（包含任务栏）

            this.titleBar = new System.Windows.Forms.Panel();
            //this.titleLabel = new System.Windows.Forms.Label();
            this.btnCustom = new System.Windows.Forms.PictureBox();
            this.btnMin = new System.Windows.Forms.PictureBox();
            this.btnMax = new System.Windows.Forms.PictureBox();
            this.btnClose = new System.Windows.Forms.PictureBox();
            this.SuspendLayout();

            // 初始化标题栏 - 高度将在Load事件中动态设置
            this.titleBar.BackColor = defaultTitleBarTopColor;
            this.titleBar.Dock = System.Windows.Forms.DockStyle.Top;
            this.titleBar.Height = 32; // 临时高度，将在Load事件中更新
            //this.titleBar.Controls.Add(this.titleLabel);
            this.titleBar.Controls.Add(this.btnCustom);
            this.titleBar.Controls.Add(this.btnMin);
            this.titleBar.Controls.Add(this.btnMax);
            this.titleBar.Controls.Add(this.btnClose);
            this.titleBar.Name = "titleBar";


            //this.titleLabel.Text = "爱复盘";
            //this.titleLabel.Font = new System.Drawing.Font("Segoe UI", 10F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point);
            //this.titleLabel.Location = new System.Drawing.Point(12, 7);
            //this.titleLabel.AutoSize = true;
            //this.titleLabel.ForeColor = System.Drawing.Color.Black;
            //this.titleLabel.Name = "titleLabel";


            // 初始化按钮 - 位置和大小将在Load事件中动态设置
            this.btnCustom.Cursor = System.Windows.Forms.Cursors.Hand;
            // 图片将在LoadButtonImages中设置以支持DPI缩放
            this.btnCustom.Location = new System.Drawing.Point(64, 2);
            this.btnCustom.Name = "pictureBox";
            this.btnCustom.Size = new System.Drawing.Size(32, 32);
            this.btnCustom.SizeMode = System.Windows.Forms.PictureBoxSizeMode.CenterImage;
            this.btnCustom.TabIndex = 4;
            this.btnCustom.TabStop = false;
            this.btnCustom.Click += PictureBox_Click;

            this.btnMin.Cursor = System.Windows.Forms.Cursors.Hand;
            this.btnMin.Location = new System.Drawing.Point(64, 2);
            this.btnMin.Name = "btnMin";
            this.btnMin.Size = new System.Drawing.Size(40, 32);
            this.btnMin.SizeMode = System.Windows.Forms.PictureBoxSizeMode.CenterImage;
            this.btnMin.TabIndex = 1;
            this.btnMin.TabStop = false;

            this.btnMax.Cursor = System.Windows.Forms.Cursors.Hand;
            this.btnMax.Location = new System.Drawing.Point(64, 2);
            this.btnMax.Name = "btnMax";
            this.btnMax.Size = new System.Drawing.Size(40, 32);
            this.btnMax.SizeMode = System.Windows.Forms.PictureBoxSizeMode.CenterImage;
            this.btnMax.TabIndex = 2;
            this.btnMax.TabStop = false;

            this.btnClose.Cursor = System.Windows.Forms.Cursors.Hand;
            this.btnClose.Location = new System.Drawing.Point(64, 2);
            this.btnClose.Name = "btnClose";
            this.btnClose.Size = new System.Drawing.Size(40, 30);
            this.btnClose.SizeMode = System.Windows.Forms.PictureBoxSizeMode.CenterImage;
            this.btnClose.TabIndex = 3;
            this.btnClose.TabStop = false;

            // 
            // Form1
            // 
            this.Controls.Add(this.titleBar);
            this.FormBorderStyle = System.Windows.Forms.FormBorderStyle.None;
            this.Text = "爱复盘";
            this.Name = "Form1";
            this.ResumeLayout(false);

        }

        /// <summary>
        /// 设计器支持所需的方法 - 不要修改
        /// 使用代码编辑器修改此方法的内容。
        /// </summary>
        private System.Windows.Forms.Panel titleBar;
        //private System.Windows.Forms.Label titleLabel;
        private System.Windows.Forms.PictureBox btnMin;
        private System.Windows.Forms.PictureBox btnMax;
        private System.Windows.Forms.PictureBox btnClose;
        private System.Windows.Forms.PictureBox btnCustom;
        private System.Windows.Forms.Button f12;

        #endregion

        private System.Windows.Forms.Panel panelTop;
    }
}

