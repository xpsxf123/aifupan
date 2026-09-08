using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Ai
{
    public class ThreadSafeSortedDictionary<TKey, TValue> where TKey : IComparable<TKey>
    {
        private readonly SortedDictionary<TKey, TValue> sortedDictionary = new SortedDictionary<TKey, TValue>();
        private readonly object lockObject = new object();

        public void Add(TKey key, TValue value)
        {
            lock (lockObject)
            {
                sortedDictionary.Add(key, value);
            }
        }

        public bool TryGetValue(TKey key, out TValue value)
        {
            lock (lockObject)
            {
                return sortedDictionary.TryGetValue(key, out value);
            }
        }

        public IEnumerable<KeyValuePair<TKey, TValue>> GetAll()
        {
            lock (lockObject)
            {
                return new List<KeyValuePair<TKey, TValue>>(sortedDictionary);
            }
        }
    }
}
