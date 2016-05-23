package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.statement.visitor.StatementPrettyPrintVisitor;

import java.util.Collection;
import java.util.List;

public class PrettyPrintVisitor implements AbstractExpressionVisitor<Byte, Void>, AbstractDefinitionVisitor<Void, Void> {
  private final StringBuilder myBuilder;
  private int myIndent;
  public static final int INDENT = 4;

  public PrettyPrintVisitor(StringBuilder builder, int indent) {
    myBuilder = builder;
    myIndent = indent;
  }

  public boolean prettyPrint(Abstract.SourceNode node, byte prec) {
    if (node instanceof Abstract.Expression) {
      ((Abstract.Expression) node).accept(this, prec);
      return true;
    }
    if (node instanceof Abstract.Argument) {
      prettyPrintArgument((Abstract.Argument) node, prec);
      return true;
    }
    if (node instanceof Abstract.Definition) {
      ((Abstract.Definition) node).accept(this, null);
      return true;
    }
    if (node instanceof Abstract.Clause) {
      prettyPrintClause((Abstract.Clause) node);
      return true;
    }
    if (node instanceof Abstract.LetClause) {
      prettyPrintLetClause((Abstract.LetClause) node);
      return true;
    }
    if (node instanceof Abstract.Condition) {
      prettyPrintCondition((Abstract.Condition) node);
      return true;
    }
    if (node instanceof Abstract.Pattern) {
      prettyPrintPattern((Abstract.Pattern) node);
      return true;
    }
    if (node instanceof Abstract.PatternArgument) {
      prettyPrintPatternArg((Abstract.PatternArgument) node);
      return true;
    }
    return false;
  }

