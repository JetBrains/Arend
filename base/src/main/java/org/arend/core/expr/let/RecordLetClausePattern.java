package org.arend.core.expr.let;

import org.arend.core.definition.ClassField;

import java.util.List;

public class RecordLetClausePattern extends TupleLetClausePattern {
  private final List<ClassField> myFields;

  public RecordLetClausePattern(List<ClassField> fields, List<LetClausePattern> patterns) {
    super(patterns);
    myFields = fields;
  }

  @Override
  public List<? extends ClassField> getFields() {
    return myFields;
  }
}
