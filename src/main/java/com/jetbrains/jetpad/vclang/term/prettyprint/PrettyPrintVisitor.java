package com.jetbrains.jetpad.vclang.term.prettyprint;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.*;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import java.util.*;

public class PrettyPrintVisitor<T> implements ConcreteExpressionVisitor<T, Byte, Void>, ConcreteLevelExpressionVisitor<T, Byte, Void>, ConcreteDefinitionVisitor<T, Void, Void> {
  public static final int INDENT = 4;
  public static final int MAX_LEN = 120;
  public static final float SMALL_RATIO = (float) 0.1;

  protected final StringBuilder myBuilder;
  private final PrettyPrinterInfoProvider myInfoProvider;
  private Map<InferenceLevelVariable, Integer> myPVariables = Collections.emptyMap();
  private Map<InferenceLevelVariable, Integer> myHVariables = Collections.emptyMap();
  protected int myIndent;
  private boolean noIndent;

  public PrettyPrintVisitor(StringBuilder builder, PrettyPrinterInfoProvider infoProvider, int indent, boolean doIndent) {
    myBuilder = builder;
    myInfoProvider = infoProvider;
    myIndent = indent;
    noIndent = !doIndent;
  }

  public PrettyPrintVisitor(StringBuilder builder, PrettyPrinterInfoProvider infoProvider, int indent) {
    this(builder, infoProvider, indent, true);
  }

  public static <T> String prettyPrint(Concrete.SourceNode<T> node, PrettyPrinterInfoProvider infoProvider) {
    StringBuilder builder = new StringBuilder();
    return new PrettyPrintVisitor<T>(builder, infoProvider, 0).prettyPrint(node, Concrete.Expression.PREC) ? builder.toString() : null;
  }

  public boolean prettyPrint(Concrete.SourceNode<T> node, byte prec) {
    if (node instanceof Concrete.Expression) {
      ((Concrete.Expression<T>) node).accept(this, prec);
      return true;
    }
    if (node instanceof Concrete.Parameter) {
      prettyPrintParameter((Concrete.Parameter<T>) node, prec);
      return true;
    }
    if (node instanceof Concrete.Definition) {
      ((Concrete.Definition<T>) node).accept(this, null);
      return true;
    }
    if (node instanceof Concrete.ClassFieldImpl) {
      visitClassFieldImpl((Concrete.ClassFieldImpl<T>) node);
      return true;
    }
    if (node instanceof Concrete.FunctionClause) {
      prettyPrintFunctionClause((Concrete.FunctionClause<T>) node);
      return true;
    }
    if (node instanceof Concrete.ConstructorClause) {
      prettyPrintConstructorClause((Concrete.ConstructorClause<T>) node);
      return true;
    }
    if (node instanceof Concrete.Clause) {
      prettyPrintClause((Concrete.Clause<T>) node);
      return true;
    }
    if (node instanceof Concrete.LetClause) {
      prettyPrintLetClause((Concrete.LetClause<T>) node, false);
      return true;
    }
    if (node instanceof Concrete.Pattern) {
      prettyPrintPattern((Concrete.Pattern<T>) node, prec);
      return true;
    }
    if (node instanceof Concrete.LevelExpression) {
      ((Concrete.LevelExpression<T>) node).accept(this, Concrete.Expression.PREC);
      return true;
    }
    return false;
  }

  @Override
  public Void visitApp(final Concrete.AppExpression<T> expr, Byte prec) {
    if (prec > Concrete.AppExpression.PREC) myBuilder.append('(');

    new BinOpLayout<T>(){
      @Override
      void printLeft(PrettyPrintVisitor<T> pp) {
        expr.getFunction().accept(pp, Concrete.AppExpression.PREC);
      }

      @Override
      void printRight(PrettyPrintVisitor<T> pp) {
        if (expr.getArgument().isExplicit()) {
          expr.getArgument().getExpression().accept(pp, (byte) (Concrete.AppExpression.PREC + 1));
        } else {
          pp.myBuilder.append("{");
          expr.getArgument().getExpression().accept(pp, Concrete.Expression.PREC);
          pp.myBuilder.append('}');
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
    }.doPrettyPrint(this, noIndent);

    if (prec > Concrete.AppExpression.PREC) myBuilder.append(')');
    return null;
  }

  public static boolean isPrefix(String name) {
    if (name == null) {
      return true;
    }
    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);
      if (ch == '_' || Character.isLetter(ch) || ch == '\'') {
        return true;
      }
    }
    return false;
  }

  @Override
  public Void visitReference(Concrete.ReferenceExpression<T> expr, Byte prec) {
    if (expr.getExpression() != null) {
      expr.getExpression().accept(this, Concrete.ReferenceExpression.PREC);
      myBuilder.append('.').append(expr.getReferent().getName());
    } else {
      if (!isPrefix(expr.getReferent().getName())) {
        myBuilder.append('`');
      }
      myBuilder.append(expr.getReferent().getName());
    }
    return null;
  }

