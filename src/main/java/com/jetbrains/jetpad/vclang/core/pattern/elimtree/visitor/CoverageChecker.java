package com.jetbrains.jetpad.vclang.core.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.definition.Referable;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.sort.SortMax;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.Prelude;

import java.util.ArrayList;
import java.util.List;

public class CoverageChecker implements ElimTreeNodeVisitor<ExprSubstitution, Boolean> {
  public interface CoverageCheckerMissingProcessor {
    void process(ExprSubstitution argsSubst);
  }

  private final CoverageCheckerMissingProcessor myProcessor;
  private final Type myResultType;

  private CoverageChecker(CoverageCheckerMissingProcessor processor, Type resultType) {
    myProcessor = processor;
    myResultType = resultType;
  }

  public static boolean check(ElimTreeNode tree, ExprSubstitution argsSubst, CoverageCheckerMissingProcessor processor, Type resultType) {
    return tree.accept(new CoverageChecker(processor, resultType), argsSubst);
  }

  @Override
  public Boolean visitBranch(BranchElimTreeNode branchNode, ExprSubstitution argsSubst) {
    Expression type = branchNode.getReference().getType().normalize(NormalizeVisitor.Mode.WHNF).toExpression();

    boolean result = true;
    for (ConCallExpression conCall : type.toDataCall().getDefinition().getMatchedConstructors(type.toDataCall())) {
      if (myResultType.toExpression() != null) {
        SortMax sorts = myResultType.toExpression().getType().toSorts();
        if (sorts != null) {
          if (sorts.isLessOrEquals(Sort.PROP)) {
            if (conCall.getDefinition() == Prelude.PROP_TRUNC_PATH_CON ||
              conCall.getDefinition() == Prelude.SET_TRUNC_PATH_CON) {
              continue;
            }
          } else if (sorts.isLessOrEquals(Sort.SET)) {
            if (conCall.getDefinition() == Prelude.SET_TRUNC_PATH_CON) {
              continue;
            }
          }
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

  public boolean checkEmptyContext(List<? extends Binding> tailContext, ExprSubstitution argsSubst) {
    if (tailContext.isEmpty()) {
      myProcessor.process(argsSubst);
      return false;
    }

    Expression type = tailContext.get(0).getType().normalize(NormalizeVisitor.Mode.WHNF).toExpression();
    DataCallExpression dType = type == null ? null : type.toDataCall();
    if (dType == null) {
      return checkEmptyContext(new ArrayList<>(tailContext.subList(1, tailContext.size())), argsSubst);
    }

    List<ConCallExpression> validConCalls = dType.getDefinition().getMatchedConstructors(dType);
    if (validConCalls == null) {
      return checkEmptyContext(new ArrayList<>(tailContext.subList(1, tailContext.size())), argsSubst);
    }

    BranchElimTreeNode fakeBranch = new BranchElimTreeNode(tailContext.get(0), new ArrayList<>(tailContext.subList(1, tailContext.size())));
    for (ConCallExpression conCall : validConCalls) {
      ConstructorClause clause = fakeBranch.addClause(conCall.getDefinition(), null);
      if (!checkEmptyContext(clause.getTailBindings(), clause.getSubst().compose(argsSubst)))
        return false;
    }
    return true;
  }
}
