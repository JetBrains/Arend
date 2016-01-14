package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.statement.visitor.StatementPrettyPrintVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.context.Utils.removeFromList;

public class DefinitionPrettyPrintVisitor implements AbstractDefinitionVisitor<Void, Void> {
  private final StringBuilder myBuilder;
  private final List<String> myNames;
  private int myIndent;

  public DefinitionPrettyPrintVisitor(StringBuilder builder, List<String> names, int indent) {
    myBuilder = builder;
    myNames = names;
    myIndent = indent;
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, Void ignored) {
    myBuilder.append("\\function");
    Abstract.Definition.Precedence precedence = def.getPrecedence();
    if (precedence != null && !precedence.equals(Abstract.Definition.DEFAULT_PRECEDENCE)) {
      myBuilder.append(" \\infix");
      if (precedence.associativity == Abstract.Definition.Associativity.LEFT_ASSOC) myBuilder.append('l');
      if (precedence.associativity == Abstract.Definition.Associativity.RIGHT_ASSOC) myBuilder.append('r');
      myBuilder.append(' ');
      myBuilder.append(precedence.priority);
    }
    myBuilder.append('\n');
    PrettyPrintVisitor.printIndent(myBuilder, myIndent);

    myBuilder.append(def.getName());
    List<? extends Abstract.Argument> arguments = def.getArguments();
    if (arguments != null) {
      for (Abstract.Argument argument : arguments) {
        myBuilder.append(' ');
        argument.prettyPrint(myBuilder, myNames, Abstract.DefCallExpression.PREC);
      }
    } else {
      myBuilder.append("{!error}");
    }

    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      myBuilder.append(" : ");
      resultType.accept(new PrettyPrintVisitor(myBuilder, myNames, myIndent), Abstract.Expression.PREC);
    }
    if (!def.isAbstract()) {
      myBuilder.append(def.getArrow() == Abstract.Definition.Arrow.RIGHT ? " => " : " <= ");
      Abstract.Expression term = def.getTerm();
      if (term != null) {
        term.accept(new PrettyPrintVisitor(myBuilder, myNames, myIndent), Abstract.Expression.PREC);
      } else {
        myBuilder.append("{!error}");
      }
    }

    if (arguments != null) {
      removeFromList(myNames, arguments);
    }

