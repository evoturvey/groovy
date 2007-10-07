package org.codehaus.groovy.reflection;

public class SingleKeyHashMap extends ComplexKeyHashMap
{
    public static class Entry extends ComplexKeyHashMap.Entry{
    public Object key;
    public Object value;

      public Object getKey() {
          return key;
      }

      public Object getValue() {
          return value;
      }

      public void setValue(Object value) {
          this.value = value;
      }
  }

    public SingleKeyHashMap () {
        super ();
    }

    public SingleKeyHashMap (boolean b) {
        super (false);
    }

    public boolean containsKey(String name) {
        return get(name) != null;
    }

    public void put(Object key, Object value) {
        ((Entry)getOrPut(key)).value = value;
    }

  public final Object get(Object key) {
    int h = hash (key.hashCode());
    ComplexKeyHashMap.Entry e = table [h & (table.length-1)];
    for (; e != null; e = e.next)
        if (e.hash == h && ((Entry) e).key.equals(key))
          return ((Entry)e).value;

    return null;
  }

  public ComplexKeyHashMap.Entry getOrPut(Object key)
  {
    int h = hash (key.hashCode());
      final ComplexKeyHashMap.Entry[] t = table;
      final int index = h & (t.length - 1);
    ComplexKeyHashMap.Entry e = t[index];
    for (; e != null; e = e.next)
        if (e.hash == h && ((Entry) e).key.equals(key))
          return e;

      Entry entry = new Entry();
      entry.next = t [index];
      entry.hash = h;
      entry.key = key;
      t[index] = entry;

    if ( ++size == threshold )
      resize(2* t.length);

    return entry;
  }

    public Entry putCopyOfUnexisting(Entry ee)
    {
      int h = ee.hash;
      final ComplexKeyHashMap.Entry[] t = table;
      final int index = h & (t.length - 1);

        Entry entry = new Entry();
        entry.next = t [index];
        entry.hash = h;
        entry.key = ee.key;
        entry.value = ee.value;
        t[index] = entry;

      if ( ++size == threshold )
        resize(2* t.length);

      return entry;
    }

    public final ComplexKeyHashMap.Entry remove(Object key) {
    int h = hash (key.hashCode());
    int index = h & (table.length -1);
    for (ComplexKeyHashMap.Entry e = table [index], prev = null; e != null; prev = e, e = e.next ) {
        if (e.hash == h && ((Entry) e).key.equals(key)) {
        if (prev == null)
          table [index] = e.next;
        else
          prev.next = e.next;
        size--;

        e.next = null;
        return e;
      }
    }

    return null;
  }

  public static SingleKeyHashMap copy (SingleKeyHashMap dst, SingleKeyHashMap src, Copier copier) {
      dst.threshold = src.threshold;
      dst.size = src.size;
      final int len = src.table.length;
      final ComplexKeyHashMap.Entry[] t = new ComplexKeyHashMap.Entry[len], tt = src.table;
      for (int i = 0; i != len; ++i) {
          for (Entry e = (Entry) tt[i]; e != null; e = (Entry) e.next) {
              Entry ee = new Entry();
              ee.hash = e.hash;
              ee.key = e.key;
              ee.value = copier.copy(e.value);
              ee.next = t [i];
              t [i] = ee;
          }
      }
      dst.table = t;
      return dst;
  }

  public static interface Copier {
      Object copy (Object value);
  }
}