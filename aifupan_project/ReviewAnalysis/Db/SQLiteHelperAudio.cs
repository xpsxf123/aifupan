using douyin.Utils;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.Dto;
using System;
using System.Collections.Generic;
using System.Data;
using System.Data.Common;
using System.Data.SQLite;
using System.IO;
using System.Linq;
using System.Reflection;

namespace ReviewAnalysis.Db
{
    public class SQLiteHelperAudio
    {
        private string _dbName = "";
        private SQLiteConnection _SQLiteConn = null;     //连接对象
        private SQLiteTransaction _SQLiteTrans = null;   //事务对象
        private bool _IsRunTrans = false;        //事务运行标识
        private string _SQLiteConnString = null; //连接字符串
        private bool _AutoCommit = false; //事务自动提交标识
        private string _dbFilePath = "DbFile";

        public string SQLiteConnString { get; }

        public SQLiteHelperAudio()
        {
            _dbName = "review_analysis_audio.db";
            _SQLiteConnString = "data source=" + _dbFilePath + "\\" + _dbName;
        }

        /// <summary>
        /// 新建数据库文件
        /// </summary>
        /// <returns>新建成功，返回true，否则返回false</returns>
        public Boolean createDbFile()
        {
            string dbFile = "";
            try
            {
                if (FileUtils.createDirectory(_dbFilePath))
                {

                    if (!FileUtils.isExitsFile(_dbFilePath + "\\" + _dbName, out dbFile))
                    {
                        SQLiteConnection.CreateFile(dbFile);
                        string sqlScriptPath = dbFile.Replace("review_analysis_audio.db", "init_audio.sql");
                        string script = File.ReadAllText(sqlScriptPath);
                        this.initDbTable(script);
                    }
                }

                return true;
            }
            catch (Exception ex)
            {
                throw new Exception("新建数据库文件" + dbFile + "失败：" + ex.Message);
            }
        }

        /// <summary>
        /// 打开当前数据库的连接
        /// </summary>
        /// <returns></returns>
        public Boolean OpenDb()
        {
            try
            {
                this._SQLiteConn = new SQLiteConnection(this._SQLiteConnString);
                this._SQLiteConn.Open();
                return true;
            }
            catch (Exception ex)
            {
                throw new Exception("打开数据库：" + _dbName + "的连接失败：" + ex.Message);
            }
        }

        /// <summary>
        /// 执行一个查询语句，返回一个包含查询结果的DataTable。 
        /// </summary>
        /// <param name="sql"></param>
        /// <param name="parameters"></param>
        /// <returns></returns>
        public DataTable ExecuteQuery(string sql, params SQLiteParameter[] parameters)
        {
            string absolutePath = Path.GetFullPath(this._SQLiteConnString);
            using (SQLiteConnection connection = new SQLiteConnection(this._SQLiteConnString))
            {
                connection.Open();
                using (SQLiteCommand command = new SQLiteCommand(sql, connection))
                {
                    if (parameters!=null&&parameters.Length != 0)
                    {
                        command.Parameters.AddRange(parameters);
                    }
                    SQLiteDataAdapter adapter = new SQLiteDataAdapter(command);
                    DataTable data = new DataTable();
                    try
                    {
                        adapter.Fill(data);
                    }
                    catch (Exception)
                    {
                        throw;
                    }
                    return data;
                }
            }
        }

        /// <summary>
        /// 通过数据表，获取数据表的所有字段
        /// </summary>
        /// <param name="tableName"></param>
        /// <returns></returns>
        public List<string> GetColumnNames(string tableName)
        {
            List<string> columnNames = new List<string>();
            using (SQLiteConnection connection = new SQLiteConnection(this._SQLiteConnString))
            {
                connection.Open();
                using (SQLiteCommand command = new SQLiteCommand($"PRAGMA table_info({tableName});", connection))
                {
                    using (SQLiteDataReader reader = command.ExecuteReader())
                    {
                        while (reader.Read())
                        {
                            string originalName = reader["name"].ToString();
                            columnNames.Add(originalName);
                        }
                    }
                }
            }
            return columnNames;
        }

