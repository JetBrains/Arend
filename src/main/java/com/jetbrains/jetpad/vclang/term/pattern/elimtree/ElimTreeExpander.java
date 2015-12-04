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
import com.jetbrains.jetpad.vclang.term.pattern.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ArgsElimTreeExpander.ArgsBranch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.getTypes;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;

class ElimTreeExpander {
  static class Branch {
    public final LeafElimTreeNode leaf;
    public final Expression expression;
    public final List<Binding> context;
    public final List<Integer> indicies;

    private Branch(Expression expression, List<Binding> context, LeafElimTreeNode value, List<Integer> indicies) {
      this.leaf = value;
      this.expression = expression;
      this.context = context;
      this.indicies = indicies;
    }
  }

  static class ExpansionResult {
    public final ElimTreeNode result;
    public final List<Branch> branches;

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
      if (patterns.get(i) instanceof ConstructorPattern) {
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

      if (matching.indices.isEmpty()) {
        continue;
      }

      ArgsElimTreeExpander.ArgsExpansionResult nestedResult = new ArgsElimTreeExpander(myLocalContext).expandElimTree(
          index, getTypes(constructorArgs), matching.nestedPatterns);
      if (nestedResult.branches.isEmpty())
        continue;
      resultTree.addClause(conCall.getDefinition(), nestedResult.tree);

      for (ArgsBranch branch : nestedResult.branches) {
        Expression expr = conCall.liftIndex(0, branch.context.size());
        expr = Apps(expr, branch.expressions.toArray(new Expression[branch.expressions.size()]));
        resultBranches.add(new Branch(expr, branch.context, branch.leaf, recalcIndicies(matching.indices, branch.indicies)));
      }
    }

    if (resultBranches.isEmpty()) {
      return new ExpansionResult(EmptyElimTreeNode.getInstance(), Collections.<Branch>emptyList());
    } else {
      return new ExpansionResult(resultTree, resultBranches);
    }
  }

  private static class MatchingPatterns {
    private final List<Integer> indices = new ArrayList<>();
    private final List<List<Pattern>> nestedPatterns = new ArrayList<>();

    private MatchingPatterns(List<Pattern> patterns, Constructor constructor, List<TypeArgument> constructorArgs) {
      List<Pattern> anyPatterns = new ArrayList<>();
      for (TypeArgument constructorArg : constructorArgs) {
        anyPatterns.add(match(constructorArg.getExplicit(), null));
      }

      for (int j = 0; j < patterns.size(); j++) {
        if (patterns.get(j) instanceof NamePattern || patterns.get(j) instanceof AnyConstructorPattern) {
          indices.add(j);
          nestedPatterns.add(anyPatterns);
        } else if (patterns.get(j) instanceof ConstructorPattern &&
            ((ConstructorPattern) patterns.get(j)).getConstructor() == constructor) {
          indices.add(j);
          nestedPatterns.add(((ConstructorPattern) patterns.get(j)).getPatterns());
        }
      }
    }
  }

  static ArrayList<Integer> recalcIndicies(List<Integer> valid, List<Integer> newValid) {
    ArrayList<Integer> indicies = new ArrayList<>();
    for (int i : newValid)
      indicies.add(valid.get(i));
    return indicies;
  }
}
