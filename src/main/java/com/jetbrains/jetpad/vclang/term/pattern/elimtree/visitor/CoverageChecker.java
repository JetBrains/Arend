package com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Index;

public class CoverageChecker implements ElimTreeNodeVisitor<List<Expression>, Boolean> {
  public interface CoverageCheckerMissingProcessor {
    void process(List<Binding> context, List<Expression> missing);
  }

  private final List<Binding> myContext;
  private final CoverageCheckerMissingProcessor myProcessor;

  private CoverageChecker(List<Binding> context, CoverageCheckerMissingProcessor processor) {
    myContext = context;
    myProcessor = processor;
  }

  public static boolean check(List<Binding> context, ElimTreeNode tree, int argsStartIndex, CoverageCheckerMissingProcessor processor) {
    List<Expression> expressions = new ArrayList<>(context.size() - argsStartIndex);
       for (int i = context.size() - argsStartIndex - 1; i >= 0; i--) {
         expressions.add(Index(i));
    }
    return tree.accept(new CoverageChecker(context, processor), expressions);
  }

  @Override
  public Boolean visitBranch(BranchElimTreeNode branchNode, List<Expression> expressions) {
    Expression type = myContext.get(myContext.size() - 1 - branchNode.getIndex()).getType().liftIndex(0, branchNode.getIndex());
    List<Expression> parameters = new ArrayList<>();
    DefCallExpression ftype = (DefCallExpression) type.normalize(NormalizeVisitor.Mode.WHNF, myContext).getFunction(parameters);
    Collections.reverse(parameters);

    boolean result = true;
    for (ConCallExpression conCall : ((DataDefinition)ftype.getDefinition()).getMatchedConstructors(parameters)) {
      try (ConCallContextExpander expander = new ConCallContextExpander(branchNode.getIndex(), conCall, myContext)) {
        if (branchNode.getChild(conCall.getDefinition()) != null) {
          result &= branchNode.getChild(conCall.getDefinition()).accept(this, expander.substIn(expressions));
        } else {
          result &= checkEmptyContext(branchNode.getIndex() - 1, expander.substIn(expressions));
        }
      }
    }

    return result;
  }

  @Override
  public Boolean visitLeaf(LeafElimTreeNode leafNode, List<Expression> expressions) {
    return true;
  }

  @Override
  public Boolean visitEmpty(EmptyElimTreeNode emptyNode, List<Expression> expressions) {
    return checkEmptyContext(expressions.size() - 1, expressions);
  }

  public boolean checkEmptyContext(int index, List<Expression> expressions) {
    if (index < 0) {
      myProcessor.process(myContext, expressions);
      return false;
    }

    Expression type = myContext.get(myContext.size() - 1 - index).getType().liftIndex(0, index);
    List<Expression> parameters = new ArrayList<>();
    Expression ftype = type.normalize(NormalizeVisitor.Mode.WHNF, myContext).getFunction(parameters);
    Collections.reverse(parameters);

    if (!(ftype instanceof DefCallExpression && ((DefCallExpression) ftype).getDefinition() instanceof DataDefinition)) {
      return checkEmptyContext(index - 1, expressions);
    }
    List<ConCallExpression> validConCalls = ((DataDefinition) ((DefCallExpression) ftype).getDefinition()).getMatchedConstructors(parameters);
    if (validConCalls == null) {
      return checkEmptyContext(index - 1, expressions);
    }

    for (ConCallExpression conCall : validConCalls) {
      try (ConCallContextExpander expander = new ConCallContextExpander(index, conCall, myContext)) {
        if (!checkEmptyContext(index - 1, expander.substIn(expressions)))
          return false;
      }
    }
    return true;
  }
}
