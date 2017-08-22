package com.jetbrains.jetpad.vclang.typechecking.termination;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.error.doc.Doc;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

class CallMatrix extends LabeledCallMatrix {
  private final DefCallExpression myCallExpression;
  private final Definition myEnclosingDefinition;

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
  public Doc getMatrixLabel() {
    return hang(hList(refDoc(myEnclosingDefinition.getReferable()), text(" ->")), termDoc(myCallExpression));
  }
}
