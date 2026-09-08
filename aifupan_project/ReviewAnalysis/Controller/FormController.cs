using CefSharp;
using douyin.Utils;
using ReviewAnalysis.Attributes;
using ReviewAnalysis.Bll;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.Utils;
using ReviewAnalysis.Websocket;
using ReviewAnalysis.Websocket.socketAddress;
using System;
using System.Collections.Concurrent;
using System.Drawing;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ReviewAnalysis.Controller
{
    [RestController("窗体控制", "api/form")]
    public class FormController
    {

        public static volatile bool isMax = false;

        [HttpGet("窗体最大化/回复正常", "/togglemaxsize")]
        public void FormToggleMaxSize() 
        {
            Form form = FormUtils.GetForm();
            form.Invoke((MethodInvoker)delegate {
                // 判断当前是最大化还是正常
                if (isMax)
                {
                    // 当前是最大化，变成普通尺寸

                    // 获取当前屏幕的工作区域（不包含任务栏）
                    System.Drawing.Rectangle screenWorkingArea = System.Windows.Forms.Screen.GetWorkingArea(form);
                    int screenWidth = screenWorkingArea.Width;
                    int screenHeight = screenWorkingArea.Height;

                    if (1494 > screenWidth && 840 > screenHeight)
                    {
                        form.Top = 0;
                        form.Left = 0;
                        form.ClientSize = new System.Drawing.Size(screenWidth, screenHeight);
                    }
                    else
                    {
                        form.Top = (screenHeight - 840) / 2;
                        form.Left = (screenWidth - 1494) / 2;
                        form.ClientSize = new System.Drawing.Size(1494, 840);
                    }

                    isMax = false;
                }
                else
                {
                    // 当前是正常，变成最大化

                    System.Drawing.Rectangle screenWorkingArea = System.Windows.Forms.Screen.GetWorkingArea(form);
                    int screenWidth = screenWorkingArea.Width;
                    int screenHeight = screenWorkingArea.Height;
                    form.ClientSize = new System.Drawing.Size(screenWidth, screenHeight);

                    form.Top = 0;
                    form.Left = 0;

                    isMax = true;
                }
            });
        }

        [HttpGet("窗体最小化", "/minsize")]
        public void FormMinSize()
        {
            Form form = FormUtils.GetForm();
            form.Invoke((MethodInvoker)delegate {
                form.WindowState = FormWindowState.Minimized;
            });
        }

        [HttpGet("关闭窗体", "/close")]
        public async Task FormClose()
        {
            OperationAnchorBll.KillBarrageGrabProcesses();
            //关闭录制
            //OperationAnchorBll operation = FormUtils.GetOperationBll();
            //operation.StopDecector();
            await AnchorBll.StopDecectorAll();
            //FormUtils.SetCloseFromStatus(true);
            Form form = FormUtils.GetForm();
            form.Close();
            //FormMain.webBrower?.Dispose();
            //FormMain.webBrower = null;
            //Cef.Shutdown();
            //Environment.Exit(666);
        }
    }
}
