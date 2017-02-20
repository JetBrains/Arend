package com.jetbrains.jetpad.vclang.core.sort;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelBinding;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.definition.Referable;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.HashMap;
import java.util.Map;

public class LevelArguments {
  private final Level myPLevel;
  private final Level myHLevel;

  public static final LevelArguments STD = new LevelArguments(new Level(LevelBinding.PLVL_BND), new Level(LevelBinding.HLVL_BND));
  public static final LevelArguments ZERO = new LevelArguments(new Level(0), new Level(0));

  public LevelArguments(Level pLevel, Level hLevel) {
    myPLevel = pLevel;
    myHLevel = hLevel;
  }

  public Level getPLevel()  {
    return myPLevel;
  }

  public Level getHLevel()  {
    return myHLevel;
  }

  public LevelSubstitution toLevelSubstitution() {
    Map<Referable, Level> polySubst = new HashMap<>();
    polySubst.put(LevelBinding.PLVL_BND, myPLevel);
    polySubst.put(LevelBinding.HLVL_BND, myHLevel);
    return new LevelSubstitution(polySubst);
  }

  public LevelArguments subst(LevelSubstitution subst) {
    return new LevelArguments(myPLevel.subst(subst), myHLevel.subst(subst));
  }

  public static LevelArguments generateInferVars(Equations equations, Abstract.Expression expr) {
    InferenceLevelVariable pl = new InferenceLevelVariable(LevelBinding.PLVL_BND, expr);
    InferenceLevelVariable hl = new InferenceLevelVariable(LevelBinding.HLVL_BND, expr);
    equations.addVariable(pl);
    equations.addVariable(hl);
    return new LevelArguments(new Level(pl), new Level(hl));
  }
}
