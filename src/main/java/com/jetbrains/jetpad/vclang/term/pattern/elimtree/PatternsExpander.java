package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.AnyConstructorPattern;
import com.jetbrains.jetpad.vclang.term.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.term.pattern.NamePattern;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.MultiPatternsExpander.MultiBranch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toContext;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Apps;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Reference;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.toPatterns;

class PatternsExpander {
  static class Branch {
    final LeafElimTreeNode leaf;
    final Expression expression;
    final List<Integer> indices;
    final List<Binding> newContext;

    private Branch(Expression expression, LeafElimTreeNode leaf, List<Integer> indices, List<Binding> newContext) {
      this.leaf = leaf;
      this.expression = expression;
      this.indices = indices;
      this.newContext = newContext;
    }
  }

  static class ExpansionResult {
    final ElimTreeNode result;
    final List<Branch> branches;

    private ExpansionResult(ElimTreeNode result, List<Branch> branches) {
      this.result = result;
      this.branches = branches;
    }
  }

  ExpansionResult expandPatterns(List<Pattern> patterns, Binding binding, List<Binding> context) {
    List<Integer> namePatternIdxs = new ArrayList<>();
    boolean hasConstructorPattern = false;
    for (int i = 0; i < patterns.size(); i++) {
      if (patterns.get(i) instanceof ConstructorPattern || patterns.get(i) instanceof AnyConstructorPattern) {
        hasConstructorPattern = true;
      } else if (patterns.get(i) instanceof NamePattern) {
        namePatternIdxs.add(i);
      }
    }

    if (!hasConstructorPattern) {
      LeafElimTreeNode leaf = new LeafElimTreeNode();
      return new ExpansionResult(leaf, Collections.singletonList(new Branch(Reference(binding), leaf, namePatternIdxs, context)));
    }

    List<Expression> parameters = new ArrayList<>();
    Expression ftype = binding.getType().normalize(NormalizeVisitor.Mode.WHNF).getFunction(parameters);
    Collections.reverse(parameters);
    List<ConCallExpression> validConstructors = ((DataDefinition) ((DefCallExpression) ftype).getDefinition()).getMatchedConstructors(parameters);

    BranchElimTreeNode resultTree = new BranchElimTreeNode(binding, context);
    List<Branch> resultBranches = new ArrayList<>();

    for (ConCallExpression conCall : validConstructors) {
      ConstructorClause clause = resultTree.addClause(conCall.getDefinition());
      MatchingPatterns matching = new MatchingPatterns(patterns, conCall.getDefinition(), context.size());
      MultiPatternsExpander.MultiElimTreeExpansionResult nestedResult = MultiPatternsExpander.expandPatterns(
        toContext(clause.getParameters()), matching.nestedPatterns, clause.getTailBindings()
      );
      clause.setChild(nestedResult.tree);
      for (MultiBranch branch : nestedResult.branches) {
        Expression expr = Apps(conCall, branch.expressions.toArray(new Expression[branch.expressions.size()]));
        resultBranches.add(new Branch(expr, branch.leaf, recalcIndices(matching.indices, branch.indices), branch.newContext));
      }
    }

    return new ExpansionResult(resultTree, resultBranches);
  }

  private static class MatchingPatterns {
    private final List<Integer> indices = new ArrayList<>();
    private final List<List<Pattern>> nestedPatterns = new ArrayList<>();

    private MatchingPatterns(List<Pattern> patterns, Constructor constructor, int numConstructorArgs) {
      List<Pattern> anyPatterns = new ArrayList<>(Collections.<Pattern>nCopies(numConstructorArgs, new NamePattern(EmptyDependentLink.getInstance())));

      for (int j = 0; j < patterns.size(); j++) {
        if (patterns.get(j) instanceof NamePattern || patterns.get(j) instanceof AnyConstructorPattern) {
          indices.add(j);
          nestedPatterns.add(anyPatterns);
        } else if (patterns.get(j) instanceof ConstructorPattern &&
            ((ConstructorPattern) patterns.get(j)).getConstructor() == constructor) {
          indices.add(j);
          nestedPatterns.add(toPatterns(((ConstructorPattern) patterns.get(j)).getArguments()));
        }
      }
    }
  }

  static List<Integer> recalcIndices(List<Integer> old, List<Integer> newValid) {
    List<Integer> indices = new ArrayList<>();
    for (int i : newValid)
      indices.add(old.get(i));
    return indices;
  }
}
