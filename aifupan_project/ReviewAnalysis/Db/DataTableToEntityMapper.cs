using System;
using System.Collections.Generic;
using System.Data;
using System.Data.SQLite;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Db
{
    public class DataTableToEntityMapper
    {
        public static List<T> ConvertToEntityList<T>(DataTable dataTable) where T : new()
        {
            List<T> entities = new List<T>();
            Dictionary<string, PropertyInfo> propertyDict = typeof(T).GetProperties()
                .ToDictionary(p => p.Name.ToLower(), p => p);

            foreach (DataRow row in dataTable.Rows)
            {
                T entity = new T();
                foreach (DataColumn column in dataTable.Columns)
                {
                    string columnName = column.ColumnName.ToLower();
                    string propertyName = ConvertToPropertyName(column.ColumnName);
                    if (propertyDict.ContainsKey(propertyName.ToLower()))
                    {
                        PropertyInfo property = propertyDict[propertyName.ToLower()];
                        if (property != null && property.CanWrite)
                        {
                            object value = row[column.ColumnName];
                            if (value != DBNull.Value)
                            {
                                property.SetValue(entity, Convert.ChangeType(value, property.PropertyType), null);
                            }
                        }
                    }
                }
                entities.Add(entity);
            }

            return entities;
        }

        private static string ConvertToPropertyName(string columnName)
        {
            string[] parts = columnName.Split('_');
            for (int i = 0; i < parts.Length; i++)
            {
                parts[i] = char.ToUpper(parts[i][0]) + parts[i].Substring(1);
            }
            return string.Join("", parts);
        }

       
    }
}
