package com.jetbrains.jetpad.vclang.term.legacy;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;

import java.util.Collection;

public class ToTextVisitor extends PrettyPrintVisitor implements LegacyAbstractStatementVisitor<Void, Void>, AbstractDefinitionVisitor<Void, Void> {
  public ToTextVisitor(StringBuilder builder, int indent) {
    super(builder, indent, true);
  }

  public static String toText(Abstract.Definition definition, int indent) {
    StringBuilder builder = new StringBuilder();
    new ToTextVisitor(builder, indent).prettyPrint(definition, Abstract.Expression.PREC);
    return builder.toString();
  }

  private void visitWhere(Collection<? extends LegacyAbstract.Statement> statements) {
    myBuilder.append(" \\where {\n");
    myIndent += INDENT;
    boolean previousWasNC = false;
    boolean isFirst = true;
    for (LegacyAbstract.Statement statement : statements) {
      boolean isNamespaceCommand = statement instanceof LegacyAbstract.NamespaceCommandStatement;
      if (!isNamespaceCommand && previousWasNC) this.myBuilder.append('\n');
      printIndent();
      statement.accept(this, null);
      if (isNamespaceCommand) this.myBuilder.append('\n');
      previousWasNC = isNamespaceCommand;
      isFirst = false;
    }
    myIndent -= INDENT;
    myBuilder.append("}");
  }

  @Override
  public Void visitFunction(final Abstract.FunctionDefinition def, Void ignored) {
    super.visitFunction(def, ignored);

    Collection<? extends LegacyAbstract.Statement> globalStatements = LegacyAbstract.getGlobalStatements(def);
    if (!globalStatements.isEmpty()) {
      printIndent();
      visitWhere(globalStatements);
    }

    return null;
  }

  public void visitModule(Abstract.ClassDefinition module) {
    boolean previousWasNC = false;
    boolean isFirst = true;
    for (LegacyAbstract.Statement statement : LegacyAbstract.getGlobalStatements(module)) {
      boolean isNamespaceCommand = statement instanceof LegacyAbstract.NamespaceCommandStatement;
      if (!isNamespaceCommand && previousWasNC) this.myBuilder.append('\n');
      statement.accept(this, null);
      if (isNamespaceCommand) this.myBuilder.append('\n');
      previousWasNC = isNamespaceCommand;
      isFirst = false;
    }
  }

  @Override
  public Void visitClass(Abstract.ClassDefinition def, Void ignored) {
    super.visitClass(def, ignored);

    Collection<? extends LegacyAbstract.Statement> globalStatements = LegacyAbstract.getGlobalStatements(def);
    if (!globalStatements.isEmpty()) {
      myBuilder.append(" ");
      visitWhere(globalStatements);
    }

    return null;
  }

  @Override
  public Void visitDefine(LegacyAbstract.DefineStatement stat, Void params) {
    stat.getDefinition().accept(this, params);
    this.myBuilder.append("\n\n");
    return null;
  }

  @Override
  public Void visitNamespaceCommand(LegacyAbstract.NamespaceCommandStatement stat, Void params) {
    switch (stat.getKind()) {
      case OPEN:
        myBuilder.append("\\open ");
        break;
      case EXPORT:
        myBuilder.append("\\export ");
        break;
      default:
        throw new IllegalStateException();
    }

    if (stat.getModulePath() != null) {
      myBuilder.append(stat.getModulePath());
    }

    if (!stat.getPath().isEmpty()){
      myBuilder.append(stat.getPath().get(0));
      for (int i = 1; i < stat.getPath().size(); i++) {
        myBuilder.append('.').append(stat.getPath().get(i));
      }
    }

    if (stat.getNames() != null) {
      if (stat.isHiding()) {
        myBuilder.append(" \\hiding");
      }
      myBuilder.append(" (");
      if (!stat.getNames().isEmpty()) {
        myBuilder.append(stat.getNames().get(0));
        for (int i = 1; i < stat.getNames().size(); i++) {
          myBuilder.append(", ").append(stat.getNames().get(i));
        }
      }
      myBuilder.append(')');
    }

    return null;
  }
}
