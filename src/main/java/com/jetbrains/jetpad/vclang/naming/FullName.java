package com.jetbrains.jetpad.vclang.naming;

import java.util.LinkedList;
import java.util.List;

public class FullName {
  private final FullName myParent;
  private final String myName;

  public FullName(String name) {
    this(null, name);
  }

  public FullName(FullName parent, String name) {
    myParent = parent;
    myName = name;
  }

  public List<String> toList() {
    LinkedList<String> res = new LinkedList<>();
    toList(res);
    return res;
  }

  private void toList(LinkedList<String> res) {
    if (myParent != null) {
      myParent.toList(res);
    }
    res.add(myName);
  }

  @Override
  public String toString() {
    return String.join(".", toList());
  }
}
