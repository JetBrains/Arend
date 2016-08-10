package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.module.FileModuleID;
import com.jetbrains.jetpad.vclang.naming.namespace.EmptyNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class Prelude extends SimpleNamespace {
  public static FileModuleID moduleID;

  public static ClassDefinition PRELUDE_CLASS;

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

  static {
    PRELUDE_CLASS = new ClassDefinition("Prelude", PRELUDE, EmptyNamespace.INSTANCE);
  }

  private Prelude() {
  }

  public static void update(Abstract.Definition abstractDef, Definition definition) {
    if (abstractDef.getName().equals("Nat")) {
      NAT = (DataDefinition) definition;
      return;
    }
    if (abstractDef.getName().equals("zero")) {
      ZERO = (Constructor) definition;
      return;
    }
    if (abstractDef.getName().equals("suc")) {
      SUC = (Constructor) definition;
      return;
    }
    if (abstractDef.getName().equals("Lvl")) {
      LVL = (DataDefinition) definition;
      return;
    }
    if (abstractDef.getName().equals("CNat")) {
      CNAT = (DataDefinition) definition;
      return;
    }
    if (abstractDef.getName().equals("inf")) {
      INF = (Constructor) definition;
      return;
    }
    if (abstractDef.getName().equals("fin")) {
      FIN = (Constructor) definition;
      return;
    }
    if (abstractDef.getName().equals("I")) {
      INTERVAL = (DataDefinition) definition;
      return;
    }
    if (abstractDef.getName().equals("left")) {
      LEFT = (Constructor) definition;
      return;
    }
    if (abstractDef.getName().equals("right")) {
      RIGHT = (Constructor) definition;
      return;
    }
    if (abstractDef.getName().equals("Path")) {
      PATH = (DataDefinition) definition;
      return;
    }
    if (abstractDef.getName().equals("path")) {
      PATH_CON = (Constructor) definition;
      return;
    }
    if (abstractDef.getName().equals("=")) {
      PATH_INFIX = (FunctionDefinition) definition;
      return;
    }
    if (abstractDef.getName().equals("@")) {
      AT = (FunctionDefinition) definition;
      return;
    }
    if (abstractDef.getName().equals("coe")) {
      COERCE = (FunctionDefinition) definition;
      COERCE.setElimTree(top(COERCE.getParameters(), branch(COERCE.getParameters().getNext().getNext(), tail(),
        clause(LEFT, EmptyDependentLink.getInstance(), Abstract.Definition.Arrow.RIGHT, Reference(COERCE.getParameters().getNext())))));
      COERCE.hasErrors(false);
      return;
    }
    if (abstractDef.getName().equals("iso")) {
      ISO = (FunctionDefinition) definition;
      ISO.setElimTree(top(ISO.getParameters(), branch(ISO.getParameters().getNext().getNext().getNext().getNext().getNext().getNext(), tail(),
        clause(LEFT, EmptyDependentLink.getInstance(), Reference(ISO.getParameters())),
        clause(RIGHT, EmptyDependentLink.getInstance(), Reference(ISO.getParameters().getNext())))));
      ISO.hasErrors(false);
      return;
    }
    if (abstractDef.getName().equals("TrP")) {
      PROP_TRUNC = (DataDefinition) definition;
      PROP_TRUNC_PATH_CON = PROP_TRUNC.getConstructor("truncP");
      return;
    }
    if (abstractDef.getName().equals("TrS")) {
      SET_TRUNC = (DataDefinition) definition;
      SET_TRUNC_PATH_CON = SET_TRUNC.getConstructor("truncS");
      return;
    }
  }
}
