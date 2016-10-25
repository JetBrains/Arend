package com.jetbrains.jetpad.vclang.term.expr.subst;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.binding.Variable;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LevelSubstitution {
  private final Map<Variable, Level> myLevels;

  public LevelSubstitution() {
    myLevels = new HashMap<>();
  }

  public LevelSubstitution(Map<Variable, Level> levels) {
    myLevels = levels;
  }

  public boolean isEmpty() {
    return myLevels.isEmpty();
  }

  public Level get(Variable variable) {
    return myLevels.get(variable);
  }

  public void add(Variable variable, Level level) {
    myLevels.put(variable, level);
  }

  public static List<TypedBinding> clone(List<TypedBinding> params, LevelSubstitution subst) {
    List<TypedBinding> newParams = new ArrayList<>();

    for (Binding param : params) {
      TypedBinding newParam = new TypedBinding(param.getName(), param.getType());
      subst.add(param, new Level(newParam));
      newParams.add(newParam);
    }

    return newParams;
  }
}
