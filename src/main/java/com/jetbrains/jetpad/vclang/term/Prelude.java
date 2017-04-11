package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.expr.AppExpression;
import com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckedReporter;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.clause;

public class Prelude {
  public static DataDefinition INTERVAL;
  public static Constructor LEFT, RIGHT, ABSTRACT;

  public static DataDefinition NAT;
  public static Constructor ZERO, SUC;

  public static FunctionDefinition COERCE;

  public static DataDefinition PATH;
  public static FunctionDefinition PATH_INFIX;
  public static Constructor PATH_CON;

  public static FunctionDefinition AT;
  public static FunctionDefinition ISO;

  public static DataDefinition PROP_TRUNC;
  public static DataDefinition SET_TRUNC;

  public static Constructor PROP_TRUNC_PATH_CON;
  public static Constructor SET_TRUNC_PATH_CON;

  private Prelude() {
  }

  public static void update(Abstract.Definition abstractDef, Definition definition) {
    if (abstractDef.getName().equals("Nat")) {
      NAT = (DataDefinition) definition;
      ZERO = NAT.getConstructor("zero");
      SUC = NAT.getConstructor("suc");
    } else
    if (abstractDef.getName().equals("I")) {
      INTERVAL = (DataDefinition) definition;
      INTERVAL.setSort(Sort.PROP);
      INTERVAL.setMatchesOnInterval();
      LEFT = INTERVAL.getConstructor("left");
      RIGHT = INTERVAL.getConstructor("right");
    } else
    if (abstractDef.getName().equals("Path")) {
      PATH = (DataDefinition) definition;
      PATH.setSort(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR, -1)));
      PATH_CON = PATH.getConstructor("path");
    } else
    if (abstractDef.getName().equals("=")) {
      PATH_INFIX = (FunctionDefinition) definition;
    } else
    if (abstractDef.getName().equals("@")) {
      AT = (FunctionDefinition) definition;
      DependentLink param4 = AT.getParameters().getNext().getNext().getNext();
      DependentLink atPath = ExpressionFactory.parameter("f", PATH_CON.getParameters().getType());
      AT.setElimTree(ExpressionFactory.top(AT.getParameters(), ExpressionFactory.branch(param4.getNext(), ExpressionFactory.tail(),
        clause(LEFT, EmptyDependentLink.getInstance(), new ReferenceExpression(AT.getParameters().getNext())),
        clause(RIGHT, EmptyDependentLink.getInstance(), new ReferenceExpression(AT.getParameters().getNext().getNext())),
        ExpressionFactory.clause(ExpressionFactory.branch(param4, ExpressionFactory.tail(param4.getNext()),
            ExpressionFactory.clause(PATH_CON, atPath, new AppExpression(new ReferenceExpression(atPath), new ReferenceExpression(param4.getNext()))))))));
      AT.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    } else
    if (abstractDef.getName().equals("coe")) {
      COERCE = (FunctionDefinition) definition;
      COERCE.setElimTree(ExpressionFactory.top(COERCE.getParameters(), ExpressionFactory.branch(COERCE.getParameters().getNext().getNext(), ExpressionFactory.tail(),
        ExpressionFactory.clause(LEFT, EmptyDependentLink.getInstance(), Abstract.Definition.Arrow.RIGHT, new ReferenceExpression(COERCE.getParameters().getNext())))));
      COERCE.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    } else
    if (abstractDef.getName().equals("iso")) {
      ISO = (FunctionDefinition) definition;
      ISO.setElimTree(ExpressionFactory.top(ISO.getParameters(), ExpressionFactory.branch(ISO.getParameters().getNext().getNext().getNext().getNext().getNext().getNext(), ExpressionFactory.tail(),
        clause(LEFT, EmptyDependentLink.getInstance(), new ReferenceExpression(ISO.getParameters())),
        clause(RIGHT, EmptyDependentLink.getInstance(), new ReferenceExpression(ISO.getParameters().getNext())))));
      ISO.setResultType(new UniverseExpression(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR))));
      ISO.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    } else
    if (abstractDef.getName().equals("TrP")) {
      PROP_TRUNC = (DataDefinition) definition;
      PROP_TRUNC.setSort(Sort.PROP);
      PROP_TRUNC_PATH_CON = PROP_TRUNC.getConstructor("truncP");
    } else
    if (abstractDef.getName().equals("TrS")) {
      SET_TRUNC = (DataDefinition) definition;
      SET_TRUNC.setSort(Sort.SetOfLevel(new Level(LevelVariable.PVAR)));
      SET_TRUNC_PATH_CON = SET_TRUNC.getConstructor("truncS");
    }
  }

  public static class UpdatePreludeReporter implements TypecheckedReporter {
    private final TypecheckerState state;

    public UpdatePreludeReporter(TypecheckerState state) {
      this.state = state;
    }

    @Override
    public void typecheckingSucceeded(Abstract.Definition definition) {
      update(definition, state.getTypechecked(definition));
    }

    @Override
    public void typecheckingFailed(Abstract.Definition definition) {
      update(definition, state.getTypechecked(definition));
    }
  }

}