  @Override
  public Void visitInferenceReference(Concrete.InferenceReferenceExpression expr, Byte params) {
    myBuilder.append("?").append(expr.getVariable().getName());
    return null;
  }

  @Override
  public Void visitModuleCall(Concrete.ModuleCallExpression expr, Byte prec) {
    myBuilder.append(expr.getPath());
    return null;
  }

  public void prettyPrintParameters(List<? extends Concrete.Parameter<T>> parameters, final byte prec) {
    if (parameters != null) {
      new ListLayout<T, Concrete.Parameter<T>>(){
        @Override
        void printListElement(PrettyPrintVisitor<T> ppv, Concrete.Parameter<T> parameter) {
          ppv.prettyPrintParameter(parameter, prec);
        }

        @Override
        String getSeparator() {
          return " ";
        }
      }.doPrettyPrint(this, parameters, noIndent);
    } else {
      myBuilder.append("{!error}");
    }
  }

  public void prettyPrintParameter(Concrete.Parameter<T> parameter, byte prec) {
    if (parameter instanceof Concrete.NameParameter) {
      Referable referable = ((Concrete.NameParameter) parameter).getReferable();
      String name = referable == null ? null : referable.getName();
      if (name == null) {
        name = "_";
      }
      myBuilder.append(parameter.getExplicit() ? name : '{' + name + '}');
    } else
    if (parameter instanceof Concrete.TelescopeParameter) {
      myBuilder.append(parameter.getExplicit() ? '(' : '{');
      for (Referable referable : ((Concrete.TelescopeParameter<T>) parameter).getReferableList()) {
        myBuilder.append(referable == null ? "_" : referable.getName()).append(' ');
      }

      myBuilder.append(": ");
      ((Concrete.TypeParameter<T>) parameter).getType().accept(this, Concrete.Expression.PREC);
      myBuilder.append(parameter.getExplicit() ? ')' : '}');
    } else
    if (parameter instanceof Concrete.TypeParameter) {
      Concrete.Expression<T> type = ((Concrete.TypeParameter<T>) parameter).getType();
      if (parameter.getExplicit()) {
        type.accept(this, prec);
      } else {
        myBuilder.append('{');
        type.accept(this, Concrete.Expression.PREC);
        myBuilder.append('}');
      }
    }
  }

  @Override
  public Void visitLam(final Concrete.LamExpression<T> expr, Byte prec) {
    if (prec > Concrete.LamExpression.PREC) myBuilder.append("(");
    myBuilder.append("\\lam ");

    new BinOpLayout<T>(){
      @Override
      void printLeft(PrettyPrintVisitor<T> pp) {
        pp.prettyPrintParameters(expr.getParameters(), Concrete.Expression.PREC);
      }

      @Override
      void printRight(PrettyPrintVisitor<T> pp) {
        expr.getBody().accept(pp, Concrete.LamExpression.PREC);
      }

      @Override
      String getOpText() {
        return "=>";
      }
    }.doPrettyPrint(this, noIndent);

    if (prec > Concrete.LamExpression.PREC) myBuilder.append(")");
    return null;
  }

  @Override
  public Void visitPi(final Concrete.PiExpression<T> expr, Byte prec) {
    if (prec > Concrete.PiExpression.PREC) myBuilder.append('(');

    new BinOpLayout<T>(){
      @Override
      void printLeft(PrettyPrintVisitor<T> pp) {
        byte domPrec = (byte) (expr.getParameters().size() > 1 ? Concrete.AppExpression.PREC + 1 : Concrete.PiExpression.PREC + 1);
        if (expr.getParameters().size() == 1 && !(expr.getParameters().get(0) instanceof Concrete.TelescopeParameter)) {
          expr.getParameters().get(0).getType().accept(pp, (byte) (Concrete.PiExpression.PREC + 1));
          pp.myBuilder.append(' ');
        } else {
          pp.myBuilder.append("\\Pi ");
          for (Concrete.Parameter<T> parameter : expr.getParameters()) {
            pp.prettyPrintParameter(parameter, domPrec);
            pp.myBuilder.append(' ');
          }
        }
      }

      @Override
      void printRight(PrettyPrintVisitor<T> ppv_right) {
        expr.getCodomain().accept(ppv_right, Concrete.PiExpression.PREC);
      }

      @Override
      String getOpText() {
        return "->";
      }

      @Override
      boolean printSpaceBefore() {
        return false;
      }
    }.doPrettyPrint(this, noIndent);

    if (prec > Concrete.PiExpression.PREC) myBuilder.append(')');
    return null;
  }

  private int getVariableNumber(InferenceLevelVariable variable) {
    Map<InferenceLevelVariable, Integer> variables = variable.getType() == LevelVariable.LvlType.PLVL ? myPVariables : myHVariables;
    Integer number = variables.get(variable);
    if (number != null) {
      return number;
    }

    if (variables.isEmpty()) {
      variables = new HashMap<>();
      if (variable.getType() == LevelVariable.LvlType.PLVL) {
        myPVariables = variables;
      } else {
        myHVariables = variables;
      }
    }

    int num = variables.size() + 1;
    variables.put(variable, num);
    return num;
  }

