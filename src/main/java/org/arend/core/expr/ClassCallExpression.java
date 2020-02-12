package org.arend.core.expr;

import org.arend.core.context.LinkList;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.UniverseKind;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.expr.visitor.StripVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.InPlaceLevelSubstVisitor;
import org.arend.core.subst.SubstVisitor;
import org.arend.ext.core.definition.CoreClassField;
import org.arend.ext.core.expr.CoreClassCallExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.naming.renamer.Renamer;

import javax.annotation.Nonnull;
import java.util.*;

public class ClassCallExpression extends DefCallExpression implements Type, CoreClassCallExpression {
  private final ClassCallBinding myThisBinding = new ClassCallBinding();
  private final Map<ClassField, Expression> myImplementations;
  private Sort mySort;
  private UniverseKind myPLevelKind;
  private UniverseKind myHLevelKind;

  public class ClassCallBinding implements Binding {
    @Override
    public String getName() {
      return "this";
    }

    @Nonnull
    @Override
    public ClassCallExpression getTypeExpr() {
      return ClassCallExpression.this;
    }

    @Override
    public void strip(StripVisitor stripVisitor) {
      ClassCallExpression.this.accept(stripVisitor, null);
    }

    @Override
    public void subst(InPlaceLevelSubstVisitor substVisitor) {
      ClassCallExpression.this.accept(substVisitor, null);
    }

    @Override
    public boolean isHidden() {
      return true;
    }
  }

  public ClassCallExpression(ClassDefinition definition, Sort sortArgument) {
    super(definition, sortArgument);
    myImplementations = Collections.emptyMap();
    mySort = definition.getSort().subst(sortArgument.toLevelSubstitution());
    myPLevelKind = definition.getPLevelKind();
    myHLevelKind = definition.getHLevelKind();
  }

  public ClassCallExpression(ClassDefinition definition, Sort sortArgument, Map<ClassField, Expression> implementations, Sort sort, UniverseKind pLevelKind, UniverseKind hLevelKind) {
    super(definition, sortArgument);
    myImplementations = implementations;
    mySort = sort;
    myPLevelKind = pLevelKind;
    myHLevelKind = hLevelKind;
  }

  @Nonnull
  @Override
  public ClassCallBinding getThisBinding() {
    return myThisBinding;
  }

  public void updateHasUniverses() {
    if (getDefinition().getUniverseKind() == UniverseKind.NO_UNIVERSES) {
      myPLevelKind = UniverseKind.NO_UNIVERSES;
      myHLevelKind = UniverseKind.NO_UNIVERSES;
      return;
    }
    if (myImplementations.isEmpty()) {
      myPLevelKind = getDefinition().getPLevelKind();
      myHLevelKind = getDefinition().getHLevelKind();
      return;
    }

    myPLevelKind = UniverseKind.NO_UNIVERSES;
    myHLevelKind = UniverseKind.NO_UNIVERSES;
    for (ClassField field : getDefinition().getFields()) {
      if (field.getPLevelKind().ordinal() > myPLevelKind.ordinal() && !isImplemented(field)) {
        myPLevelKind = field.getPLevelKind();
      }
      if (field.getHLevelKind().ordinal() > myHLevelKind.ordinal() && !isImplemented(field)) {
        myHLevelKind = field.getHLevelKind();
      }
      if (myPLevelKind == UniverseKind.WITH_UNIVERSES && myHLevelKind == UniverseKind.WITH_UNIVERSES) {
        return;
      }
    }
  }

  public Map<ClassField, Expression> getImplementedHere() {
    return myImplementations;
  }

  @Nonnull
  @Override
  public Collection<? extends Map.Entry<? extends CoreClassField, ? extends CoreExpression>> getImplementations() {
    return myImplementations.entrySet();
  }

  @Override
  public Expression getAbsImplementationHere(@Nonnull CoreClassField field) {
    return field instanceof ClassField ? myImplementations.get(field) : null;
  }

  public Expression getImplementationHere(CoreClassField field, Expression thisExpr) {
    Expression expr = field instanceof ClassField ? myImplementations.get(field) : null;
    return expr != null ? expr.subst(myThisBinding, thisExpr) : null;
  }

  public Expression getImplementation(ClassField field, Expression thisExpr) {
    Expression expr = myImplementations.get(field);
    if (expr != null) {
      return expr.subst(myThisBinding, thisExpr);
    }
    AbsExpression impl = getDefinition().getImplementation(field);
    return impl == null ? null : impl.apply(thisExpr);
  }

  @Override
  public boolean isImplementedHere(@Nonnull CoreClassField field) {
    return field instanceof ClassField && myImplementations.containsKey(field);
  }

  @Override
  public boolean isImplemented(@Nonnull CoreClassField field) {
    return field instanceof ClassField && (myImplementations.containsKey(field) || getDefinition().isImplemented(field));
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

  public void copyImplementationsFrom(ClassCallExpression classCall) {
    if (classCall.myImplementations.isEmpty()) {
      return;
    }

    ReferenceExpression thisExpr = new ReferenceExpression(myThisBinding);
    for (Map.Entry<ClassField, Expression> entry : classCall.myImplementations.entrySet()) {
      myImplementations.put(entry.getKey(), entry.getValue().subst(classCall.myThisBinding, thisExpr));
    }
  }

  public DependentLink getClassFieldParameters() {
    Map<ClassField, Expression> implementations = new HashMap<>();
    NewExpression newExpr = new NewExpression(null, new ClassCallExpression(getDefinition(), getSortArgument(), implementations, Sort.PROP, UniverseKind.NO_UNIVERSES, UniverseKind.NO_UNIVERSES));
    newExpr.getClassCall().copyImplementationsFrom(this);

    Collection<? extends ClassField> fields = getDefinition().getFields();
    if (fields.isEmpty()) {
      return EmptyDependentLink.getInstance();
    }

    LinkList list = new LinkList();
    for (ClassField field : fields) {
      if (isImplemented(field)) {
        continue;
      }

      PiExpression piExpr = field.getType(getSortArgument());
      Expression type = piExpr.applyExpression(newExpr);
      DependentLink link = new TypedDependentLink(true, Renamer.getNameFromType(type, field.getName()), type instanceof Type ? (Type) type : new TypeExpression(type, piExpr.getResultSort()), EmptyDependentLink.getInstance());
      implementations.put(field, new ReferenceExpression(link));
      list.append(link);
    }

    return list.getFirst();
  }

  @Override
  public Integer getUseLevel() {
    return getDefinition().getUseLevel(myImplementations, myThisBinding);
  }

  @Nonnull
  @Override
  public ClassDefinition getDefinition() {
    return (ClassDefinition) super.getDefinition();
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
    return substVisitor.isEmpty() ? this : (ClassCallExpression) substVisitor.visitClassCall(this, null);
  }

  @Override
  public void subst(InPlaceLevelSubstVisitor substVisitor) {
    substVisitor.visitClassCall(this, null);
  }

  @Override
  public ClassCallExpression strip(StripVisitor visitor) {
    return visitor.visitClassCall(this, null);
  }

  @Nonnull
  @Override
  public ClassCallExpression normalize(@Nonnull NormalizationMode mode) {
    return NormalizeVisitor.INSTANCE.visitClassCall(this, mode);
  }

  public Sort getSort() {
    return mySort;
  }

  public void setSort(Sort sort) {
    mySort = sort;
  }

  @Override
  public UniverseKind getUniverseKind() {
    return myPLevelKind.max(myHLevelKind);
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClassCall(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitClassCall(this, param1, param2);
  }

  @Override
  public <P, R> R accept(@Nonnull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClassCall(this, params);
  }
}
