package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class ClassDefinition extends Definition {
  private final Namespace myOwnNamespace;
  private final Namespace myInstanceNamespace;

  private SortMax mySorts;

  private final Map<ClassField, FieldImplementation> myFields = new HashMap<>();
  private Set<ClassDefinition> mySuperClasses = null;

  public static class FieldImplementation {
    public String name;
    public DependentLink thisParameter;
    public Expression implementation;

    public FieldImplementation(String name, DependentLink thisParameter, Expression implementation) {
      this.name = name;
      this.thisParameter = thisParameter;
      this.implementation = implementation;
    }

    public boolean isImplemented() {
      return implementation != null;
    }
  }

  public ClassDefinition(String name, Namespace ownNamespace, Namespace instanceNamespace) {
    super(name, Abstract.Binding.DEFAULT_PRECEDENCE);
    super.hasErrors(false);
    myOwnNamespace = ownNamespace;
    myInstanceNamespace = instanceNamespace;
    mySorts = new SortMax();
  }

  public boolean isSubClassOf(ClassDefinition classDefinition) {
    if (this == classDefinition) {
      return true;
    }
    if (mySuperClasses == null) {
      return false;
    }
    for (ClassDefinition superClass : mySuperClasses) {
      if (superClass.isSubClassOf(classDefinition)) {
        return true;
      }
    }
    return false;
  }

  public void addSuperClass(ClassDefinition superClass) {
    if (mySuperClasses == null) {
      mySuperClasses = new HashSet<>();
    }
    mySuperClasses.add(superClass);
  }

  public ExprSubstitution getImplementedFields() {
    ExprSubstitution result = new ExprSubstitution();
    for (Map.Entry<ClassField, FieldImplementation> entry : myFields.entrySet()) {
      if (entry.getValue().isImplemented()) {
        result.add(entry.getKey(), Lam(entry.getValue().thisParameter, entry.getValue().implementation));
      }
    }
    return result;
  }

  public SortMax getSorts() {
    return mySorts;
  }

  public void setSorts(SortMax sorts) {
    mySorts = sorts;
  }

  public void updateSort() {
    mySorts = new SortMax();
    ExprSubstitution substitution = getImplementedFields();
    for (Map.Entry<ClassField, FieldImplementation> entry : myFields.entrySet()) {
      if (!entry.getKey().hasErrors() && !entry.getValue().isImplemented()) {
        entry.getKey().updateSort(mySorts, substitution);
      }
    }
  }

  @Override
  public Expression getType() {
    // TODO: [sorts] return type
    return new UniverseExpression(mySorts.toSort());
  }

  @Override
  public ClassCallExpression getDefCall() {
    return ClassCall(this, new HashMap<ClassField, ClassCallExpression.ImplementStatement>());
  }

  public Map.Entry<ClassField, FieldImplementation> getFieldEntry(String name) {
    for (Map.Entry<ClassField, FieldImplementation> entry : myFields.entrySet()) {
      if (entry.getValue().name.equals(name)) {
        return entry;
      }
    }
    return null;
  }

  public ClassField getField(String name) {
    Map.Entry<ClassField, FieldImplementation> entry = getFieldEntry(name);
    return entry == null ? null : entry.getKey();
  }

  public String getFieldName(ClassField field) {
    return myFields.get(field).name;
  }

  public FieldImplementation getFieldImpl(ClassField field) {
    return myFields.get(field);
  }

  public void setFieldImpl(ClassField field, DependentLink thisParameter, Expression implementation) {
    FieldImplementation impl = myFields.get(field);
    impl.thisParameter = thisParameter;
    impl.implementation = implementation;
  }

  public Collection<ClassField> getFields() {
    return myFields.keySet();
  }

  public Set<Map.Entry<ClassField, FieldImplementation>> getFieldsMap() {
    return myFields.entrySet();
  }

  public int getNumberOfVisibleFields() {
    int result = myFields.size();
    if (getParentField() != null) {
      --result;
    }
    return result;
  }

  public void addField(ClassField field) {
    addField(field, field.getName(), null, null);
  }

  public void addField(ClassField field, String name, DependentLink thisParameter, Expression implementation) {
    if (name.equals("\\parent")) {
      ClassField oldField = getField(name);
      if (oldField != null) {
        myFields.remove(oldField);
      }
    }
    myFields.put(field, new FieldImplementation(name, thisParameter, implementation));
    field.setThisClass(this);
  }

  public void addExistingField(ClassField field, FieldImplementation impl) {
    myFields.put(field, impl);
  }

  public ClassField removeField(String name) {
    ClassField field = getField(name);
    myFields.remove(field);
    return field;
  }

  public ClassField getParentField() {
    return getField("\\parent");
  }

  public void addParentField(ClassDefinition parentClass) {
    setThisClass(parentClass);
    ClassField field = new ClassField("\\parent", Abstract.Binding.DEFAULT_PRECEDENCE, ClassCall(parentClass), this, param("\\this", ClassCall(this)));
    addField(field);
    // TODO[\\parent] is this required?
    //getResolvedName().toNamespace().addDefinition(field);
  }

  @Override
  public Expression getTypeWithThis() {
    Expression type = getType();
    if (getThisClass() != null) {
      type = Pi(ClassCall(getThisClass()), type);
    }
    return type;
  }

  @Override
  public Namespace getOwnNamespace() {
    return myOwnNamespace;
  }

  @Override
  public Namespace getInstanceNamespace() {
    return myInstanceNamespace;
  }
}
