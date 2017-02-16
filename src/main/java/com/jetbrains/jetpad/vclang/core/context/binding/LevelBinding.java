package com.jetbrains.jetpad.vclang.core.context.binding;

import java.util.ArrayList;
import java.util.List;

public class LevelBinding implements LevelVariable {
  private final String myName;
  private final LvlType myType;

  public final static LevelBinding PLVL_BND = new LevelBinding("\\lp", LvlType.PLVL);
  public final static LevelBinding HLVL_BND = new LevelBinding("\\lh", LvlType.HLVL);

  public static LevelBinding bindingByName(String name) {
    return name.equals(PLVL_BND.getName()) ? PLVL_BND : name.equals(HLVL_BND.getName()) ? HLVL_BND : null;
  }

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

  @Override
  public String toString() {
    return myName;
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
