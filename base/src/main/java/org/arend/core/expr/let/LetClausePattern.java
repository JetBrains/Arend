package org.arend.core.expr.let;

import org.arend.core.definition.ClassField;

import java.util.List;

public interface LetClausePattern {
  boolean isMatching();
  String getName();
  List<? extends ClassField> getFields();
  List<? extends LetClausePattern> getPatterns();
}
