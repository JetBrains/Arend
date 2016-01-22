package com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CoverageChecker implements ElimTreeNodeVisitor<List<Expression>, Boolean> {
  public interface CoverageCheckerMissingProcessor {
    void process(List<Expression> expressions);
  }

  private final CoverageCheckerMissingProcessor myProcessor;

  private CoverageChecker(CoverageCheckerMissingProcessor processor) {
    myProcessor = processor;
  }

  public static boolean check(ElimTreeNode tree, List<Expression> expressions, CoverageCheckerMissingProcessor processor) {
    return tree.accept(new CoverageChecker(processor), expressions);
  }

  @Override
  public Boolean visitBranch(BranchElimTreeNode branchNode, List<Expression> expressions) {
    List<Expression> parameters = new ArrayList<>();
    DefCallExpression ftype = (DefCallExpression) branchNode.getReference().getType().normalize(NormalizeVisitor.Mode.WHNF).getFunction(parameters);
    Collections.reverse(parameters);

    boolean result = true;
    for (ConCallExpression conCall : ((DataDefinition)ftype.getDefinition()).getMatchedConstructors(parameters)) {
      if (branchNode.getClause(conCall.getDefinition()) == null) {
        branchNode.addClause(conCall.getDefinition());
      }
      ConstructorClause clause = branchNode.getClause(conCall.getDefinition());
      result &= clause.getChild().accept(this, clause.getSubst().substExprs(expressions));
    }

    return result;
  }

  @Override
  public Boolean visitLeaf(LeafElimTreeNode leafNode, List<Expression> expressions) {
    return true;
  }

  @Override
  public Boolean visitEmpty(EmptyElimTreeNode emptyNode, List<Expression> expressions) {
    List<Binding> tailContext = new ArrayList<>();
    for (Expression expression : expressions) {
      if (expression instanceof ReferenceExpression) {
        tailContext.add(((ReferenceExpression) expression).getBinding());
      }
    }
    return checkEmptyContext(tailContext, expressions);
  }

  public boolean checkEmptyContext(List<Binding> tailContext, List<Expression> expressions) {
    if (tailContext.isEmpty()) {
      myProcessor.process(expressions);
      return false;
    }

    List<Expression> parameters = new ArrayList<>();
    Expression ftype = tailContext.get(0).getType().normalize(NormalizeVisitor.Mode.WHNF).getFunction(parameters);
    Collections.reverse(parameters);

    if (!(ftype instanceof DefCallExpression && ((DefCallExpression) ftype).getDefinition() instanceof DataDefinition)) {
      return checkEmptyContext(new ArrayList<>(tailContext.subList(1, tailContext.size())), expressions);
    }
    List<ConCallExpression> validConCalls = ((DataDefinition) ((DefCallExpression) ftype).getDefinition()).getMatchedConstructors(parameters);
    if (validConCalls == null) {
      return checkEmptyContext(new ArrayList<>(tailContext.subList(1, tailContext.size())), expressions);
    }

    BranchElimTreeNode fakeBranch = new BranchElimTreeNode(tailContext.get(0), tailContext.subList(1, tailContext.size()));
    for (ConCallExpression conCall : validConCalls) {
      ConstructorClause clause = fakeBranch.addClause(conCall.getDefinition());
      if (!checkEmptyContext(clause.getTailBindings(), clause.getSubst().substExprs(expressions)))
        return false;
    }
    return true;
  }
}
