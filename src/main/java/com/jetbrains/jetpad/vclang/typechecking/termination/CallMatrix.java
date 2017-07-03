package com.jetbrains.jetpad.vclang.typechecking.termination;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.expr.DefCallExpression;

class CallMatrix extends LabeledCallMatrix {
  private DefCallExpression myCallExpression;
  private Definition myEnclosingDefinition;

  CallMatrix(Definition enclosingDefinition, DefCallExpression call) {
    super(DependentLink.Helper.size(call.getDefinition().getParameters()), DependentLink.Helper.size(enclosingDefinition.getParameters()));
    myCallExpression = call;
    myEnclosingDefinition = enclosingDefinition;
  }

  @Override
  public Definition getCodomain() {
    return myCallExpression.getDefinition();
  }

  @Override
  public Definition getDomain() {
    return myEnclosingDefinition;
  }

  @Override
  public int getCompositeLength() {
    return 1;
  }

  @Override
  public String getMatrixLabel() {
    return "In " + myEnclosingDefinition.getName() + ": " + myCallExpression.toString();
  }
}
