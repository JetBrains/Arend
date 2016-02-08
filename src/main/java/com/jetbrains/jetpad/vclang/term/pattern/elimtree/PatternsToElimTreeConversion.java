package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toSubstitution;

public class PatternsToElimTreeConversion {

  public static abstract class Result {}

  public static class OKResult extends Result {
    public final ElimTreeNode elimTree;

    public OKResult(ElimTreeNode elimTree) {
      this.elimTree = elimTree;
    }
  }

  public static class EmptyReachableResult extends Result {
    public final Collection<Integer> reachable;

    public EmptyReachableResult(Collection<Integer> reachable) {
      this.reachable = reachable;
    }
  }

  public static Result convert(DependentLink eliminatingArgs, List<List<Pattern>> patterns, List<Expression> expressions, List<Abstract.Definition.Arrow> arrows) {
    MultiPatternsExpander.MultiElimTreeExpansionResult treeExpansionResult = MultiPatternsExpander.expandPatterns(eliminatingArgs, patterns);
    if (treeExpansionResult.branches.isEmpty())
      return new OKResult(treeExpansionResult.tree);

    Set<Integer> emptyReachable = new HashSet<>();
    for (MultiPatternsExpander.MultiBranch branch : treeExpansionResult.branches) {
      for (int i : branch.indices) {
        if (expressions.get(i) == null) {
          emptyReachable.add(i);
        }
      }

      if (expressions.get(branch.indices.get(0)) == null) {
        continue;
      }

      branch.leaf.setArrow(arrows.get(branch.indices.get(0)));
      List<Pattern> curPatterns = patterns.get(branch.indices.get(0));
      Substitution subst = new Substitution();
      for (int i = 0; i < curPatterns.size(); i++) {
        Substitution curSubst = toSubstitution(curPatterns.get(i).getParameters(),
            ((Pattern.MatchOKResult)curPatterns.get(i).match(branch.expressions.get(i), false)).expressions);
        for (Binding binding : curSubst.getDomain()) {
          subst.add(binding, curSubst.get(binding));
        }
      }
      branch.leaf.setExpression(expressions.get(branch.indices.get(0)).subst(subst));
    }

    if (!emptyReachable.isEmpty()) {
      return new EmptyReachableResult(emptyReachable);
    } else {
      return new OKResult(treeExpansionResult.tree);
    }
  }
}