        /// <summary>
        /// 执行查询语句，并返回第一个结果。
        /// </summary>
        /// <param name="sql"></param>
        /// <param name="parameters"></param>
        /// <returns></returns>
        public object ExecuteScalar(string sql, params SQLiteParameter[] parameters)
        {
            using (SQLiteConnection conn = new SQLiteConnection(this._SQLiteConnString))
            {
                using (SQLiteCommand cmd = new SQLiteCommand(conn))
                {
                    try
                    {
                        conn.Open();
                        cmd.CommandText = sql;
                        if (parameters!=null&&parameters.Length != 0)
                        {
                            cmd.Parameters.AddRange(parameters);
                        }
                        return cmd.ExecuteScalar();
                    }
                    catch (Exception) { throw; }
                }
            }
        }


        /// <summary>
        /// 不带参数，SQL执行语句
        /// update、delete、insert
        /// </summary>
        /// <param name="a_Sql">SQL</param>
        /// <returns></returns>
        public int Execute(string a_Sql)
        {
            SQLiteConnection connection = new SQLiteConnection(this._SQLiteConnString);
            SQLiteCommand cmd = new SQLiteCommand(connection);
            try
            {
                connection.Open();
                cmd.CommandText = a_Sql;
                return cmd.ExecuteNonQuery();
            }
            catch (Exception ex)
            {
                FileUtils.log(ex.Message);
                return 0;
            }
            finally
            {
                cmd.Dispose();
                connection.Close();
            }
        }

        /// <summary>
        /// 带参数，执行脚本
        /// insert,update,delete
        /// </summary>
        /// <param name="sql">sql</param>
        /// <param name="parameters">可变参数，目的是省略了手动构造数组的过程，直接指定对象，编译器会帮助我们构造数组，并将对象加入数组中，传递过来</param>
        /// <returns></returns>
        public int ExecuteNonQuery(string sql, params SQLiteParameter[] parameters)
        {
            using (SQLiteConnection connection = new SQLiteConnection(this._SQLiteConnString))
            {
                using (SQLiteCommand command = new SQLiteCommand(connection))
                {
                    try
                    {
                        connection.Open();
                        command.CommandText = sql;
                        if (parameters!=null&&parameters.Length > 0)
                        {
                            command.Parameters.AddRange(parameters);
                        }
                        return command.ExecuteNonQuery();
                    }
                    catch (Exception e) { throw new Exception(e.Message.ToString()); }
                }
            }
        }

       

        /// <summary>
        /// 带事务回滚批量执行脚本
        /// </summary>
        /// <param name="a_listSqls">SQL脚本</param>
        /// <returns></returns>
        public int ExecuteNonQuery(List<string> a_listSqls)
        {
            SQLiteConnection connection = new SQLiteConnection(this.SQLiteConnString);
            SQLiteCommand cmd = new SQLiteCommand(connection);
            DbTransaction trans = connection.BeginTransaction();
            try
            {
                try
                {
                    foreach (string sql in a_listSqls)
                    {
                        cmd.CommandText = sql;
                        cmd.ExecuteNonQuery();
                    }
                    trans.Commit();
                }
                catch
                {
                    trans.Rollback();//回滚
                }

                return 1;
            }
            catch (Exception e)
            {
                throw new Exception(e.Message.ToString());
              
            }
            finally
            {
                trans.Dispose();
                cmd.Dispose();
                connection.Close();
            }
        }

        /// <summary>
        /// 执行sql脚本
        /// </summary>
        /// <param name="script"></param>
        public void initDbTable(string script)
        {
            string[] commands = script.Split(new[] { ';' }, StringSplitOptions.RemoveEmptyEntries);

            using (SQLiteConnection connection = new SQLiteConnection(this._SQLiteConnString))
            {
                connection.Open();
                using (SQLiteTransaction transaction = connection.BeginTransaction())
                using (SQLiteCommand command = connection.CreateCommand())
                {
                    command.Transaction = transaction;
                    try
                    {
                        foreach (string commandText in commands)
                        {
                            if (!string.IsNullOrWhiteSpace(commandText))
                            {
                                command.CommandText = commandText.Trim();
                                command.ExecuteNonQuery();
                            }
                        }
                        transaction.Commit();
                        FileUtils.log("SQL script executed successfully.");
                    }
                    catch (Exception ex)
                    {
                        transaction.Rollback();
                        FileUtils.log("An error occurred while executing the SQL script.");
                        FileUtils.log(ex.Message);
                        throw new Exception(ex.Message.ToString());
                    }
                }
            }
        }

      

