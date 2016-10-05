package com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Referable;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;

import java.util.ArrayList;
import java.util.List;

public class CoverageChecker implements ElimTreeNodeVisitor<ExprSubstitution, Boolean> {
  public interface CoverageCheckerMissingProcessor {
    void process(ExprSubstitution argsSubst);
  }

  private final CoverageCheckerMissingProcessor myProcessor;
  private final Expression myResultType;

  private CoverageChecker(CoverageCheckerMissingProcessor processor, Expression resultType) {
    myProcessor = processor;
    myResultType = resultType;
  }

  public static boolean check(ElimTreeNode tree, ExprSubstitution argsSubst, CoverageCheckerMissingProcessor processor, Expression resultType) {
    return tree.accept(new CoverageChecker(processor, resultType), argsSubst);
  }

  @Override
  public Boolean visitBranch(BranchElimTreeNode branchNode, ExprSubstitution argsSubst) {
    Expression type = branchNode.getReference().getType().normalize(NormalizeVisitor.Mode.WHNF);

    boolean result = true;
    for (ConCallExpression conCall : type.toDataCall().getDefinition().getMatchedConstructors(type.toDataCall())) {
      if (myResultType.getType().isLessOrEquals(Sort.PROP)) {
        if (conCall.getDefinition() == Prelude.PROP_TRUNC_PATH_CON) {
          continue;
        }
      } else if (myResultType.getType().isLessOrEquals(Sort.SET)) {
        if (conCall.getDefinition() == Prelude.SET_TRUNC_PATH_CON) {
          continue;
        }
      }
      if (branchNode.getClause(conCall.getDefinition()) == null) {
        branchNode.addClause(conCall.getDefinition(), null);
      }
      Clause clause = branchNode.getClause(conCall.getDefinition());
      result &= clause.getChild().accept(this, clause.getSubst().compose(argsSubst));
    }

    return result;
  }

  @Override
  public Boolean visitLeaf(LeafElimTreeNode leafNode, ExprSubstitution argsSubst) {
    return true;
  }

  @Override
  public Boolean visitEmpty(EmptyElimTreeNode emptyNode, ExprSubstitution argsSubst) {
    List<Binding> tailContext = new ArrayList<>();
    for (Referable binding : argsSubst.getDomain()) {
      ReferenceExpression ref = argsSubst.get(binding).toReference();
      if (ref != null) {
        tailContext.add(ref.getBinding());
      }
    }
    return checkEmptyContext(tailContext, argsSubst);
  }

  public boolean checkEmptyContext(List<Binding> tailContext, ExprSubstitution argsSubst) {
    if (tailContext.isEmpty()) {
      myProcessor.process(argsSubst);
      return false;
    }

    DataCallExpression dType = tailContext.get(0).getType().normalize(NormalizeVisitor.Mode.WHNF).toDataCall();
    if (dType == null) {
      return checkEmptyContext(new ArrayList<>(tailContext.subList(1, tailContext.size())), argsSubst);
    }

    List<ConCallExpression> validConCalls = dType.getDefinition().getMatchedConstructors(dType);
    if (validConCalls == null) {
      return checkEmptyContext(new ArrayList<>(tailContext.subList(1, tailContext.size())), argsSubst);
    }

    BranchElimTreeNode fakeBranch = new BranchElimTreeNode(tailContext.get(0), tailContext.subList(1, tailContext.size()));
    for (ConCallExpression conCall : validConCalls) {
      ConstructorClause clause = fakeBranch.addClause(conCall.getDefinition(), null);
      if (!checkEmptyContext(clause.getTailBindings(), clause.getSubst().compose(argsSubst)))
        return false;
    }
    return true;
  }
}
