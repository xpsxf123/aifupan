using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using douyin.Utils;
using ReviewAnalysis.api;
using ReviewAnalysis.vo.system;

namespace ReviewAnalysis.Utils
{
    public class KvHelper
    {

        // 存储服务器kv的值，使用锁确保线程安全
        private static readonly object _lock = new object();
        public static volatile Dictionary<string, string> dictValues = new Dictionary<string, string>();

        /// <summary>
        /// 通过key获取value
        /// 1、查询dictValues中有对应的key，有就返回
        /// 2、没有就查询服务器 SystemApi.getSystenKvByKey(key)，再把服务器返回的值设置到dictValues中
        /// </summary>
        /// <param name="key"></param>
        /// <returns></returns>
        public static string GetKvByKey(string key)
        {
            // 检查key是否为空
            if (string.IsNullOrEmpty(key))
            {
                throw new CustomException("Key不能为空");
            }

            // 先从本地缓存查询
            if (dictValues.TryGetValue(key, out string value))
            {
                return value;
            }

            // 本地缓存没有，查询服务器
            SystemKvVo kvVo = null;
            try
            {

                kvVo = SystemApi.getSystenKvByKey(key);
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"key = {key}, Message = {ex.Message}, StackTrace = {ex.StackTrace}", "获取字典报错");
            }

            // 检查服务器返回结果
            if (kvVo == null)
            {
                return null;
            }

            // 确保返回的kvKey与请求的key一致
            if (!string.Equals(kvVo.kvKey, key, StringComparison.Ordinal))
            {
                return null;
            }

            // 将结果存入本地缓存（线程安全处理）
            lock (_lock)
            {
                if (!dictValues.ContainsKey(key))
                {
                    dictValues[key] = kvVo.kvValue;
                }
            }

            return kvVo.kvValue;
        }

        /// <summary>
        /// 泛型方法：获取指定类型的Kv值
        /// </summary>
        /// <typeparam name="T">目标类型</typeparam>
        /// <param name="key">键</param>
        /// <returns>转换后的类型值</returns>
        public static T GetKvByKey<T>(string key)
        {
            return GetKvByKey<T>(key, default(T));
        }

        /// <summary>
        /// 泛型方法：获取指定类型的Kv值，带默认值
        /// </summary>
        /// <typeparam name="T">目标类型</typeparam>
        /// <param name="key">键</param>
        /// <param name="defaultValue">转换失败时的默认值</param>
        /// <returns>转换后的类型值</returns>
        public static T GetKvByKey<T>(string key, T defaultValue)
        {
            string value = GetKvByKey(key);

            // 如果获取到的值为空，返回默认值
            if (string.IsNullOrEmpty(value))
            {
                return defaultValue;
            }

            try
            {
                // 处理可空类型
                Type targetType = typeof(T);
                if (targetType.IsGenericType && targetType.GetGenericTypeDefinition() == typeof(Nullable<>))
                {
                    targetType = Nullable.GetUnderlyingType(targetType);
                }

                // 进行类型转换
                return (T)Convert.ChangeType(value, targetType);
            }
            catch (Exception ex)
            {
                // 转换失败时返回默认值，可以根据需要记录日志
                // FileUtils.log($"转换Kv值失败，Key: {key}, Value: {value}, 目标类型: {typeof(T)}, 错误: {ex.Message}");
                return defaultValue;
            }
        }

        // 以下是特定类型的辅助方法（如果需要）
        public static int GetIntKvByKey(string key)
        {
            return GetKvByKey<int>(key);
        }

        public static int GetIntKvByKey(string key, int defaultValue)
        {
            return GetKvByKey<int>(key, defaultValue);
        }

        public static double GetDoubleKvByKey(string key)
        {
            return GetKvByKey<double>(key);
        }

        public static double GetDoubleKvByKey(string key, double defaultValue)
        {
            return GetKvByKey<double>(key, defaultValue);
        }

        public static decimal GetDecimalKvByKey(string key)
        {
            return GetKvByKey<decimal>(key);
        }

        public static decimal GetDecimalKvByKey(string key, decimal defaultValue)
        {
            return GetKvByKey<decimal>(key, defaultValue);
        }

    }
}
