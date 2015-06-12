package com.jetbrains.jetpad.vclang.module;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Module {
  private final List<String> myNames;

  public Module(List<String> names) {
    if (names.size() < 1) throw new IllegalStateException();
    myNames = names;
  }

  public File getFile(File dir, String ext) {
    File result = dir;
    for (int i = 0; i < myNames.size() - 1; ++i) {
      result = new File(result, myNames.get(i));
    }
    return new File(result, myNames.get(myNames.size() - 1) + ext);
  }

  public Module getParent() {
    if (myNames.size() < 2) return null;
    List<String> parentNames = new ArrayList<>(myNames.size() - 1);
    for (int i = 0; i < myNames.size() - 1; ++i) {
      parentNames.add(myNames.get(i));
    }
    return new Module(parentNames);
  }

  public String getName() {
    return myNames.get(myNames.size() - 1);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof Module)) return false;
    List<String> otherNames = ((Module) other).myNames;
    if (myNames.size() != otherNames.size()) return false;
    for (int i = myNames.size() - 1; i >= 0; --i) {
      if (!myNames.get(i).equals(otherNames.get(i))) return false;
    }
    return true;
  }

  @Override
  public String toString() {
    if (myNames.isEmpty()) return "";
    String result = myNames.get(0);
    for (int i = 1; i < myNames.size(); ++i) {
      result += "." + myNames.get(i);
    }
    return result;
  }
}
