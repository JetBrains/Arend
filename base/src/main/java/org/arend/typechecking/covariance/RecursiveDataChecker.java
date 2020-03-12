package org.arend.typechecking.covariance;

import org.arend.core.context.binding.Variable;
import org.arend.core.definition.DataDefinition;
import org.arend.core.expr.Expression;
import org.arend.ext.error.ErrorReporter;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.NonPositiveDataError;

import java.util.Set;

public class RecursiveDataChecker extends CovarianceChecker {
  private final Set<DataDefinition> myDataDefinitions;
  private final ErrorReporter myErrorReporter;
  private final Concrete.Constructor myConstructor;
  private final Concrete.Parameter myParameter;

  public RecursiveDataChecker(Set<DataDefinition> dataDefinitions, ErrorReporter errorReporter, Concrete.Constructor constructor, Concrete.Parameter parameter) {
    myDataDefinitions = dataDefinitions;
    myErrorReporter = errorReporter;
    myConstructor = constructor;
    myParameter = parameter;
  }

  @Override
  protected boolean checkNonCovariant(Expression expr) {
    Variable def = expr.findBinding(myDataDefinitions);
    if (def == null) {
      return false;
    }

    myErrorReporter.report(new NonPositiveDataError((DataDefinition) def, myConstructor, myParameter == null ? myConstructor : myParameter.getType() != null ? myParameter.getType() : myParameter));
    return true;
  }
}
