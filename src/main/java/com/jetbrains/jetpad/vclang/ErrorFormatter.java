package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;
import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.error.ModuleCycleError;
import com.jetbrains.jetpad.vclang.module.error.ModuleLoadingError;
import com.jetbrains.jetpad.vclang.parser.ParserError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.*;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.ListEquations;

import java.util.ArrayList;
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
    if (error instanceof TypeCheckingError) {
      Abstract.Definition def = ((TypeCheckingError) error).getDefinition();
      if (def != null) {
        ModuleID module = mySrc.moduleOf(def);
        builder.append(' ').append(module != null ? module : "<Unknown module>");

        String name = mySrc.nameFor(def);
        if (name == null && def.getName() != null) {
          name = "???." + def.getName();
        }

        if (name != null) {
          builder.append('(').append(name).append(')');
        }

        if (module != null) {
          if (error.getCause() instanceof Concrete.SourceNode) {
            Concrete.Position pos = ((Concrete.SourceNode) error.getCause()).getPosition();
            builder.append(':').append(pos.line).append(':').append(pos.column);
          }
        }
      }
    } else if (error instanceof ModuleLoadingError) {
      builder.append(' ');
      if (((ModuleLoadingError) error).module == null) {
        builder.append("<Unknown module>");
      } else {
        builder.append(((ModuleLoadingError) error).module.getModulePath().toString());
      }
      if (error instanceof ParserError && ((ParserError) error).position != null) {
        builder.append(':').append(((ParserError) error).position.line);
        builder.append(':').append(((ParserError) error).position.column);
      }
    }
    return builder.toString();
  }

  private String printData(GeneralError error) {
    StringBuilder builder = new StringBuilder();

    if (error instanceof UnsolvedEquations) {
      for (ListEquations.CmpEquation equation : ((UnsolvedEquations) error).equations) {
        equation.expr1.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, 0);
        builder.append(" = ");
        equation.expr2.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, 0);
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
    } else if (error instanceof SolveEquationsError) {
      String text = "1st expression: ";
      builder.append(text);
      ((SolveEquationsError) error).expr1.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, text.length());
      builder.append('\n')
          .append("2nd expression: ");
      ((SolveEquationsError) error).expr2.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, text.length());
      if (((SolveEquationsError) error).binding != null) {
        builder.append('\n')
            .append("Since '").append(((SolveEquationsError) error).binding).append("' is free in these expressions");
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
    } else if (error instanceof ModuleCycleError) {
      for (ModuleID moduleID : ((ModuleCycleError) error).cycle) {
        builder.append(moduleID.getModulePath()).append(" - ");
      }
      builder.append(((ModuleCycleError) error).cycle.get(0));
      return builder.toString();
    }

    return builder.toString();
  }

  private String printCause(Abstract.SourceNode cause) {
    String text = "In: ";
    return text + PrettyPrintVisitor.prettyPrint(cause, text.length());
  }

  private String printBody(GeneralError error) {
    StringBuilder builder = new StringBuilder();
    String data = printData(error);
    builder.append(data);
    if (error.getCause() != null) {
      if (!data.isEmpty()) builder.append('\n');
      builder.append(printCause(error.getCause()));
    }
    return builder.toString();
  }

  public String printError(GeneralError error) {
    String body = printBody(error);
    return printHeader(error) + ": " + error.getMessage() + (body.isEmpty() ? "" : '\n' + indented(body));
  }


  private static String indented(String data) {
    return data.replaceAll("(?m)^", "\t");
  }
}
