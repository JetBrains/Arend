package com.jetbrains.jetpad.vclang.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// TODO[abstract]: Merge this class with FullName
public class ModulePath {
  private final List<String> myPath;

  public ModulePath(List<String> myPath) {
    this.myPath = new ArrayList<>(myPath);
  }

  public ModulePath(String name) {
    myPath = Collections.singletonList(name);
  }

  public ModulePath(ModulePath parent, String child) {
    myPath = new ArrayList<>(parent.myPath);
    myPath.add(child);
  }

  public static ModulePath moduleName(String... module) {
    return new ModulePath(Arrays.asList(module));
  }

  public String getName() {
    return myPath.isEmpty() ? null : myPath.get(myPath.size() - 1);
  }

  public ModulePath getParent() {
    return myPath.isEmpty() ? null : new ModulePath(myPath.subList(0, myPath.size() - 1));
  }

  public String[] toArray() {
    return myPath.toArray(new String[myPath.size()]);
  }

  public List<String> toList() {
    return Collections.unmodifiableList(myPath);
  }

  @Override
  public boolean equals(Object o) {
    return this == o || o instanceof ModulePath && myPath.equals(((ModulePath) o).myPath);
  }

  @Override
  public int hashCode() {
    return myPath.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    for (String aPath : myPath) {
      result.append("::");
      result.append(aPath);
    }
    return result.toString();
  }
}
