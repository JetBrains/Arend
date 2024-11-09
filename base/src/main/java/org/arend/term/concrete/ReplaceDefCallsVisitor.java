package org.arend.term.concrete;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.TypecheckingError;
import org.arend.naming.reference.ErrorReference;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;
import org.arend.typechecking.visitor.VoidConcreteVisitor;

import java.util.Set;

public class ReplaceDefCallsVisitor extends VoidConcreteVisitor<Void> {
  private final Set<? extends TCReferable> myReferables;
  private final TCReferable mySelfReferable;
  private final ErrorReporter myErrorReporter;
  private boolean myRecursive;

  public ReplaceDefCallsVisitor(Set<? extends TCReferable> referables, TCReferable selfReferable, ErrorReporter errorReporter) {
    myReferables = referables;
    mySelfReferable = selfReferable;
    myErrorReporter = errorReporter;
  }

  public boolean isRecursive() {
    return myRecursive;
  }

  @Override
  protected void visitClassFieldImpl(Concrete.ClassFieldImpl classFieldImpl, Void params) {
    if (classFieldImpl.implementation != null && !(classFieldImpl instanceof Concrete.CoClauseFunctionReference && classFieldImpl.isDefault())) {
      classFieldImpl.implementation.accept(this, params);
    }
    visitElements(classFieldImpl.getSubCoclauseList(), params);
  }

  private ErrorReference checkReferable(Referable referable, Concrete.SourceNode sourceNode) {
    if (!(referable instanceof TCReferable)) {
      return null;
    }
    if (mySelfReferable == referable) {
      myRecursive = true;
    }
    if (myReferables.contains(referable)) {
      TypecheckingError error = new TypecheckingError("A cyclic reference to a \\use definition", sourceNode);
      myErrorReporter.report(error);
      return new ErrorReference(error, referable.getRefName());
    }
    return null;
  }

  @Override
  protected void visitPattern(Concrete.Pattern pattern, Void params) {
    if (pattern instanceof Concrete.ConstructorPattern conPattern) {
      ErrorReference error = checkReferable(conPattern.getConstructor(), conPattern);
      if (error != null) {
        conPattern.setConstructor(error);
      }
    }
    super.visitPattern(pattern, null);
  }

  @Override
  public Void visitReference(Concrete.ReferenceExpression expr, Void params) {
    ErrorReference error = checkReferable(expr.getReferent(), expr);
    if (error != null) {
      expr.setReferent(error);
    }
    return null;
  }
}
