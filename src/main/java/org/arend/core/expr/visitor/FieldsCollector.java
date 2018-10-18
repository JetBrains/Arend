package org.arend.core.expr.visitor;

import org.arend.core.definition.ClassField;
import org.arend.core.expr.Expression;
import org.arend.core.expr.FieldCallExpression;

import java.util.HashSet;
import java.util.Set;

public class FieldsCollector extends VoidExpressionVisitor<Void> {
  private final Set<? extends ClassField> myFields;
  private final Set<ClassField> myResult = new HashSet<>();

  public FieldsCollector(Set<? extends ClassField> fields) {
    myFields = fields;
  }

  public Set<ClassField> getResult() {
    return myResult;
  }

  public static Set<ClassField> getFields(Expression expr, Set<? extends ClassField> fields) {
    FieldsCollector collector = new FieldsCollector(fields);
    expr.accept(collector, null);
    return collector.myResult;
  }

  @Override
  public Void visitFieldCall(FieldCallExpression expr, Void params) {
    if (myFields == null || myFields.contains(expr.getDefinition())) {
      myResult.add(expr.getDefinition());
    }
    return super.visitFieldCall(expr, params);
  }
}
