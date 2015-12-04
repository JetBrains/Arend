package com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.match;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;

public class CoverageChecker implements ElimTreeNodeVisitor<Boolean, List<List<Pattern>>> {

  private final List<Binding> myContext;

  public CoverageChecker(List<Binding> context) {
    myContext = context;
  }

  @Override
  public List<List<Pattern>> visitBranch(BranchElimTreeNode branchNode, Boolean isExplicit) {
    Expression type = myContext.get(myContext.size() - 1 - branchNode.getIndex()).getType().liftIndex(0, branchNode.getIndex());
    List<Expression> parameters = new ArrayList<>();
    DefCallExpression ftype = (DefCallExpression) type.normalize(NormalizeVisitor.Mode.WHNF, myContext).getFunction(parameters);
    Collections.reverse(parameters);

    // TODO: pattern construction is completely senseless
    List<List<Pattern>> result = new ArrayList<>();
    for (ConCallExpression conCall : ((DataDefinition)ftype.getDefinition()).getConstructors(parameters, myContext)) {
      try (ConCallContextExpander ignore = new ConCallContextExpander(branchNode.getIndex(), conCall, myContext)) {
        if (branchNode.getChild(conCall.getDefinition()) != null) {
          for (List<Pattern> nestedPatterns : branchNode.getChild(conCall.getDefinition()).accept(this, isExplicit)) {
            List<Pattern> my = nestedPatterns.subList(nestedPatterns.size() - 1 - branchNode.getIndex() - conCall.getParameters().size(), nestedPatterns.size() - 1 - branchNode.getIndex());
            ConstructorPattern newPattern = new ConstructorPattern(conCall.getDefinition(), new ArrayList<>(my), isExplicit);
            my.clear();
            my.add(newPattern);
            result.add(nestedPatterns);
          }
        } else {
          if (!checkEmptyContext(branchNode.getIndex() - 1)) {
            List<Pattern> args = new ArrayList<>();
            for (TypeArgument arg : splitArguments(conCall.getDefinition().getArguments())) {
              args.add(match(arg.getExplicit(), null));
            }
            List<Pattern> failed = new ArrayList<>(Collections.<Pattern>nCopies(branchNode.getIndex(), match(true, null)));
            failed.add(new ConstructorPattern(conCall.getDefinition(), args, isExplicit));
            result.add(failed);
          }
        }
      }
    }

    return result;
  }

  @Override
  public List<List<Pattern>> visitLeaf(LeafElimTreeNode leafNode, Boolean params) {
    return Collections.emptyList();
  }

  @Override
  public List<List<Pattern>> visitEmpty(EmptyElimTreeNode emptyNode, Boolean params) {
    return Collections.emptyList();
  }

  public boolean checkEmptyContext(int index) {
    if (index < 0) {
      return false;
    }

    Expression type = myContext.get(myContext.size() - 1 - index).getType().liftIndex(0, index);
    List<Expression> parameters = new ArrayList<>();
    Expression ftype = type.normalize(NormalizeVisitor.Mode.WHNF, myContext).getFunction(parameters);
    Collections.reverse(parameters);
    if (!(ftype instanceof DefCallExpression && ((DefCallExpression) ftype).getDefinition() instanceof DataDefinition)) {
      return checkEmptyContext(index - 1);
    }
    List<ConCallExpression> validConCalls = ((DataDefinition) ((DefCallExpression) ftype).getDefinition()).getConstructors(parameters, myContext);

    if (validConCalls == null) {
      return checkEmptyContext(index - 1);
    }

    for (ConCallExpression conCall : validConCalls) {
      try (ConCallContextExpander ignore = new ConCallContextExpander(index, conCall, myContext)) {
        if (!checkEmptyContext(index - 1)) {
          return false;
        }
      }
    }
    return true;
  }
}
