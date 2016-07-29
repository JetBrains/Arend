package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.error.ModuleCycleError;
import com.jetbrains.jetpad.vclang.parser.ParserError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.inference.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.*;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equation;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.LevelEquation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    } else if (error instanceof TypeCheckingError && ((TypeCheckingError) error).getDefinition() instanceof Concrete.SourceNode) {
      pos = ((Concrete.SourceNode) ((TypeCheckingError) error).getDefinition()).getPosition();
    } else {
      pos = null;
    }

    if (pos != null) {
      builder.append(' ').append(pos.module != null ? pos.module : "<Unknown module>");
      builder.append(':').append(pos.line).append(':').append(pos.column);
    }
    return builder.toString();
  }

  private void printEquation(StringBuilder builder, PrettyPrintable expr1, PrettyPrintable expr2) {
    expr1.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, 0);
    builder.append(" = ");
    expr2.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, 0);
  }

  private String printData(GeneralError error) {
    StringBuilder builder = new StringBuilder();

    if (error instanceof UnsolvedEquations) {
      boolean first = true;
      for (Equation equation : ((UnsolvedEquations) error).equations) {
        if (!first) builder.append('\n');
        printEquation(builder, equation.type, equation.expr);
        first = false;
      }
    } else if (error instanceof GoalError) {
      boolean printContext = !((GoalError) error).context.isEmpty();
      boolean printType = ((GoalError) error).type != null;
      if (printType) {
        String text = "Expected type: ";
        builder.append(text);
        List<String> names = new ArrayList<>(((GoalError) error).context.size());
        for (Binding binding : ((GoalError) error).context) {
          names.add(binding.getName() == null ? null : binding.getName());
        }
        ((GoalError) error).type.prettyPrint(builder, names, Abstract.Expression.PREC, text.length());
      }
      if (printContext) {
        if (printType) builder.append('\n');
        builder.append("Context:");
        List<String> names = new ArrayList<>(((GoalError) error).context.size());
        for (Binding binding : ((GoalError) error).context) {
          builder.append("\n  ").append(binding.getName() == null ? "_" : binding.getName()).append(" : ");
          Expression type = binding.getType();
          if (type != null) {
            type.prettyPrint(builder, names, Abstract.Expression.PREC, 0);
          } else {
            builder.append("{!error}");
          }
          names.add(binding.getName() == null ? null : binding.getName());
        }
      }
    } else if (error instanceof TypeMismatchError) {
      String text = "Expected type: ";
      builder.append(text);
      ((TypeMismatchError) error).expected.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, text.length());
      builder.append('\n');
      builder.append("  Actual type: ");
      ((TypeMismatchError) error).actual.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, text.length());
    } else if (error instanceof SolveEquationError) {
      String text = "1st expression: ";
      builder.append(text);
      ((SolveEquationError) error).expr1.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, text.length());
      builder.append('\n')
          .append("2nd expression: ");
      ((SolveEquationError) error).expr2.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, text.length());
      if (((SolveEquationError) error).binding != null) {
        builder.append('\n')
            .append("Since '").append(((SolveEquationError) error).binding).append("' is free in these expressions");
      }
    } else if (error instanceof SolveEquationsError) {
      boolean first = true;
      for (LevelEquation<? extends Binding> equation : ((SolveEquationsError) error).equations) {
        if (!first) builder.append('\n');
        printEqExpr(builder, equation.var1, -equation.constant);
        builder.append(" <= ");
        printEqExpr(builder, equation.var2, equation.constant);
        first = false;
      }
    } else if (error instanceof UnsolvedBindings) {
      boolean first = true;
      for (InferenceBinding binding : ((UnsolvedBindings) error).bindings) {
        if (!first) builder.append('\n');
        builder.append(binding);
        if (binding.getSourceNode() instanceof Concrete.SourceNode) {
          builder.append(" at ").append(((Concrete.SourceNode) binding.getSourceNode()).getPosition());
        }
        first = false;
      }
    } else if (error instanceof ArgInferenceError) {
      if (((ArgInferenceError) error).candidates.length > 0) {
        builder.append("\nCandidates are:");
        for (Expression candidate : ((ArgInferenceError) error).candidates) {
          builder.append("\n");
          PrettyPrintVisitor.printIndent(builder, 2);
          candidate.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, 2);
        }
      }

      if (((ArgInferenceError) error).expected != null || ((ArgInferenceError) error).actual != null) {
        builder.append("\nSince types of the candidates are not less or equal to the expected type");
        if (((ArgInferenceError) error).expected != null) {
          String text = "Expected type: ";
          builder.append('\n').append(text);
          ((ArgInferenceError) error).expected.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, text.length());
        }
        if (((ArgInferenceError) error).actual != null) {
          String text = "  Actual type: ";
          builder.append('\n').append(text);
          ((ArgInferenceError) error).actual.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, text.length());
        }
      }
    } else if (error instanceof LevelInferenceError) {
      if (((LevelInferenceError) error).candidates.length > 0) {
        builder.append("\nCandidates are:");
        for (Level candidate : ((LevelInferenceError) error).candidates) {
          builder.append("\n");
          PrettyPrintVisitor.printIndent(builder, 2);
          candidate.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, 2);
        }
      }
    } else if (error instanceof MemberNotFoundError) {
      builder.append(((MemberNotFoundError) error).name).append(" of ").append("some compiled definition called ").append(((MemberNotFoundError) error).targetDefinition.getName());
    } else if (error instanceof ModuleCycleError) {
      for (ModuleID moduleID : ((ModuleCycleError) error).cycle) {
        builder.append(moduleID.getModulePath()).append(" - ");
      }
      builder.append(((ModuleCycleError) error).cycle.get(0));
      return builder.toString();
    }

    return builder.toString();
  }

  private void printEqExpr(StringBuilder builder, Binding var, int constant) {
    if (var != null) {
      builder.append(var);
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
      Abstract.Definition def = ((TypeCheckingError) error).getDefinition();
      if (def != null) {
        builder.append('\n').append("While typechecking: ");

        String name = mySrc.nameFor(def);
        if (name == null && def.getName() != null) {
          name = "???." + def.getName();
        }

        if (name != null) {
          ModuleID module = mySrc.moduleOf(def);
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
