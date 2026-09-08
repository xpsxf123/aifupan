using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.BeanCache
{
    public class ControllerMethod
    {
        public Type ControllerType { get; set; }
        public MethodInfo Method { get; set; }

        public ControllerMethod(Type controllerType, MethodInfo method)
        {
            ControllerType = controllerType;
            Method = method;
        }
    }
}
