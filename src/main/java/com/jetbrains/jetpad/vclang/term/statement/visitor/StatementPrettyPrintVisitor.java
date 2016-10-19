package com.jetbrains.jetpad.vclang.term.statement.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;

public class StatementPrettyPrintVisitor implements AbstractStatementVisitor<Void, Void> {
  private final StringBuilder myBuilder;
  private int myIndent;
  private Abstract.DefineStatement.StaticMod myDefaultStaticMod;

  public StatementPrettyPrintVisitor(StringBuilder builder, int indent, Abstract.DefineStatement.StaticMod defaultStaticMod) {
    myBuilder = builder;
    myIndent = indent;
    myDefaultStaticMod = defaultStaticMod;
  }

  @Override
  public Void visitDefine(Abstract.DefineStatement stat, Void params) {
    if (stat.getStaticMod() != myDefaultStaticMod)
      if (stat.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC) {
        myBuilder.append("\\static ");
      } else if (stat.getStaticMod() == Abstract.DefineStatement.StaticMod.DYNAMIC) {
        myBuilder.append("\\dynamic ");
      }
    stat.getDefinition().accept(new PrettyPrintVisitor(myBuilder, myIndent), null);
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

    if (stat.getModulePath() != null) {
      for (int i = 0; i < stat.getModulePath().size(); i++) {
        myBuilder.append("::").append(stat.getModulePath().get(i));
      }
    }

    if (!stat.getPath().isEmpty()){
      myBuilder.append(stat.getPath().get(0));
      for (int i = 1; i < stat.getPath().size(); i++) {
        myBuilder.append('.').append(stat.getPath().get(i));
      }
    }

    if (stat.getNames() != null) {
      myBuilder.append(" (");
      if (!stat.getNames().isEmpty()) {
        myBuilder.append(new Name(stat.getNames().get(0)).getPrefixName());
        for (int i = 1; i < stat.getNames().size(); i++) {
          myBuilder.append(", ").append(new Name(stat.getNames().get(i)).getPrefixName());
        }
      }
      myBuilder.append(')');
    }
    return null;
  }

  @Override
  public Void visitDefaultStaticCommand(Abstract.DefaultStaticStatement stat, Void params) {
    if (stat.isStatic()) {
      myBuilder.append("\\allstatic");
      myDefaultStaticMod = Abstract.DefineStatement.StaticMod.STATIC;
    } else {
      myBuilder.append("\\alldynamic");
      myDefaultStaticMod = Abstract.DefineStatement.StaticMod.DYNAMIC;
    }
    return null;
  }
}
