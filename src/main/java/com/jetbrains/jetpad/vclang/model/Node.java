package com.jetbrains.jetpad.vclang.model;

import jetbrains.jetpad.otmodel.node.NodeConceptId;
import jetbrains.jetpad.otmodel.wrapper.NodeWrapper;
import jetbrains.jetpad.otmodel.wrapper.WrapperContext;
import com.jetbrains.jetpad.vclang.model.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.model.definition.TypedDefinition;

import java.util.List;

import static com.jetbrains.jetpad.vclang.model.expr.Model.*;

public abstract class Node extends NodeWrapper<Node> {

  protected Node(WrapperContext ctx, NodeConceptId conceptId) {
    super(ctx, conceptId);
  }

  protected Node(WrapperContext ctx, jetbrains.jetpad.otmodel.node.Node node) {
    super(ctx, node);
  }

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
      for (Argument arg : piExpression.getArguments()) {
        if (arg == this) {
          return Position.ARR_DOM;
        }
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

  public int prec() {
    Position pos = position();
    if (pos == Position.ARG) {
      Argument arg = (Argument) parent();
      if (!(arg instanceof TelescopeArgument) && arg.getExplicit()) {
        return parent().get().prec();
      }
    }
    return pos.prec();
  }

  public void replaceWith(Node node) {
    switch (position()) {
      case FUN_CLAUSE:
        ((FunctionDefinition) parent().get()).term().set((Expression) node);
        break;
      case ARG:
        ((TypeArgument) parent().get()).type().set((Expression) node);
        break;
      case PARENS:
        ((ParensExpression) parent().get()).expression().set((Expression) node);
        break;
      case DEF_RESULT_TYPE:
        ((TypedDefinition) parent().get()).resultType().set((Expression) node);
        break;
      case ARR_DOM:
        List<TypeArgument> arguments = ((PiExpression) parent().get()).getArguments();
        for (int i = 0; i < arguments.size(); ++i) {
          if (arguments.get(i) == this) {
            arguments.set(i, (TypeArgument) node);
          }
        }
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
