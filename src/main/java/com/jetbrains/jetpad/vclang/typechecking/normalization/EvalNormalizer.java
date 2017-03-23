package com.jetbrains.jetpad.vclang.typechecking.normalization;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Function;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Prelude;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EvalNormalizer implements Normalizer {
  @Override
  public Expression normalize(LamExpression fun, List<? extends Expression> arguments, NormalizeVisitor.Mode mode) {
    int i = 0;
    SingleDependentLink link = fun.getParameters();
    ExprSubstitution subst = new ExprSubstitution();
    while (link.hasNext() && i < arguments.size()) {
      subst.add(link, arguments.get(i++));
      link = link.getNext();
    }

    Expression result = fun.getBody();
    if (link.hasNext()) {
      result = new LamExpression(fun.getPLevel(), link, result);
    }
    result = result.subst(subst);
    if (result != fun.getBody()) {
      result = result.addArguments(arguments.subList(i, arguments.size()));
    } else {
      result = ExpressionFactory.Apps(result, arguments.subList(i, arguments.size()));
    }
    return result.normalize(mode);
  }

  @Override
  public Expression normalize(Function fun, LevelSubstitution polySubst, DependentLink params, List<? extends Expression> paramArgs, List<? extends Expression> arguments, NormalizeVisitor.Mode mode) {
    if (fun == Prelude.COERCE) {
      Expression result = null;

      Binding binding = new TypedBinding("i", ExpressionFactory.Interval());
      Expression normExpr = arguments.get(0).addArgument(new ReferenceExpression(binding)).normalize(NormalizeVisitor.Mode.NF);
      if (!normExpr.findBinding(binding)) {
        result = arguments.get(1);
      } else {
        if (normExpr.toFunCall() != null && normExpr.toFunCall().getDefinition() == Prelude.ISO) {
          List<? extends Expression> isoArgs = normExpr.toFunCall().getDefCallArguments();
          boolean noFreeVar = true;
          for (int i = 0; i < isoArgs.size() - 1; i++) {
            if (isoArgs.get(i).findBinding(binding)) {
              noFreeVar = false;
              break;
            }
          }
          if (noFreeVar) {
            ConCallExpression normedPtCon = arguments.get(2).normalize(NormalizeVisitor.Mode.NF).toConCall();
            if (normedPtCon != null && normedPtCon.getDefinition() == Prelude.RIGHT) {
              result = isoArgs.get(2).addArgument(arguments.get(1));
            }
          }
        }
      }

      if (result != null) {
        return result.subst(polySubst).normalize(mode);
      }
    }

    List<Expression> matchedArguments = new ArrayList<>(arguments);
    LeafElimTreeNode leaf = fun.getElimTree().match(matchedArguments);
    if (leaf == null) {
      return null;
    }

    ExprSubstitution subst = leaf.matchedToSubst(matchedArguments);
    for (Expression argument : paramArgs) {
      subst.add(params, argument);
      params = params.getNext();
    }
    return leaf.getExpression().subst(subst, polySubst).normalize(mode);
  }

  @Override
  public Expression normalize(LetExpression expression) {
    Expression term = expression.getExpression().normalize(NormalizeVisitor.Mode.NF);
    Set<Binding> bindings = expression.getClauses().stream().collect(Collectors.toSet());
    return term.findBinding(bindings) != null ? new LetExpression(expression.getClauses(), term) : term;
  }
}
