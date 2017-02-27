package com.jetbrains.jetpad.vclang.core.pattern.elimtree;

import com.jetbrains.jetpad.vclang.core.context.Utils;
import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ToAbstractVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.visitor.ElimTreeNodeVisitor;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;

import java.util.*;

import static com.jetbrains.jetpad.vclang.core.context.param.DependentLink.Helper.toSubstitution;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.ConCall;

public class BranchElimTreeNode extends ElimTreeNode {
  private final Binding myReference;
  private final boolean myIsInterval;
  private final Map<Constructor, ConstructorClause> myClauses = new HashMap<>();
  private OtherwiseClause myOtherwiseClause;

  private final List<Binding> myContextTail;

  public BranchElimTreeNode(Binding reference, List<Binding> contextTail) {
    myReference = reference;
    myContextTail = contextTail;

    Expression type = reference.getType().normalize(NormalizeVisitor.Mode.WHNF).toExpression();
    DataCallExpression dType = type != null ? type.toDataCall() : null;
    myIsInterval = dType != null && dType.getDefinition() == Prelude.INTERVAL;
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
    assert constructor.status().headerIsOK();
    DataCallExpression dataCall = myReference.getType().normalize(NormalizeVisitor.Mode.WHNF).toExpression().toDataCall();
    List<? extends Expression> dataTypeArguments = dataCall.getDefCallArguments();

    dataTypeArguments = constructor.matchDataTypeArguments(new ArrayList<>(dataTypeArguments));
    DependentLink constructorArgs = DependentLink.Helper.subst(constructor.getParameters(), toSubstitution(constructor.getDataTypeParameters(), dataTypeArguments), LevelSubstitution.EMPTY);
    if (names != null) {
      constructorArgs = DependentLink.Helper.subst(constructorArgs, new ExprSubstitution());
      int i = 0;
      for (DependentLink link = constructorArgs; link.hasNext() && i < names.size(); link = link.getNext(), i++) {
        link.setName(names.get(i));
      }
    }

    List<Expression> arguments = new ArrayList<>();
    for (DependentLink link = constructorArgs; link.hasNext(); link = link.getNext()) {
      arguments.add(ExpressionFactory.Reference(link));
    }

    List<TypedBinding> tailBindings = new ExprSubstitution(myReference, ConCall(constructor, dataCall.getLevelArguments(), new ArrayList<>(dataTypeArguments), arguments)).extendBy(myContextTail);
    ConstructorClause result = new ConstructorClause(constructor, constructorArgs, tailBindings, this);
    myClauses.put(constructor, result);
    return result;
  }

  public OtherwiseClause addOtherwiseClause() {
    myOtherwiseClause = new OtherwiseClause(this);
    return myOtherwiseClause;
  }

  public void addClause(Constructor constructor, DependentLink constructorParams, List<TypedBinding> tailBindings, ElimTreeNode child) {
    ConstructorClause clause = new ConstructorClause(constructor, constructorParams, tailBindings, this);
    myClauses.put(constructor, clause);
    clause.setChild(child);
  }

  public void addOtherwiseClause(ElimTreeNode child) {
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

  public ElimTreeNode matchUntilStuck(ExprSubstitution subst, boolean normalize) {
    Expression func = subst.get(myReference);
    if (normalize) {
      func = func.normalize(NormalizeVisitor.Mode.WHNF);
    }
    ConCallExpression conFunc = func.toConCall();

    if (conFunc == null) {
      if (myIsInterval && myOtherwiseClause != null) {
        return myOtherwiseClause.getChild().matchUntilStuck(subst, normalize);
      } else {
        return this;
      }
    }

    ConstructorClause clause = myClauses.get(conFunc.getDefinition());
    if (clause == null) {
      return myOtherwiseClause == null ? this : myOtherwiseClause.getChild().matchUntilStuck(subst, normalize);
    }

    int i = 0;
    for (DependentLink link = clause.getParameters(); link.hasNext(); link = link.getNext(), i++) {
      subst.add(link, conFunc.getDefCallArguments().get(i));
    }
    for (i = 0; i < myContextTail.size(); i++) {
      subst.add(clause.getTailBindings().get(i), subst.get(myContextTail.get(i)));
    }
    subst.remove(myReference);
    subst.removeAll(myContextTail);
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
        context.addAll(DependentLink.Helper.toContext(clause.getParameters()));
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

  @Override
  public LeafElimTreeNode match(List<Expression> expressions) {
    int idx = expressions.size() - 1 - myContextTail.size();
    ConCallExpression conFunc = expressions.get(idx).normalize(NormalizeVisitor.Mode.WHNF).toConCall();
    if (conFunc == null) {
      if (myIsInterval && myOtherwiseClause != null) {
        return myOtherwiseClause.getChild().match(expressions);
      } else {
        return null;
      }
    }

    ConstructorClause clause = myClauses.get(conFunc.getDefinition());
    if (clause == null) {
      return myOtherwiseClause == null ? null : myOtherwiseClause.getChild().match(expressions);
    }

    expressions.remove(idx);
    expressions.addAll(idx, conFunc.getDefCallArguments());
    return clause.getChild().match(expressions);
  }
}