  @Override
  public Void visitApp(Abstract.AppExpression expr, Byte prec) {
    if (prec > Abstract.AppExpression.PREC) myBuilder.append('(');

    expr.getFunction().accept(this, Abstract.AppExpression.PREC);
    if (expr.getArgument().isExplicit()) {
      myBuilder.append(' ');
      if (expr.getArgument().isHidden()) {
        myBuilder.append('_');
      } else {
        expr.getArgument().getExpression().accept(this, (byte) (Abstract.AppExpression.PREC + 1));
      }
    } else {
      if (!expr.getArgument().isHidden()) {
        myBuilder.append(" {");
        expr.getArgument().getExpression().accept(this, Abstract.Expression.PREC);
        myBuilder.append('}');
      }
    }

    if (prec > Abstract.AppExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitDefCall(Abstract.DefCallExpression expr, Byte prec) {
    if (expr.getExpression() != null) {
      expr.getExpression().accept(this, Abstract.DefCallExpression.PREC);
      myBuilder.append(".");
    }
    myBuilder.append(new Name(expr.getName()));
    return null;
  }

  @Override
  public Void visitModuleCall(Abstract.ModuleCallExpression expr, Byte prec) {
    myBuilder.append(new ModulePath(expr.getPath()));
    return null;
  }

  public void prettyPrintArguments(List<? extends Abstract.Argument> arguments) {
    if (arguments != null) {
      for (Abstract.Argument argument : arguments) {
        myBuilder.append(' ');
        prettyPrintArgument(argument, Abstract.DefCallExpression.PREC);
      }
    } else {
      myBuilder.append("{!error}");
    }
  }

  public void prettyPrintArgument(Abstract.Argument argument, byte prec) {
    if (argument instanceof Abstract.NameArgument) {
      String name = ((Abstract.NameArgument) argument).getName();
      if (name == null) {
        name = "_";
      }
      myBuilder.append(argument.getExplicit() ? name : '{' + name + '}');
    } else
    if (argument instanceof Abstract.TelescopeArgument) {
      myBuilder.append(argument.getExplicit() ? '(' : '{');
      for (String name : ((Abstract.TelescopeArgument) argument).getNames()) {
        myBuilder.append(name == null ? "_" : name).append(' ');
      }

      myBuilder.append(": ");
      ((Abstract.TypeArgument) argument).getType().accept(this, Abstract.Expression.PREC);
      myBuilder.append(argument.getExplicit() ? ')' : '}');
    } else
    if (argument instanceof Abstract.TypeArgument) {
      Abstract.Expression type = ((Abstract.TypeArgument) argument).getType();
      if (argument.getExplicit()) {
        type.accept(this, prec);
      } else {
        myBuilder.append('{');
        type.accept(this, Abstract.Expression.PREC);
        myBuilder.append('}');
      }
    }
  }

  @Override
  public Void visitLam(Abstract.LamExpression expr, Byte prec) {
    if (prec > Abstract.LamExpression.PREC) myBuilder.append("(");
    myBuilder.append("\\lam ");
    for (Abstract.Argument arg : expr.getArguments()) {
      prettyPrintArgument(arg, Abstract.Expression.PREC);
      myBuilder.append(" ");
    }
    myBuilder.append("=> ");
    expr.getBody().accept(this, Abstract.LamExpression.PREC);
    if (prec > Abstract.LamExpression.PREC) myBuilder.append(")");
    return null;
  }

  @Override
  public Void visitPi(Abstract.PiExpression expr, Byte prec) {
    if (prec > Abstract.PiExpression.PREC) myBuilder.append('(');
    byte domPrec = (byte) (expr.getArguments().size() > 1 ? Abstract.AppExpression.PREC + 1 : Abstract.PiExpression.PREC + 1);
    if (expr.getArguments().size() == 1 && !(expr.getArguments().get(0) instanceof Abstract.TelescopeArgument)) {
      expr.getArguments().get(0).getType().accept(this, (byte) (Abstract.PiExpression.PREC + 1));
      myBuilder.append(' ');
    } else {
      myBuilder.append("\\Pi ");
      for (Abstract.Argument argument : expr.getArguments()) {
        prettyPrintArgument(argument, domPrec);
        myBuilder.append(' ');
      }
    }
    myBuilder.append("-> ");
    expr.getCodomain().accept(this, Abstract.PiExpression.PREC);
    if (prec > Abstract.PiExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitUniverse(Abstract.UniverseExpression expr, Byte prec) {
    myBuilder.append(expr.getUniverse());
    return null;
  }

  @Override
  public Void visitPolyUniverse(Abstract.PolyUniverseExpression expr, Byte prec) {
    myBuilder.append("\\Type (");
    if (expr.getPLevel() == null) {
      myBuilder.append("_");
    } else {
      expr.getPLevel().accept(this, (byte) (Abstract.AppExpression.PREC + 1));
    }
    myBuilder.append(",");
    expr.getHLevel().accept(this, (byte) (Abstract.AppExpression.PREC + 1));
    myBuilder.append(")");
    return null;
  }

  @Override
  public Void visitInferHole(Abstract.InferHoleExpression expr, Byte prec) {
    myBuilder.append('_');
    return null;
  }

  @Override
  public Void visitError(Abstract.ErrorExpression expr, Byte prec) {
    myBuilder.append("{?");
    if (expr.getExpr() != null) {
      expr.getExpr().accept(this, Abstract.Expression.PREC);
    }
    myBuilder.append('}');
    return null;
  }

  @Override
  public Void visitTuple(Abstract.TupleExpression expr, Byte prec) {
    myBuilder.append('(');
    for (int i = 0; i < expr.getFields().size(); ++i) {
      expr.getFields().get(i).accept(this, Abstract.Expression.PREC);
      if (i < expr.getFields().size() - 1) {
        myBuilder.append(", ");
      }
    }
    myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitSigma(Abstract.SigmaExpression expr, Byte prec) {
    if (prec > Abstract.SigmaExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\Sigma");
    for (Abstract.Argument argument : expr.getArguments()) {
      myBuilder.append(' ');
      prettyPrintArgument(argument, (byte) (Abstract.AppExpression.PREC + 1));
    }
    if (prec > Abstract.SigmaExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitBinOp(Abstract.BinOpExpression expr, Byte prec) {
    if (prec > expr.getResolvedBinOp().getPrecedence().priority) myBuilder.append('(');
    expr.getLeft().accept(this, (byte) (expr.getResolvedBinOp().getPrecedence().priority + (expr.getResolvedBinOp().getPrecedence().associativity == Abstract.Binding.Associativity.LEFT_ASSOC ? 0 : 1)));
    myBuilder.append(' ').append(new Name(expr.getResolvedBinOp().getName()).getInfixName()).append(' ');
    expr.getRight().accept(this, (byte) (expr.getResolvedBinOp().getPrecedence().priority + (expr.getResolvedBinOp().getPrecedence().associativity == Abstract.Binding.Associativity.RIGHT_ASSOC ? 0 : 1)));
    if (prec > expr.getResolvedBinOp().getPrecedence().priority) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Byte prec) {
    if (prec > Abstract.BinOpSequenceExpression.PREC) myBuilder.append('(');
    expr.getLeft().accept(this, (byte) 10);
    for (Abstract.BinOpSequenceElem elem : expr.getSequence()) {
      myBuilder.append(' ').append(new Name(elem.binOp.getName()).getInfixName()).append(' ');
      elem.argument.accept(this, (byte) 10);
    }
    if (prec > Abstract.BinOpSequenceExpression.PREC) myBuilder.append(')');
    return null;
  }

  public void prettyPrintClause(Abstract.Clause clause) {
    if (clause == null) return;

    printIndent();
    myBuilder.append("| ");
    for (int i = 0; i < clause.getPatterns().size(); i++) {
      prettyPrintPattern(clause.getPatterns().get(i));
      if (i != clause.getPatterns().size() - 1) {
        myBuilder.append(", ");
      }
    }

    if (clause.getArrow() != null && clause.getExpression() != null) {
      myBuilder.append(prettyArrow(clause.getArrow()));
      clause.getExpression().accept(this, Abstract.Expression.PREC);
    }
    myBuilder.append('\n');
  }

  private void visitElimCaseExpression(Abstract.ElimCaseExpression expr, Byte prec) {
    if (prec > Abstract.ElimExpression.PREC) myBuilder.append('(');
    myBuilder.append(expr instanceof Abstract.ElimExpression ? "\\elim" : "\\case");
    for (int i = 0; i < expr.getExpressions().size(); i++) {
      myBuilder.append(" ");
      expr.getExpressions().get(i).accept(this, Abstract.Expression.PREC);
      if (i != expr.getExpressions().size() - 1)
        myBuilder.append(",");
    }
    myBuilder.append('\n');
    myIndent += INDENT;
    for (Abstract.Clause clause : expr.getClauses()) {
      prettyPrintClause(clause);
    }

    printIndent();
    myBuilder.append(';');
    myIndent -= INDENT;
    if (prec > Abstract.ElimExpression.PREC) myBuilder.append(')');
  }

  @Override
  public Void visitElim(Abstract.ElimExpression expr, Byte prec) {
    visitElimCaseExpression(expr, prec);
    return null;
  }

  @Override
  public Void visitCase(Abstract.CaseExpression expr, Byte params) {
    visitElimCaseExpression(expr, params);
    return null;
  }

  @Override
  public Void visitProj(Abstract.ProjExpression expr, Byte prec) {
    if (prec > Abstract.ProjExpression.PREC) myBuilder.append('(');
    expr.getExpression().accept(this, Abstract.ProjExpression.PREC);
    myBuilder.append('.').append(expr.getField() + 1);
    if (prec > Abstract.ProjExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitClassExt(Abstract.ClassExtExpression expr, Byte prec) {
    if (prec > Abstract.ClassExtExpression.PREC) myBuilder.append('(');
    expr.getBaseClassExpression().accept(this, (byte) -Abstract.ClassExtExpression.PREC);
    myBuilder.append(" {\n");
    myIndent += INDENT;
    for (Abstract.ImplementStatement statement : expr.getStatements()) {
      printIndent();
      myBuilder.append("| ").append(new Name(statement.getName()).getPrefixName()).append(" => ");
      statement.getExpression().accept(this, Abstract.Expression.PREC);
      myBuilder.append("\n");
    }
    myIndent -= INDENT;
    printIndent();
    myBuilder.append("}");
    if (prec > Abstract.ClassExtExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitNew(Abstract.NewExpression expr, Byte prec) {
    if (prec > Abstract.NewExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\new ");
    expr.getExpression().accept(this, Abstract.NewExpression.PREC);
    if (prec > Abstract.NewExpression.PREC) myBuilder.append(')');
    return null;
  }

  private static String prettyArrow(Abstract.Definition.Arrow arrow) {
    switch (arrow) {
      case LEFT: return " <= ";
      case RIGHT: return " => ";
      default: return null;
    }
  }

  public void prettyPrintLetClause(Abstract.LetClause letClause) {
    myBuilder.append("| ").append(letClause.getName());
    for (Abstract.Argument arg : letClause.getArguments()) {
      myBuilder.append(" ");
      prettyPrintArgument(arg, Abstract.LetExpression.PREC);
    }

    if (letClause.getResultType()!=null) {
      myBuilder.append(" : ");
      letClause.getResultType().accept(this, Abstract.Expression.PREC);
    }

    myBuilder.append(prettyArrow(letClause.getArrow()));
    letClause.getTerm().accept(this, Abstract.LetExpression.PREC);
  }

  @Override
  public Void visitLet(Abstract.LetExpression expr, Byte prec) {
    if (prec > Abstract.LetExpression.PREC) myBuilder.append('(');
    myBuilder.append("\n");
    myIndent += INDENT;
    printIndent();
    String let = "\\let ";
    myBuilder.append(let);

    final int INDENT0 = let.length();
    myIndent += INDENT0;
    for (int i = 0; i < expr.getClauses().size(); ++i) {
      prettyPrintLetClause(expr.getClauses().get(i));
      myBuilder.append("\n");
      if (i == expr.getClauses().size() - 1) {
        myIndent -= INDENT0;
      }
      printIndent();
    }

    String in = "\\in ";
    myBuilder.append(in);
    final int INDENT1 = in.length();
    myIndent += INDENT1;
    expr.getExpression().accept(this, Abstract.LetExpression.PREC);
    myIndent -= INDENT1;
    myIndent -= INDENT;

    if (prec > Abstract.LetExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitNumericLiteral(Abstract.NumericLiteral expr, Byte params) {
    myBuilder.append(expr.getNumber());
    return null;
  }

  public void printIndent() {
    for (int i = 0; i < myIndent; ++i) {
      myBuilder.append(' ');
    }
  }

  public static void printIndent(StringBuilder builder, int indent) {
    for (int i = 0; i < indent; i++) {
      builder.append(' ');
    }
  }

  private Void prettyPrintBinding (Abstract.Binding def) {
    Abstract.Definition.Precedence precedence = def.getPrecedence();
    if (precedence != null && !precedence.equals(Abstract.Binding.DEFAULT_PRECEDENCE)) {
      myBuilder.append(" \\infix");
      if (precedence.associativity == Abstract.Binding.Associativity.LEFT_ASSOC) myBuilder.append('l');
      if (precedence.associativity == Abstract.Binding.Associativity.RIGHT_ASSOC) myBuilder.append('r');
      myBuilder.append(' ');
      myBuilder.append(precedence.priority);
    }

    myBuilder.append(' ');
    myBuilder.append(new Name(def.getName()).getPrefixName());
    return null;
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, Void ignored) {
    myBuilder.append("\\function");
    prettyPrintBinding(def);
    prettyPrintArguments(def.getArguments());

    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      myBuilder.append(" : ");
      resultType.accept(new PrettyPrintVisitor(myBuilder, myIndent), Abstract.Expression.PREC);
    }
    if (!def.isAbstract()) {
      myBuilder.append(def.getArrow() == Abstract.Definition.Arrow.RIGHT ? " => " : " <= ");
      Abstract.Expression term = def.getTerm();
      if (term != null) {
        term.accept(new PrettyPrintVisitor(myBuilder, myIndent), Abstract.Expression.PREC);
      } else {
        myBuilder.append("{!error}");
      }
    }

    Collection<? extends Abstract.Statement> statements = def.getStatements();
    if (!statements.isEmpty()) {
      myBuilder.append("\n");
      printIndent();
      myBuilder.append("\\where ");
      myIndent += "\\where ".length();
      boolean isFirst = true;
      for (Abstract.Statement statement : statements) {
        if (!isFirst)
          printIndent();
        statement.accept(new StatementPrettyPrintVisitor(myBuilder, myIndent), null);
        myBuilder.append("\n");
        isFirst = false;
      }
      myIndent -= "\\where ".length();
    }
    return null;
  }

  @Override
  public Void visitAbstract(Abstract.AbstractDefinition def, Void params) {
    myBuilder.append("\\abstract");
    prettyPrintBinding(def);
    prettyPrintArguments(def.getArguments());

    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      myBuilder.append(" : ");
      resultType.accept(new PrettyPrintVisitor(myBuilder, myIndent), Abstract.Expression.PREC);
    }

    return null;
  }

  public void prettyPrintCondition(Abstract.Condition condition) {
    myBuilder.append(condition.getConstructorName());
    for (Abstract.PatternArgument patternArg : condition.getPatterns()) {
      if (!patternArg.isHidden()) {
        myBuilder.append(" ");
        prettyPrintPatternArg(patternArg);
      }
    }
    myBuilder.append(" => ");
    condition.getTerm().accept(this, Abstract.Expression.PREC);
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, Void ignored) {
    myBuilder.append("\\data");
    prettyPrintBinding(def);

    List<? extends Abstract.TypeArgument> parameters = def.getParameters();
    if (parameters != null) {
      for (Abstract.TypeArgument parameter : parameters) {
        myBuilder.append(' ');
        prettyPrintArgument(parameter, Abstract.DefCallExpression.PREC);
      }
    } else {
      myBuilder.append("{!error}");
    }

    Abstract.Expression universe = def.getUniverse();
    if (universe != null) {
      myBuilder.append(" : ");
      universe.accept(this, null);
    }
    ++myIndent;

    for (Abstract.Constructor constructor : def.getConstructors()) {
      myBuilder.append('\n');
      printIndent();
      myBuilder.append("| ");
      constructor.accept(this, null);
    }
    if (def.getConditions() != null) {
      myBuilder.append("\n\\with");
      myIndent++;
      for (Abstract.Condition condition : def.getConditions()) {
        myBuilder.append('\n');
        printIndent();
        printIndent();
        myBuilder.append("| ");
        prettyPrintCondition(condition);
      }
      --myIndent;
    }
    --myIndent;
    return null;
  }

  public void prettyPrintPatternArg(Abstract.PatternArgument patternArg) {
    myBuilder.append(patternArg.isExplicit() ? "(" : "{");
    prettyPrintPattern(patternArg.getPattern());
    myBuilder.append(patternArg.isExplicit() ? ")" : "}");
  }

  public void prettyPrintPattern(Abstract.Pattern pattern) {
    if (pattern instanceof Abstract.NamePattern) {
      if (((Abstract.NamePattern) pattern).getName() == null) {
        myBuilder.append('_');
      } else {
        myBuilder.append(((Abstract.NamePattern) pattern).getName());
      }
    } else if (pattern instanceof Abstract.AnyConstructorPattern) {
      myBuilder.append("_!");
    } else if (pattern instanceof Abstract.ConstructorPattern) {
      myBuilder.append(new Name(((Abstract.ConstructorPattern) pattern).getConstructorName()).getPrefixName());
      for (Abstract.PatternArgument patternArg : ((Abstract.ConstructorPattern) pattern).getArguments()) {
        if (!patternArg.isHidden()) {
          myBuilder.append(' ');
          prettyPrintPatternArg(patternArg);
        }
      }
    }
  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, Void ignored) {
    List<? extends Abstract.PatternArgument> patternArgs = def.getPatterns();
    if (patternArgs != null) {
      myBuilder.append(def.getDataType().getName()).append(' ');
      for (Abstract.PatternArgument patternArg : patternArgs) {
        if (!patternArg.isHidden()) {
          prettyPrintPatternArg(patternArg);
          myBuilder.append(' ');
        }
      }
      myBuilder.append("=> ");
    }

    prettyPrintBinding(def);
    List<? extends Abstract.TypeArgument> arguments = def.getArguments();
    if (arguments == null) {
      myBuilder.append("{!error}");
    } else {
      for (Abstract.TypeArgument argument : arguments) {
        myBuilder.append(' ');
        prettyPrintArgument(argument, Abstract.DefCallExpression.PREC);
      }
    }
    return null;
  }

  @Override
  public Void visitClass(Abstract.ClassDefinition def, Void ignored) {
    myBuilder.append("\\class ").append(def.getName()).append(" {");
    Collection<? extends Abstract.Statement> statements = def.getStatements();
    if (statements != null) {
      ++myIndent;
      StatementPrettyPrintVisitor visitor = new StatementPrettyPrintVisitor(myBuilder, myIndent);
      for (Abstract.Statement statement : statements) {
        myBuilder.append('\n');
        printIndent();
        statement.accept(visitor, null);
        myBuilder.append('\n');
      }
      --myIndent;
    }
    printIndent();
    myBuilder.append("}");
    return null;
  }
}
