package com.jetbrains.jetpad.vclang.term.internal;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.TypeUniverse;
import com.jetbrains.jetpad.vclang.term.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class FieldSet {
  public static class Implementation {
    public final DependentLink thisParam;
    public final Expression term;

    public Implementation(DependentLink thisParam, Expression term) {
      this.thisParam = thisParam;
      this.term = term;
    }

    public Expression getWithThisParam() {
      return Lam(thisParam, term);
    }

    public Expression substThisParam(Expression thisExpr) {
      return thisParam == null ? term : term.subst(thisParam, thisExpr);
    }
  }

  private final Set<ClassField> myFields;
  private final Map<ClassField, Implementation> myImplemented;
  private TypeUniverse myUniverse;

  public FieldSet() {
    this(new HashSet<ClassField>(), new HashMap<ClassField, Implementation>(), TypeUniverse.PROP);
  }

  public FieldSet(FieldSet other) {
    this(new HashSet<>(other.myFields), new HashMap<>(other.myImplemented), other.myUniverse);
  }

  private FieldSet(Set<ClassField> fields, Map<ClassField, Implementation> implemented, TypeUniverse universe) {
    myFields = fields;
    myImplemented = implemented;
    myUniverse = universe;
  }

  public void addField(ClassField field, ClassCallExpression thisClass) {
    myFields.add(field);
    updateUniverseFieldAdded(field, thisClass);
  }

  public void addFieldsFrom(FieldSet other, ClassCallExpression thisClass) {
    for (ClassField field : other.myFields) {
      addField(field, thisClass);
    }
  }

  public boolean implementField(ClassField field, Implementation impl, ClassCallExpression thisClass) {
    assert myFields.contains(field);
    ReferenceExpression thisRef = Reference(param("\\this", thisClass));
    Implementation old = myImplemented.put(field, impl);  // TODO[java8]: putIfAbsent
    updateUniverseFieldImplemented(field, thisClass);
    return old == null || old.substThisParam(thisRef).equals(impl.substThisParam(thisRef));
  }

  public CheckTypeVisitor.Result implementField(ClassField field, Abstract.Expression implBody, CheckTypeVisitor visitor, ClassCallExpression fieldSetClass, DependentLink thisParam) {
    CheckTypeVisitor.Result result = visitor.typeCheck(implBody, field.getBaseType().subst(field.getThisParameter(), Reference(thisParam != null ? thisParam : param("\\this", fieldSetClass))));
    if (result != null) {
      implementField(field, new FieldSet.Implementation(thisParam, result.expression), fieldSetClass);
    }
    return result;
  }

  public boolean isImplemented(ClassField field) {
    return myImplemented.containsKey(field);
  }

  public Set<ClassField> getFields() {
    return myFields;
  }

  public Set<Map.Entry<ClassField, Implementation>> getImplemented() {
    return myImplemented.entrySet();
  }

  public Implementation getImplementation(ClassField field) {
    return myImplemented.get(field);
  }

  public TypeUniverse getUniverse(ClassCallExpression thisClass) {
    if (myUniverse == null) {
      updateUniverse(thisClass);
    }
    return myUniverse;
  }

  private void updateUniverseFieldAdded(ClassField field, ClassCallExpression thisClass) {
    myUniverse = null;  // just invalidate
  }

  private void updateUniverseFieldImplemented(ClassField ignored, ClassCallExpression thisClass) {
    myUniverse = null;  // just invalidate
  }

  private void updateUniverse(ClassCallExpression thisClass) {
    myUniverse = TypeUniverse.PROP;
    for (ClassField field : myFields) {
      updateUniverseSingleField(field, thisClass);
    }
  }

  private void updateUniverseSingleField(ClassField field, ClassCallExpression thisClass) {
    if (myImplemented.containsKey(field)) return;

    Expression baseType = field.getBaseType();
    if (baseType.toError() != null) return;

    DependentLink thisParam = param("\\this", thisClass);
    Expression expr1 = baseType.subst(field.getThisParameter(), Reference(thisParam)).normalize(NormalizeVisitor.Mode.WHNF);
    UniverseExpression expr = null;
    if (expr1.toOfType() != null) {
      Expression expr2 = expr1.toOfType().getExpression().getType();
      if (expr2 != null) {
        expr = expr2.normalize(NormalizeVisitor.Mode.WHNF).toUniverse();
      }
    }
    if (expr == null) {
      expr = expr1.getType().normalize(NormalizeVisitor.Mode.WHNF).toUniverse();
    }

    assert expr != null;
    TypeUniverse fieldUniverse = expr.getUniverse();
    myUniverse = myUniverse.max(fieldUniverse);
  }

  @Override
  public String toString() {
    ArrayList<String> fields = new ArrayList<>();
    for (ClassField f : myFields) {
      fields.add(f.getName());
    }
    ArrayList<String> impl = new ArrayList<>();
    for (ClassField f : myImplemented.keySet()) {
      impl.add(f.getName());
    }

    return "All: " + fields.toString() + " Impl: " + impl.toString();
  }
}
