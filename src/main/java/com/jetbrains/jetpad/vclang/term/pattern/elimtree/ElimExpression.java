package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.IndexExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Index;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.match;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.prettyPrintClause;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;

public class ElimExpression implements Abstract.ElimExpression {
  private static class Clause implements Abstract.Clause {
    private final Abstract.Expression myExpression;
    private final Abstract.Definition.Arrow myArrow;
    private final ConstructorPattern myPattern;
    private final Abstract.ElimExpression myElimExpression;

    private Clause(ConstructorPattern pattern, Abstract.Definition.Arrow arrow, Abstract.Expression expression, Abstract.ElimExpression elimExpression) {
      myExpression = expression;
      myArrow = arrow;
      myPattern = pattern;
      myElimExpression = elimExpression;
    }

    @Override
    public Abstract.Definition.Arrow getArrow() {
      return myArrow;
    }

    @Override
    public Abstract.Expression getExpression() {
      return myExpression;
    }

    @Override
    public List<? extends Abstract.Pattern> getPatterns() {
      return Collections.singletonList(myPattern);
    }

    @Override
    public void replacePatternWithConstructor(int index) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
      prettyPrintClause(myElimExpression, this, builder, names, 0);
    }
  }

  private final int myIndex;
  private final List<Abstract.Clause> myClauses;

  private ElimExpression(BranchElimTreeNode elimTreeNode) {
    myIndex = elimTreeNode.getIndex();
    myClauses = new ArrayList<>(elimTreeNode.getConstructorClauses().size());
    for (ConstructorClause constructorClause : elimTreeNode.getConstructorClauses()) {
      List<Pattern> patternArgs = new ArrayList<>();
      for (TypeArgument arg : splitArguments(constructorClause.getConstructor().getArguments())) {
        patternArgs.add(match(arg.getExplicit(), null));
      }
      final ConstructorPattern pattern = new ConstructorPattern(constructorClause.getConstructor(), patternArgs, true);

      myClauses.add(constructorClause.getChild().accept(new ElimTreeNodeVisitor<Void, Abstract.Clause>() {
        @Override
        public Abstract.Clause visitBranch(BranchElimTreeNode branchNode, Void params) {
          return new Clause(pattern, Abstract.Definition.Arrow.LEFT, new ElimExpression(branchNode), ElimExpression.this);
        }

        @Override
        public Abstract.Clause visitLeaf(LeafElimTreeNode leafNode, Void params) {
          return new Clause(pattern, leafNode.getArrow(), leafNode.getExpression(), ElimExpression.this);
        }

        @Override
        public Abstract.Clause visitEmpty(EmptyElimTreeNode emptyNode, Void params) {
          throw new IllegalStateException();
        }
      }, null));
    }
  }

  @Override
  public List<IndexExpression> getExpressions() {
    return Collections.singletonList(Index(myIndex));
  }

  @Override
  public List<Abstract.Clause> getClauses() {
    return myClauses;
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitElim(this, params);
  }

  @Override
  public void setWellTyped(List<Binding> context, Expression wellTyped) {}

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    accept(new PrettyPrintVisitor(builder, names, 0), prec);
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    prettyPrint(builder, new ArrayList<String>(), Definition.DEFAULT_PRECEDENCE.priority);
    return builder.toString();
  }

  public static Abstract.Expression toElimExpression(ElimTreeNode elimTree) {
    if (elimTree instanceof LeafElimTreeNode) {
      return ((LeafElimTreeNode) elimTree).getExpression();
    } else {
      return new ElimExpression((BranchElimTreeNode) elimTree);
    }
  }

  public static Abstract.Definition.Arrow toArrow(ElimTreeNode elimTree) {
    if (elimTree instanceof LeafElimTreeNode) {
      return ((LeafElimTreeNode) elimTree).getArrow();
    } else {
      return Abstract.Definition.Arrow.LEFT;
    }
  }
}
