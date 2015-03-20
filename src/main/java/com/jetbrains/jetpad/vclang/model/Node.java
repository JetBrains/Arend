package com.jetbrains.jetpad.vclang.model;

import com.jetbrains.jetpad.vclang.model.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.model.definition.TypedDefinition;
import jetbrains.jetpad.model.children.HasParent;

import static com.jetbrains.jetpad.vclang.model.expr.Model.*;

public abstract class Node extends HasParent<Node, Node> {
  public Position position() {
    Node parent = parent().get();
    if (parent instanceof FunctionDefinition) {
      if (((FunctionDefinition) parent).getTerm() == this) {
        return Position.FUN_CLAUSE;
      }
    }
    if (parent instanceof Argument) {
      return Position.ARG;
    }
    if (parent instanceof ParensExpression) {
      return Position.PARENS;
    }
    if (parent instanceof TypedDefinition) {
      if (((TypedDefinition) parent).getResultType() == this) {
        return Position.DEF_RESULT_TYPE;
      }
    }
    if (parent instanceof PiExpression) {
      PiExpression piExpression = (PiExpression) parent;
      if (piExpression.domain().get() == this) {
        return Position.ARR_DOM;
      }
      if (piExpression.codomain().get() == this) {
        return Position.ARR_COD;
      }
    }
    if (parent instanceof AppExpression) {
      AppExpression appExpression = (AppExpression) parent;
      if (appExpression.getFunction() == this) {
        return Position.APP_FUN;
      }
      if (appExpression.getArgument() == this) {
        return Position.APP_ARG;
      }
    }
    if (parent instanceof LamExpression) {
      if (((LamExpression) parent).getBody() == this) {
        return Position.LAM;
      }
    }
    throw new IllegalStateException();
  }

  public void replaceWith(Node node) {
    switch (position()) {
      case FUN_CLAUSE:
        ((FunctionDefinition) parent().get()).term().set((Expression) node);
        break;
      case ARG:
        ((Argument) parent().get()).type().set((Expression) node);
        break;
      case PARENS:
        ((ParensExpression) parent().get()).expression().set((Expression) node);
        break;
      case DEF_RESULT_TYPE:
        ((TypedDefinition) parent().get()).resultType().set((Expression) node);
        break;
      case ARR_DOM:
        ((PiExpression) parent().get()).domain().set((Argument) node);
        break;
      case ARR_COD:
        ((PiExpression) parent().get()).codomain().set((Expression) node);
        break;
      case APP_FUN:
        ((AppExpression) parent().get()).function().set((Expression) node);
        break;
      case APP_ARG:
        ((AppExpression) parent().get()).argument().set((Expression) node);
        break;
      case LAM:
        ((LamExpression) parent().get()).body().set((Expression) node);
        break;
      default:
        throw new IllegalStateException();
    }
  }

  public abstract Node[] children();
}
