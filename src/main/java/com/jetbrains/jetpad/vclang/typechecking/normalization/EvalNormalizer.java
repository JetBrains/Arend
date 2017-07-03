package com.jetbrains.jetpad.vclang.typechecking.normalization;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
      result = new LamExpression(fun.getResultSort(), link, result);
    }
    result = result.subst(subst);
    for (; i < arguments.size(); i++) {
      result = new AppExpression(result, arguments.get(i));
    }
    return result.normalize(mode);
  }

  @Override
  public Expression normalize(LetClause fun, LevelSubstitution polySubst, DependentLink params, List<? extends Expression> paramArgs, List<? extends Expression> arguments, NormalizeVisitor.Mode mode) {
    List<Expression> matchedArguments = new ArrayList<>(arguments);
    LeafElimTreeNode leaf = fun.getElimTree().subst(new ExprSubstitution(), polySubst).match(matchedArguments);
    if (leaf == null) {
      return null;
    }

    ExprSubstitution subst = leaf.matchedToSubst(matchedArguments);
    for (Expression argument : paramArgs) {
      subst.add(params, argument);
      params = params.getNext();
    }
    return leaf.getExpression().subst(subst).normalize(mode);
  }

  @Override
  public Expression normalize(LetExpression expression) {
    Expression term = expression.getExpression().normalize(NormalizeVisitor.Mode.NF);
    Set<Binding> bindings = new HashSet<>(expression.getClauses());
    return term.findBinding(bindings) != null ? new LetExpression(expression.getClauses(), term) : term;
  }
}
