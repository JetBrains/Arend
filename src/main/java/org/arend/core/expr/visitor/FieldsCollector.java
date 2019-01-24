package org.arend.core.expr.visitor;

import org.arend.core.definition.ClassField;
import org.arend.core.expr.Expression;
import org.arend.core.expr.FieldCallExpression;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FieldsCollector extends VoidExpressionVisitor<Void> {
  private final Set<? extends ClassField> myFields;
  private final Set<ClassField> myResult;

  private FieldsCollector(Set<? extends ClassField> fields, Set<ClassField> result) {
    myFields = fields;
    myResult = result;
  }

  public Set<ClassField> getResult() {
    return myResult;
  }

  public static void getFields(Expression expr, Set<? extends ClassField> fields, Set<ClassField> result) {
    if (!fields.isEmpty()) {
      expr.accept(new FieldsCollector(fields, result), null);
    }
  }

  public static Set<ClassField> getFields(Expression expr, Set<? extends ClassField> fields) {
    if (fields.isEmpty()) {
      return Collections.emptySet();
    }

    Set<ClassField> result = new HashSet<>();
    getFields(expr, fields, result);
    return result;
  }

  @Override
  public Void visitFieldCall(FieldCallExpression expr, Void params) {
    if (myFields == null || myFields.contains(expr.getDefinition())) {
      myResult.add(expr.getDefinition());
    }
    return super.visitFieldCall(expr, params);
  }
}
