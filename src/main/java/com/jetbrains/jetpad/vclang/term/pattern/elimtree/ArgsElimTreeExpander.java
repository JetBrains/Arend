package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeExpander.Branch;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.pattern.Utils.expandPatternSubstitute;
import static com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeExpander.recalcIndicies;

public class ArgsElimTreeExpander {
  public static class ArgsBranch {
    public final LeafElimTreeNode<List<Integer>> leaf;
    public final List<Expression> expressions;
    public final List<Binding> context;

    private ArgsBranch(LeafElimTreeNode<List<Integer>> leaf, List<Expression> expressions, List<Binding> context) {
      this.leaf = leaf;
      this.expressions = expressions;
      this.context = context;}
  }

  public static class ArgsExpansionResult {
    public final ElimTreeNode<List<Integer>> tree;
    public final List<ArgsBranch> branches;

    private ArgsExpansionResult(ElimTreeNode<List<Integer>> tree, List<ArgsBranch> branches) {
      this.tree = tree;
      this.branches = branches;
    }
  }

  private final List<Binding> myLocalContext;
  private final int myOldContextSize;

  private List<List<Pattern>> myNestedPatterns;
  private int myIndex;

  private final List<Branch> currentBranches = new ArrayList<>();

  private List<ArgsBranch> resultBranches;
  private ElimTreeNode<List<Integer>> resultTree;

  public ArgsElimTreeExpander(List<Binding> localContext) {
    this.myLocalContext = localContext;
    this.myOldContextSize = localContext.size();
  }

  public ArgsExpansionResult expandElimTree(List<Expression> types, List<List<Pattern>> patterns, int numPatterns) {
    return expandElimTree(0, types, patterns, numPatterns);
  }

  public ArgsExpansionResult expandElimTree(int index, List<Expression> types, List<List<Pattern>> patterns, int numPatterns) {
    if (numPatterns == 0)
      return null;
    myIndex = index;
    myNestedPatterns = patterns;
    List<Integer> valid = new ArrayList<>();
    for (int i = 0; i < numPatterns; i++)
      valid.add(i);
    resultBranches = new ArrayList<>();
    LeafElimTreeNode<List<Integer>> init = new LeafElimTreeNode<>(valid);
    resultTree = init;

    expandElimTreeRecurse(types, init);
    return new ArgsExpansionResult(resultTree, resultBranches);
  }

  private void expandElimTreeRecurse(List<Expression> types, LeafElimTreeNode<List<Integer>> leaf) {
    if (types.isEmpty()) {
      List<Expression> expressions = new ArrayList<>(currentBranches.size());
      for (int lift = 0, i = currentBranches.size() - 1; i >= 0; lift += currentBranches.get(i).context.size(), --i) {
        expressions.add(currentBranches.get(i).expression.liftIndex(0, lift));
      }
      Collections.reverse(expressions);

      resultBranches.add(new ArgsBranch(leaf, expressions, new ArrayList<>(myLocalContext.subList(myOldContextSize, myLocalContext.size()))));
      return;
    }

    List<Pattern> patterns = new ArrayList<>();
    for (int i : leaf.getValue())
      patterns.add(myNestedPatterns.get(myNestedPatterns.size() - types.size()).get(i));
    final boolean isExplicit = myNestedPatterns.get(myNestedPatterns.size() - types.size()).isEmpty() || myNestedPatterns.get(myNestedPatterns.size() - types.size()).get(0).getExplicit();
    ElimTreeExpander.ExpansionResult nestedResult = new ElimTreeExpander(myLocalContext).expandElimTree(myIndex + types.size() - 1, patterns, types.get(0), isExplicit);
    resultTree = leaf.replaceWith(resultTree, nestedResult.result);

    for (Branch branch : nestedResult.branches) {
      try (Utils.MultiContextSaver ignore = new Utils.MultiContextSaver(myLocalContext, currentBranches)) {
        myLocalContext.addAll(branch.context);
        currentBranches.add(branch);
        branch.leaf.setValue(recalcIndicies(leaf.getValue(), branch.leaf.getValue()));
        expandElimTreeRecurse(substituteInTypes(types, branch), branch.leaf);
      }
    }
  }

  private List<Expression> substituteInTypes(List<Expression> types, Branch branch) {
    List<Expression> newTypes = new ArrayList<>();
    for (int i = 1; i < types.size(); i++)
      newTypes.add(expandPatternSubstitute(branch.context.size(), i - 1, branch.expression, types.get(i)));
    return newTypes;
  }
}
