using System;


namespace ReviewAnalysis.Attributes
{
    [AttributeUsage(AttributeTargets.Class, Inherited = false)]
    public class RestControllerAttribute: Attribute
    {
        public string Name { get; }
        public string Value { get; }

        public RestControllerAttribute(string name, string value)
        {
            Name = name;
            Value = value;
        }
    }
}