        /// <summary>
        /// 将dataTable转换成为指定的实体
        /// </summary>
        /// <typeparam name="T">要转换的目标实体</typeparam>
        /// <param name="dataTable">数据表</param>
        /// <returns></returns>
        public List<T> ConvertToEntityList<T>(DataTable dataTable) where T : class,new()
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
                               // string valueStr = value.ToString().Trim();
                                property.SetValue(entity, Convert.ChangeType(value, property.PropertyType), null);
                            }
                        }
                    }
                }
                entities.Add(entity);
            }

            return entities;
        }

        /// <summary>
        /// 创建查询条件
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="instance">目标实体</param>
        /// <param name="whereClause">条件字符串</param>
        /// <param name="parameters">条件参数</param>
        public void GenerateSqlAndParameters<T>(T instance, out string whereClause, out SQLiteParameter[] parameters)
        {
            List<SQLiteParameter> parameterList = new List<SQLiteParameter>();
            List<string> conditions = new List<string>();

            PropertyInfo[] properties = typeof(T).GetProperties(BindingFlags.Public | BindingFlags.Instance);

            foreach (PropertyInfo property in properties)
            {
                object value = property.GetValue(instance);

                if (value != null &&
                    !(value is string stringValue && string.IsNullOrEmpty(stringValue)) &&
                    !(value is int intValue && intValue == 0))
                {
                    string paramName = $"@{property.Name}";
                    parameterList.Add(new SQLiteParameter(paramName, value));
                    conditions.Add($"{ConvertToColumnName(property.Name)} = {paramName}");
                }
            }

            whereClause = conditions.Count > 0 ? string.Join(" AND ", conditions) : "1=1";
            parameters = parameterList.ToArray();
        }

        /// <summary>
        /// 将字段名称aa_bb_bc 转义成为 AaBbCc
        /// </summary>
        /// <param name="columnName">要转义的数据字段名称</param>
        /// <returns></returns>
        private string ConvertToPropertyName(string columnName)
        {
            string[] parts = columnName.Split('_');
            for (int i = 0; i < parts.Length; i++)
            {
                parts[i] = char.ToUpper(parts[i][0]) + parts[i].Substring(1);
            }
            return string.Join("", parts);
        }

        /// <summary>
        /// 将属性名称转义成为字段名称 XxYyZz 转义成为xx_yy_zz
        /// </summary>
        /// <param name="propertyName">要转义的属性名称</param>
        /// <returns></returns>
        private string ConvertToColumnName(string propertyName)
        {
            if (string.IsNullOrEmpty(propertyName))
            {
                return propertyName;
            }

            var sb = new System.Text.StringBuilder();
            foreach (char c in propertyName)
            {
                if (char.IsUpper(c))
                {
                    if (sb.Length > 0)
                    {
                        sb.Append('_');
                    }
                    sb.Append(char.ToLower(c));
                }
                else
                {
                    sb.Append(c);
                }
            }
            return sb.ToString();
        }


        /// <summary>
        /// 执行添加
        /// </summary>
        /// <param name="tableName">数据表的名字</param>
        /// <param name="parameters">要插入的列的数组（插入的是大写开头的驼峰命名，要将他转换成对应的数据库字段即 YyXxZz 要转成 yy_xx_zz）</param>
        /// <returns></returns>
        public int Insert(string tableName, SQLiteParameter[] parameters)
        {
            //1.通过参数转换成列名
            var columns = parameters.Select(p => ConvertToColumnName(p.ParameterName.TrimStart('@'))).ToArray();
           
            //2.要插入的值
            var values = parameters.Select(p => $"@{p.ParameterName.TrimStart('@')}").ToArray();
            string sql = $"INSERT INTO {tableName} ({string.Join(", ", columns)}) VALUES ({string.Join(", ", values)});";
            //FileUtils.log($"sql：{sql}", "Insert插入");
            ExecuteNonQuery(sql, parameters);
            return Convert.ToInt32(ExecuteScalar("SELECT last_insert_rowid();"));
        }

        /// <summary>
        /// 更新数据
        /// </summary>
        /// <param name="tableName">需要更新的数据表</param>
        /// <param name="updateParameters">更新的字段</param>
        /// <param name="whereParameters">更新的条件</param>
        /// <returns></returns>
        public int Update(string tableName, SQLiteParameter[] updateParameters, SQLiteParameter[] whereParameters)
        {
            var setClause = string.Join(", ", updateParameters.Select(p => $"{ConvertToColumnName(p.ParameterName.TrimStart('@'))} = @{p.ParameterName.TrimStart('@')}"));
            if (whereParameters != null && whereParameters.Length > 0)
            {
                var whereClause = string.Join(" AND ", whereParameters.Select(p => $"{ConvertToColumnName(p.ParameterName.TrimStart('@'))} = @{p.ParameterName.TrimStart('@')}"));

                string sql = $"UPDATE {tableName} SET {setClause} WHERE {whereClause};";
                var allParameters = updateParameters.Concat(whereParameters).ToArray();
                return ExecuteNonQuery(sql, allParameters);
            }
            else 
            {
                string sql = $"UPDATE {tableName} SET {setClause};";
                return ExecuteNonQuery(sql, updateParameters);
            }
        }

        /// <summary>
        /// 根据条件删除数据
        /// </summary>
        /// <param name="tableName">要删除的数据表名称</param>
        /// <param name="whereParameters">删除条件</param>
        /// <returns></returns>
        public int Delete(string tableName, SQLiteParameter[] whereParameters=null)
        {
            if (whereParameters != null && whereParameters.Length > 0)
            {
                var whereClause = string.Join(" ", whereParameters.Select(p => $"{ConvertToColumnName(p.ParameterName.TrimStart('@'))} = @{p.ParameterName.TrimStart('@')}"));
                if (whereParameters.Length > 1)
                {
                    whereClause = string.Join(" AND ", whereParameters.Select(p => $"{ConvertToColumnName(p.ParameterName.TrimStart('@'))} = @{p.ParameterName.TrimStart('@')}"));
                   
                }
                string sql = $"DELETE FROM {tableName} WHERE {whereClause};";
                return ExecuteNonQuery(sql, whereParameters);
            }
            else 
            {
                string sql = $"DELETE FROM {tableName}";
                return ExecuteNonQuery(sql, null);
            }
        }

        /// <summary>
        /// 根据条件获取dataTable
        /// </summary>
        /// <param name="tableName"></param>
        /// <param name="whereClause"></param>
        /// <param name="whereParameters"></param>
        /// <returns></returns>
        public DataTable Select(string tableName, string whereClause = null, SQLiteParameter[] whereParameters = null)
        {
            string sql = $"SELECT * FROM {tableName}" + (whereClause != null ? $" WHERE {whereClause}" : "") + " order by id desc;";
            return ExecuteQuery(sql, whereParameters);
        }

        /// <summary>
        /// 根据条件获取dataTable(分析时间倒序排)
        /// </summary>
        /// <param name="tableName"></param>
        /// <param name="whereClause"></param>
        /// <param name="whereParameters"></param>
        /// <returns></returns>
        public DataTable SelectOrderByTime(string tableName, string whereClause = null, SQLiteParameter[] whereParameters = null)
        {
            string sql = $"SELECT * FROM {tableName}" + (whereClause != null ? $" WHERE {whereClause}" : "") + " order by analysis_time desc;";
            return ExecuteQuery(sql, whereParameters);
        }

        /// <summary>
        /// 根据条件查询 数据总数
        /// </summary>
        /// <param name="tableName"></param>
        /// <param name="field"></param>
        /// <param name="whereParameters"></param>
        /// <returns></returns>
        public object GetCountFunction(string tableName, string field=null, SQLiteParameter[] whereParameters=null)
        {
            string sql = $"select count(*) from {tableName}";
            if (string.IsNullOrEmpty(field)) 
            {
                sql = $"select count({field}) from {tableName}";
            }
            if (whereParameters != null && whereParameters.Length > 0) 
            {
                string whereClause = whereParameters.Length == 1? $"{ConvertToColumnName(whereParameters[0].ParameterName.TrimStart('@'))} = @{whereParameters[0].ParameterName.TrimStart('@')}"
                :string.Join(" AND ", whereParameters.Select(p => $"{ConvertToColumnName(p.ParameterName.TrimStart('@'))} = @{p.ParameterName.TrimStart('@')}"));
                sql += " where " + whereClause;
            }
            object result = ExecuteScalar(sql, whereParameters);
            return result;
        }

        /// <summary>
        /// 根据条件查询 数据总数
        /// </summary>
        /// <param name="tableName"></param>
        /// <param name="field"></param>
        /// <param name="whereParameters"></param>
        /// <returns></returns>
        public object GetMaxFunction(string tableName, string field , SQLiteParameter[] whereParameters = null)
        {
            string sql = $"select Max({field}) from {tableName}";
           
            if (whereParameters != null && whereParameters.Length > 0)
            {
                string whereClause = whereParameters.Length == 1 ? $"{ConvertToColumnName(whereParameters[0].ParameterName.TrimStart('@'))} = @{whereParameters[0].ParameterName.TrimStart('@')}"
                : string.Join(" AND ", whereParameters.Select(p => $"{ConvertToColumnName(p.ParameterName.TrimStart('@'))} = @{p.ParameterName.TrimStart('@')}"));
                sql += " where " + whereClause;
            }
            object result = ExecuteScalar(sql, whereParameters);
            return result;
        }

        /// <summary>
        /// 根据条件获取实体列表
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="tableName"></param>
        /// <param name="whereClause"></param>
        /// <param name="whereParameters"></param>
        /// <returns></returns>
        public List<T> GetModelList<T>(string tableName, string whereClause = null, SQLiteParameter[] whereParameters = null) where T : class,new()
        {
           DataTable dt= Select(tableName, whereClause, whereParameters);
           return ConvertToEntityList<T>(dt);
        }

        /// <summary>
        /// 根据条件获取实体列表(分析时间倒序排)
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="tableName"></param>
        /// <param name="whereClause"></param>
        /// <param name="whereParameters"></param>
        /// <returns></returns>
        public List<T> GetModelListOrderByTime<T>(string tableName, string whereClause = null, SQLiteParameter[] whereParameters = null) where T : class, new()
        {
            DataTable dt = SelectOrderByTime(tableName, whereClause, whereParameters);
            return ConvertToEntityList<T>(dt);
        }


        public List<T> GetModelListOrder<T>(string tableName, string[] orderFile,int orderType=0, string whereClause = null, SQLiteParameter[] whereParameters = null) where T : class, new()
        {
            string orderbyStr = "";
            for (int i = 0; i < orderFile.Length; i++) 
            {
                if (i > 0) 
                {
                    orderbyStr += ",";
                }
                orderbyStr += orderFile[i];
                if (orderType == 0)
                {
                    orderbyStr += " desc";
                }
            }
            string sql = $"SELECT * FROM {tableName}" + (whereClause != null ? $" WHERE {whereClause}" : "") + (orderbyStr!=""?" order by "+orderbyStr:"");
            DataTable dt= ExecuteQuery(sql, whereParameters);
            return ConvertToEntityList<T>(dt);
        }

        /// <summary>
        /// 将实体的属性赋值给指定的实例（相同类型的实例）
        /// </summary>
        /// <typeparam name="T">目标类型</typeparam>
        /// <param name="target">需要赋值的实例</param>
        /// <param name="source">提供数据的实例</param>
        /// <exception cref="ArgumentNullException"></exception>
        public void AssignPropertiesFrom<T>(T target, T source)
        {
            if (target == null || source == null) throw new ArgumentNullException();

            var properties = typeof(T).GetProperties(BindingFlags.Public | BindingFlags.Instance)
                                      .Where(p => p.CanWrite && p.CanRead);

            foreach (var property in properties)
            {
                var value = property.GetValue(source);
                property.SetValue(target, value);
            }
        }

        /// <summary>
        /// 查询分页列表
        /// </summary>
        /// <param name="tableName">要查询的数据表</param>
        /// <param name="pageSize">分页大小</param>
        /// <param name="pageNumber">页码</param>
        /// <param name="parameters">查询参数</param>
        public PageDto<T> ExecutePagedQuery<T>(string TableName, int pageSize, int pageNumber, string whereClause = null, SQLiteParameter[] whereParameters = null) where T:class,new()
        {
            string sql = $"SELECT * FROM {TableName}" + (whereClause != null ? $" WHERE {whereClause}" : "") + $" order by id desc LIMIT {pageSize} OFFSET {(pageNumber-1)*pageSize}";
            DataTable dt= ExecuteQuery(sql, whereParameters);
            List<T> list = ConvertToEntityList<T>(dt);
            string sqlTotal = $"select count(*) from {TableName} "+ (whereClause != null ? $" WHERE {whereClause}" : "");
            int total = Convert.ToInt32(ExecuteScalar(sqlTotal, whereParameters));
            int pageTotal = 1;
            if (total > pageSize) 
            {
                pageTotal = total / pageSize;
                if (total % pageSize > 0) 
                {
                    pageSize += 1;
                }
            }
            PageDto<T> pageDto = new PageDto<T>();
            pageDto.PageTotal = pageTotal;
            pageDto.DataList = list;
            pageDto.Total = total;
            return pageDto;
        }

        /// <summary>
        /// 查询分页列表(根据分析时间倒序)
        /// </summary>
        /// <param name="tableName">要查询的数据表</param>
        /// <param name="pageSize">分页大小</param>
        /// <param name="pageNumber">页码</param>
        /// <param name="parameters">查询参数</param>
        public PageDto<T> ExecutePagedQueryOrderByTime<T>(string TableName, int pageSize, int pageNumber, string whereClause = null, SQLiteParameter[] whereParameters = null) where T : class, new()
        {
            string sql = $"SELECT * FROM {TableName}" + (whereClause != null ? $" WHERE {whereClause}" : "") + $" order by analysis_time desc LIMIT {pageSize} OFFSET {(pageNumber - 1) * pageSize}";
            DataTable dt = ExecuteQuery(sql, whereParameters);
            List<T> list = ConvertToEntityList<T>(dt);
            string sqlTotal = $"select count(*) from {TableName} " + (whereClause != null ? $" WHERE {whereClause}" : "");
            int total = Convert.ToInt32(ExecuteScalar(sqlTotal, whereParameters));
            int pageTotal = 1;
            if (total > pageSize)
            {
                pageTotal = total / pageSize;
                if (total % pageSize > 0)
                {
                    pageSize += 1;
                }
            }
            PageDto<T> pageDto = new PageDto<T>();
            pageDto.PageTotal = pageTotal;
            pageDto.DataList = list;
            pageDto.Total = total;
            return pageDto;
        }

        /// <summary>
        /// 根据Id获取实体
        /// </summary>
        /// <typeparam name="T">需要获取的实体</typeparam>
        /// <param name="TableName">需要查询的数据表</param>
        /// <param name="Id">主键</param>
        /// <returns></returns>
        public T GetModelById<T>(string TableName, int Id) where T:class,new()
        {
            string sql = $"select * from {TableName} where id=@Id";
            SQLiteParameter[] whereParamters = new SQLiteParameter[1];
            whereParamters[0] =  new SQLiteParameter("@Id", Id);
            DataTable dt = ExecuteQuery(sql, whereParamters);
            if (dt != null && dt.Rows.Count > 0) 
            {
                List<T> list = ConvertToEntityList<T>(dt);
                return list[0];
            }
            return null;
        }

        public void GetModelById<T>(T tager, string TableName, int Id) where T : class, new()
        {
            T source = GetModelById<T>(TableName, Id);
            AssignPropertiesFrom<T>(tager, source);
        }

        /// <summary>
        /// 查询最新的记录id
        /// </summary>
        /// <param name="tableName">表名</param>
        /// <returns></returns>
        /// <exception cref="NotImplementedException"></exception>
        public int SelectLastId(string tableName)
        {
            string sql = "SELECT * FROM " + tableName + " ORDER BY id DESC LIMIT 1;";
            DataTable dt = ExecuteQuery(sql, null);
            foreach (DataRow row in dt.Rows)
            {

                object v = row["id"];
                object v1 = Convert.ChangeType(v, TypeCode.Int32);
                return Convert.ToInt32(v1);
            }
            return 0;
        }
    }
}
