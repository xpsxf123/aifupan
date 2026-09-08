using System;

namespace ReviewAnalysis.Attributes
{
    [AttributeUsage(AttributeTargets.Method, Inherited = false)]
    public class HttpPostAttribute:Attribute
    {
        public string Name { get; }
        public string Value { get; }

        public HttpPostAttribute(string name, string value)
        {
            Name = name;
            Value = value;
        }
    }
}
