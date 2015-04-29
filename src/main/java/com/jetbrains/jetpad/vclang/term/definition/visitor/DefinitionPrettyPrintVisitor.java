package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.removeFromList;

public class DefinitionPrettyPrintVisitor implements AbstractDefinitionVisitor<Byte, Void> {
  private final StringBuilder myBuilder;
  private final List<String> myNames;

  public DefinitionPrettyPrintVisitor(StringBuilder builder, List<String> names) {
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
      def.getResultType().accept(new PrettyPrintVisitor(myBuilder, myNames, 0), Abstract.Expression.PREC);
    }
    myBuilder.append(def.getArrow() == Abstract.Definition.Arrow.RIGHT ? " => " : " <= ");
    def.getTerm().accept(new PrettyPrintVisitor(myBuilder, myNames, 0), Abstract.Expression.PREC);
    removeFromList(myNames, def.getArguments());
    return null;
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, Byte params) {
    myBuilder.append("\\dat a");
    if (def.getFixity() == Abstract.Definition.Fixity.PREFIX) {
      myBuilder.append(def.getName());
    } else {
      myBuilder.append('(').append(def.getName()).append(')');
    }
    for (Abstract.TypeArgument parameter : def.getParameters()) {
      myBuilder.append(' ');
      parameter.prettyPrint(myBuilder, myNames, Abstract.VarExpression.PREC);
    }
    if (def.getUniverse() != null) {
      myBuilder.append(" : ").append(def.getUniverse());
    }
    for (Abstract.Constructor constructor : def.getConstructors()) {
      myBuilder.append("\n    | ");
      constructor.accept(this, Abstract.Expression.PREC);
    }
    removeFromList(myNames, def.getParameters());
    return null;
  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, Byte params) {
    myBuilder.append(def.getName());
    for (Abstract.TypeArgument argument : def.getArguments()) {
      myBuilder.append(' ');
      argument.prettyPrint(myBuilder, myNames, Abstract.VarExpression.PREC);
    }
    removeFromList(myNames, def.getArguments());
    return null;
  }
}
