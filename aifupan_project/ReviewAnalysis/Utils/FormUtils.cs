using ReviewAnalysis.BeanCache;
using ReviewAnalysis.Bll;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ReviewAnalysis.Utils
{
   
    public class FormUtils
    {

        private static readonly Dictionary<string, Form> FormCache = new Dictionary<string, Form>();

        //private static readonly Dictionary<string, TcpHandle> TcpCache = new Dictionary<string, TcpHandle>();

        private static ConcurrentDictionary<string, OperationAnchorBll> operationAnchorBll = new ConcurrentDictionary<string, OperationAnchorBll>();


        public static OperationAnchorBll GetOperationBll() 
        {
            if (operationAnchorBll.TryGetValue("operation", out OperationAnchorBll operation))
            {
                return operation;
            }
            else 
            {
                OperationAnchorBll operationNew = new OperationAnchorBll();
                operationAnchorBll.AddOrUpdate("operation", operationNew, (oldKey, oldValue) => operationNew);
                return operationNew;
            }
        }

        private static bool IsCloseFrom = false;

        public static bool GetCloseFromStatus() 
        {
            return IsCloseFrom;
        }

        public static void SetCloseFromStatus(bool _IsCloseFrom) 
        {
            IsCloseFrom = _IsCloseFrom;
        }

        public static void SetFormValue(Form form) 
        {
            FormCache["form"] = form;
        }

        public static Form GetForm()
        {
            FormCache.TryGetValue("form", out Form form);
            return form;
        }

        //public static void SetTcpHandleValue(TcpHandle tcpHandle) 
        //{
        //    TcpCache["tcpHandle"] = tcpHandle;
        //}

        //public static TcpHandle GetTcpHandle() 
        //{
        //    if (TcpCache.TryGetValue("tcpHandle", out TcpHandle tcpHandle)) 
        //    {
        //        return tcpHandle;
        //    }
        //    return null;

        //}
    }
}
