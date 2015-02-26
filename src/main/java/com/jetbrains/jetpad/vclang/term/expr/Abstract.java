package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.visitor.ExpressionVisitor;

public class Abstract {
  public static interface Expression {
    abstract <T> T accept(ExpressionVisitor<? extends T> visitor);
  }

  public static interface AppExpression extends Expression {
    Expression getFunction();
    Expression getArgument();
  }

  public static interface DefCallExpression extends Expression {
    Definition getDefinition();
  }

  public static interface IndexExpression extends Expression {
    int getIndex();
  }

  public static interface LamExpression extends Expression {
    String getVariable();
    Expression getBody();
  }

  public static interface NatExpression extends Expression {}

  public static interface NelimExpression extends Expression {}

  public static interface PiExpression extends Expression {
    boolean isExplicit();
    String getVariable();
    Expression getLeft();
    Expression getRight();
  }

  public static interface SucExpression extends Expression {}

  public static interface UniverseExpression extends Expression {
    int getLevel();
  }

  public static interface VarExpression extends Expression {
    String getName();
  }

  public static interface ZeroExpression extends Expression {}
}
