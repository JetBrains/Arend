package org.arend.library.resolver;

import org.arend.library.Library;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class SimpleLibraryResolver implements LibraryResolver {
  private final Map<String, Library> myMap = new HashMap<>();

  public Library registerLibrary(String name, Library library) {
    return myMap.putIfAbsent(name, library);
  }

  @Nullable
  @Override
  public Library resolve(String name) {
    return myMap.get(name);
  }
}
