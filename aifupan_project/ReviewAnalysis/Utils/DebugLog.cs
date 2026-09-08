using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using OpenCvSharp;
using ReviewAnalysis.vo;
using douyin.Utils;

public static class DebugLog
{

    [Conditional("DEBUG")]
    public static void WriteLine()
    {
    }

    [Conditional("DEBUG")]
    public static void WriteLine(object message)
    {
        FileUtils.log(message?.ToString(), "DebugLog");
    }

    [Conditional("DEBUG")]
    public static void WriteLine(string message)
    {
        FileUtils.log(message, "DebugLog");
    }

    [Conditional("DEBUG")]
    public static void WriteLine(string format, params object[] args)
    {
        FileUtils.log(string.Format(format, args), "DebugLog");
    }
}