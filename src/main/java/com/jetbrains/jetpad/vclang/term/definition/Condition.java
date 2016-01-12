package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.param.Utils;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.pattern.Utils.prettyPrintPatternArg;

public class Condition implements Abstract.Condition {
  private final Constructor myConstructor;
  private final ElimTreeNode myElimTree;

  public Condition(Constructor constructor, ElimTreeNode elimTree) {
    myConstructor = constructor;
    myElimTree = elimTree;
  }

  public Constructor getConstructor() {
    return myConstructor;
  }

  @Override
  public Name getConstructorName() {
    return myConstructor.getName();
  }

  @Override
  public List<Abstract.PatternArgument> getPatterns() {
    throw new UnsupportedOperationException();
  }

  public ElimTreeNode getElimTree() {
    return myElimTree;
  }

  @Override
  public Abstract.Expression getTerm() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    prettyPrintCondition(this, builder, names);
  }

  public static void prettyPrintCondition(Abstract.Condition condition, StringBuilder builder, List<String> names) {
    try (Utils.ContextSaver ignore = new Utils.ContextSaver(names)) {
      builder.append(condition.getConstructorName());
      for (Abstract.PatternArgument patternArg : condition.getPatterns()) {
        if (!patternArg.isHidden()) {
          builder.append(" ");
          prettyPrintPatternArg(patternArg, builder, names);
        }
      }
      builder.append(" => ");
      condition.getTerm().prettyPrint(builder, names, Abstract.Expression.PREC);
    }
  }
}
