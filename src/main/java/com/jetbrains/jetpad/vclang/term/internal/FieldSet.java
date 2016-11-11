package com.jetbrains.jetpad.vclang.term.internal;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
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

  private final LinkedHashSet<ClassField> myFields;
  private final Map<ClassField, Implementation> myImplemented;
  private SortMax mySorts;

  public FieldSet() {
    this(new LinkedHashSet<ClassField>(), new HashMap<ClassField, Implementation>(), new SortMax());
  }

  public FieldSet(FieldSet other) {
    this(new LinkedHashSet<>(other.myFields), new HashMap<>(other.myImplemented), other.mySorts);
  }

  private FieldSet(LinkedHashSet<ClassField> fields, Map<ClassField, Implementation> implemented, SortMax sorts) {
    myFields = fields;
    myImplemented = implemented;
    mySorts = sorts;
  }

  public void addField(ClassField field) {
    myFields.add(field);
    mySorts = null;
  }

  public void addFieldsFrom(FieldSet other) {
    for (ClassField field : other.myFields) {
      addField(field);
    }
  }

  public boolean implementField(ClassField field, Implementation impl) {
    assert myFields.contains(field);
    Implementation old = myImplemented.put(field, impl);
    mySorts = null;
    return old == null;
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

  public SortMax getSorts(ClassCallExpression thisClass) {
    if (mySorts == null) {
      updateUniverse(thisClass);
    }
    return mySorts;
  }

  private void updateUniverse(ClassCallExpression thisClass) {
    mySorts = new SortMax();
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
    SortMax sorts = null;
    if (expr1.toOfType() != null) {
      sorts = expr1.toOfType().getExpression().getType().toSorts();
    }
    if (sorts == null) {
      sorts = expr1.getType().toSorts();
    }

    assert sorts != null;
    mySorts.add(sorts);
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
