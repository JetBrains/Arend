package org.arend.term.prettyprint;

import org.arend.ext.module.LongReference;
import org.arend.ext.prettyprinting.DefinitionRenamer;
import org.arend.naming.reference.LocatedReferable;
import org.arend.term.concrete.BaseConcreteExpressionVisitor;
import org.arend.term.concrete.Concrete;

public class DefinitionRenamerConcreteVisitor extends BaseConcreteExpressionVisitor<Void> {
  private final DefinitionRenamer myDefinitionRenamer;

  public DefinitionRenamerConcreteVisitor(DefinitionRenamer definitionRenamer) {
    myDefinitionRenamer = definitionRenamer;
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Void params) {
    if (expr.getReferent() instanceof LocatedReferable) {
      LongReference longReference = myDefinitionRenamer.renameDefinition(expr.getReferent());
      if (longReference != null) {
        return new Concrete.LongReferenceExpression(expr.getData(), longReference, expr.getReferent(), expr.getPLevel(), expr.getHLevel());
      }
    }
    return expr;
  }
}
