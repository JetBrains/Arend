package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.pattern.Utils.prettyPrintPattern;

public class Condition implements Abstract.Condition {
  private final Constructor myConstructor;
  private final List<Pattern>  myPatterns;
  private final Expression  myTerm;

  public Condition(Constructor constructor, List<Pattern> patterns, Expression term) {
    myConstructor = constructor;
    myPatterns = patterns;
    myTerm = term;
  }

  public Constructor getConstructor() {
    return myConstructor;
  }

  @Override
  public Name getConstructorName() {
    return myConstructor.getName();
  }

  @Override
  public List<Pattern> getPatterns() {
    return myPatterns;
  }

  @Override
  public void replacePatternWithConstructor(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Expression getTerm() {
    return myTerm;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    prettyPrintCondition(this, builder, names);
  }

  public static void prettyPrintCondition(Abstract.Condition condition, StringBuilder builder, List<String> names) {
    try (Utils.ContextSaver ignore = new Utils.ContextSaver(names)) {
      builder.append(condition.getConstructorName());
      for (Abstract.Pattern pattern : condition.getPatterns()) {
        builder.append(" ");
        prettyPrintPattern(pattern, builder, names);
      }
      builder.append(" => ");
      condition.getTerm().prettyPrint(builder, names, Abstract.Expression.PREC);
    }
  }
}
