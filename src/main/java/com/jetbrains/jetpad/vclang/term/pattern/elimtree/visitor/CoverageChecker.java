package com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.definition.UniverseOld;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CoverageChecker implements ElimTreeNodeVisitor<Substitution, Boolean> {
  public interface CoverageCheckerMissingProcessor {
    void process(Substitution argsSubst);
  }

  private final CoverageCheckerMissingProcessor myProcessor;
  private final Expression myResultType;

  private CoverageChecker(CoverageCheckerMissingProcessor processor, Expression resultType) {
    myProcessor = processor;
    myResultType = resultType;
  }

  public static boolean check(ElimTreeNode tree, Substitution argsSubst, CoverageCheckerMissingProcessor processor, Expression resultType) {
    return tree.accept(new CoverageChecker(processor, resultType), argsSubst);
  }

  @Override
  public Boolean visitBranch(BranchElimTreeNode branchNode, Substitution argsSubst) {
    List<Expression> parameters = new ArrayList<>();
    DefCallExpression ftype = (DefCallExpression) branchNode.getReference().getType().normalize(NormalizeVisitor.Mode.WHNF).getFunction(parameters);
    Collections.reverse(parameters);

    boolean result = true;
    for (ConCallExpression conCall : ((DataDefinition)ftype.getDefinition()).getMatchedConstructors(parameters)) {
      if (((UniverseExpression) myResultType.getType()).getUniverse().lessOrEquals(new UniverseOld.Type(0, UniverseOld.Type.PROP))) {
        if (Prelude.isTruncP(conCall.getDefinition())) {
          continue;
        }
      } else if (((UniverseExpression) myResultType.getType()).getUniverse().lessOrEquals(new UniverseOld.Type(0, UniverseOld.Type.SET))) {
        if (Prelude.isTruncS(conCall.getDefinition())) {
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
  public Boolean visitLeaf(LeafElimTreeNode leafNode, Substitution argsSubst) {
    return true;
  }

  @Override
  public Boolean visitEmpty(EmptyElimTreeNode emptyNode, Substitution argsSubst) {
    List<Binding> tailContext = new ArrayList<>();
    for (Binding binding : argsSubst.getDomain()) {
      if (argsSubst.get(binding) instanceof ReferenceExpression) {
        tailContext.add(((ReferenceExpression) argsSubst.get(binding)).getBinding());
      }
    }
    return checkEmptyContext(tailContext, argsSubst);
  }

  public boolean checkEmptyContext(List<Binding> tailContext, Substitution argsSubst) {
    if (tailContext.isEmpty()) {
      myProcessor.process(argsSubst);
      return false;
    }

    List<Expression> parameters = new ArrayList<>();
    Expression ftype = tailContext.get(0).getType().normalize(NormalizeVisitor.Mode.WHNF).getFunction(parameters);
    Collections.reverse(parameters);

    if (!(ftype instanceof DefCallExpression && ((DefCallExpression) ftype).getDefinition() instanceof DataDefinition)) {
      return checkEmptyContext(new ArrayList<>(tailContext.subList(1, tailContext.size())), argsSubst);
    }
    List<ConCallExpression> validConCalls = ((DataDefinition) ((DefCallExpression) ftype).getDefinition()).getMatchedConstructors(parameters);
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