    Collection<? extends Abstract.Statement> statements = def.getStatements();
    if (!statements.isEmpty()) {
      myBuilder.append("\n");
      PrettyPrintVisitor.printIndent(myBuilder, myIndent);
      myBuilder.append("\\where ");
      myIndent += "\\where ".length();
      boolean isFirst = true;
      for (Abstract.Statement statement : statements) {
        if (!isFirst)
          PrettyPrintVisitor.printIndent(myBuilder, myIndent);
        statement.accept(new StatementPrettyPrintVisitor(myBuilder, myNames, myIndent), null);
        myBuilder.append("\n");
        isFirst = false;
      }
      myIndent -= "\\where ".length();
    }
    return null;
  }

  @Override
  public Void visitAbstract(Abstract.AbstractDefinition def, Void params) {
    myBuilder.append("\\abstract ");
    Abstract.Definition.Precedence precedence = def.getPrecedence();
    if (precedence != null && !precedence.equals(Abstract.Definition.DEFAULT_PRECEDENCE)) {
      myBuilder.append("\\infix");
      if (precedence.associativity == Abstract.Definition.Associativity.LEFT_ASSOC) myBuilder.append('l');
      if (precedence.associativity == Abstract.Definition.Associativity.RIGHT_ASSOC) myBuilder.append('r');
      myBuilder.append(' ');
      myBuilder.append(precedence.priority);
      myBuilder.append(' ');
    }

    myBuilder.append(def.getName());
    List<? extends Abstract.Argument> arguments = def.getArguments();
    if (arguments != null) {
      for (Abstract.Argument argument : arguments) {
        myBuilder.append(' ');
        argument.prettyPrint(myBuilder, myNames, Abstract.DefCallExpression.PREC);
      }
    } else {
      myBuilder.append("{!error}");
    }

    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      myBuilder.append(" : ");
      resultType.accept(new PrettyPrintVisitor(myBuilder, myNames, myIndent), Abstract.Expression.PREC);
    }

    if (arguments != null) {
      removeFromList(myNames, arguments);
    }
    return null;
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, Void ignored) {
    myBuilder.append("\\data ");
    myBuilder.append(def.getName());

    List<? extends Abstract.TypeArgument> parameters = def.getParameters();
    if (parameters != null) {
      for (Abstract.TypeArgument parameter : parameters) {
        myBuilder.append(' ');
        parameter.prettyPrint(myBuilder, myNames, Abstract.DefCallExpression.PREC);
      }
    } else {
      myBuilder.append("{!error}");
    }

    Universe universe = def.getUniverse();
    if (universe != null) {
      myBuilder.append(" : ").append(universe);
    }
    ++myIndent;

    for (Abstract.Constructor constructor : def.getConstructors()) {
      myBuilder.append('\n');
      PrettyPrintVisitor.printIndent(myBuilder, myIndent);
      myBuilder.append("| ");
      constructor.accept(this, null);
    }
    if (def.getConditions() != null) {
      myBuilder.append("\n\\with");
      myIndent++;
      for (Abstract.Condition condition : def.getConditions()) {
        myBuilder.append('\n');
        PrettyPrintVisitor.printIndent(myBuilder, myIndent);
        myBuilder.append("| ");
        prettyPrintCondition(condition, myBuilder, myNames);
      }
      --myIndent;
    }
    --myIndent;
    if (parameters != null) {
      removeFromList(myNames, parameters);
    }
    return null;
  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, Void ignored) {
    List<String> tail = new ArrayList<>();
    int origSize = myNames.size();
    List<? extends Abstract.PatternArgument> patternArgs = def.getPatterns();
    if (patternArgs == null) {
      myBuilder.append("_ ");
    } else {
      if (!myNames.isEmpty()) { //Inside data def, so remove previous
         tail.addAll(myNames.subList(myNames.size() - patternArgs.size(), myNames.size()));
         myNames.subList(myNames.size() -  patternArgs.size(), myNames.size()).clear();
         origSize = myNames.size();
      }

      myBuilder.append(def.getDataType().getName()).append(' ');
      for (Abstract.PatternArgument patternArg : patternArgs) {
        if (!patternArg.isHidden()) {
          patternArg.prettyPrint(myBuilder, myNames, Abstract.DefCallExpression.PREC);
          myBuilder.append(' ');
        }
      }
    }
    myBuilder.append("=> ");
    myBuilder.append(def.getName());
    List<? extends Abstract.TypeArgument> arguments = def.getArguments();
    if (arguments == null) {
      myBuilder.append("{!error}");
    } else {
      for (Abstract.TypeArgument argument : arguments) {
        myBuilder.append(' ');
        argument.prettyPrint(myBuilder, myNames, Abstract.DefCallExpression.PREC);
      }
      removeFromList(myNames, arguments);
    }
    trimToSize(myNames, origSize);
    myNames.addAll(tail);
    return null;
  }

  @Override
  public Void visitClass(Abstract.ClassDefinition def, Void ignored) {
    myBuilder.append("\\class ").append(def.getName()).append(" {");
    Collection<? extends Abstract.Statement> statements = def.getStatements();
    if (statements != null) {
      ++myIndent;
      StatementPrettyPrintVisitor visitor = new StatementPrettyPrintVisitor(myBuilder, myNames, myIndent);
      for (Abstract.Statement statement : statements) {
        myBuilder.append('\n');
        PrettyPrintVisitor.printIndent(myBuilder, myIndent);
        statement.accept(visitor, null);
        myBuilder.append('\n');
      }
      --myIndent;
    }
    PrettyPrintVisitor.printIndent(myBuilder, myIndent);
    myBuilder.append("}");
    return null;
  }
}
