package com.jetbrains.jetpad.vclang.model;

import com.jetbrains.jetpad.vclang.model.definition.Argument;
import com.jetbrains.jetpad.vclang.model.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.model.definition.TypedDefinition;
import com.jetbrains.jetpad.vclang.model.expr.*;
import jetbrains.jetpad.model.children.HasParent;

public class Node extends HasParent<Node, Node> {
  public Position position;

  public void replaceWith(Node node) {
    switch (position) {
      case FUN_CLAUSE:
        ((FunctionDefinition) parent().get()).setTerm((Expression) node);
        break;
      case ARG:
        ((Argument) parent().get()).setType((Expression) node);
        break;
      case PARENS:
        ((ParensExpression) parent().get()).setExpression((Expression) node);
        break;
      case DEF_RESULT_TYPE:
        ((TypedDefinition) parent().get()).setResultType((Expression) node);
        break;
      case ARR_DOM:
        ((PiExpression) parent().get()).setDomain((Argument) node);
        break;
      case ARR_COD:
        ((PiExpression) parent().get()).setCodomain((Expression) node);
        break;
      case APP_FUN:
        ((AppExpression) parent().get()).setFunction((Expression) node);
        break;
      case APP_ARG:
        ((AppExpression) parent().get()).setArgument((Expression) node);
        break;
      case LAM:
        ((LamExpression) parent().get()).setBody((Expression) node);
        break;
    }
    assert false;
  }
}
