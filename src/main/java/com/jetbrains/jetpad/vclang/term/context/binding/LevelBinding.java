package com.jetbrains.jetpad.vclang.term.context.binding;

import java.util.ArrayList;
import java.util.List;

public class LevelBinding implements LevelVariable {
  private final String myName;
  private final LvlType myType;

  public LevelBinding(String name, LvlType type) {
    myName = name;
    myType = type;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public LvlType getType() {
    return myType;
  }

  public static List<Integer> getSublistOfUserBindings(List<LevelBinding> bindings) {
    List<Integer> userBindings = new ArrayList<>();
    for (int i = 0; i < bindings.size(); ++i) {
      if (!bindings.get(i).getName().startsWith("\\")) {
        userBindings.add(i);
      }
    }
    return userBindings;
  }
}
