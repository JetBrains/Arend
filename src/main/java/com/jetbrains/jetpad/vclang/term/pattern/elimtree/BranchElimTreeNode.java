package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
import com.jetbrains.jetpad.vclang.term.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ToAbstractVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toContext;
import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toSubstitution;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class BranchElimTreeNode extends ElimTreeNode {
  private final Binding myReference;
  private final boolean myIsInterval;
  private final Map<Constructor, ConstructorClause> myClauses = new HashMap<>();
  private OtherwiseClause myOtherwiseClause;

  private final List<Binding> myContextTail;

  public BranchElimTreeNode(Binding reference, List<Binding> contextTail) {
    myReference = reference;
    myContextTail = contextTail;

    Expression ftype = reference.getType().normalize(NormalizeVisitor.Mode.WHNF).getFunction(new ArrayList<Expression>());
    myIsInterval = ftype instanceof DataCallExpression && ((DataCallExpression) ftype).getDefinition() == Prelude.INTERVAL;
  }

  @Override
  public <P, R> R accept(ElimTreeNodeVisitor<P, R> visitor, P params) {
    return visitor.visitBranch(this, params);
  }

  public Binding getReference() {
    return myReference;
  }

  public List<Binding> getContextTail() {
    return myContextTail;
  }

  public ConstructorClause addClause(Constructor constructor, List<String> names) {
    List<Expression> dataTypeArguments = new ArrayList<>();
    myReference.getType().getFunction(dataTypeArguments);
    Collections.reverse(dataTypeArguments);

    dataTypeArguments = constructor.matchDataTypeArguments(dataTypeArguments);
    DependentLink constructorArgs = DependentLink.Helper.subst(constructor.getParameters(), toSubstitution(constructor.getDataTypeParameters(), dataTypeArguments));
    if (names != null) {
      constructorArgs = DependentLink.Helper.subst(constructorArgs, new Substitution());
      int i = 0;
      for (DependentLink link = constructorArgs; link.hasNext() && i < names.size(); link = link.getNext(), i++) {
        link.setName(names.get(i));
      }
    }

    Expression substExpr = ConCall(constructor, dataTypeArguments);
    for (DependentLink link = constructorArgs; link.hasNext(); link = link.getNext()) {
      substExpr = Apps(substExpr, Reference(link));
    }

    List<Binding> tailBindings = new Substitution(myReference, substExpr).extendBy(myContextTail);

    ConstructorClause result = new ConstructorClause(constructor, constructorArgs, tailBindings, this);
    myClauses.put(constructor, result);
    return result;
  }

  public OtherwiseClause addOtherwiseClause() {
    myOtherwiseClause = new OtherwiseClause(this);
    return myOtherwiseClause;
  }

  void addClause(Constructor constructor, DependentLink constructorArgs, List<Binding> tailBindings, ElimTreeNode child) {
    ConstructorClause clause = new ConstructorClause(constructor, constructorArgs, tailBindings, this);
    myClauses.put(constructor, clause);
    clause.setChild(child);
  }

  void addOtherwiseClause(ElimTreeNode child) {
    myOtherwiseClause = new OtherwiseClause(this);
    myOtherwiseClause.setChild(child);
  }

  public Clause getClause(Constructor constructor) {
    Clause result = myClauses.get(constructor);
    return result != null ? result : myOtherwiseClause;
  }

  public Collection<ConstructorClause> getConstructorClauses() {
    return myClauses.values();
  }

  public OtherwiseClause getOtherwiseClause() {
    return myOtherwiseClause;
  }

  public ElimTreeNode matchUntilStuck(Substitution subst, boolean normalize) {
    List<Expression> arguments = new ArrayList<>();
    Expression func = (normalize ? subst.get(myReference).normalize(NormalizeVisitor.Mode.WHNF) : subst.get(myReference)).getFunction(arguments);
    if (!(func instanceof ConCallExpression)) {
      if (myIsInterval && myOtherwiseClause != null) {
        return myOtherwiseClause.getChild().matchUntilStuck(subst, normalize);
      } else {
        return this;
      }
    }

    ConstructorClause clause = myClauses.get(((ConCallExpression) func).getDefinition());
    if (clause == null) {
      return myOtherwiseClause == null ? this : myOtherwiseClause.getChild().matchUntilStuck(subst, normalize);
    }

    for (DependentLink link = clause.getParameters(); link.hasNext(); link = link.getNext()) {
      subst.add(link, arguments.get(arguments.size() - 1));
      arguments.remove(arguments.size() - 1);
    }
    for (int i = 0; i < myContextTail.size(); i++) {
      subst.add(clause.getTailBindings().get(i), subst.get(myContextTail.get(i)));
    }
    subst.getDomain().remove(myReference);
    subst.getDomain().removeAll(myContextTail);
    return clause.getChild().matchUntilStuck(subst, normalize);
  }

  @Override
  public void updateLeavesMatched(List<Binding> context) {
    if (myOtherwiseClause != null) {
      myOtherwiseClause.getChild().updateLeavesMatched(context);
    }

    List<Binding> tail = new ArrayList<>(context.subList(context.size() - 1 - myContextTail.size(), context.size()));
    context.subList(context.size() - 1 - myContextTail.size(), context.size()).clear();
    for (ConstructorClause clause : myClauses.values()) {
      try (Utils.ContextSaver ignore = new Utils.ContextSaver(context)) {
        context.addAll(toContext(clause.getParameters()));
        context.addAll(clause.getTailBindings());
        clause.getChild().updateLeavesMatched(context);
      }
    }

    context.addAll(tail);
  }

  @Override
  public String toString() {
    return accept(new ToAbstractVisitor(new ConcreteExpressionFactory()), null).toString();
  }

  @Override
  public Abstract.Definition.Arrow getArrow() {
    return Abstract.Definition.Arrow.LEFT;
  }

  public LeafElimTreeNode match(List<Expression> expressions) {
    List<Expression> arguments = new ArrayList<>();
    int idx = expressions.size() - 1 - myContextTail.size();
    Expression func = expressions.get(idx).normalize(NormalizeVisitor.Mode.WHNF).getFunction(arguments);

    if (!(func instanceof ConCallExpression)) {
      if (myIsInterval && myOtherwiseClause != null) {
        return myOtherwiseClause.getChild().match(expressions);
      } else {
        return null;
      }
    }

    ConstructorClause clause = myClauses.get(((ConCallExpression) func).getDefinition());
    if (clause == null) {
      return myOtherwiseClause == null ? null : myOtherwiseClause.getChild().match(expressions);
    }

    Collections.reverse(arguments);
    expressions.remove(idx);
    expressions.addAll(idx, arguments);
    return clause.getChild().match(expressions);
  }
}
