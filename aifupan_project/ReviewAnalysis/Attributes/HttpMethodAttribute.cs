using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Attributes
{
    /// <summary>
    /// HTTP方法注解基类
    /// </summary>
    public abstract class HttpMethodAttribute : Attribute
    {
        public string Value { get; }
        protected HttpMethodAttribute(string value) => Value = value;
    }
}
