package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import javax.annotation.Nonnull;
import java.util.*;

public class ClassDefinition extends Definition {
  public static class Implementation {
    @Nonnull public final TypedDependentLink thisParam;
    @Nonnull public final Expression term;

    public Implementation(@Nonnull TypedDependentLink thisParam, @Nonnull Expression term) {
      this.thisParam = thisParam;
      this.term = term;
    }

    public Expression substThisParam(Expression thisExpr) {
      return term.subst(thisParam, thisExpr);
    }
  }

  private final Set<ClassDefinition> mySuperClasses;
  private final LinkedHashSet<ClassField> myFields;
  private final List<ClassField> myPersonalFields;
  private final Map<ClassField, Implementation> myImplemented;
  private Sort mySort;

  private ClassField myEnclosingThisField = null;

  public ClassDefinition(GlobalReferable referable) {
    super(referable, TypeCheckingStatus.HEADER_HAS_ERRORS);
    mySuperClasses = new HashSet<>();
    myFields = new LinkedHashSet<>();
    myPersonalFields = new ArrayList<>();
    myImplemented = new HashMap<>();
    mySort = Sort.PROP;
  }

  public void updateSorts() {
    ClassCallExpression thisClass = new ClassCallExpression(this, Sort.STD, Collections.emptyMap(), mySort);
    mySort = Sort.PROP;

    for (ClassField field : myFields) {
      if (myImplemented.containsKey(field)) {
        continue;
      }

      Expression baseType = field.getBaseType(thisClass.getSortArgument());
      if (baseType.isInstance(ErrorExpression.class)) {
        continue;
      }

      DependentLink thisParam = ExpressionFactory.parameter("this", thisClass);
      Expression expr = baseType.subst(field.getThisParameter(), new ReferenceExpression(thisParam)).normalize(NormalizeVisitor.Mode.WHNF);
      Sort sort = expr.getType().toSort();
      if (sort != null) {
        mySort = mySort.max(sort);
      }
    }
  }

  public Sort getSort() {
    return mySort;
  }

  public void setSort(Sort sort) {
    mySort = sort;
  }

  public boolean isSubClassOf(ClassDefinition classDefinition) {
    if (this.equals(classDefinition)) return true;
    for (ClassDefinition superClass : mySuperClasses) {
      if (superClass.isSubClassOf(classDefinition)) return true;
    }
    return false;
  }

  public Set<? extends ClassDefinition> getSuperClasses() {
    return mySuperClasses;
  }

  public void addSuperClass(ClassDefinition superClass) {
    mySuperClasses.add(superClass);
  }

  public Set<? extends ClassField> getFields() {
    return myFields;
  }

  public List<? extends ClassField> getPersonalFields() {
    return myPersonalFields;
  }

  public void addField(ClassField field) {
    myFields.add(field);
  }

  public void addPersonalField(ClassField field) {
    myPersonalFields.add(field);
  }

  public void addFields(Collection<? extends ClassField> fields) {
    myFields.addAll(fields);
  }

  public boolean isImplemented(ClassField field) {
    return myImplemented.containsKey(field);
  }

  public Set<Map.Entry<ClassField, Implementation>> getImplemented() {
    return myImplemented.entrySet();
  }

  public Implementation getImplementation(ClassField field) {
    return myImplemented.get(field);
  }

  public void implementField(ClassField field, Implementation impl) {
    myImplemented.put(field, impl);
  }

  @Override
  public Expression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument) {
    if (getThisClass() != null) {
      params.add(ExpressionFactory.parameter(null, new ClassCallExpression(getThisClass(), sortArgument)));
    }
    return new UniverseExpression(getSort().subst(sortArgument.toLevelSubstitution()));
  }

  @Override
  public ClassCallExpression getDefCall(Sort sortArgument, Expression thisExpr, List<Expression> args) {
    return new ClassCallExpression(this, sortArgument, thisExpr == null ? Collections.emptyMap() : Collections.singletonMap(myEnclosingThisField, thisExpr), mySort);
  }

  @Override
  public void setThisClass(ClassDefinition enclosingClass) {
    assert myEnclosingThisField == null;
    super.setThisClass(enclosingClass);
    if (enclosingClass != null) {
      myEnclosingThisField = new ClassField(null, new ClassCallExpression(enclosingClass, Sort.STD), this, ExpressionFactory.parameter("\\this", new ClassCallExpression(this, Sort.STD)));
      myEnclosingThisField.setThisClass(this);
      myFields.add(myEnclosingThisField);
    }
  }

  public ClassField getEnclosingThisField() {
    return myEnclosingThisField;
  }

  public void setEnclosingThisField(ClassField field) {
    myEnclosingThisField = field;
  }
}
