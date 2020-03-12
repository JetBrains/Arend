package org.arend.library.classLoader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MultiClassLoader<T> extends ClassLoader {
  private final Map<T, ClassLoaderDelegate> myDelegates = new LinkedHashMap<>();

  public MultiClassLoader(ClassLoader parent) {
    super(parent);
  }

  public void addDelegate(T name, ClassLoaderDelegate delegate) {
    myDelegates.put(name, delegate);
  }

  public void removeDelegate(T name) {
    myDelegates.remove(name);
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    for (ClassLoaderDelegate delegate : myDelegates.values()) {
      byte[] bytes = delegate.findClass(name);
      if (bytes != null) {
        return defineClass(name, bytes, 0, bytes.length);
      }
    }
    throw new ClassNotFoundException("Cannot find class " + name + " in any of the following locations " + new ArrayList<>(myDelegates.values()) + " or in the classpath");
  }
}
