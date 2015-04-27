package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.removeFromList;

public class PrettyPrintVisitor implements AbstractDefinitionVisitor<Byte, Void> {
  private final StringBuilder myBuilder;
  private final List<String> myNames;

  public PrettyPrintVisitor(StringBuilder builder, List<String> names) {
    myBuilder = builder;
    myNames = names;
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, Byte prec) {
    myBuilder.append("\\function\n");
    if (def.getFixity() == Definition.Fixity.PREFIX) {
      myBuilder.append(def.getName());
    } else {
      myBuilder.append('(').append(def.getName()).append(')');
    }
    for (Abstract.TelescopeArgument argument : def.getArguments()) {
      myBuilder.append(' ');
      argument.prettyPrint(myBuilder, myNames, Abstract.VarExpression.PREC);
    }
    if (def.getResultType() != null) {
      myBuilder.append(" : ");
      def.getResultType().prettyPrint(myBuilder, myNames, Abstract.Expression.PREC);
    }
    myBuilder.append(def.getArrow() == Abstract.Definition.Arrow.RIGHT ? " => " : " <= ");
    def.getTerm().prettyPrint(myBuilder, myNames, Abstract.Expression.PREC);
    removeFromList(myNames, def.getArguments());
    return null;
  }
}
