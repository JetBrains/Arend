package com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.IndexExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;

import java.util.ArrayList;
import java.util.List;


public class SubstituteExpander<T> implements ElimTreeNodeVisitor<T, Void, List<Expression>> {
  public interface SubstituteExpansionProcessor<T> {
    void process(List<Expression> expressions, List<Binding> context, T value);
  }

  private final List<Binding> myContext;
  private List<Expression> mySubst;
  private final SubstituteExpansionProcessor<T> myProcessor;

  private SubstituteExpander(List<Binding> context, SubstituteExpansionProcessor<T> processor, List<Expression> subst) {
    myContext = context;
    myProcessor = processor;
    mySubst = subst;
  }

  public static <T> void substituteExpand(List<Binding> context, List<Expression> subst, ElimTreeNode<T> tree, List<Expression> expressions, SubstituteExpansionProcessor<T> processor) {
    tree.accept(new SubstituteExpander<>(context, processor, subst), expressions);
  }

  @Override
  public Void visitBranch(BranchElimTreeNode<T> branchNode, List<Expression> expressions) {
    List<Expression> arguments = new ArrayList<>();
    Expression func = mySubst.get(branchNode.getIndex()).getFunction(arguments);

    if (func instanceof ConCallExpression) {
      ElimTreeNode<T> child = branchNode.getChild(((ConCallExpression) func).getDefinition());
      if (child != null) {
        try (Utils.CompleteContextSaver<Expression> ignore = new Utils.CompleteContextSaver<>(mySubst)) {
          mySubst.remove(branchNode.getIndex());
          mySubst.addAll(branchNode.getIndex(), arguments);
          child.accept(this, expressions);
        }
      }
    } else if (func instanceof IndexExpression) {
      final int varIndex = ((IndexExpression) func).getIndex();
      List<Expression> parameters = new ArrayList<>();
      Expression ftype = myContext.get(myContext.size() - 1 - varIndex).getType().liftIndex(0, varIndex).
          normalize(NormalizeVisitor.Mode.NF, myContext).getFunction(parameters);
      for (ConCallExpression conCall : ((DataCallExpression) ftype).getDefinition().getConstructors(parameters, myContext)) {
        try (ConCallContextExpander expander = new ConCallContextExpander(varIndex, conCall, myContext)) {
          List<Expression> oldSubst = mySubst;
          mySubst = expander.substIn(mySubst);
          visitBranch(branchNode, expander.substIn(expressions));
          mySubst = oldSubst;
        }
      }
    } else {
      throw new IllegalStateException();
    }

    return null;
  }

  @Override
  public Void visitLeaf(LeafElimTreeNode<T> leafNode, List<Expression> expressions) {
    myProcessor.process(new ArrayList<>(expressions), myContext, leafNode.getValue());
    return null;
  }
 }
