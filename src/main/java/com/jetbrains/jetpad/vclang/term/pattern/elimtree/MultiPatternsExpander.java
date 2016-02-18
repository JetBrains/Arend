package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.PatternsExpander.Branch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toContext;
import static com.jetbrains.jetpad.vclang.term.pattern.elimtree.PatternsExpander.recalcIndices;

public class MultiPatternsExpander {
  public static class MultiBranch {
    public final LeafElimTreeNode leaf;
    public final List<Expression> expressions;
    public final List<Integer> indices;
    final List<Binding> newContext;

    private MultiBranch(LeafElimTreeNode leaf, List<Expression> expressions, List<Integer> indices, List<Binding> newContext) {
      this.leaf = leaf;
      this.expressions = expressions;
      this.indices = indices;
      this.newContext = newContext;
    }
  }

  public static class MultiElimTreeExpansionResult {
    public final ElimTreeNode tree;
    public final List<MultiBranch> branches;

    private MultiElimTreeExpansionResult(ElimTreeNode tree, List<MultiBranch> branches) {
      this.tree = tree;
      this.branches = branches;
    }
  }

  private final List<List<Pattern>> myNestedPatterns;
  private final List<MultiBranch> myResultBranches = new ArrayList<>();
  private final List<Expression> myExpressions = new ArrayList<>();

  private MultiPatternsExpander(List<List<Pattern>> patterns) {
    myNestedPatterns = patterns;
  }

  public static MultiElimTreeExpansionResult expandPatterns(DependentLink eliminatingArgs, List<List<Pattern>> patterns) {
    return expandPatterns(toContext(eliminatingArgs), patterns, new ArrayList<Binding>());
  }

  static MultiElimTreeExpansionResult expandPatterns(List<Binding> patternBindings, List<List<Pattern>> patterns, List<Binding> context) {
    return new MultiPatternsExpander(patterns).expandPatterns(patternBindings, context);
  }

  private MultiElimTreeExpansionResult expandPatterns(List<Binding> patternBindings, List<Binding> context) {
    if (myNestedPatterns.isEmpty())
      return new MultiElimTreeExpansionResult(EmptyElimTreeNode.getInstance(), Collections.<MultiBranch>emptyList());

    List<Integer> valid = new ArrayList<>();
    for (int i = 0; i < myNestedPatterns.size(); i++)
      valid.add(i);

    return new MultiElimTreeExpansionResult(expandPatternsRecurse(valid, patternBindings, context), myResultBranches);
  }


  private ElimTreeNode expandPatternsRecurse(List<Integer> valid, List<Binding> patternBindings, List<Binding> context) {
    if (patternBindings.isEmpty()) {
      LeafElimTreeNode leaf = new LeafElimTreeNode();
      myResultBranches.add(new MultiBranch(leaf, new ArrayList<>(myExpressions), valid, context));
      return leaf;
    }

    List<Pattern> patterns = new ArrayList<>();
    for (int i : valid)
      patterns.add(myNestedPatterns.get(i).get(myNestedPatterns.get(i).size() - patternBindings.size()));

    List<Binding> totalContext = new ArrayList<>(patternBindings.subList(1, patternBindings.size()));
    totalContext.addAll(context);
    PatternsExpander.ExpansionResult nestedResult = new PatternsExpander().expandPatterns(patterns, patternBindings.get(0), totalContext);

    ElimTreeNode resultTree = nestedResult.result;
    for (Branch branch : nestedResult.branches) {
      myExpressions.add(branch.expression);
      resultTree = branch.leaf.replaceWith(resultTree, expandPatternsRecurse(recalcIndices(valid, branch.indices),
              branch.newContext.subList(0, patternBindings.size() - 1),
              branch.newContext.subList(patternBindings.size() - 1, branch.newContext.size())
          ));
      myExpressions.remove(branch.expression);
    }
    return resultTree;
  }
}
