package com.jetbrains.jetpad.vclang.core.pattern.elimtree;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    List<Integer> anyPatternIdxs = new ArrayList<>();
    boolean hasConstructorPattern = false;
    for (int i = 0; i < patterns.size(); i++) {
      if (patterns.get(i) instanceof ConstructorPattern || patterns.get(i) instanceof EmptyPattern) {
        hasConstructorPattern = true;
      } else if (patterns.get(i) instanceof NamePattern) {
        anyPatternIdxs.add(i);
      }
    }

    if (!hasConstructorPattern) {
      LeafElimTreeNode leaf = new LeafElimTreeNode();
      return new ExpansionResult(leaf, Collections.singletonList(new Branch(new ReferenceExpression(binding), leaf, anyPatternIdxs, context)));
    }

    DataCallExpression dType = binding.getType().getExpr().normalize(NormalizeVisitor.Mode.WHNF).toDataCall();
    List<ConCallExpression> validConstructors = dType.getMatchedConstructors();

    BranchElimTreeNode resultTree = new BranchElimTreeNode(binding, context);
    List<Branch> resultBranches = new ArrayList<>();

    for (ConCallExpression conCall : validConstructors) {
      MatchingPatterns matching = MatchingPatterns.findMatching(patterns, conCall.getDefinition());
      if (matching == null) {
        continue;
      }

      ConstructorClause clause = resultTree.addClause(conCall.getDefinition(), matching.names);
      MultiPatternsExpander.MultiElimTreeExpansionResult nestedResult = MultiPatternsExpander.expandPatterns(
        DependentLink.Helper.toContext(clause.getParameters()), matching.nestedPatterns, new ArrayList<>(clause.getTailBindings())
      );
      clause.setChild(nestedResult.tree);
      for (MultiPatternsExpander.MultiBranch branch : nestedResult.branches) {
        Expression expr = new ConCallExpression(conCall.getDefinition(), conCall.getSortArgument(), new ArrayList<>(conCall.getDataTypeArguments()), branch.expressions);
        resultBranches.add(new Branch(expr, branch.leaf, recalcIndices(matching.indices, branch.indices), branch.newContext));
      }
    }
    if (!anyPatternIdxs.isEmpty()) {
      OtherwiseClause clause = resultTree.addOtherwiseClause();
      LeafElimTreeNode leaf = new LeafElimTreeNode();
      clause.setChild(leaf);
      resultBranches.add(new Branch(new ReferenceExpression(binding), leaf, anyPatternIdxs, context));
    }

    return new ExpansionResult(resultTree, resultBranches);
  }

  private static class MatchingPatterns {
    private final List<Integer> indices;
    private final List<List<Pattern>> nestedPatterns;
    private final List<String> names;

    private MatchingPatterns(List<Integer> indices, List<List<Pattern>> nestedPatterns, List<String> names) {
      this.indices = indices;
      this.nestedPatterns = nestedPatterns;
      this.names = names;
    }

    private static MatchingPatterns findMatching(List<Pattern> patterns, Constructor constructor) {
      List<Pattern> anyPatterns = new ArrayList<>(Collections.<Pattern>nCopies(DependentLink.Helper.size(constructor.getParameters()), new NamePattern(EmptyDependentLink.getInstance())));
      List<Integer> indices = new ArrayList<>();
      List<List<Pattern>> nestedPatterns = new ArrayList<>();
      List<String> names = null;

      boolean hasConstructor = false;
      for (int j = 0; j < patterns.size(); j++) {
        if (patterns.get(j) instanceof NamePattern) {
          indices.add(j);
          nestedPatterns.add(anyPatterns);
        } else if (patterns.get(j) instanceof EmptyPattern) {
          indices.add(j);
          nestedPatterns.add(anyPatterns);
          hasConstructor = true;
        } else if (patterns.get(j) instanceof ConstructorPattern &&
            ((ConstructorPattern) patterns.get(j)).getConstructor() == constructor) {
          hasConstructor = true;
          indices.add(j);
          nestedPatterns.add(((ConstructorPattern) patterns.get(j)).getArguments());

          if (names == null) {
            names = new ArrayList<>(((ConstructorPattern) patterns.get(j)).getArguments().size());
            for (Pattern pattern : ((ConstructorPattern) patterns.get(j)).getArguments()) {
              if (pattern instanceof NamePattern) {
                names.add(((NamePattern) pattern).getName());
              } else {
                names.add(null);
              }
            }
          }
        }
      }

      if (!hasConstructor) {
        return null;
      }

      return new MatchingPatterns(indices, nestedPatterns, names);
    }
  }

  static List<Integer> recalcIndices(List<Integer> old, List<Integer> newValid) {
    return newValid.stream().map(old::get).collect(Collectors.toList());
  }
}
