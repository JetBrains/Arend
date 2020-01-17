package org.arend.typechecking.termination;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Definition;
import org.arend.core.expr.DefCallExpression;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

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
  public Doc getMatrixLabel(PrettyPrinterConfig ppConfig) {
    return hang(hList(refDoc(myEnclosingDefinition.getReferable()), text(" ->")), termDoc(myCallExpression, ppConfig));
  }
}
