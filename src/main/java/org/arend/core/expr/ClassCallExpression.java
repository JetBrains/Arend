package org.arend.core.expr;

import org.arend.core.context.LinkList;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.expr.visitor.StripVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.SubstVisitor;
import org.arend.error.ErrorReporter;
import org.arend.naming.renamer.Renamer;

import java.util.*;

public class ClassCallExpression extends DefCallExpression implements Type {
  private final Sort mySortArgument;
  private final Map<ClassField, Expression> myImplementations;
  private final Sort mySort;
  private boolean myHasUniverses;

  public ClassCallExpression(ClassDefinition definition, Sort sortArgument) {
    super(definition);
    mySortArgument = sortArgument;
    myImplementations = Collections.emptyMap();
    mySort = definition.getSort().subst(sortArgument.toLevelSubstitution());
    myHasUniverses = definition.hasUniverses();
  }

  public ClassCallExpression(ClassDefinition definition, Sort sortArgument, Map<ClassField, Expression> implementations, Sort sort, boolean hasUniverses) {
    super(definition);
    mySortArgument = sortArgument;
    myImplementations = implementations;
    mySort = sort;
    myHasUniverses = hasUniverses;
  }

  public void updateHasUniverses() {
    if (!getDefinition().hasUniverses()) {
      myHasUniverses = false;
      return;
    }
    if (myImplementations.isEmpty()) {
      myHasUniverses = true;
      return;
    }

    myHasUniverses = false;
    for (ClassField field : getDefinition().getFields()) {
      if (field.hasUniverses() && !isImplemented(field)) {
        myHasUniverses = true;
        return;
      }
    }
  }

  public Map<ClassField, Expression> getImplementedHere() {
    return myImplementations;
  }

  public Expression getImplementationHere(ClassField field) {
    return myImplementations.get(field);
  }

  public Expression getImplementation(ClassField field, Expression thisExpr) {
    Expression expr = myImplementations.get(field);
    if (expr != null) {
      return expr;
    }
    LamExpression impl = getDefinition().getImplementation(field);
    return impl == null ? null : impl.substArgument(thisExpr);
  }

  public boolean isImplemented(ClassField field) {
    return myImplementations.containsKey(field) || getDefinition().isImplemented(field);
  }

  public boolean isUnit() {
    return myImplementations.size() == getDefinition().getNumberOfNotImplementedFields();
  }

  public List<ClassField> getNotImplementedFields() {
    List<ClassField> result = new ArrayList<>();
    for (ClassField field : getDefinition().getFields()) {
      if (!isImplemented(field)) {
        result.add(field);
      }
    }
    return result;
  }

  public int getNumberOfNotImplementedFields() {
    return getDefinition().getNumberOfNotImplementedFields() - myImplementations.size();
  }

  public DependentLink getClassFieldParameters() {
    Map<ClassField, Expression> implementations = new HashMap<>(myImplementations);
    Expression newExpr = new NewExpression(null, new ClassCallExpression(getDefinition(), mySortArgument, implementations, Sort.PROP, false));
    Collection<? extends ClassField> fields = getDefinition().getOrderedFields();
    if (fields.isEmpty()) {
      return EmptyDependentLink.getInstance();
    }

    LinkList list = new LinkList();
    for (ClassField field : fields) {
      if (isImplemented(field)) {
        continue;
      }

      PiExpression piExpr = field.getType(mySortArgument);
      Expression type = piExpr.applyExpression(newExpr);
      DependentLink link = new TypedDependentLink(true, Renamer.getNameFromType(type, field.getName()), type instanceof Type ? (Type) type : new TypeExpression(type, piExpr.getResultSort()), EmptyDependentLink.getInstance());
      implementations.put(field, new ReferenceExpression(link));
      list.append(link);
    }

    return list.getFirst();
  }

  @Override
  public Integer getUseLevel() {
    return getDefinition().getUseLevel(myImplementations);
  }

  @Override
  public ClassDefinition getDefinition() {
    return (ClassDefinition) super.getDefinition();
  }

  @Override
  public Sort getSortArgument() {
    return mySortArgument;
  }

  @Override
  public Expression getExpr() {
    return this;
  }

  @Override
  public Sort getSortOfType() {
    return getSort();
  }

  @Override
  public ClassCallExpression subst(SubstVisitor substVisitor) {
    return substVisitor.visitClassCall(this, null);
  }

  @Override
  public ClassCallExpression strip(ErrorReporter errorReporter) {
    return new StripVisitor(errorReporter).visitClassCall(this, null);
  }

  @Override
  public ClassCallExpression normalize(NormalizeVisitor.Mode mode) {
    return NormalizeVisitor.INSTANCE.visitClassCall(this, mode);
  }

  public Sort getSort() {
    return mySort;
  }

  @Override
  public boolean hasUniverses() {
    return myHasUniverses;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClassCall(this, params);
  }
}
