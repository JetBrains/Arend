package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ConditionViolationsCollector;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.CoverageChecker;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.numberOfVariables;
import static com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError.getNames;

public class TypecheckingElim {

  public static TypeCheckingError checkConditions(final Abstract.Function def, List<Binding> context, ElimTreeNode elimTree) {
    return checkConditions(def.getName(), def, def.getArguments(), context, elimTree);
  }

  public static TypeCheckingError checkConditions(final Name name, final Abstract.SourceNode source, final List<? extends Abstract.Argument> arguments, List<Binding> context, ElimTreeNode elimTree) {
    final StringBuilder errorMsg = new StringBuilder();

    ConditionViolationsCollector.check(context, elimTree, new ConditionViolationsCollector.ConditionViolationChecker() {
      @Override
      public void check(List<Binding> context, Expression expr1, List<Expression> subst1, Expression expr2, List<Expression> subst2) {
        expr1 = expr1.normalize(NormalizeVisitor.Mode.NF, context);
        expr2 = expr2.normalize(NormalizeVisitor.Mode.NF, context);

        if (!expr1.equals(expr2)){
          errorMsg.append("\n").append(name);
          printArgs(subst1, arguments, errorMsg);
          errorMsg.append(" = ").append(expr1).append(" =/= ").append(expr2).append(" = ").append(arguments);
          printArgs(subst2, arguments, errorMsg);
       }
      }
    }, context.size() - numberOfVariables(arguments));

    if (errorMsg.length() != 0) {
      return new TypeCheckingError("Condition check failed: " + errorMsg.toString(), source, getNames(context));
    }
    return null;
  }

  public static TypeCheckingError checkCoverage(final Abstract.Function def, List<Binding> context, ElimTreeNode elimTree) {
    final StringBuilder incompleteCoverageMessage = new StringBuilder();
    if (!CoverageChecker.check(context, elimTree, context.size() - numberOfVariables(def.getArguments()), new CoverageChecker.CoverageCheckerMissingProcessor() {
      @Override
      public void process(List<Binding> missingContext, List<Expression> missing) {
        incompleteCoverageMessage.append("\n ").append(def.getName());
        printArgs(missing, def.getArguments(), incompleteCoverageMessage);
      }
    })) {
      return new TypeCheckingError("Coverage check failed for: " + incompleteCoverageMessage.toString(), def, getNames(context));
    } else {
      return null;
    }
  }

  private static void printArgs(List<Expression> subst1, List<? extends Abstract.Argument> arguments, StringBuilder errorMsg) {
    for (int i = 0, ii = 0; i < arguments.size(); i++) {
      if (arguments.get(i) instanceof TelescopeArgument) {
        for (String ignore : ((TelescopeArgument) arguments.get(i)).getNames()) {
          errorMsg.append(" ").append(arguments.get(i).getExplicit() ? "(" : "{");
          errorMsg.append(subst1.get(ii++));
          errorMsg.append(arguments.get(i).getExplicit() ? ")" : "}");
        }
      } else {
        errorMsg.append(" ").append(arguments.get(i).getExplicit() ? "(" : "{");
        errorMsg.append(subst1.get(ii++));
        errorMsg.append(arguments.get(i).getExplicit() ? ")" : "}");
      }
    }
  }
}
