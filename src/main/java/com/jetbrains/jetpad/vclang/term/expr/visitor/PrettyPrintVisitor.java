package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.Referable;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.statement.visitor.StatementPrettyPrintVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PrettyPrintVisitor implements AbstractExpressionVisitor<Byte, Void>, AbstractDefinitionVisitor<Void, Void> {
  private final StringBuilder myBuilder;
  private int myIndent;
  public static final int INDENT = 4;
  public static final int MAX_LEN = 120;
  public static final float SMALL_RATIO = (float) 0.1;

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
  public Void visitApp(final Abstract.AppExpression expr, Byte prec) {
    if (prec > Abstract.AppExpression.PREC) myBuilder.append('(');

    new BinOpLayout(){
      @Override
      void printLeft(PrettyPrintVisitor pp) {
        expr.getFunction().accept(pp, Abstract.AppExpression.PREC);
      }

      @Override
      void printRight(PrettyPrintVisitor pp) {
        if (expr.getArgument().isExplicit()) {
          if (expr.getArgument().isHidden()) {
            pp.myBuilder.append('_');
          } else {
            expr.getArgument().getExpression().accept(pp, (byte) (Abstract.AppExpression.PREC + 1));
          }
        } else {
          if (!expr.getArgument().isHidden()) {
            pp.myBuilder.append("{");
            expr.getArgument().getExpression().accept(pp, Abstract.Expression.PREC);
            pp.myBuilder.append('}');
          }
        }
      }

      @Override
      boolean printSpaceBefore() {
        return false;
      }

      @Override
      String getOpText() {
        return "";
      }
    }.doPrettyPrint(this);




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
      new ListLayout<Abstract.Argument>(){
        @Override
        void printListElement(PrettyPrintVisitor ppv, Abstract.Argument argument) {
          ppv.prettyPrintArgument(argument, Abstract.DefCallExpression.PREC);
        }

        @Override
        String getSeparator() {
          return " ";
        }
      }.doPrettyPrint(this, (List<Abstract.Argument>) arguments);
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
  public Void visitLam(final Abstract.LamExpression expr, Byte prec) {
    if (prec > Abstract.LamExpression.PREC) myBuilder.append("(");
    myBuilder.append("\\lam ");

    new BinOpLayout(){
      @Override
      void printLeft(PrettyPrintVisitor pp) {
        pp.prettyPrintArguments(expr.getArguments());
      }

      @Override
      void printRight(PrettyPrintVisitor pp) {
        expr.getBody().accept(pp, Abstract.LamExpression.PREC);
      }

      @Override
      String getOpText() {
        return "=>";
      }
    }.doPrettyPrint(this);

    if (prec > Abstract.LamExpression.PREC) myBuilder.append(")");
    return null;
  }

  @Override
  public Void visitPi(final Abstract.PiExpression expr, Byte prec) {
    if (prec > Abstract.PiExpression.PREC) myBuilder.append('(');

    new BinOpLayout(){
      @Override
      void printLeft(PrettyPrintVisitor pp) {
        byte domPrec = (byte) (expr.getArguments().size() > 1 ? Abstract.AppExpression.PREC + 1 : Abstract.PiExpression.PREC + 1);
        if (expr.getArguments().size() == 1 && !(expr.getArguments().get(0) instanceof Abstract.TelescopeArgument)) {
          expr.getArguments().get(0).getType().accept(pp, (byte) (Abstract.PiExpression.PREC + 1));
          pp.myBuilder.append(' ');
        } else {
          pp.myBuilder.append("\\Pi ");
          for (Abstract.Argument argument : expr.getArguments()) {
            pp.prettyPrintArgument(argument, domPrec);
            pp.myBuilder.append(' ');
          }
        }
      }

      @Override
      void printRight(PrettyPrintVisitor ppv_right) {
        expr.getCodomain().accept(ppv_right, Abstract.PiExpression.PREC);
      }

      @Override
      String getOpText() {
        return "->";
      }

      @Override
      boolean printSpaceBefore() {
        return false;
      }
    }.doPrettyPrint(this);

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

    new ListLayout<Abstract.Expression>(){
      @Override
      void printListElement(PrettyPrintVisitor ppv, Abstract.Expression o) {
        o.accept(ppv, Abstract.Expression.PREC);
      }

      @Override
      String getSeparator() {
        return ",";
      }
    }.doPrettyPrint(this, (List<Abstract.Expression>) expr.getFields());

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
  public Void visitBinOp(final Abstract.BinOpExpression expr, final Byte prec) {
    new BinOpLayout() {
      @Override
      void printLeft(PrettyPrintVisitor pp) {
        if (prec > expr.getResolvedBinOp().getPrecedence().priority) pp.myBuilder.append('(');
        expr.getLeft().accept(pp, (byte) (expr.getResolvedBinOp().getPrecedence().priority + (expr.getResolvedBinOp().getPrecedence().associativity == Abstract.Binding.Associativity.LEFT_ASSOC ? 0 : 1)));
      }

      @Override
      void printRight(PrettyPrintVisitor pp) {
        expr.getRight().accept(pp, (byte) (expr.getResolvedBinOp().getPrecedence().priority + (expr.getResolvedBinOp().getPrecedence().associativity == Abstract.Binding.Associativity.RIGHT_ASSOC ? 0 : 1)));
        if (prec > expr.getResolvedBinOp().getPrecedence().priority) pp.myBuilder.append(')');
      }

      @Override
      String getOpText() {
        return new Name(expr.getResolvedBinOp().getName()).getInfixName();
      }

      @Override
      boolean increaseIndent(List<String> right_strings) {
        Abstract.Expression r = expr.getRight();
        if (r instanceof Abstract.BinOpExpression) {
          Referable referable = ((Abstract.BinOpExpression) r).getResolvedBinOp();
          if (referable!=null) {
            if (prec <= referable.getPrecedence().priority) return false; // no bracket drawn
          }
        }
        return super.increaseIndent(right_strings);
      }
    }.doPrettyPrint(this);

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

  public void prettyPrintClause(final Abstract.Clause clause) {
    if (clause == null) return;

    printIndent();
    myBuilder.append("| ");

    if (clause.getArrow() != null && clause.getExpression() != null) {
      new BinOpLayout(){
        @Override
        void printLeft(PrettyPrintVisitor pp) {
          for (int i = 0; i < clause.getPatterns().size(); i++) {
            pp.prettyPrintPattern(clause.getPatterns().get(i));
            if (i != clause.getPatterns().size() - 1) {
              pp.myBuilder.append(", ");
            }
          }
        }

        @Override
        void printRight(PrettyPrintVisitor pp) {
          clause.getExpression().accept(pp, Abstract.Expression.PREC);
        }

        @Override
        String getOpText() {
          return prettyArrow(clause.getArrow());
        }
      }.doPrettyPrint(this);
    } else {
      for (int i = 0; i < clause.getPatterns().size(); i++) {
        prettyPrintPattern(clause.getPatterns().get(i));
        if (i != clause.getPatterns().size() - 1) {
          myBuilder.append(", ");
        }
      }
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

    List<? extends Abstract.Clause> clauses = expr.getClauses();
    if (!clauses.isEmpty()) {
      myBuilder.append('\n');
      myIndent += INDENT;
      for (Abstract.Clause clause : clauses) {
        prettyPrintClause(clause);
      }

      printIndent();
    }

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

  public void prettyPrintLetClause(final Abstract.LetClause letClause) {
    myBuilder.append("| ").append(letClause.getName());
    for (Abstract.Argument arg : letClause.getArguments()) {
      myBuilder.append(" ");
      prettyPrintArgument(arg, Abstract.LetExpression.PREC);
    }

    if (letClause.getResultType()!=null) {
      new BinOpLayout() {
        @Override
        void printLeft(PrettyPrintVisitor pp) {
          myBuilder.append(" : ");
          letClause.getResultType().accept(pp, Abstract.Expression.PREC);
        }

        @Override
        void printRight(PrettyPrintVisitor pp) {
          letClause.getTerm().accept(pp, Abstract.LetExpression.PREC);
        }

        @Override
        String getOpText() {
          return prettyArrow(letClause.getArrow());
        }
      }.doPrettyPrint(this);
    } else {
      myBuilder.append(prettyArrow(letClause.getArrow()));
      letClause.getTerm().accept(this, Abstract.LetExpression.PREC);
    }
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
      myBuilder.append("\\infix");
      if (precedence.associativity == Abstract.Binding.Associativity.LEFT_ASSOC) myBuilder.append('l');
      if (precedence.associativity == Abstract.Binding.Associativity.RIGHT_ASSOC) myBuilder.append('r');
      myBuilder.append(' ');
      myBuilder.append(precedence.priority);
      myBuilder.append(' ');
    }

    myBuilder.append(new Name(def.getName()).getPrefixName());
    return null;
  }

  @Override
  public Void visitFunction(final Abstract.FunctionDefinition def, Void ignored) {
    myBuilder.append("\\function\n");
    printIndent();
    prettyPrintBinding(def);
    myBuilder.append(" ");


    final BinOpLayout l = new BinOpLayout(){
      @Override
      void printLeft(PrettyPrintVisitor pp) {
        pp.prettyPrintArguments(def.getArguments());
      }

      @Override
      void printRight(PrettyPrintVisitor pp) {
        def.getResultType().accept(pp, Abstract.Expression.PREC);
      }

      @Override
      boolean printSpaceBefore() {
        return def.getArguments().size() > 0;
      }

      @Override
      String getOpText() {
        return ":";
      }
    };

    final BinOpLayout r = new BinOpLayout(){
      @Override
      String getOpText() {
        return def.getArrow() == Abstract.Definition.Arrow.RIGHT ? "=>" : "<=";
      }

      @Override
      void printRight(PrettyPrintVisitor pp) {
        if (def.getTerm() != null) {
          def.getTerm().accept(pp, Abstract.Expression.PREC);
        } else {
          pp.myBuilder.append("{!error}");
        }
      }

      @Override
      void printLeft(PrettyPrintVisitor pp) {
        if (def.getResultType() != null) {
          l.doPrettyPrint(pp);
        } else {
          l.printLeft(pp);
        }
      }

      @Override
      boolean printSpaceBefore() {
        return def.getArguments().size() > 0 || def.getResultType() != null;
      }

      @Override
      boolean increaseIndent(List<String> rhs_strings) {
        return !(rhs_strings.size() > 0 && (spacesCount(rhs_strings.get(0)) > 0 || rhs_strings.get(0).isEmpty()));
      }

      @Override
      boolean doHyphenation(int leftLen, int rightLen) {
        if (def.getResultType() != null) return true;
        return super.doHyphenation(leftLen, rightLen);
      }
    };

    r.doPrettyPrint(this);

    Collection<? extends Abstract.Statement> statements = def.getStatements();
    if (!statements.isEmpty()) {
      myBuilder.append("\n");
      printIndent();
      myBuilder.append("\\where ");
      myIndent += INDENT;
      boolean isFirst = true;
      for (Abstract.Statement statement : statements) {
        if (!isFirst)
          printIndent();
        statement.accept(new StatementPrettyPrintVisitor(myBuilder, myIndent, null), null);
        myBuilder.append("\n");
        isFirst = false;
      }
      myIndent -= INDENT;
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
    myBuilder.append("\\data ");
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
    myIndent += INDENT;

    for (Abstract.Constructor constructor : def.getConstructors()) {
      myBuilder.append('\n');
      printIndent();
      myBuilder.append("| ");
      constructor.accept(this, null);
    }
    if (def.getConditions() != null) {
      myBuilder.append("\n\\with");
      for (Abstract.Condition condition : def.getConditions()) {
        myBuilder.append('\n');
        printIndent();
        myBuilder.append("| ");
        prettyPrintCondition(condition);
      }
    }
    myIndent -= INDENT;
    return null;
  }

  public void prettyPrintPatternArg(Abstract.PatternArgument patternArg) {
    Abstract.Pattern pat = patternArg.getPattern();
    boolean isName = pat instanceof Abstract.NamePattern;
    myBuilder.append(patternArg.isExplicit() ? (isName ? "" : "(") : "{");
    prettyPrintPattern(pat);
    myBuilder.append(patternArg.isExplicit() ? (isName ? "": ")") : "}");
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
      StatementPrettyPrintVisitor visitor = new StatementPrettyPrintVisitor(myBuilder, myIndent, null);
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

  public static abstract class ListLayout<T>{
    abstract void printListElement(PrettyPrintVisitor ppv, T t);

    abstract String getSeparator();

    public void doPrettyPrint(PrettyPrintVisitor ppv_default, List<T> l){
      int rem = -1;
      int indent = 0;
      for (T t : l) {
        StringBuilder sb = new StringBuilder();
        PrettyPrintVisitor ppv = new PrettyPrintVisitor(sb, 0);
        printListElement(ppv, t);

        String[] strs = sb.toString().split("[\\r\\n]+");
        int sz = strs.length;

        if (rem != -1) {
          String separator = getSeparator();
          ppv_default.myBuilder.append(separator.trim());
          if (rem + strs[0].length() + separator.length() > MAX_LEN) {
            if (indent == 0) ppv_default.myIndent += INDENT;
            indent = INDENT;
            ppv_default.myBuilder.append('\n');
            rem = 0;
          } else {
            ppv_default.myBuilder.append(' ');
            rem++;
          }
        }

        for (int i = 0; i < sz; i++) {
          String s = strs[i];
          if (rem == 0) ppv_default.printIndent();
          ppv_default.myBuilder.append(s);
          rem += s.trim().length();
          if (i < sz - 1) {
            ppv_default.myBuilder.append('\n');
            rem = 0;
          }
        }
      }
      ppv_default.myIndent -= indent;
    }
  }

  public static abstract class BinOpLayout {

    abstract void printLeft(PrettyPrintVisitor pp);
    abstract void printRight(PrettyPrintVisitor pp);
    abstract String getOpText();
    boolean printSpaceBefore() {return true;}
    boolean printSpaceAfter() {return true;}

    boolean doHyphenation(int leftLen, int rightLen) {
      if (leftLen == 0) leftLen = 1; if (leftLen > MAX_LEN) leftLen = MAX_LEN;
      if (rightLen == 0) rightLen = 1; if (rightLen > MAX_LEN) rightLen = MAX_LEN;
      double ratio = rightLen / leftLen;
      if (ratio > 1.0) ratio = 1/ratio;

      int myMaxLen = (ratio > SMALL_RATIO) ? MAX_LEN : Math.round(MAX_LEN * (1 + SMALL_RATIO));

      return (leftLen + rightLen + getOpText().trim().length() + 1 > myMaxLen);
    }

    boolean increaseIndent(List<String> rhs_strings) {
      return !(rhs_strings.size() > 0 && spacesCount(rhs_strings.get(0)) > 0 || rhs_strings.size() > 1 && spacesCount(rhs_strings.get(1)) > 0);
    }

    public static int spacesCount(String s) {
      int i = 0;
      for (; i<s.length(); i++) if (s.charAt(i) != ' ') break;
      return i;
    }

    public void doPrettyPrint(PrettyPrintVisitor ppv_default) {
      StringBuilder lhs = new StringBuilder();
      StringBuilder rhs = new StringBuilder();
      PrettyPrintVisitor ppv_left = new PrettyPrintVisitor(lhs, 0);
      PrettyPrintVisitor ppv_right = new PrettyPrintVisitor(rhs, 0);

      //TODO: I don't like this implementation for it works quadratically wrt to the total number of binary operations
      printLeft(ppv_left);
      printRight(ppv_right);


      List<String> lhs_strings = new ArrayList<>(); Collections.addAll(lhs_strings, lhs.toString().split("[\\r\\n]+"));
      List<String> rhs_strings = new ArrayList<>(); Collections.addAll(rhs_strings, rhs.toString().split("[\\r\\n]+"));

      int lhs_sz = lhs_strings.size();
      int rhs_sz = rhs_strings.size();

      int leftLen = lhs_sz == 0 ? 0 : lhs_strings.get(lhs_sz-1).trim().length();
      int rightLen = rhs_sz == 0 ? 0 : rhs_strings.get(0).trim().length();

      boolean hyph = doHyphenation(leftLen, rightLen) && !(rhs_sz > 0 && rhs_strings.get(0).isEmpty());

      for (int i=0; i<lhs_sz; i++) {
        String s = lhs_strings.get(i);
        if (i>0) ppv_default.printIndent(); ppv_default.myBuilder.append(s);
        if (i<lhs_sz-1) ppv_default.myBuilder.append('\n');
      }

      if (printSpaceBefore()) ppv_default.myBuilder.append(' ');
      ppv_default.myBuilder.append(getOpText().trim());

      if (hyph) {
        ppv_default.myBuilder.append('\n');
      } else {
        if (printSpaceAfter()) ppv_default.myBuilder.append(' ');
      }

      boolean ii = increaseIndent(rhs_strings);

      if (ii) ppv_default.myIndent+=INDENT;

      for (int i=0; i<rhs_sz; i++) {
        String s = rhs_strings.get(i);

        if (i>0 || hyph) {
          ppv_default.printIndent();
        }

        ppv_default.myBuilder.append(s);

        if (i<rhs_strings.size()-1) ppv_default.myBuilder.append('\n');
      }
      if (ii) ppv_default.myIndent-=INDENT;
    }
  }
}
