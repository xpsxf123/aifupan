using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Utils
{
    public class CustomException : Exception
    {
        public int ErrorCode { get; }

        public CustomException()
        {
        }

        public CustomException(string message, int errorCode = 500)
        : base(message)
        {
            ErrorCode = errorCode;
        }

        public static void create(string message, int errorCode = 7005)
        {
            throw new CustomException(message, errorCode);
        }
    }
}