  @Override
  public Void visitInf(Concrete.InfLevelExpression expr, Byte param) {
    myBuilder.append("\\inf");
    return null;
  }

  @Override
  public Void visitLP(Concrete.PLevelExpression expr, Byte param) {
    myBuilder.append("\\lp");
    return null;
  }

  @Override
  public Void visitLH(Concrete.HLevelExpression expr, Byte param) {
    myBuilder.append("\\lh");
    return null;
  }

  @Override
  public Void visitNumber(Concrete.NumberLevelExpression expr, Byte param) {
    myBuilder.append(expr.getNumber());
    return null;
  }

  @Override
  public Void visitSuc(Concrete.SucLevelExpression<T> expr, Byte prec) {
    if (prec > Concrete.AppExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\suc ");
    expr.getExpression().accept(this, (byte) (Concrete.AppExpression.PREC + 1));
    if (prec > Concrete.AppExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitMax(Concrete.MaxLevelExpression<T> expr, Byte prec) {
    if (prec > Concrete.AppExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\max ");
    expr.getLeft().accept(this, (byte) (Concrete.AppExpression.PREC + 1));
    myBuilder.append(" ");
    expr.getRight().accept(this, (byte) (Concrete.AppExpression.PREC + 1));
    if (prec > Concrete.AppExpression.PREC) myBuilder.append(')');
    return null;
  }

  public void prettyPrintInferLevelVar(InferenceLevelVariable variable) {
    myBuilder.append(variable).append(getVariableNumber(variable));
  }

  @Override
  public Void visitVar(Concrete.InferVarLevelExpression expr, Byte param) {
    InferenceLevelVariable variable = expr.getVariable();
    prettyPrintInferLevelVar(variable);
    return null;
  }

  @Override
  public Void visitUniverse(Concrete.UniverseExpression<T> expr, Byte prec) {
    if (expr.getHLevel() instanceof Concrete.NumberLevelExpression && ((Concrete.NumberLevelExpression) expr.getHLevel()).getNumber() == -1) {
      myBuilder.append("\\Prop");
      return null;
    }

    boolean hParens = !(expr.getHLevel() instanceof Concrete.InfLevelExpression || expr.getHLevel() instanceof Concrete.NumberLevelExpression || expr.getHLevel() == null);
    boolean parens = prec > Concrete.AppExpression.PREC && (hParens || !(expr.getPLevel() instanceof Concrete.NumberLevelExpression || expr.getPLevel() == null));
    if (parens) myBuilder.append('(');

    if (expr.getHLevel() instanceof Concrete.InfLevelExpression) {
      myBuilder.append("\\oo-Type");
    } else
    if (expr.getHLevel() instanceof Concrete.NumberLevelExpression) {
      int hLevel = ((Concrete.NumberLevelExpression) expr.getHLevel()).getNumber();
      if (hLevel == 0) {
        myBuilder.append("\\Set");
      } else {
        myBuilder.append("\\").append(hLevel).append("-Type");
      }
    } else {
      myBuilder.append("\\Type");
    }

    if (expr.getPLevel() instanceof Concrete.NumberLevelExpression) {
      myBuilder.append(((Concrete.NumberLevelExpression) expr.getPLevel()).getNumber());
    } else if (expr.getPLevel() != null) {
      myBuilder.append(" ");
      expr.getPLevel().accept(this, (byte) (Concrete.AppExpression.PREC + 1));
    }

    if (hParens) {
      myBuilder.append(" ");
      expr.getHLevel().accept(this, (byte) (Concrete.AppExpression.PREC + 1));
    }

    if (parens) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitInferHole(Concrete.InferHoleExpression expr, Byte prec) {
    myBuilder.append('_');
    return null;
  }

  @Override
  public Void visitGoal(Concrete.GoalExpression expr, Byte prec) {
    myBuilder.append("{?");
    if (expr.getName() != null) {
      myBuilder.append(expr.getName());
    }
    myBuilder.append('}');
    return null;
  }

  @Override
  public Void visitTuple(Concrete.TupleExpression<T> expr, Byte prec) {
    myBuilder.append('(');

    new ListLayout<T, Concrete.Expression<T>>(){
      @Override
      void printListElement(PrettyPrintVisitor<T> ppv, Concrete.Expression<T> o) {
        o.accept(ppv, Concrete.Expression.PREC);
      }

      @Override
      String getSeparator() {
        return ",";
      }
    }.doPrettyPrint(this, expr.getFields(), noIndent);

    myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitSigma(Concrete.SigmaExpression<T> expr, Byte prec) {
    if (prec > Concrete.SigmaExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\Sigma ");

    prettyPrintParameters(expr.getParameters(), (byte) (Concrete.AppExpression.PREC + 1));

    if (prec > Concrete.SigmaExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitBinOp(final Concrete.BinOpExpression<T> expr, final Byte prec) {
    Referable referable = expr.getReferent();
    Precedence precedence = referable instanceof GlobalReferable ? myInfoProvider.precedenceOf((GlobalReferable) expr.getReferent()) : Precedence.DEFAULT;
    if (expr.getRight() == null) {
      if (prec > precedence.priority) {
        myBuilder.append('(');
      }
      expr.getLeft().accept(this, (byte) (precedence.priority + (precedence.associativity == Precedence.Associativity.LEFT_ASSOC ? 0 : 1)));
      String name = referable.getName();
      myBuilder.append(expr.getRight() == null ? " " + name + "`" : (isPrefix(name) ? " `" : " ") + name);
      if (prec > precedence.priority) {
        myBuilder.append(')');
      }
    } else {
      new BinOpLayout<T>() {
        @Override
        void printLeft(PrettyPrintVisitor<T> pp) {
          if (prec > precedence.priority) pp.myBuilder.append('(');
          expr.getLeft().accept(pp, (byte) (precedence.priority + (precedence.associativity == Precedence.Associativity.LEFT_ASSOC ? 0 : 1)));
        }

        @Override
        void printRight(PrettyPrintVisitor<T> pp) {
          expr.getRight().accept(pp, (byte) (precedence.priority + (precedence.associativity == Precedence.Associativity.RIGHT_ASSOC ? 0 : 1)));
          if (prec > precedence.priority) pp.myBuilder.append(')');
        }

        @Override
        String getOpText() {
          String result = referable.getName();
          return expr.getRight() == null ? result + "`" : (isPrefix(result) ? "`" : "") + result;
        }

        @Override
        boolean increaseIndent(List<String> right_strings) {
          Concrete.Expression r = expr.getRight();
          if (r instanceof Concrete.BinOpExpression) {
            Referable ref = ((Concrete.BinOpExpression) r).getReferent();
            Precedence refPrec = ref instanceof GlobalReferable ? myInfoProvider.precedenceOf(((GlobalReferable) ref)) : Precedence.DEFAULT;
            if (prec <= refPrec.priority)
              return false; // no bracket drawn
          }
          return super.increaseIndent(right_strings);
        }
      }.doPrettyPrint(this, noIndent);
    }

    return null;
  }

  @Override
  public Void visitBinOpSequence(Concrete.BinOpSequenceExpression<T> expr, Byte prec) {
    if (expr.getSequence().isEmpty()) {
      expr.getLeft().accept(this, prec);
      return null;
    }
    if (prec > Concrete.BinOpSequenceExpression.PREC) myBuilder.append('(');
    expr.getLeft().accept(this, (byte) 10);
    for (Concrete.BinOpSequenceElem<T> elem : expr.getSequence()) {
      myBuilder.append(isPrefix(elem.binOp.getReferent().getName()) ? " `" : " ").append(elem.binOp.getReferent().getName()).append(elem.argument == null ? "` " : " ");
      if (elem.argument != null) {
        elem.argument.accept(this, (byte) 10);
      }
    }
    if (prec > Concrete.BinOpSequenceExpression.PREC) myBuilder.append(')');
    return null;
  }

  public void prettyPrintFunctionClause(final Concrete.FunctionClause<T> clause) {
    if (clause == null) return;

    printIndent();
    myBuilder.append("| ");

    if (clause.getExpression() != null) {
      new BinOpLayout<T>(){
        @Override
        void printLeft(PrettyPrintVisitor<T> pp) {
          pp.prettyPrintClause(clause);
        }

        @Override
        void printRight(PrettyPrintVisitor<T> pp) {
          clause.getExpression().accept(pp, Concrete.Expression.PREC);
        }

        @Override
        String getOpText() {
          return "=>";
        }
      }.doPrettyPrint(this, noIndent);
    } else {
      for (int i = 0; i < clause.getPatterns().size(); i++) {
        prettyPrintPattern(clause.getPatterns().get(i), Concrete.Pattern.PREC);
        if (i != clause.getPatterns().size() - 1) {
          myBuilder.append(", ");
        }
      }
    }

    myBuilder.append('\n');
  }

  private void prettyPrintClauses(List<? extends Concrete.Expression<T>> expressions, List<? extends Concrete.FunctionClause<T>> clauses) {
    if (!expressions.isEmpty()) {
      myBuilder.append(" ");
      for (int i = 0; i < expressions.size(); i++) {
        expressions.get(i).accept(this, Concrete.Expression.PREC);
        if (i != expressions.size() - 1) {
          myBuilder.append(", ");
        }
      }
    }

    if (!clauses.isEmpty()) {
      myBuilder.append(" {\n");
      myIndent += INDENT;
      for (Concrete.FunctionClause<T> clause : clauses) {
        prettyPrintFunctionClause(clause);
      }

      printIndent();
      myBuilder.append('}');
    }
  }

  @Override
  public Void visitCase(Concrete.CaseExpression<T> expr, Byte prec) {
    if (prec > Concrete.CaseExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\case");
    prettyPrintClauses(expr.getExpressions(), expr.getClauses());
    myIndent -= INDENT;
    if (prec > Concrete.CaseExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitProj(Concrete.ProjExpression<T> expr, Byte prec) {
    if (prec > Concrete.ProjExpression.PREC) myBuilder.append('(');
    expr.getExpression().accept(this, Concrete.ProjExpression.PREC);
    myBuilder.append('.').append(expr.getField() + 1);
    if (prec > Concrete.ProjExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitClassExt(Concrete.ClassExtExpression<T> expr, Byte prec) {
    if (prec > Concrete.ClassExtExpression.PREC) myBuilder.append('(');
    expr.getBaseClassExpression().accept(this, (byte) -Concrete.ClassExtExpression.PREC);
    myBuilder.append(" ");
    visitClassFieldImpls(expr.getStatements());
    if (prec > Concrete.ClassExtExpression.PREC) myBuilder.append(')');
    return null;
  }

  private void visitClassFieldImpls(Collection<? extends Concrete.ClassFieldImpl<T>> classFieldImpls) {
    myBuilder.append("{\n");
    myIndent += INDENT;
    for (Concrete.ClassFieldImpl<T> classFieldImpl : classFieldImpls) {
      printIndent();
      myBuilder.append("| ");
      visitClassFieldImpl(classFieldImpl);
      myBuilder.append("\n");
    }
    myIndent -= INDENT;
    printIndent();
    myBuilder.append("}");
  }

  private void visitClassFieldImpl(Concrete.ClassFieldImpl<T> classFieldImpl) {
    myBuilder.append(classFieldImpl.getImplementedField().getName()).append(" => ");
    classFieldImpl.getImplementation().accept(this, Concrete.Expression.PREC);
  }

  @Override
  public Void visitNew(Concrete.NewExpression<T> expr, Byte prec) {
    if (prec > Concrete.NewExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\new ");
    expr.getExpression().accept(this, Concrete.NewExpression.PREC);
    if (prec > Concrete.NewExpression.PREC) myBuilder.append(')');
    return null;
  }

  public void prettyPrintLetClause(final Concrete.LetClause<T> letClause, boolean printPipe) {
    if (printPipe) {
      myBuilder.append("| ");
    }
    myBuilder.append(letClause.getReferable().getName());
    for (Concrete.Parameter<T> arg : letClause.getParameters()) {
      myBuilder.append(" ");
      prettyPrintParameter(arg, Concrete.LetExpression.PREC);
    }

    if (letClause.getResultType()!=null) {
      new BinOpLayout<T>() {
        @Override
        void printLeft(PrettyPrintVisitor<T> pp) {
          myBuilder.append(" : ");
          letClause.getResultType().accept(pp, Concrete.Expression.PREC);
        }

        @Override
        void printRight(PrettyPrintVisitor<T> pp) {
          letClause.getTerm().accept(pp, Concrete.LetExpression.PREC);
        }

        @Override
        String getOpText() {
          return "=>";
        }
      }.doPrettyPrint(this, noIndent);
    } else {
      myBuilder.append(" => ");
      letClause.getTerm().accept(this, Concrete.LetExpression.PREC);
    }
  }

  @Override
  public Void visitLet(Concrete.LetExpression<T> expr, Byte prec) {
    if (prec > Concrete.LetExpression.PREC) myBuilder.append('(');
    myBuilder.append("\n");
    myIndent += INDENT;
    printIndent();
    String let = "\\let ";
    myBuilder.append(let);

    final int INDENT0 = let.length();
    myIndent += INDENT0;
    for (int i = 0; i < expr.getClauses().size(); ++i) {
      prettyPrintLetClause(expr.getClauses().get(i), expr.getClauses().size() > 1);
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
    expr.getExpression().accept(this, Concrete.LetExpression.PREC);
    myIndent -= INDENT1;
    myIndent -= INDENT;

    if (prec > Concrete.LetExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitNumericLiteral(Concrete.NumericLiteral expr, Byte params) {
    myBuilder.append(expr.getNumber());
    return null;
  }

  public void printIndent() {
    for (int i = 0; i < myIndent; ++i) {
      myBuilder.append(' ');
    }
  }

  private void prettyPrintNameWithPrecedence(GlobalReferable def) {
    Precedence precedence = myInfoProvider.precedenceOf(def);
    if (!precedence.equals(Precedence.DEFAULT)) {
      myBuilder.append("\\infix");
      if (precedence.associativity == Precedence.Associativity.LEFT_ASSOC) myBuilder.append('l');
      if (precedence.associativity == Precedence.Associativity.RIGHT_ASSOC) myBuilder.append('r');
      myBuilder.append(' ');
      myBuilder.append(precedence.priority);
      myBuilder.append(' ');
    }

    myBuilder.append(myInfoProvider.nameFor(def));
  }

  private void prettyPrintBody(Concrete.FunctionBody<T> body) {
    if (body instanceof Concrete.TermFunctionBody) {
      myBuilder.append(" => ");
      ((Concrete.TermFunctionBody<T>) body).getTerm().accept(this, Concrete.Expression.PREC);
    } else {
      Concrete.ElimFunctionBody<T> elimFunctionBody = (Concrete.ElimFunctionBody<T>) body;
      prettyPrintEliminatedReferences(elimFunctionBody.getEliminatedReferences());
      prettyPrintClauses(Collections.emptyList(), elimFunctionBody.getClauses());
    }
  }

  @Override
  public Void visitFunction(final Concrete.FunctionDefinition<T> def, Void ignored) {
    myBuilder.append("\\function\n");
    printIndent();
    prettyPrintNameWithPrecedence(def);
    myBuilder.append(" ");

    final BinOpLayout<T> l = new BinOpLayout<T>(){
      @Override
      void printLeft(PrettyPrintVisitor<T> pp) {
        pp.prettyPrintParameters(def.getParameters(), Concrete.ReferenceExpression.PREC);
      }

      @Override
      void printRight(PrettyPrintVisitor<T> pp) {
        //noinspection ConstantConditions
        def.getResultType().accept(pp, Concrete.Expression.PREC);
      }

      @Override
      boolean printSpaceBefore() {
        return def.getParameters().size() > 0;
      }

      @Override
      String getOpText() {
        return ":";
      }
    };

    final BinOpLayout<T> r = new BinOpLayout<T>(){
      @Override
      String getOpText() {
        return "";
      }

      @Override
      void printRight(PrettyPrintVisitor<T> pp) {
        pp.prettyPrintBody(def.getBody());
      }

      @Override
      void printLeft(PrettyPrintVisitor<T> pp) {
        if (def.getResultType() != null) {
          l.doPrettyPrint(pp, noIndent);
        } else {
          l.printLeft(pp);
        }
      }

      @Override
      boolean printSpaceBefore() {
        return def.getParameters().size() > 0 || def.getResultType() != null;
      }

      @Override
      boolean increaseIndent(List<String> rhs_strings) {
        return !(rhs_strings.size() > 0 && (spacesCount(rhs_strings.get(0)) > 0 || rhs_strings.get(0).isEmpty()));
      }

      @Override
      boolean doHyphenation(int leftLen, int rightLen) {
        return def.getResultType() != null || super.doHyphenation(leftLen, rightLen);
      }
    };

    r.doPrettyPrint(this, noIndent);

    return null;
  }

  @Override
  public Void visitData(Concrete.DataDefinition<T> def, Void ignored) {
    myBuilder.append("\\data ");
    prettyPrintNameWithPrecedence(def);

    List<? extends Concrete.TypeParameter<T>> parameters = def.getParameters();
    for (Concrete.TypeParameter<T> parameter : parameters) {
      myBuilder.append(' ');
      prettyPrintParameter(parameter, Concrete.ReferenceExpression.PREC);
    }

    Concrete.Expression<T> universe = def.getUniverse();
    if (universe != null) {
      myBuilder.append(" : ");
      universe.accept(this, Concrete.Expression.PREC);
    }
    myIndent += INDENT;

    prettyPrintEliminatedReferences(def.getEliminatedReferences());

    for (Concrete.ConstructorClause<T> clause : def.getConstructorClauses()) {
      if (clause.getPatterns() == null) {
        for (Concrete.Constructor<T> constructor : clause.getConstructors()) {
          myBuilder.append('\n');
          printIndent();
          myBuilder.append("| ");
          constructor.accept(this, null);
        }
      } else {
        myBuilder.append('\n');
        prettyPrintClause(clause);
      }
    }
    myIndent -= INDENT;
    return null;
  }

  private void prettyPrintEliminatedReferences(List<? extends Concrete.ReferenceExpression> references) {
    if (references == null) {
      myBuilder.append(" \\with");
      return;
    }
    if (references.isEmpty()) {
      return;
    }

    myBuilder.append(" => \\elim ");
    boolean first = true;
    for (Concrete.ReferenceExpression ref : references) {
      if (first) {
        first = false;
      } else {
        myBuilder.append(", ");
      }
      myBuilder.append(ref.getReferent().getName());
    }
  }

  private void prettyPrintConstructorClause(Concrete.ConstructorClause<T> clause) {
    printIndent();
    myBuilder.append("| ");
    prettyPrintClause(clause);
    myBuilder.append(" => ");

    if (clause.getConstructors().size() > 1) {
      myBuilder.append("{ ");
    }
    boolean first = true;
    for (Concrete.Constructor<T> constructor : clause.getConstructors()) {
      if (first) {
        first = false;
      } else {
        myBuilder.append(" | ");
      }
      constructor.accept(this, null);
    }
    if (clause.getConstructors().size() > 1) {
      myBuilder.append(" }");
    }
  }

  private void prettyPrintClause(Concrete.Clause<T> clause) {
    if (clause.getPatterns() == null) {
      return;
    }
    boolean first = true;
    for (Concrete.Pattern<T> pattern : clause.getPatterns()) {
      if (first) {
        first = false;
      } else {
        myBuilder.append(", ");
      }
      prettyPrintPattern(pattern, Concrete.Pattern.PREC);
    }
  }

  public void prettyPrintPattern(Concrete.Pattern<T> pattern, byte prec) {
    if (!pattern.isExplicit()) {
      myBuilder.append("{");
    }

    if (pattern instanceof Concrete.NamePattern) {
      Referable referable = ((Concrete.NamePattern) pattern).getReferable();
      String name = referable == null ? null : referable.getName();
      if (name == null) {
        name = "_";
      }
      myBuilder.append(name);
    } else if (pattern instanceof Concrete.EmptyPattern) {
      myBuilder.append("()");
    } else if (pattern instanceof Concrete.ConstructorPattern) {
      Concrete.ConstructorPattern<T> conPattern = (Concrete.ConstructorPattern<T>) pattern;
      if (!conPattern.getPatterns().isEmpty() && prec > Concrete.Pattern.PREC && pattern.isExplicit()) myBuilder.append('(');

      if (!isPrefix(conPattern.getConstructor().getName())) {
        myBuilder.append('`');
      }
      myBuilder.append(conPattern.getConstructor().getName());
      for (Concrete.Pattern<T> patternArg : conPattern.getPatterns()) {
        myBuilder.append(' ');
        prettyPrintPattern(patternArg, (byte) (Concrete.Pattern.PREC + 1));
      }

      if (!conPattern.getPatterns().isEmpty() && prec > Concrete.Pattern.PREC && pattern.isExplicit()) myBuilder.append(')');
    }

    if (!pattern.isExplicit()) {
      myBuilder.append("}");
    }
  }

  @Override
  public Void visitConstructor(Concrete.Constructor<T> def, Void ignored) {
    prettyPrintNameWithPrecedence(def);
    for (Concrete.TypeParameter<T> parameter : def.getParameters()) {
      myBuilder.append(' ');
      prettyPrintParameter(parameter, Concrete.ReferenceExpression.PREC);
    }

    if (!def.getEliminatedReferences().isEmpty()) {
      prettyPrintEliminatedReferences(def.getEliminatedReferences());
      prettyPrintClauses(Collections.emptyList(), def.getClauses());
    }
    return null;
  }

  private void prettyPrintClassDefinitionHeader(Concrete.ClassDefinition<T> def) {
    myBuilder.append("\\class ").append(def.getName());
    prettyPrintParameters(def.getPolyParameters(), Concrete.ReferenceExpression.PREC);
    if (!def.getSuperClasses().isEmpty()) {
      myBuilder.append(" \\extends");
      int i = def.getSuperClasses().size();
      for (Concrete.ReferenceExpression<T> superClass : def.getSuperClasses()) {
        myBuilder.append(" ");
        visitReference(superClass, Concrete.Expression.PREC);
        if (--i == 0) {
          myBuilder.append(",");
        }
      }
    }
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition<T> def, Void ignored) {
    prettyPrintClassDefinitionHeader(def);

    Collection<? extends Concrete.ClassField<T>> fields = def.getFields();
    Collection<? extends Concrete.ClassFieldImpl<T>> implementations = def.getImplementations();
    Collection<? extends Concrete.Definition<T>> instanceDefinitions = def.getInstanceDefinitions();

    if (!fields.isEmpty() || !implementations.isEmpty() || !instanceDefinitions.isEmpty()) {
      myBuilder.append(" {");
      myIndent += INDENT;

      for (Concrete.ClassField<T> field : fields) {
        myBuilder.append('\n');
        printIndent();
        myBuilder.append("| ");
        prettyPrintNameWithPrecedence(field);
        myBuilder.append(" : ");
        field.getResultType().accept(this, Concrete.Expression.PREC);
        myBuilder.append('\n');
      }

      for (Concrete.ClassFieldImpl<T> impl : implementations) {
        myBuilder.append('\n');
        printIndent();
        myBuilder.append("| ").append(impl.getImplementedField().getName()).append(" => ");
        impl.getImplementation().accept(this, Concrete.Expression.PREC);
        myBuilder.append('\n');
      }

      for (Concrete.Definition<T> definition : instanceDefinitions) {
        myBuilder.append('\n');
        printIndent();
        definition.accept(this, null);
        myBuilder.append('\n');
      }

      myIndent -= INDENT;
      printIndent();
      myBuilder.append("}");
    }

    return null;
  }

  @Override
  public Void visitClassView(Concrete.ClassView<T> def, Void params) {
    myBuilder.append("\\view ").append(def.getName()).append(" \\on ");
    def.getUnderlyingClass().accept(this, Concrete.Expression.PREC);
    myBuilder.append(" \\by ").append(def.getClassifyingField().getName()).append(" {");

    if (!def.getFields().isEmpty()) {
      boolean hasImplemented = false;
      for (Concrete.ClassViewField<T> field : def.getFields()) {
        if (!Objects.equals(field.getName(), field.getUnderlyingField().getName())) {
          hasImplemented = true;
          break;
        }
      }

      if (hasImplemented) {
        myIndent += INDENT;
        for (Concrete.ClassViewField field : def.getFields()) {
          myBuilder.append("\n");
          printIndent();
          visitClassViewField(field, null);
        }
        myIndent -= INDENT;
        myBuilder.append("\n");
        printIndent();
      } else {
        for (Concrete.ClassViewField field : def.getFields()) {
          myBuilder.append(" ").append(field.getUnderlyingField().getName());
        }
        myBuilder.append(" ");
      }
    }
    myBuilder.append("}");
    return null;
  }

  @Override
  public Void visitClassViewField(Concrete.ClassViewField def, Void params) {
    myBuilder.append(def.getUnderlyingField().getName()).append(" => ").append(def.getName());
    return null;
  }

  @Override
  public Void visitClassViewInstance(Concrete.ClassViewInstance<T> def, Void params) {
    myBuilder.append("\\instance ");
    prettyPrintNameWithPrecedence(def);
    prettyPrintParameters(def.getParameters(), Concrete.ReferenceExpression.PREC);

    myBuilder.append(" => \\new ");
    def.getClassView().accept(this, Concrete.Expression.PREC);
    myBuilder.append(" ");
    visitClassFieldImpls(def.getClassFieldImpls());

    return null;
  }

  public static abstract class ListLayout<T, E>{
    abstract void printListElement(PrettyPrintVisitor<T> ppv, E e);

    abstract String getSeparator();

    public void doPrettyPrint(PrettyPrintVisitor<T> pp, List<? extends E> l, boolean disabled){
      if (disabled) {
        if (l.size() > 0)
        printListElement(pp, l.get(0));
        if (l.size() > 1)
        for (E e : l.subList(1, l.size())) {
          pp.myBuilder.append(getSeparator());
          printListElement(pp, e);
        }
        return;
      }

      int rem = -1;
      int indent = 0;
      boolean isMultLine = false;
      boolean splitMultiLineArgs;
      for (E e : l) {
        StringBuilder sb = new StringBuilder();
        PrettyPrintVisitor<T> ppv = new PrettyPrintVisitor<>(sb, pp.myInfoProvider, 0, !pp.noIndent);
        printListElement(ppv, e);

        String[] strs = sb.toString().split("[\\r\\n]+");
        int sz = strs.length;

        splitMultiLineArgs = false;
        if (sz > 1) {
          //This heuristic enforces line break if both the present and the previous arguments were multi-line
          if (isMultLine) {
            splitMultiLineArgs = true;
          }
          isMultLine = true;
        } else {
          isMultLine = false;
        }

        if (rem != -1) {
          String separator = getSeparator();

          pp.myBuilder.append(separator.trim());
          if (rem + strs[0].length() + separator.length() > MAX_LEN || splitMultiLineArgs) {
            if (indent == 0) pp.myIndent += INDENT;
            indent = INDENT;
            pp.myBuilder.append('\n');
            rem = 0;
          } else {
            pp.myBuilder.append(' ');
            rem++;
          }
        }

        for (int i = 0; i < sz; i++) {
          String s = strs[i];
          if (rem == 0) pp.printIndent();
          pp.myBuilder.append(s);
          rem += s.trim().length();
          if (i < sz - 1) {
            pp.myBuilder.append('\n');
            rem = 0;
          }
        }
      }
      pp.myIndent -= indent;
    }
  }

  public static abstract class BinOpLayout<T> {
    abstract void printLeft(PrettyPrintVisitor<T> pp);
    abstract void printRight(PrettyPrintVisitor<T> pp);
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

    public void doPrettyPrint(PrettyPrintVisitor<T> ppv_default, boolean disabled) {
      if (disabled) {
        printLeft(ppv_default);
        if (printSpaceBefore()) ppv_default.myBuilder.append(" ");
        ppv_default.myBuilder.append(getOpText().trim());
        if (printSpaceAfter()) ppv_default.myBuilder.append(" ");
        printRight(ppv_default);
        return;
      }

      StringBuilder lhs = new StringBuilder();
      StringBuilder rhs = new StringBuilder();
      PrettyPrintVisitor<T> ppv_left = new PrettyPrintVisitor<>(lhs, ppv_default.myInfoProvider, 0, !ppv_default.noIndent);
      PrettyPrintVisitor<T> ppv_right = new PrettyPrintVisitor<>(rhs, ppv_default.myInfoProvider, 0, !ppv_default.noIndent);

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

  /*
  private void visitWhere(Collection<? extends LegacyAbstract.Statement> statements) {
    myBuilder.append("\\where {");
    myIndent += INDENT;
    for (LegacyAbstract.Statement statement : statements) {
      myBuilder.append("\n");
      printIndent();
      statement.accept(this, null);
      myBuilder.append("\n");
    }
    myIndent -= INDENT;
    myBuilder.append("}");
  }

  @Override
  public Void visitFunction(final Abstract.FunctionDefinition def, Void ignored) {
    super.visitFunction(def, ignored);

    Collection<? extends LegacyAbstract.Statement> globalStatements = LegacyAbstract.getGlobalStatements(def);
    if (!globalStatements.isEmpty()) {
      myBuilder.append("\n");
      printIndent();
      visitWhere(globalStatements);
    }

    return null;
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
  */
}
