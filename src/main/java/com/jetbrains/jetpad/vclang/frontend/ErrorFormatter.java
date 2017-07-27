package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.frontend.parser.ParserError;
import com.jetbrains.jetpad.vclang.module.error.ModuleCycleError;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.*;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equation;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.LevelEquation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ErrorFormatter {
  private final SourceInfoProvider mySrc;

  public ErrorFormatter(SourceInfoProvider src) {
    mySrc = src;
  }

  private String printLevel(GeneralError error) {
    return "[" + error.getLevel() + "]";
  }

  private String printHeader(GeneralError error) {
    StringBuilder builder = new StringBuilder();
    builder.append(printLevel(error));

    final Concrete.Position pos;
    if (error.getCause() instanceof Concrete.SourceNode) {
      pos = ((Concrete.SourceNode) error.getCause()).getPosition();
    } else if (error instanceof ParserError) {
      pos = ((ParserError) error).position;
    } else if (error instanceof TypeCheckingError && ((TypeCheckingError) error).definition instanceof Concrete.SourceNode) {
      pos = ((Concrete.SourceNode) ((TypeCheckingError) error).definition).getPosition();
    } else {
      pos = null;
    }

    if (pos != null) {
      builder.append(' ').append(pos.module != null ? pos.module : "<Unknown module>");
      builder.append(':').append(pos.line).append(':').append(pos.column);
    }
    return builder.toString();
  }

  private String printData(GeneralError error) {
    StringBuilder builder = new StringBuilder();

    if (error instanceof TypeCheckingError) {
      printTypeCheckingErrorData(((TypeCheckingError) error).localError, builder);
    } else if (error instanceof ModuleCycleError) {
      for (SourceId sourceId: ((ModuleCycleError) error).cycle) {
        builder.append(sourceId.getModulePath()).append(" - ");
      }
      builder.append(((ModuleCycleError) error).cycle.get(0));
      return builder.toString();
    } else if (error instanceof CycleError) {
      List<Abstract.Definition> cycle = ((CycleError) error).cycle;
      builder.append(cycle.get(cycle.size() - 1));
      for (Abstract.Definition definition : cycle) {
        builder.append(" - ");
        builder.append(definition.getName());
      }
    }

    return builder.toString();
  }

  private void printTypeCheckingErrorData(LocalTypeCheckingError error, StringBuilder builder) {
    if (error instanceof GoalError) {
      boolean printContext = !((GoalError) error).context.isEmpty();
      boolean printType = ((GoalError) error).type != null;
      if (printType) {
        String text = "Expected type: ";
        builder.append(text);
        List<String> names = new ArrayList<>(((GoalError) error).context.size());
        for (Binding binding : ((GoalError) error).context.values()) {
          names.add(binding.getName());
        }
        ((GoalError) error).type.prettyPrint(builder, names, Abstract.Expression.PREC, text.length());
      }
      if (printContext) {
        if (printType) builder.append('\n');
        builder.append("Context:");
        List<String> names = new ArrayList<>(((GoalError) error).context.size());
        for (Binding binding : ((GoalError) error).context.values()) {
          builder.append("\n  ").append(binding.getName() == null ? "_" : binding.getName()).append(" : ");
          Expression type = binding.getTypeExpr();
          if (type != null) {
            type.prettyPrint(builder, names, Abstract.Expression.PREC, 0);
          } else {
            builder.append("{!error}");
          }
          names.add(binding.getName());
        }
      }
    } else if (error instanceof TypeMismatchError) {
      String text = "Expected type: ";
      builder.append(text);
      ((TypeMismatchError) error).expected.prettyPrint(builder, new ArrayList<>(), Abstract.Expression.PREC, text.length());
      builder.append('\n')
        .append("  Actual type: ");
      ((TypeMismatchError) error).actual.prettyPrint(builder, new ArrayList<>(), Abstract.Expression.PREC, text.length());
    } else if (error instanceof MissingClausesError) {
      for (List<Expression> missingClause : ((MissingClausesError) error).getMissingClauses()) {
        boolean first = true;
        for (Expression expression : missingClause) {
          if (first) {
            first = false;
          } else {
            builder.append(", ");
          }
          expression.prettyPrint(builder, new ArrayList<>(), Abstract.Expression.PREC, 0);
        }
        builder.append('\n');
      }
    } else if (error instanceof PathEndpointMismatchError) {
      String text = "Expected: ";
      builder.append(text);
      ((PathEndpointMismatchError) error).expected.prettyPrint(builder, new ArrayList<>(), Abstract.Expression.PREC, text.length());
      builder.append('\n')
        .append("  Actual: ");
      ((PathEndpointMismatchError) error).actual.prettyPrint(builder, new ArrayList<>(), Abstract.Expression.PREC, text.length());
    } else if (error instanceof ConditionsError) {
      ConditionsError condError = (ConditionsError) error;
      condError.expr1.prettyPrint(builder, new ArrayList<>(), Abstract.Expression.PREC, 0);
      if (condError.substitution1 != null && !condError.substitution1.isEmpty()) {
        builder.append(" with ");
        printSubstitution(builder, condError.substitution1);
      }
      if (condError.evaluatedExpr1 != null) {
        builder.append(" evaluates to ");
        condError.evaluatedExpr1.prettyPrint(builder, new ArrayList<>(), Abstract.Expression.PREC, 0);
      }
      builder.append('\n');
      condError.expr2.prettyPrint(builder, new ArrayList<>(), Abstract.Expression.PREC, 0);
      if (condError.substitution2 != null && !condError.substitution2.isEmpty()) {
        builder.append(" with ");
        printSubstitution(builder, condError.substitution2);
      }
      if (condError.evaluatedExpr2 != null) {
        builder.append(" evaluates to ");
        condError.evaluatedExpr2.prettyPrint(builder, new ArrayList<>(), Abstract.Expression.PREC, 0);
      }
    } else if (error instanceof SolveEquationError) {
      String text = "1st expression: ";
      builder.append(text);
      ((SolveEquationError) error).expr1.prettyPrint(builder, new ArrayList<>(), Abstract.Expression.PREC, text.length());
      builder.append('\n')
          .append("2nd expression: ");
      ((SolveEquationError) error).expr2.prettyPrint(builder, new ArrayList<>(), Abstract.Expression.PREC, text.length());
    } else if (error instanceof SolveEquationsError) {
      boolean first = true;
      for (Equation equation : ((SolveEquationsError) error).equations) {
        if (!first) builder.append('\n');
        equation.type.prettyPrint(builder, new ArrayList<>(), Abstract.Expression.PREC, 0);
        builder.append(equation.cmp == Equations.CMP.LE ? " <= " : equation.cmp == Equations.CMP.EQ ? " = " : " >= ");
        equation.expr.prettyPrint(builder, new ArrayList<>(), Abstract.Expression.PREC, 0);
        first = false;
      }
    } else if (error instanceof SolveLevelEquationsError) {
      boolean first = true;
      PrettyPrintVisitor ppv = new PrettyPrintVisitor(builder, 0);
      for (LevelEquation<? extends Variable> equation : ((SolveLevelEquationsError) error).equations) {
        if (!first) builder.append('\n');
        if (equation.isInfinity()) {
          builder.append(equation.getVariable()).append(" = inf");
        } else {
          printEqExpr(builder, ppv, equation.getVariable1(), -equation.getConstant());
          builder.append(" <= ");
          printEqExpr(builder, ppv, equation.getVariable2(), equation.getConstant());
        }
        first = false;
      }
    } else if (error instanceof ArgInferenceError) {
      if (((ArgInferenceError) error).candidates.length > 0) {
        builder.append("\nCandidates are:");
        for (Expression candidate : ((ArgInferenceError) error).candidates) {
          builder.append("\n");
          PrettyPrintVisitor.printIndent(builder, 2);
          candidate.prettyPrint(builder, new ArrayList<>(), Abstract.Expression.PREC, 2);
        }
      }

      if (((ArgInferenceError) error).expected != null || ((ArgInferenceError) error).actual != null) {
        builder.append("\nSince types of the candidates are not less or equal to the expected type");
        if (((ArgInferenceError) error).expected != null) {
          String text = "Expected type: ";
          builder.append('\n').append(text);
          ((ArgInferenceError) error).expected.prettyPrint(builder, new ArrayList<>(), Abstract.Expression.PREC, text.length());
        }
        if (((ArgInferenceError) error).actual != null) {
          String text = "  Actual type: ";
          builder.append('\n').append(text);
          ((ArgInferenceError) error).actual.prettyPrint(builder, new ArrayList<>(), Abstract.Expression.PREC, text.length());
        }
      }
    } else if (error instanceof MemberNotFoundError) {
      builder.append(((MemberNotFoundError) error).name).append(" of ").append("some compiled definition called ").append(((MemberNotFoundError) error).targetDefinition.getName());
    }
  }

  private void printSubstitution(StringBuilder builder, ExprSubstitution substitution) {
    boolean first = true;
    for (Map.Entry<Variable, Expression> entry : substitution.getEntries()) {
      if (first) {
        first = false;
      } else {
        builder.append(", ");
      }
      builder.append(entry.getKey().getName()).append(" = ");
      entry.getValue().prettyPrint(builder, new ArrayList<>(), Abstract.Expression.PREC, 0);
    }
  }

  private void printEqExpr(StringBuilder builder, PrettyPrintVisitor ppv, Variable var, Integer constant) {
    if (var != null) {
      if (var instanceof InferenceLevelVariable) {
        ppv.prettyPrintInferLevelVar((InferenceLevelVariable) var);
      } else {
        builder.append(var);
      }
      if (constant > 0) {
        builder.append(" + ").append(constant);
      }
    } else {
      builder.append(constant > 0 ? constant : 0);
    }
  }

  private String printBody(GeneralError error) {
    StringBuilder builder = new StringBuilder();
    String data = printData(error);
    builder.append(data);

    if (error.getCause() != null) {
      String text = "In: ";
      String causeString = PrettyPrintVisitor.prettyPrint(error.getCause(), text.length());
      if (causeString != null) {
        if (!data.isEmpty()) builder.append('\n');
        builder.append(text).append(causeString);
      }
    }

    if (error instanceof TypeCheckingError) {
      Abstract.Definition def = ((TypeCheckingError) error).definition;
      if (def != null) {
        builder.append('\n').append("While typechecking: ");

        String name = mySrc.nameFor(def);
        if (name == null && def.getName() != null) {
          name = "???." + def.getName();
        }

        if (name != null) {
          SourceId module = mySrc.sourceOf(def);
          builder.append(' ').append(module != null ? module : "<Unknown module>");
          builder.append("::").append(name);
        }
      }
    }
    return builder.toString();
  }

  public String printError(GeneralError error) {
    String body = printBody(error);
    return printHeader(error) + ": " + error.getMessage() + (body.isEmpty() ? "" : '\n' + indented(body));
  }

  public String printErrors(Collection<? extends GeneralError> errors) {
    StringBuilder builder = new StringBuilder();
    for (GeneralError error : errors) {
      builder.append(printError(error)).append('\n');
    }
    return builder.toString();
  }


  private static String indented(String data) {
    return data.replaceAll("(?m)^", "\t");
  }
}
