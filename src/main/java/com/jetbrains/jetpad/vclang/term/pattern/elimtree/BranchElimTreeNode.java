package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.Substitution;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toSubstitution;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class BranchElimTreeNode extends ElimTreeNode {
  private final Binding myReference;
  private final Map<Constructor, ConstructorClause> myClauses = new HashMap<>();
  private final List<Binding> myContextTail;

  public BranchElimTreeNode(Binding reference, List<Binding> contextTail) {
    myReference = reference;
    myContextTail = contextTail;
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

  public ConstructorClause addClause(Constructor constructor) {
    List<Expression> dataTypeParameters = new ArrayList<>();
    myReference.getType().getFunction(dataTypeParameters);
    Collections.reverse(dataTypeParameters);

    if (constructor.getPatterns() != null) {
      dataTypeParameters = ((Pattern.MatchOKResult) constructor.getPatterns().match(dataTypeParameters)).expressions;
    }

    DependentLink constructorArgs = constructor.getParameters().subst(toSubstitution(constructor.getDataTypeParameters(), dataTypeParameters));

    Expression substExpr = ConCall(constructor, dataTypeParameters);
    for (DependentLink link = constructorArgs; link.hasNext(); link = link.getNext()) {
      substExpr = Apps(substExpr, Reference(link));
    }

    List<Binding> tailBindings = new Substitution(myReference, substExpr).extendBy(myContextTail);

    ConstructorClause result = new ConstructorClause(constructor, constructorArgs, tailBindings, this);
    myClauses.put(constructor, result);
    return result;
  }

  public void addClause(Constructor constructor, DependentLink constructorArgs, List<Binding> tailBindings, ElimTreeNode child) {
    ConstructorClause clause = new ConstructorClause(constructor, constructorArgs, tailBindings, this);
    myClauses.put(constructor, clause);
    clause.setChild(child);
  }

  public ConstructorClause getClause(Constructor constructor) {
    return myClauses.get(constructor);
  }

  public Collection<ConstructorClause> getConstructorClauses() {
    return myClauses.values();
  }

  public ElimTreeNode matchUntilStuck(Substitution subst) {
    List<Expression> arguments = new ArrayList<>();
    Expression func = subst.get(myReference).getFunction(arguments);
    if (!(func instanceof ConCallExpression)) {
      return this;
    }
    ConstructorClause clause = getClause(((ConCallExpression) func).getDefinition());
    if (clause == null)
      return this;

    for (DependentLink link = clause.getParameters(); link.hasNext(); link = link.getNext()) {
      subst.addMapping(link, arguments.get(arguments.size() - 1));
      arguments.remove(arguments.size() - 1);
    }
    for (int i = 0; i < myContextTail.size(); i++) {
      subst.addMapping(clause.getTailBindings().get(i), subst.get(myContextTail.get(i)));
    }
    subst.getDomain().remove(myReference);
    subst.getDomain().removeAll(myContextTail);
    return clause.getChild().matchUntilStuck(subst);
  }

  @Override
  public Abstract.Definition.Arrow getArrow() {
    return Abstract.Definition.Arrow.LEFT;
  }
}
