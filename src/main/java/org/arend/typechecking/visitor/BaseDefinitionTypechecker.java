package org.arend.typechecking.visitor;

import org.arend.core.context.binding.Variable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.DataDefinition;
import org.arend.core.expr.*;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.ErrorReporter;
import org.arend.prelude.Prelude;
import org.arend.term.FunctionKind;
import org.arend.term.concrete.Concrete;
import org.arend.ext.error.TypecheckingError;
import org.arend.typechecking.error.local.CertainTypecheckingError;
import org.arend.typechecking.error.local.NonPositiveDataError;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BaseDefinitionTypechecker {
  public ErrorReporter errorReporter;

  protected BaseDefinitionTypechecker(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
  }

  protected void checkFunctionLevel(Concrete.BaseFunctionDefinition def, FunctionKind kind) {
    if (def.getResultTypeLevel() != null && !(kind == FunctionKind.LEMMA || kind == FunctionKind.COCLAUSE_FUNC || def.getBody() instanceof Concrete.ElimFunctionBody)) {
      errorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.LEVEL_IGNORED, def.getResultTypeLevel()));
      def.setResultTypeLevel(null);
    }
  }

  protected boolean checkElimBody(Concrete.BaseFunctionDefinition def) {
    if (def.isRecursive() && !(def.getBody() instanceof Concrete.ElimFunctionBody)) {
      errorReporter.report(new TypecheckingError("Recursive functions must be defined by pattern matching", def));
      return false;
    } else {
      return true;
    }
  }

  public static int checkNumberInPattern(int n, ErrorReporter errorReporter, Concrete.SourceNode sourceNode) {
    if (n < 0) {
      n = -n;
    }
    if (n > Concrete.NumberPattern.MAX_VALUE) {
      n = Concrete.NumberPattern.MAX_VALUE;
    }
    if (n == Concrete.NumberPattern.MAX_VALUE) {
      errorReporter.report(new TypecheckingError("Value too big", sourceNode));
    }
    return n;
  }

  private boolean checkNonPositiveError(Expression expr, int index, List<? extends Concrete.Parameter> parameters, Concrete.Constructor constructor, Set<? extends Variable> variables, boolean reportErrors) {
    Variable def = expr.findBinding(variables);
    if (def == null) {
      return true;
    }

    if (!reportErrors) {
      return false;
    }

    int i = 0;
    Concrete.Parameter parameter = null;
    for (Concrete.Parameter parameter1 : parameters) {
      i += parameter1.getNumberOfParameters();
      if (i > index) {
        parameter = parameter1;
        break;
      }
    }

    errorReporter.report(new NonPositiveDataError((DataDefinition) def, constructor, parameter == null ? constructor : parameter));
    return false;
  }

  protected boolean isCovariantParameter(DataDefinition dataDefinition, DependentLink parameter) {
    for (Constructor constructor : dataDefinition.getConstructors()) {
      if (!constructor.status().headerIsOK()) {
        continue;
      }
      for (DependentLink link1 = constructor.getParameters(); link1.hasNext(); link1 = link1.getNext()) {
        link1 = link1.getNextTyped(null);
        if (!checkPositiveness(link1.getTypeExpr(), -1, null, null, Collections.singleton(parameter), false)) {
          return false;
        }
      }
    }
    return true;
  }

  protected boolean checkPositiveness(Expression type, int index, List<? extends Concrete.Parameter> parameters, Concrete.Constructor constructor, Set<? extends Variable> variables, boolean reportErrors) {
    type = type.getUnderlyingExpression();
    while (type instanceof PiExpression) {
      if (!checkNonPositiveError(((PiExpression) type).getParameters().getTypeExpr(), index, parameters, constructor, variables, reportErrors)) {
        return false;
      }
      type = ((PiExpression) type).getCodomain().getUnderlyingExpression();
    }

    if (type instanceof FunCallExpression && ((FunCallExpression) type).getDefinition() == Prelude.PATH_INFIX) {
      List<? extends Expression> exprs = ((FunCallExpression) type).getDefCallArguments();
      if (!checkPositiveness(exprs.get(0), index, parameters, constructor, variables, reportErrors) || !checkNonPositiveError(exprs.get(1), index, parameters, constructor, variables, reportErrors) || !checkNonPositiveError(exprs.get(2), index, parameters, constructor, variables, reportErrors)) {
        return false;
      }
    } else if (type instanceof SigmaExpression) {
      for (DependentLink link = ((SigmaExpression) type).getParameters(); link.hasNext(); link = link.getNext()) {
        link = link.getNextTyped(null);
        if (!checkPositiveness(link.getTypeExpr(), index, parameters, constructor, variables, reportErrors)) {
          return false;
        }
      }
    } else if (type instanceof DataCallExpression) {
      List<? extends Expression> exprs = ((DataCallExpression) type).getDefCallArguments();
      DataDefinition typeDef = ((DataCallExpression) type).getDefinition();

      for (int i = 0; i < exprs.size(); i++) {
        if (typeDef.isCovariant(i)) {
          Expression expr = exprs.get(i).normalize(NormalizationMode.WHNF);
          for (LamExpression lam = expr.cast(LamExpression.class); lam != null; lam = expr.cast(LamExpression.class)) {
            expr = lam.getBody().normalize(NormalizationMode.WHNF);
          }
          if (!checkPositiveness(expr, index, parameters, constructor, variables, reportErrors)) {
            return false;
          }
        } else {
          if (!checkNonPositiveError(exprs.get(i), index, parameters, constructor, variables, reportErrors)) {
            return false;
          }
        }
      }
    } else {
      while (type instanceof AppExpression) {
        if (!checkNonPositiveError(((AppExpression) type).getArgument(), index, parameters, constructor, variables, reportErrors)) {
          return false;
        }
        type = type.getFunction().getUnderlyingExpression();
      }
      if (!(type instanceof ReferenceExpression)) {
        if (!checkNonPositiveError(type, index, parameters, constructor, variables, reportErrors)) {
          return false;
        }
      }
    }

    return true;
  }
}
