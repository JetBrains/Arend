package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.pattern.ConstructorPattern;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.pattern.Utils.prettyPrintPattern;

public class Condition implements Abstract.Condition {
  private final ConstructorPattern myPattern;
  private final Expression  myTerm;

  public Condition(ConstructorPattern pattern, Expression term) {
    this.myPattern = pattern;
    this.myTerm = term;
  }


  @Override
  public Abstract.ConstructorPattern getPattern() {
    return myPattern;
  }

  @Override
  public Abstract.Expression getTerm() {
    return myTerm;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    prettyPrintCondition(this, builder, names);
  }

  public static void prettyPrintCondition(Abstract.Condition condition, StringBuilder builder, List<String> names) {
    try (Utils.ContextSaver ignore = new Utils.ContextSaver(names)) {
      prettyPrintPattern(condition.getPattern(), builder, names);
      builder.append(" => ");
      condition.getTerm().prettyPrint(builder, names, Abstract.Expression.PREC);
    }
  }
}
