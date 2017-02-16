package com.jetbrains.jetpad.vclang.core.sort;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.definition.Referable;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelBinding;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.*;

public class LevelArguments {
  private final List<Level> myLevels;

  public LevelArguments() {
    // myLevels = Collections.emptyList();
    this (0, 0);
  }

  public LevelArguments(int plevel, int hlevel) {
    myLevels = Arrays.asList(new Level(plevel), new Level(hlevel));
  }

  public LevelArguments(List<Level> levels) {
    myLevels = levels;
  }

  public boolean isEmpty() {
    return myLevels.isEmpty();
  }

  public List<? extends Level> getLevels()  {
    return myLevels;
  }

  public LevelSubstitution toLevelSubstitution(Definition definition) {
    assert definition.getPolyParams().size() == myLevels.size();
    Map<Referable, Level> polySubst = new HashMap<>();
    for (int i = 0; i < myLevels.size(); i++) {
      polySubst.put(definition.getPolyParams().get(i), myLevels.get(i));
    }
    return new LevelSubstitution(polySubst);
  }

  public LevelArguments subst(LevelSubstitution subst) {
    if (subst.isEmpty()) {
      return this;
    }

    List<Level> levels = new ArrayList<>(myLevels);
    for (int i = 0; i < levels.size(); i++) {
      levels.set(i, levels.get(i).subst(subst));
    }
    return new LevelArguments(levels);
  }

  public static LevelArguments generateInferVars(List<LevelBinding> polyParams, Equations equations, Abstract.Expression expr) {
    List<Level> levels = new ArrayList<>(polyParams.size());
    for (LevelBinding polyVar : polyParams) {
      InferenceLevelVariable l = new InferenceLevelVariable(polyVar.getName(), polyVar.getType(), expr);
      levels.add(new Level(l));
      equations.addVariable(l);
    }
    return new LevelArguments(levels);
  }
}
