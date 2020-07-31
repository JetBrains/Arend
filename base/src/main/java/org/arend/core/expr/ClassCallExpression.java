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
import org.arend.ext.core.definition.CoreClassField;
import org.arend.ext.core.expr.CoreClassCallExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.typechecking.TypedExpression;
import org.arend.naming.renamer.Renamer;
import org.arend.typechecking.result.TypecheckingResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ClassCallExpression extends DefCallExpression implements Type, CoreClassCallExpression {
  private final ClassCallBinding myThisBinding = new ClassCallBinding();
  private final Map<ClassField, Expression> myImplementations;
  private Sort mySort;
  private UniverseKind myUniverseKind;

  public class ClassCallBinding implements Binding {
    @Override
    public String getName() {
      return "this";
    }

    @NotNull
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
    myUniverseKind = definition.getUniverseKind();
  }

  public ClassCallExpression(ClassDefinition definition, Sort sortArgument, Map<ClassField, Expression> implementations, Sort sort, UniverseKind universeKind) {
    super(definition, sortArgument);
    myImplementations = implementations;
    mySort = sort;
    myUniverseKind = universeKind;
  }

  @NotNull
  @Override
  public ClassCallBinding getThisBinding() {
    return myThisBinding;
  }

  public void updateHasUniverses() {
    if (getDefinition().getUniverseKind() == UniverseKind.NO_UNIVERSES) {
      myUniverseKind = UniverseKind.NO_UNIVERSES;
      return;
    }
    if (myImplementations.isEmpty()) {
      myUniverseKind = getDefinition().getUniverseKind();
      return;
    }

    myUniverseKind = UniverseKind.NO_UNIVERSES;
    for (ClassField field : getDefinition().getFields()) {
      if (field.getUniverseKind().ordinal() > myUniverseKind.ordinal() && !isImplemented(field)) {
        myUniverseKind = field.getUniverseKind();
        if (myUniverseKind == UniverseKind.WITH_UNIVERSES) {
          return;
        }
      }
    }
  }

  public Map<ClassField, Expression> getImplementedHere() {
    return myImplementations;
  }

  @NotNull
  @Override
  public Collection<? extends Map.Entry<? extends CoreClassField, ? extends CoreExpression>> getImplementations() {
    return myImplementations.entrySet();
  }

  @Override
  public Expression getAbsImplementationHere(@NotNull CoreClassField field) {
    return field instanceof ClassField ? myImplementations.get(field) : null;
  }

  @Override
  public @Nullable Expression getImplementationHere(@NotNull CoreClassField field, @NotNull CoreExpression thisExpr) {
    if (!(field instanceof ClassField && thisExpr instanceof Expression)) {
      throw new IllegalArgumentException();
    }
    Expression expr = myImplementations.get(field);
    return expr != null ? expr.subst(myThisBinding, (Expression) thisExpr) : null;
  }

  @Override
  public @Nullable Expression getImplementation(@NotNull CoreClassField field, @NotNull CoreExpression thisExpr) {
    if (!(field instanceof ClassField && thisExpr instanceof Expression)) {
      throw new IllegalArgumentException();
    }
    Expression expr = myImplementations.get(field);
    if (expr != null) {
      return expr.subst(myThisBinding, (Expression) thisExpr);
    }
    AbsExpression impl = getDefinition().getImplementation(field);
    return impl == null ? null : impl.apply((Expression) thisExpr);
  }

  private static void checkImplementation(CoreClassField field, Expression type) {
    type = type.normalize(NormalizationMode.WHNF);
    if (!(type instanceof CoreClassCallExpression && ((CoreClassCallExpression) type).getDefinition().isSubClassOf(field.getParentClass()))) {
      throw new IllegalArgumentException("Expected an expression of type '" + field.getParentClass().getName() + "'");
    }
  }

  @Override
  public @Nullable CoreExpression getImplementationHere(@NotNull CoreClassField field, @NotNull TypedExpression thisExpr) {
    if (!(field instanceof ClassField)) {
      throw new IllegalArgumentException();
    }

    Expression expr = myImplementations.get(field);
    if (expr == null) {
      return null;
    }

    TypecheckingResult result = TypecheckingResult.fromChecked(thisExpr);
    checkImplementation(field, result.type);
    return expr.subst(myThisBinding, result.expression);
  }

  @Override
  public @Nullable CoreExpression getImplementation(@NotNull CoreClassField field, @NotNull TypedExpression thisExpr) {
    if (!(field instanceof ClassField)) {
      throw new IllegalArgumentException();
    }
    TypecheckingResult result = TypecheckingResult.fromChecked(thisExpr);

    Expression expr = myImplementations.get(field);
    if (expr != null) {
      checkImplementation(field, result.type);
      return expr.subst(myThisBinding, result.expression);
    }

    AbsExpression impl = getDefinition().getImplementation(field);
    if (impl == null) {
      return null;
    }
    checkImplementation(field, result.type);
    return impl.apply(result.expression);
  }

  @Override
  public @Nullable Expression getClosedImplementation(@NotNull CoreClassField field) {
    if (!(field instanceof ClassField)) {
      throw new IllegalArgumentException();
    }
    Expression expr = myImplementations.get(field);
    if (expr != null) {
      return expr.removeUnusedBinding(myThisBinding);
    }
    AbsExpression impl = getDefinition().getImplementation(field);
    return impl == null ? null : impl.getBinding() == null ? impl.getExpression() : impl.getExpression().removeUnusedBinding(impl.getBinding());
  }

  @Override
  public boolean isImplementedHere(@NotNull CoreClassField field) {
    return field instanceof ClassField && myImplementations.containsKey(field);
  }

  @Override
  public boolean isImplemented(@NotNull CoreClassField field) {
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
    NewExpression newExpr = new NewExpression(null, new ClassCallExpression(getDefinition(), getSortArgument(), implementations, Sort.PROP, UniverseKind.NO_UNIVERSES));
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
    return getDefinition().getUseLevel(myImplementations, myThisBinding, false);
  }

  @NotNull
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
  public void subst(InPlaceLevelSubstVisitor substVisitor) {
    substVisitor.visitClassCall(this, null);
  }

  @Override
  public ClassCallExpression strip(StripVisitor visitor) {
    return visitor.visitClassCall(this, null);
  }

  @Override
  public Type normalize(NormalizeVisitor visitor, NormalizationMode mode) {
    return visitor.visitClassCall(this, mode);
  }

  @NotNull
  @Override
  public ClassCallExpression normalize(@NotNull NormalizationMode mode) {
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
    return myUniverseKind;
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
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClassCall(this, params);
  }
}
