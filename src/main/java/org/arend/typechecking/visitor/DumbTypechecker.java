package org.arend.typechecking.visitor;

import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.LocalErrorReporter;

public class DumbTypechecker extends VoidConcreteVisitor<Void, Void> {
  private final BaseDefinitionTypechecker myTypechecker;
  private Concrete.Definition myDefinition;

  public DumbTypechecker(LocalErrorReporter errorReporter) {
    myTypechecker = new BaseDefinitionTypechecker(errorReporter);
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition def, Void params) {
    myDefinition = def;
    myTypechecker.checkFunctionLevel(def);
    super.visitFunction(def, null);
    myTypechecker.checkElimBody(def);
    return null;
  }

  @Override
  public Void visitData(Concrete.DataDefinition def, Void params) {
    myDefinition = def;
    super.visitData(def, null);
    return null;
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Void params) {
    myDefinition = def;
    super.visitClass(def, null);
    return null;
  }

  @Override
  public Void visitReference(Concrete.ReferenceExpression expr, Void params) {
    if (expr.getReferent() == myDefinition.getData()) {
      myDefinition.setRecursive(true);
    }

    super.visitReference(expr, null);
    return null;
  }
}
