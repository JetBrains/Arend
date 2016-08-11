package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.module.FileModuleID;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class Prelude extends SimpleNamespace {
  public static FileModuleID moduleID;

  public static SimpleNamespace PRELUDE = new Prelude();

  public static DataDefinition INTERVAL;
  public static Constructor LEFT, RIGHT, ABSTRACT;

  public static DataDefinition NAT;
  public static Constructor ZERO, SUC;

  public static DataDefinition CNAT;
  public static Constructor FIN, INF;

  public static DataDefinition LVL;

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
    if (abstractDef.getName().equals("Lvl")) {
      LVL = (DataDefinition) definition;
    } else
    if (abstractDef.getName().equals("CNat")) {
      CNAT = (DataDefinition) definition;
      INF = CNAT.getConstructor("inf");
      FIN = CNAT.getConstructor("fin");
    } else
    if (abstractDef.getName().equals("I")) {
      INTERVAL = (DataDefinition) definition;
      INTERVAL.setSorts(new SortMax(Sort.PROP));
      INTERVAL.setMatchesOnInterval();
      LEFT = INTERVAL.getConstructor("left");
      RIGHT = INTERVAL.getConstructor("right");
    } else
    if (abstractDef.getName().equals("Path")) {
      PATH = (DataDefinition) definition;
      PATH_CON = PATH.getConstructor("path");
    } else
    if (abstractDef.getName().equals("=")) {
      PATH_INFIX = (FunctionDefinition) definition;
    } else
    if (abstractDef.getName().equals("@")) {
      AT = (FunctionDefinition) definition;
      DependentLink param4 = AT.getParameters().getNext().getNext().getNext();
      DependentLink atPath = param("f", PATH_CON.getParameters().getType());
      AT.setElimTree(top(AT.getParameters(), branch(param4.getNext(), tail(),
        clause(LEFT, EmptyDependentLink.getInstance(), Reference(AT.getParameters().getNext())),
        clause(RIGHT, EmptyDependentLink.getInstance(), Reference(AT.getParameters().getNext().getNext())),
        clause(branch(param4, tail(param4.getNext()),
            clause(PATH_CON, atPath, Apps(Reference(atPath), Reference(param4.getNext()))))))));
      AT.hasErrors(false);
    } else
    if (abstractDef.getName().equals("coe")) {
      COERCE = (FunctionDefinition) definition;
      COERCE.setElimTree(top(COERCE.getParameters(), branch(COERCE.getParameters().getNext().getNext(), tail(),
        clause(LEFT, EmptyDependentLink.getInstance(), Abstract.Definition.Arrow.RIGHT, Reference(COERCE.getParameters().getNext())))));
      COERCE.hasErrors(false);
    } else
    if (abstractDef.getName().equals("iso")) {
      ISO = (FunctionDefinition) definition;
      ISO.setElimTree(top(ISO.getParameters(), branch(ISO.getParameters().getNext().getNext().getNext().getNext().getNext().getNext(), tail(),
        clause(LEFT, EmptyDependentLink.getInstance(), Reference(ISO.getParameters())),
        clause(RIGHT, EmptyDependentLink.getInstance(), Reference(ISO.getParameters().getNext())))));
      ISO.hasErrors(false);
    } else
    if (abstractDef.getName().equals("TrP")) {
      PROP_TRUNC = (DataDefinition) definition;
      PROP_TRUNC.setSorts(new SortMax(Sort.PROP));
      PROP_TRUNC_PATH_CON = PROP_TRUNC.getConstructor("truncP");
    } else
    if (abstractDef.getName().equals("TrS")) {
      SET_TRUNC = (DataDefinition) definition;
      SET_TRUNC.setSorts(new SortMax(Sort.SetOfLevel(new Level(SET_TRUNC.getPolyParams().get(0)))));
      SET_TRUNC_PATH_CON = SET_TRUNC.getConstructor("truncS");
    }
  }
}
