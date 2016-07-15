package com.jetbrains.jetpad.vclang.typechecking.normalization;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Function;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class EvalNormalizer implements Normalizer {
  @Override
  public Expression normalize(LamExpression fun, List<? extends Expression> arguments, List<? extends EnumSet<AppExpression.Flag>> flags, NormalizeVisitor.Mode mode) {
    int i = 0;
    DependentLink link = fun.getParameters();
    ExprSubstitution subst = new ExprSubstitution();
    while (link.hasNext() && i < arguments.size()) {
      subst.add(link, arguments.get(i++));
      link = link.getNext();
    }

    Expression result = fun.getBody();
    if (link.hasNext()) {
      result = Lam(link, result);
    }
    result = result.subst(subst);
    if (result != fun.getBody()) {
      result = result.addArguments(arguments.subList(i, arguments.size()), flags.subList(i, flags.size()));
    } else {
      result = Apps(result, arguments.subList(i, arguments.size()), flags.subList(i, flags.size()));
    }
    return result.normalize(mode);
  }

  @Override
  public Expression normalize(Function fun, LevelSubstitution polySubst, DependentLink params, List<? extends Expression> paramArgs, List<? extends Expression> arguments, List<? extends Expression> otherArguments, List<? extends EnumSet<AppExpression.Flag>> otherFlags, NormalizeVisitor.Mode mode) {
    assert fun.getNumberOfRequiredArguments() == arguments.size();

    if (fun instanceof FunctionDefinition && Prelude.isCoe((FunctionDefinition) fun)) {
      Expression result = null;

      Binding binding = new TypedBinding("i", DataCall(Preprelude.INTERVAL));
      Expression normExpr = Apps(arguments.get(0), Reference(binding)).normalize(NormalizeVisitor.Mode.NF);
      if (!normExpr.findBinding(binding)) {
        result = arguments.get(1);
      } else {
        FunCallExpression mbIsoFun = normExpr.getFunction().toFunCall();
        List<? extends Expression> mbIsoArgs = normExpr.getArguments();
        if (mbIsoFun != null && Prelude.isIso(mbIsoFun.getDefinition()) && mbIsoArgs.size() == 7) {
          boolean noFreeVar = true;
          for (int i = 0; i < mbIsoArgs.size() - 1; i++) {
            if (mbIsoArgs.get(i).findBinding(binding)) {
              noFreeVar = false;
              break;
            }
          }
          if (noFreeVar) {
            ConCallExpression normedPtCon = arguments.get(2).normalize(NormalizeVisitor.Mode.NF).toConCall();
            if (normedPtCon != null && normedPtCon.getDefinition() == Preprelude.RIGHT) {
              result = Apps(mbIsoArgs.get(2), arguments.get(1));
            }
          }
        }
      }

      if (result != null) {
        return Apps(result.subst(polySubst), otherArguments, otherFlags).normalize(mode);
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
    return Apps(leaf.getExpression().subst(polySubst).subst(subst), otherArguments, otherFlags).normalize(mode);
  }

  @Override
  public Expression normalize(LetExpression expression) {
    Expression term = expression.getExpression().normalize(NormalizeVisitor.Mode.NF);
    Set<Binding> bindings = new HashSet<>();
    for (LetClause clause : expression.getClauses()) {
      bindings.add(clause);
    }
    return term.findBinding(bindings) ? Let(expression.getClauses(), term) : term;
  }
}
