using System;


namespace ReviewAnalysis.Attributes
{
    [AttributeUsage(AttributeTargets.Method,Inherited = false)]
    public class HttpGetAttribute:Attribute
    {
        public string Name { get; }
        public string Value { get; }

        public HttpGetAttribute(string name, string value)
        {
            Name = name;
            Value = value;
        }
    }
}
