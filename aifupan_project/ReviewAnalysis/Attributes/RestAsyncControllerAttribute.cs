using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Attributes
{
    [AttributeUsage(AttributeTargets.Class, Inherited = false)]
    public class RestAsyncControllerAttribute : Attribute
    {
        public string Name { get; }
        public string Value { get; }

        public RestAsyncControllerAttribute(string name, string value)
        {
            Name = name;
            Value = value;
        }
    }
}
