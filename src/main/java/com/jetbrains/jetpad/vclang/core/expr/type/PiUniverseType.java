package com.jetbrains.jetpad.vclang.core.expr.type;

import com.jetbrains.jetpad.vclang.core.expr.PiExpression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Referable;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.core.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.core.sort.SortMax;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ToAbstractVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class PiUniverseType implements TypeMax {
  private final DependentLink myParameters;
  private final SortMax mySorts;

  public PiUniverseType(DependentLink parameters, SortMax sorts) {
    myParameters = parameters;
    mySorts = sorts;
  }

  @Override
  public DependentLink getPiParameters() {
    return myParameters;
  }

  @Override
  public PiUniverseType getPiCodomain() {
    return new PiUniverseType(EmptyDependentLink.getInstance(), mySorts);
  }

  @Override
  public PiUniverseType normalize(NormalizeVisitor.Mode mode) {
    if (myParameters.hasNext() && mode == NormalizeVisitor.Mode.NF) {
      DependentLink params = DependentLink.Helper.clone(myParameters);
      for (DependentLink link = params; link.hasNext(); link = link.getNext()) {
        params.setType(params.getType().normalize(NormalizeVisitor.Mode.NF));
      }
      return new PiUniverseType(params, mySorts);
    } else {
      return this;
    }
  }

  @Override
  public PiUniverseType strip(Set<Binding> bounds, LocalErrorReporter errorReporter) {
    if (!myParameters.hasNext()) {
      return this;
    }

    DependentLink params = DependentLink.Helper.clone(myParameters);
    for (DependentLink link = params; link.hasNext(); link = link.getNext()) {
      DependentLink link1 = link.getNextTyped(null);
      for (; link != link1; link = link.getNext()) {
        bounds.add(link);
      }
      bounds.add(link);
      link.setType(link.getType().strip(bounds, errorReporter));
    }

    for (DependentLink link = params; link.hasNext(); link = link.getNext()) {
      bounds.remove(link);
    }

    return new PiUniverseType(params, mySorts);
  }

  @Override
  public Expression toExpression() {
    Sort sort = mySorts.toSort();
    return sort == null || sort.isOmega() ? null : myParameters.hasNext() ? new PiExpression(myParameters, new UniverseExpression(sort)) : new UniverseExpression(sort);
  }

  @Override
  public TypeMax getType() {
    return new PiUniverseType(DependentLink.Helper.clone(myParameters), mySorts.succ());
  }

  @Override
  public boolean findBinding(Referable binding) {
    return DependentLink.Helper.findBinding(myParameters, binding);
  }

  public SortMax getSorts() {
    return mySorts;
  }

  @Override
  public SortMax toSorts() {
    return myParameters.hasNext() ? null : mySorts;
  }

  @Override
  public PiUniverseType getPiParameters(List<DependentLink> params, boolean normalize, boolean implicitOnly) {
    DependentLink link = myParameters;
    for (; link.hasNext() && (!implicitOnly || !link.isExplicit()); link = link.getNext()) {
      params.add(link);
    }
    return new PiUniverseType(link, mySorts);
  }

  @Override
  public PiUniverseType fromPiParameters(List<DependentLink> params) {
    return new PiUniverseType(params.isEmpty() ? EmptyDependentLink.getInstance() : params.get(0), mySorts);
  }

  @Override
  public PiUniverseType addParameters(DependentLink params, boolean modify) {
    if (!params.hasNext()) {
      return this;
    }
    if (!myParameters.hasNext()) {
      return new PiUniverseType(params, mySorts);
    }
    if (modify) {
      return new PiUniverseType(ExpressionFactory.params(params, myParameters), mySorts);
    }

    ExprSubstitution subst = new ExprSubstitution();
    params = DependentLink.Helper.subst(params, subst);
    return new PiUniverseType(ExpressionFactory.params(params, DependentLink.Helper.subst(myParameters, subst)), mySorts);
  }

  @Override
  public PiUniverseType subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst) {
    return new PiUniverseType(DependentLink.Helper.subst(myParameters, exprSubst, levelSubst), mySorts.subst(levelSubst));
  }

  @Override
  public PiUniverseType applyExpressions(List<? extends Expression> expressions) {
    DependentLink link = myParameters;
    ExprSubstitution subst = new ExprSubstitution();
    for (Expression expr : expressions) {
      if (link.hasNext()) {
        subst.add(link, expr);
        link = link.getNext();
      } else {
        return null;
      }
    }
    return new PiUniverseType(DependentLink.Helper.subst(link, subst), mySorts);
  }

  @Override
  public boolean isLessOrEquals(Sort sort) {
    return !myParameters.hasNext() && mySorts.isLessOrEquals(sort);
  }

  @Override
  public boolean isLessOrEquals(TypeMax type, Equations equations, Abstract.SourceNode sourceNode) {
    if (type instanceof Expression) {
      Expression exprType = (Expression)type;
      InferenceVariable binding = CompareVisitor.checkIsInferVar(exprType);
      if (binding != null) {
        return equations.add(this, exprType, sourceNode, binding);
      }
    }

    List<DependentLink> params = new ArrayList<>();
    TypeMax cod = type.getPiParameters(params, false, false);
    PiUniverseType normalized = normalize(NormalizeVisitor.Mode.NF);

    if (!CompareVisitor.compare(equations, DependentLink.Helper.toList(normalized.getPiParameters()), params, sourceNode)) {
      return false;
    }

    if (cod.toExpression() != null) {
      InferenceVariable binding = CompareVisitor.checkIsInferVar(cod.toExpression());
      if (binding != null) {
        return equations.add(getPiCodomain(), cod.toExpression(), sourceNode, binding);
      }
    }

    if (cod.toSorts() == null) {
      return false;
    }

    Sort sortCod = cod.toSorts().toSort();

    return  sortCod != null && mySorts.isLessOrEquals(sortCod, equations, sourceNode);
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent) {
    new ToAbstractVisitor(new ConcreteExpressionFactory(), names).visitTypeMax(this).accept(new PrettyPrintVisitor(builder, indent), prec);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    prettyPrint(builder, Collections.<String>emptyList(), Abstract.Expression.PREC, 0);
    return builder.toString();
  }
}
