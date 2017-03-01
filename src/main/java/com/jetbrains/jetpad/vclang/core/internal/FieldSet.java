package com.jetbrains.jetpad.vclang.core.internal;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.SortMax;

import java.util.*;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.param;

public class FieldSet implements ReadonlyFieldSet {
  public static class Implementation {
    public final DependentLink thisParam;
    public final Expression term;

    public Implementation(DependentLink thisParam, Expression term) {
      this.thisParam = thisParam;
      this.term = term;
    }

    public Expression substThisParam(Expression thisExpr) {
      return thisParam == null ? term : term.subst(thisParam, thisExpr);
    }
  }

  private final LinkedHashSet<ClassField> myFields;
  private final Map<ClassField, Implementation> myImplemented;
  private SortMax mySorts;

  public FieldSet() {
    this(new LinkedHashSet<ClassField>(), new HashMap<ClassField, Implementation>(), null);
  }

  public FieldSet(FieldSet other) {
    this(new LinkedHashSet<>(other.myFields), new HashMap<>(other.myImplemented), other.mySorts);
  }

  private FieldSet(LinkedHashSet<ClassField> fields, Map<ClassField, Implementation> implemented, SortMax sorts) {
    myFields = fields;
    myImplemented = implemented;
    mySorts = sorts;
  }

  public void addField(ClassField field, SortMax sorts) {
    myFields.add(field);
    if (mySorts != null) {
      if (sorts != null) {
        mySorts.add(sorts);
      } else {
        mySorts = null;
      }
    }
  }

  public void addFieldsFrom(ReadonlyFieldSet other) {
    for (ClassField field : other.getFields()) {
      addField(field, null);
    }
  }

  public boolean implementField(ClassField field, Implementation impl) {
    assert myFields.contains(field);
    Implementation old = myImplemented.put(field, impl);
    mySorts = null;
    return old == null;
  }

  @Override
  public boolean isImplemented(ClassField field) {
    return myImplemented.containsKey(field);
  }

  @Override
  public Set<ClassField> getFields() {
    return myFields;
  }

  @Override
  public Set<Map.Entry<ClassField, Implementation>> getImplemented() {
    return myImplemented.entrySet();
  }

  @Override
  public Implementation getImplementation(ClassField field) {
    return myImplemented.get(field);
  }

  @Override
  public SortMax getSorts() {
    return mySorts;
  }

  public void setSorts(SortMax sorts) {
    mySorts = sorts;
  }

  @Override
  public void updateSorts(ClassCallExpression thisClass) {
    mySorts = new SortMax();
    for (ClassField field : myFields) {
      updateUniverseSingleField(field, thisClass);
    }
  }

  private void updateUniverseSingleField(ClassField field, ClassCallExpression thisClass) {
    if (myImplemented.containsKey(field)) return;

    Expression baseType = field.getBaseType(thisClass.getLevelArguments());
    if (baseType.toError() != null) return;

    DependentLink thisParam = param("\\this", thisClass);
    Expression expr1 = baseType.subst(field.getThisParameter(), ExpressionFactory.Reference(thisParam)).normalize(NormalizeVisitor.Mode.WHNF);
    SortMax sorts = null;
    if (expr1.toOfType() != null) {
      TypeMax type = expr1.toOfType().getExpression().getType();
      if (type != null) {
        sorts = type.toSorts();
      }
    }
    if (sorts == null) {
      TypeMax type = expr1.getType();
      if (type != null) {
        sorts = type.toSorts();
      }
    }

    if (sorts != null) {
      mySorts.add(sorts);
    }
  }

  public static <P> FieldSet applyVisitorToImplemented(ReadonlyFieldSet fieldSet, ReadonlyFieldSet parentFieldSet, ExpressionVisitor<P, Expression> visitor, P arg) {
    FieldSet newFieldSet = new FieldSet();
    newFieldSet.addFieldsFrom(fieldSet);
    for (Map.Entry<ClassField, FieldSet.Implementation> entry : fieldSet.getImplemented()) {
      if (parentFieldSet != null && parentFieldSet.isImplemented(entry.getKey())) {
        newFieldSet.implementField(entry.getKey(), entry.getValue());
      } else {
        newFieldSet.implementField(entry.getKey(), new FieldSet.Implementation(entry.getValue().thisParam, entry.getValue().term.accept(visitor, arg)));
      }
    }
    return newFieldSet;
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
