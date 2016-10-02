package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.type.PiUniverseType;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.internal.FieldSet;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.ClassCall;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.param;

public class ClassDefinition extends Definition {
  private final Namespace myOwnNamespace;
  private final Namespace myInstanceNamespace;
  private final FieldSet myFieldSet;
  private final Set<ClassDefinition> mySuperClasses;
  private final Map<ClassField, Abstract.ReferableSourceNode> myAliases;

  private ClassField myEnclosingThisField = null;

  public ClassDefinition(Abstract.ClassDefinition abstractDef, Namespace ownNamespace, Namespace instanceNamespace, Map<ClassField, Abstract.ReferableSourceNode> aliases) {
    this(abstractDef, new FieldSet(), new HashSet<ClassDefinition>(), ownNamespace, instanceNamespace, aliases);
  }

  public ClassDefinition(Abstract.ClassDefinition abstractDef, FieldSet fieldSet, Set<ClassDefinition> superClasses, Namespace ownNamespace, Namespace instanceNamespace, Map<ClassField, Abstract.ReferableSourceNode> aliases) {
    super(abstractDef);
    super.hasErrors(false);
    myFieldSet = fieldSet;
    mySuperClasses = superClasses;
    myOwnNamespace = ownNamespace;
    myInstanceNamespace = instanceNamespace;
    myAliases = aliases;
  }

  @Override
  public Abstract.ClassDefinition getAbstractDefinition() {
    return (Abstract.ClassDefinition) super.getAbstractDefinition();
  }

  public FieldSet getFieldSet() {
    return myFieldSet;
  }

  public SortMax getSorts() {
    return myFieldSet.getSorts(getDefCall());
  }

  public boolean isSubClassOf(ClassDefinition classDefinition) {
    if (this.equals(classDefinition)) return true;
    for (ClassDefinition superClass : mySuperClasses) {
      if (superClass.isSubClassOf(classDefinition)) return true;
    }
    return false;
  }

  @Override
  public Type getType(LevelSubstitution polyParams) {
    return new PiUniverseType(EmptyDependentLink.getInstance(), getSorts().subst(polyParams));
  }

  @Override
  public ClassCallExpression getDefCall() {
    return ClassCall(this, myFieldSet);
  }

  @Override
  public ClassCallExpression getDefCall(LevelSubstitution polyParams, List<Expression> args) {
    ClassCallExpression classCall = new ClassCallExpression(this, myFieldSet);
    classCall.setPolyParamsSubst(polyParams);
    return classCall;
  }

  @Override
  public int getNumberOfParameters() {
    return 0;
  }

  @Override
  public void setThisClass(ClassDefinition enclosingClass) {
    assert myEnclosingThisField == null;
    super.setThisClass(enclosingClass);
    if (enclosingClass != null) {
      myEnclosingThisField = new ClassField(null, ClassCall(enclosingClass), this, param("\\this", ClassCall(this)));
      myEnclosingThisField.setThisClass(this);
      myFieldSet.addField(myEnclosingThisField, getDefCall());
    }
  }

  public ClassField getEnclosingThisField() {
    return myEnclosingThisField;
  }

  @Override
  public Type getTypeWithThis(LevelSubstitution polyParams) {
    DependentLink link = EmptyDependentLink.getInstance();
    if (getThisClass() != null) {
      // TODO: set polyParams?
      link = param(ClassCall(getThisClass()));
    }
    return new PiUniverseType(link, getSorts().subst(polyParams));
  }

  @Override
  public Namespace getOwnNamespace() {
    return myOwnNamespace;
  }

  @Override
  public Namespace getInstanceNamespace() {
    return myInstanceNamespace;
  }

  public Abstract.ReferableSourceNode getFieldAlias(ClassField field) {
    Abstract.ReferableSourceNode alias = myAliases.get(field);
    return alias != null ? alias : field.getAbstractDefinition();
  }
}
