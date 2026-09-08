using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.BeanCache
{
    public class HttpReponse
    {
        public int code{ get; }

        public string msg { get; }

        public object data { get; }
        public HttpReponse(int _code,string _msg,object _data) 
        {
            this.code = _code;
            this.msg = _msg;
            this.data = _data;
        }
    }
}
