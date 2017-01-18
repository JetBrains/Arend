package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelBinding;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class DefCallExpression extends Expression {
  private final Definition myDefinition;
  private LevelArguments myPolyArguments;

  public DefCallExpression(Definition definition, LevelArguments polyParams) {
    myDefinition = definition;
    if (polyParams == null) {
      List<Level> args = new ArrayList<>();
      for (LevelBinding param : definition.getPolyParams()) {
        args.add(new Level(param));
      }
      myPolyArguments = new LevelArguments(args);
    } else {
      myPolyArguments = polyParams;
    }
  }

  public List<? extends Expression> getDefCallArguments() {
    return Collections.emptyList();
  }

  public LevelArguments getPolyArguments() {
    return myPolyArguments;
  }

  public abstract Expression applyThis(Expression thisExpr);

  public Definition getDefinition() {
    return myDefinition;
  }

  public void setPolyParamsSubst(LevelArguments polyParams) {
    myPolyArguments = polyParams;
  }

  @Override
  public DefCallExpression toDefCall() {
    return this;
  }
}
