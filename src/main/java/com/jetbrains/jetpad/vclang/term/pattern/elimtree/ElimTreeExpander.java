package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.definition.TypedBinding;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.AnyConstructorPattern;
import com.jetbrains.jetpad.vclang.term.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.term.pattern.NamePattern;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ArgsElimTreeExpander.ArgsBranch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Apps;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Index;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.getTypes;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.toPatterns;

class ElimTreeExpander {
  static class Branch {
    final LeafElimTreeNode leaf;
    final Expression expression;
    final List<Binding> context;
    final List<Integer> indices;

    private Branch(Expression expression, List<Binding> context, LeafElimTreeNode value, List<Integer> indices) {
      this.leaf = value;
      this.expression = expression;
      this.context = context;
      this.indices = indices;
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

  private final List<Binding> myLocalContext;

  ElimTreeExpander(List<Binding> localContext) {
    this.myLocalContext = localContext;
  }

  ExpansionResult expandElimTree(int index, List<Pattern> patterns, Expression type) {
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
      return new ExpansionResult(leaf, Collections.singletonList(new Branch(Index(0),
          Collections.<Binding>singletonList(new TypedBinding((String) null, type)), leaf, namePatternIdxs)));
    }

    List<Expression> parameters = new ArrayList<>();
    Expression ftype = type.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext).getFunction(parameters);
    Collections.reverse(parameters);
    List<ConCallExpression> validConstructors = ((DataDefinition) ((DefCallExpression) ftype).getDefinition()).getConstructors(parameters, myLocalContext);

    BranchElimTreeNode resultTree = new BranchElimTreeNode(index);
    List<Branch> resultBranches = new ArrayList<>();

    for (ConCallExpression conCall : validConstructors) {
      List<TypeArgument> constructorArgs = new ArrayList<>();
      splitArguments(conCall.getType(myLocalContext), constructorArgs, myLocalContext);
      MatchingPatterns matching = new MatchingPatterns(patterns, conCall.getDefinition(), constructorArgs);

      ArgsElimTreeExpander.ArgsExpansionResult nestedResult = new ArgsElimTreeExpander(myLocalContext).expandElimTree(
          index, getTypes(constructorArgs), matching.nestedPatterns);
      if (nestedResult.tree == EmptyElimTreeNode.getInstance())
        continue;

      resultTree.addClause(conCall.getDefinition(), nestedResult.tree);
      for (ArgsBranch branch : nestedResult.branches) {
        Expression expr = conCall.liftIndex(0, branch.context.size());
        expr = Apps(expr, branch.expressions.toArray(new Expression[branch.expressions.size()]));
        resultBranches.add(new Branch(expr, branch.context, branch.leaf, recalcIndices(matching.indices, branch.indices)));
      }
    }

    return new ExpansionResult(resultTree, resultBranches);
  }

  private static class MatchingPatterns {
    private final List<Integer> indices = new ArrayList<>();
    private final List<List<Pattern>> nestedPatterns = new ArrayList<>();

    private MatchingPatterns(List<Pattern> patterns, Constructor constructor, List<TypeArgument> constructorArgs) {
      List<Pattern> anyPatterns = new ArrayList<>(Collections.<Pattern>nCopies(constructorArgs.size(), new NamePattern(null)));

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

  static ArrayList<Integer> recalcIndices(List<Integer> old, List<Integer> newValid) {
    ArrayList<Integer> indices = new ArrayList<>();
    for (int i : newValid)
      indices.add(old.get(i));
    return indices;
  }
}
