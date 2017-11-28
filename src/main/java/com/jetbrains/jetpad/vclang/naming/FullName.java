package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.util.LongName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FullName extends LongName {
  public FullName(List<String> path) {
    super(path);
  }

  public FullName(String name) {
    super(Collections.singletonList(name));
  }

  public static FullName make(FullName parent, String name) {
    List<String> path = parent.toList();
    List<String> newPath = new ArrayList<>(path.size() + 1);
    newPath.addAll(path);
    newPath.add(name);
    return new FullName(newPath);
  }
}
