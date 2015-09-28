package com.jetbrains.jetpad.vclang.term.statement.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionPrettyPrintVisitor;

import java.util.List;

public class StatementPrettyPrintVisitor implements AbstractStatementVisitor<Void, Void> {
  private final StringBuilder myBuilder;
  private final List<String> myNames;
  private int myIndent;

  public StatementPrettyPrintVisitor(StringBuilder builder, List<String> names, int indent) {
    myBuilder = builder;
    myNames = names;
    myIndent = indent;
  }

  @Override
  public Void visitDefine(Abstract.DefineStatement stat, Void params) {
    stat.getDefinition().accept(new DefinitionPrettyPrintVisitor(myBuilder, myNames, myIndent), null);
    return null;
  }

  @Override
  public Void visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, Void params) {
    switch (stat.getKind()) {
      case OPEN:
        myBuilder.append("\\open ");
        break;
      case CLOSE:
        myBuilder.append("\\close ");
        break;
      case EXPORT:
        myBuilder.append("\\export ");
        break;
      default:
        throw new IllegalStateException();
    }

    myBuilder.append(stat.getPath().get(0).getName());
    for (int i = 1; i < stat.getPath().size(); i++) {
      myBuilder.append('.').append(stat.getPath().get(0).getName());
    }
    if (stat.getNames() != null) {
      myBuilder.append('(');
      if (!stat.getNames().isEmpty()) {
        myBuilder.append(stat.getNames().get(0).getName());
        for (int i = 1; i < stat.getNames().size(); i++) {
          myBuilder.append(", ").append(stat.getNames().get(0).getName());
        }
      }
      myBuilder.append(')');
    }
    return null;
  }
}
