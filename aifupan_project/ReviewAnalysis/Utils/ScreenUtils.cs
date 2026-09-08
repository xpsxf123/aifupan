using douyin.Utils;
using System;
using System.Drawing;
using System.Runtime.InteropServices;
using System.Windows.Forms;

namespace ReviewAnalysis.Utils
{


    public static class ScreenUtils
    {

        [DllImport("user32.dll")]
        static extern IntPtr GetDC(IntPtr hwnd);
        [DllImport("gdi32.dll")]
        static extern int GetDeviceCaps(IntPtr hdc, int nIndex);
        [DllImport("user32.dll")]
        static extern int ReleaseDC(IntPtr hwnd, IntPtr hdc);
        [DllImport("user32.dll")]
        static extern bool SetProcessDPIAware();

        /// <summary>
        /// 获取系统推荐的缩放比例 1为100%
        /// </summary>
        /// <returns></returns>
        public static float GetDpiRatio()
        {
            using (Graphics g = Graphics.FromHwnd(IntPtr.Zero))
            {
                // 声明当前进程DPI感知
                SetProcessDPIAware();

                IntPtr hdc = GetDC(IntPtr.Zero);
                int dpi = GetDeviceCaps(hdc, 88);
                ReleaseDC(IntPtr.Zero, hdc);

                return dpi / 96f;

            }

            return 1;
        }

        /// <summary>
        /// 获取当前屏幕工作区的宽高（不含任务栏）
        /// </summary>
        /// <param name="workAreaWidth">工作区宽度</param>
        /// <param name="workAreaHeight">工作区高度</param>
        public static void GetWorkAreaDimensions(out int workAreaWidth, out int workAreaHeight)
        {
            // 获取当前鼠标所在屏幕（或主屏幕）的工作区
            Screen currentScreen = Screen.FromPoint(Cursor.Position);
            Rectangle workArea = currentScreen.WorkingArea;

            workAreaWidth = workArea.Width;
            workAreaHeight = workArea.Height;
        }

        /// <summary>
        /// 计算窗体所需的缩放比例
        /// </summary>
        /// <param name="minFormWidth">窗体最小宽度（如1494）</param>
        /// <param name="minFormHeight">窗体最小高度（如840）</param>
        /// <returns>缩放比例（0-1之间，屏幕足够大时返回1）</returns>
        public static double GetScaleRatio(int minFormWidth, int minFormHeight)
        {
            // 获取当前屏幕可用工作区（不含任务栏）
            var workArea = GetWorkAreaDimensions();

            // 计算宽度方向的缩放比例
            double widthRatio = (double)workArea.Width / minFormWidth;
            // 计算高度方向的缩放比例
            double heightRatio = (double)workArea.Height / minFormHeight;

            // 取两个方向中较小的比例（确保窗体能完整显示）
            double scaleRatio = Math.Min(widthRatio, heightRatio);

            // 如果缩放比例大于1，则返回1（不需要放大，保持原尺寸）
            return Math.Min(scaleRatio, 1.0);
        }

        /// <summary>
        /// 重载：直接返回包含工作区宽高的元组
        /// </summary>
        /// <returns>(宽度, 高度)</returns>
        public static (int Width, int Height) GetWorkAreaDimensions()
        {
            Screen currentScreen = Screen.FromPoint(Cursor.Position);
            Rectangle workArea = currentScreen.WorkingArea;
            return (workArea.Width, workArea.Height);
        }

        /// <summary>
        /// 获取最小的宽高
        /// </summary>
        /// <param name="minFormWidth"></param>
        /// <param name="minFormHeight"></param>
        /// <returns></returns>
        public static (int Width, int Height) GetMinimumSize(int minFormWidth, int minFormHeight)
        {
            // 获取当前屏幕可用工作区（不含任务栏）
            var workArea = GetWorkAreaDimensions();
            
            int w = Math.Min(workArea.Width, minFormWidth);

            int h = Math.Min(workArea.Height, minFormHeight);
            return (w, h);
        }

        /// <summary>
        /// 获取窗体的宽高
        /// </summary>
        /// <param name="minFormWidth"></param>
        /// <param name="minFormHeight"></param>
        /// <param name="rate"></param>
        /// <returns></returns>
        public static (int Width, int Height) GetClientSize(int minFormWidth, int minFormHeight, double rate)
        {
            var workAreaOne = GetWorkAreaDimensions();
            var workAreaTwo = GetMinimumSize(minFormWidth, minFormHeight);

            int w = workAreaOne.Width;
            if (workAreaOne.Width != workAreaTwo.Width)
            {
                w = Math.Min((int)(workAreaOne.Width * rate), workAreaTwo.Width);
            }

            int h = workAreaOne.Height;
            if (workAreaOne.Height != workAreaTwo.Height)
            {
                h = Math.Min((int)(workAreaOne.Height * rate), workAreaTwo.Height);
            }
            return (w, h);
        }

        //public static (int Width, int Height) GetFormMinSize(int formWidth, int formHeight, double rate)
        //{
        //    (int Width, int Height) value = GetWorkAreaDimensions();
        //}
    }
}
