using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Ai.config
{
    /// <summary>
    /// 实现线程安全的list
    /// </summary>
    /// <typeparam name="T"></typeparam>
    public class ThreadSafeList<T>
    {
        private volatile List<T> _list = new List<T>();
        private readonly object _lock = new object();

        public void Add(T item)
        {
            lock (_lock)
            {
                if (!_list.Contains(item))
                {
                    _list.Add(item);
                }
            }
        }

        public void AddAll(List<T> list)
        {

            lock (_lock)
            {
                foreach (T item in list)
                {
                    Add(item);
                }
            }

        }

        public List<T> GetAll()
        {
            lock (_lock)
            {
                return _list;
            }
        }

        public void Delete(T item)
        {
            lock ( _lock)
            {
                _list.Remove(item);
            }
        }

        public bool IsNotEmpty()
        {
            lock (_lock)
            {
                return _list != null && _list.Count > 0;
            }
        }

        public T GetOne()
        {
            lock(_lock)
            {
                if (_list.Count > 0)
                {
                    return _list[0];
                }
                return default(T);
            }
        }

        /// <summary>
        /// 更新列表中的元素
        /// </summary>
        /// <param name="predicate">用于查找要更新元素的条件</param>
        /// <param name="updateAction">更新元素的操作</param>
        /// <returns>是否找到并更新了元素</returns>
        public bool Update(Predicate<T> predicate, Action<T> updateAction)
        {
            lock (_lock)
            {
                // 查找符合条件的元素
                T item = _list.Find(predicate);
                if (item != null)
                {
                    // 执行更新操作
                    updateAction(item);
                    return true;
                }
                return false;
            }
        }

        /// <summary>
        /// 更新列表中的所有符合条件的元素
        /// </summary>
        /// <param name="predicate">用于查找要更新元素的条件</param>
        /// <param name="updateAction">更新元素的操作</param>
        /// <returns>更新的元素数量</returns>
        public int UpdateAll(Predicate<T> predicate, Action<T> updateAction)
        {
            lock (_lock)
            {
                // 查找所有符合条件的元素
                List<T> items = _list.FindAll(predicate);
                if (items.Count > 0)
                {
                    // 对每个元素执行更新操作
                    foreach (T item in items)
                    {
                        updateAction(item);
                    }
                    return items.Count;
                }
                return 0;
            }
        }

        /// <summary>
        /// 根据条件保存元素，如果条件满足则保存
        /// </summary>
        /// <param name="item">要保存的元素</param>
        /// <param name="condition">保存条件</param>
        /// <returns>是否成功保存</returns>
        public bool SaveIf(T item, Func<T, bool> condition)
        {
            lock (_lock)
            {
                if (condition(item))
                {
                    if (!_list.Contains(item))
                    {
                        _list.Add(item);
                    }
                    else
                    {
                        // 如果元素已存在，则更新它
                        int index = _list.IndexOf(item);
                        _list[index] = item;
                    }
                    return true;
                }
                return false;
            }
        }

        /// <summary>
        /// 批量保存满足条件的元素
        /// </summary>
        /// <param name="items">要保存的元素集合</param>
        /// <param name="condition">保存条件</param>
        /// <returns>成功保存的元素数量</returns>
        public int SaveAllIf(IEnumerable<T> items, Func<T, bool> condition)
        {
            int savedCount = 0;
            lock (_lock)
            {
                foreach (T item in items)
                {
                    if (condition(item))
                    {
                        if (!_list.Contains(item))
                        {
                            _list.Add(item);
                        }
                        else
                        {
                            // 如果元素已存在，则更新它
                            int index = _list.IndexOf(item);
                            _list[index] = item;
                        }
                        savedCount++;
                    }
                }
            }
            return savedCount;
        }

        /// <summary>
        /// 根据条件删除元素
        /// </summary>
        /// <param name="predicate">删除条件</param>
        /// <returns>删除的元素数量</returns>
        public int DeleteIf(Predicate<T> predicate)
        {
            lock (_lock)
            {
                return _list.RemoveAll(predicate);
            }
        }

        /// <summary>
        /// 根据条件删除第一个匹配的元素
        /// </summary>
        /// <param name="predicate">删除条件</param>
        /// <returns>是否成功删除</returns>
        public bool DeleteFirstIf(Predicate<T> predicate)
        {
            lock (_lock)
            {
                T item = _list.Find(predicate);
                if (item != null)
                {
                    return _list.Remove(item);
                }
                return false;
            }
        }
    }
}
