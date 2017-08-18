package com.jetbrains.jetpad.vclang.term.prettyprint;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Abstract.BinOpSequenceElem;
import com.jetbrains.jetpad.vclang.term.Abstract.Constructor;
import com.jetbrains.jetpad.vclang.term.Abstract.Expression;
import com.jetbrains.jetpad.vclang.term.Abstract.ReferenceExpression;
import com.jetbrains.jetpad.vclang.term.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.AbstractLevelExpressionVisitor;

import java.util.*;

public class PrettyPrintVisitor implements AbstractExpressionVisitor<Byte, Void>, AbstractLevelExpressionVisitor<Byte, Void>, AbstractDefinitionVisitor<Void, Void> {
  public static final int INDENT = 4;
  public static final int MAX_LEN = 120;
  public static final float SMALL_RATIO = (float) 0.1;

  protected final StringBuilder myBuilder;
  private Map<InferenceLevelVariable, Integer> myPVariables = Collections.emptyMap();
  private Map<InferenceLevelVariable, Integer> myHVariables = Collections.emptyMap();
  protected int myIndent;
  private boolean noIndent;

  public PrettyPrintVisitor(StringBuilder builder, int indent, boolean doIndent) {
    myBuilder = builder;
    myIndent = indent;
    noIndent = !doIndent;
  }

  public PrettyPrintVisitor(StringBuilder builder, int indent) {
    this(builder, indent, true);
  }

  public static String prettyPrint(Abstract.SourceNode node) {
    StringBuilder builder = new StringBuilder();
    return new PrettyPrintVisitor(builder, 0).prettyPrint(node, Abstract.Expression.PREC) ? builder.toString() : null;
  }

  public boolean prettyPrint(Abstract.SourceNode node, byte prec) {
    if (node instanceof Abstract.Expression) {
      ((Abstract.Expression) node).accept(this, prec);
      return true;
    }
    if (node instanceof Abstract.Parameter) {
      prettyPrintParameter((Abstract.Parameter) node, prec);
      return true;
    }
    if (node instanceof Abstract.Definition) {
      ((Abstract.Definition) node).accept(this, null);
      return true;
    }
    if (node instanceof Abstract.ClassFieldImpl) {
      visitClassFieldImpl((Abstract.ClassFieldImpl) node);
      return true;
    }
    if (node instanceof Abstract.FunctionClause) {
      prettyPrintFunctionClause((Abstract.FunctionClause) node);
      return true;
    }
    if (node instanceof Abstract.ConstructorClause) {
      prettyPrintConstructorClause((Abstract.ConstructorClause) node);
      return true;
    }
    if (node instanceof Abstract.Clause) {
      prettyPrintClause((Abstract.Clause) node);
      return true;
    }
    if (node instanceof Abstract.LetClause) {
      prettyPrintLetClause((Abstract.LetClause) node, false);
      return true;
    }
    if (node instanceof Abstract.Pattern) {
      prettyPrintPattern((Abstract.Pattern) node, prec);
      return true;
    }
    if (node instanceof Abstract.LevelExpression) {
      ((Abstract.LevelExpression) node).accept(this, Abstract.Expression.PREC);
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
          expr.getArgument().getExpression().accept(pp, (byte) (Abstract.AppExpression.PREC + 1));
        } else {
          pp.myBuilder.append("{");
          expr.getArgument().getExpression().accept(pp, Abstract.Expression.PREC);
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

    if (prec > Abstract.AppExpression.PREC) myBuilder.append(')');
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
  public Void visitReference(Abstract.ReferenceExpression expr, Byte prec) {
    if (expr.getExpression() != null) {
      expr.getExpression().accept(this, Abstract.ReferenceExpression.PREC);
      myBuilder.append('.').append(expr.getName());
    } else {
      if (!isPrefix(expr.getName())) {
        myBuilder.append('`');
      }
      myBuilder.append(expr.getName());
    }
    return null;
  }

  @Override
  public Void visitInferenceReference(Abstract.InferenceReferenceExpression expr, Byte params) {
    myBuilder.append("?").append(expr.getVariable().getName());
    return null;
  }

  @Override
  public Void visitModuleCall(Abstract.ModuleCallExpression expr, Byte prec) {
    myBuilder.append(expr.getPath());
    return null;
  }

  public void prettyPrintParameters(List<? extends Abstract.Parameter> parameters, final byte prec) {
    if (parameters != null) {
      new ListLayout<Abstract.Parameter>(){
        @Override
        void printListElement(PrettyPrintVisitor ppv, Abstract.Parameter parameter) {
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

  public void prettyPrintParameter(Abstract.Parameter parameter, byte prec) {
    if (parameter instanceof Abstract.NameParameter) {
      String name = ((Abstract.NameParameter) parameter).getName();
      if (name == null) {
        name = "_";
      }
      myBuilder.append(parameter.getExplicit() ? name : '{' + name + '}');
    } else
    if (parameter instanceof Abstract.TelescopeParameter) {
      myBuilder.append(parameter.getExplicit() ? '(' : '{');
      for (Abstract.ReferableSourceNode referable : ((Abstract.TelescopeParameter) parameter).getReferableList()) {
        myBuilder.append(referable == null ? "_" : referable.getName()).append(' ');
      }

      myBuilder.append(": ");
      ((Abstract.TypeParameter) parameter).getType().accept(this, Abstract.Expression.PREC);
      myBuilder.append(parameter.getExplicit() ? ')' : '}');
    } else
    if (parameter instanceof Abstract.TypeParameter) {
      Abstract.Expression type = ((Abstract.TypeParameter) parameter).getType();
      if (parameter.getExplicit()) {
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
        pp.prettyPrintParameters(expr.getParameters(), Abstract.Expression.PREC);
      }

      @Override
      void printRight(PrettyPrintVisitor pp) {
        expr.getBody().accept(pp, Abstract.LamExpression.PREC);
      }

      @Override
      String getOpText() {
        return "=>";
      }
    }.doPrettyPrint(this, noIndent);

    if (prec > Abstract.LamExpression.PREC) myBuilder.append(")");
    return null;
  }

  @Override
  public Void visitPi(final Abstract.PiExpression expr, Byte prec) {
    if (prec > Abstract.PiExpression.PREC) myBuilder.append('(');

    new BinOpLayout(){
      @Override
      void printLeft(PrettyPrintVisitor pp) {
        byte domPrec = (byte) (expr.getParameters().size() > 1 ? Abstract.AppExpression.PREC + 1 : Abstract.PiExpression.PREC + 1);
        if (expr.getParameters().size() == 1 && !(expr.getParameters().get(0) instanceof Abstract.TelescopeParameter)) {
          expr.getParameters().get(0).getType().accept(pp, (byte) (Abstract.PiExpression.PREC + 1));
          pp.myBuilder.append(' ');
        } else {
          pp.myBuilder.append("\\Pi ");
          for (Abstract.Parameter parameter : expr.getParameters()) {
            pp.prettyPrintParameter(parameter, domPrec);
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
    }.doPrettyPrint(this, noIndent);

    if (prec > Abstract.PiExpression.PREC) myBuilder.append(')');
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
  public Void visitInf(Abstract.InfLevelExpression expr, Byte param) {
    myBuilder.append("\\inf");
    return null;
  }

  @Override
  public Void visitLP(Abstract.PLevelExpression expr, Byte param) {
    myBuilder.append("\\lp");
    return null;
  }

  @Override
  public Void visitLH(Abstract.HLevelExpression expr, Byte param) {
    myBuilder.append("\\lh");
    return null;
  }

  @Override
  public Void visitNumber(Abstract.NumberLevelExpression expr, Byte param) {
    myBuilder.append(expr.getNumber());
    return null;
  }

  @Override
  public Void visitSuc(Abstract.SucLevelExpression expr, Byte prec) {
    if (prec > Abstract.AppExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\suc ");
    expr.getExpression().accept(this, (byte) (Abstract.AppExpression.PREC + 1));
    if (prec > Abstract.AppExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitMax(Abstract.MaxLevelExpression expr, Byte prec) {
    if (prec > Abstract.AppExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\max ");
    expr.getLeft().accept(this, (byte) (Abstract.AppExpression.PREC + 1));
    myBuilder.append(" ");
    expr.getRight().accept(this, (byte) (Abstract.AppExpression.PREC + 1));
    if (prec > Abstract.AppExpression.PREC) myBuilder.append(')');
    return null;
  }

  public void prettyPrintInferLevelVar(InferenceLevelVariable variable) {
    myBuilder.append(variable).append(getVariableNumber(variable));
  }

  @Override
  public Void visitVar(Abstract.InferVarLevelExpression expr, Byte param) {
    InferenceLevelVariable variable = expr.getVariable();
    prettyPrintInferLevelVar(variable);
    return null;
  }

  @Override
  public Void visitUniverse(Abstract.UniverseExpression expr, Byte prec) {
    if (expr.getHLevel() instanceof Abstract.NumberLevelExpression && ((Abstract.NumberLevelExpression) expr.getHLevel()).getNumber() == -1) {
      myBuilder.append("\\Prop");
      return null;
    }

    boolean hParens = !(expr.getHLevel() instanceof Abstract.InfLevelExpression || expr.getHLevel() instanceof Abstract.NumberLevelExpression || expr.getHLevel() == null);
    boolean parens = prec > Abstract.AppExpression.PREC && (hParens || !(expr.getPLevel() instanceof Abstract.NumberLevelExpression || expr.getPLevel() == null));
    if (parens) myBuilder.append('(');

    if (expr.getHLevel() instanceof Abstract.InfLevelExpression) {
      myBuilder.append("\\oo-Type");
    } else
    if (expr.getHLevel() instanceof Abstract.NumberLevelExpression) {
      int hLevel = ((Abstract.NumberLevelExpression) expr.getHLevel()).getNumber();
      if (hLevel == 0) {
        myBuilder.append("\\Set");
      } else {
        myBuilder.append("\\").append(hLevel).append("-Type");
      }
    } else {
      myBuilder.append("\\Type");
    }

    if (expr.getPLevel() instanceof Abstract.NumberLevelExpression) {
      myBuilder.append(((Abstract.NumberLevelExpression) expr.getPLevel()).getNumber());
    } else if (expr.getPLevel() != null) {
      myBuilder.append(" ");
      expr.getPLevel().accept(this, (byte) (Abstract.AppExpression.PREC + 1));
    }

    if (hParens) {
      myBuilder.append(" ");
      expr.getHLevel().accept(this, (byte) (Abstract.AppExpression.PREC + 1));
    }

    if (parens) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitInferHole(Abstract.InferHoleExpression expr, Byte prec) {
    myBuilder.append('_');
    return null;
  }

  @Override
  public Void visitGoal(Abstract.GoalExpression expr, Byte prec) {
    myBuilder.append("{?");
    if (expr.getName() != null) {
      myBuilder.append(expr.getName());
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
    }.doPrettyPrint(this, expr.getFields(), noIndent);

    myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitSigma(Abstract.SigmaExpression expr, Byte prec) {
    if (prec > Abstract.SigmaExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\Sigma ");

    prettyPrintParameters(expr.getParameters(), (byte) (Abstract.AppExpression.PREC + 1));

    if (prec > Abstract.SigmaExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitBinOp(final Abstract.BinOpExpression expr, final Byte prec) {
    Abstract.Precedence precedence = expr.getReferent() instanceof Abstract.Definition ? ((Abstract.Definition) expr.getReferent()).getPrecedence() : Abstract.Precedence.DEFAULT;
    if (expr.getRight() == null) {
      if (prec > precedence.priority) {
        myBuilder.append('(');
      }
      expr.getLeft().accept(this, (byte) (precedence.priority + (precedence.associativity == Abstract.Precedence.Associativity.LEFT_ASSOC ? 0 : 1)));
      String name = expr.getReferent().getName();
      myBuilder.append(" ").append(name).append("`");
      if (prec > precedence.priority) {
        myBuilder.append(')');
      }
    } else {
      new BinOpLayout() {
        @Override
        void printLeft(PrettyPrintVisitor pp) {
          if (prec > precedence.priority) pp.myBuilder.append('(');
          expr.getLeft().accept(pp, (byte) (precedence.priority + (precedence.associativity == Abstract.Precedence.Associativity.LEFT_ASSOC ? 0 : 1)));
        }

        @Override
        void printRight(PrettyPrintVisitor pp) {
          expr.getRight().accept(pp, (byte) (precedence.priority + (precedence.associativity == Abstract.Precedence.Associativity.RIGHT_ASSOC ? 0 : 1)));
          if (prec > precedence.priority) pp.myBuilder.append(')');
        }

        @Override
        String getOpText() {
          String result = expr.getReferent().getName();
          return (isPrefix(result) ? "`" : "") + result;
        }

        @Override
        boolean increaseIndent(List<String> right_strings) {
          Abstract.Expression r = expr.getRight();
          if (r instanceof Abstract.BinOpExpression) {
            Abstract.ReferableSourceNode ref = ((Abstract.BinOpExpression) r).getReferent();
            Abstract.Precedence refPrec = ref instanceof Abstract.Definition ? ((Abstract.Definition) ref).getPrecedence() : Abstract.Precedence.DEFAULT;
            if (prec <= refPrec.priority)
              return false; // no bracket drawn
          }
          return super.increaseIndent(right_strings);
        }
      }.doPrettyPrint(this, noIndent);
    }

    return null;
  }

  private AbstractLayout createBinOpLayout(final Expression lhs, List<BinOpSequenceElem> elems) {
    if (elems.isEmpty()) {
      if (lhs != null) return (ppv_default, disabled) -> lhs.accept(ppv_default, (byte) 10);
      else return new EmptyLayout();
    }

    final BinOpSequenceElem elem = elems.get(0);
    final AbstractLayout layout = createBinOpLayout(elem.argument, elems.subList(1, elems.size()));
    return new BinOpLayout(){
      @Override
      void printLeft(PrettyPrintVisitor pp) {
        if (lhs != null) lhs.accept(pp, (byte) 10);
      }

      @Override
      void printRight(PrettyPrintVisitor pp) {
        layout.doPrettyPrint(pp, noIndent);
      }

      @Override
      String getOpText() {
        String result = elem.binOp.getName();
        return elem.argument == null ? result + "`" : (isPrefix(result) ? "`" : "") + result;
      }
    };
  }

  @Override
  public Void visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Byte prec) {
    if (expr.getSequence().isEmpty()) {
      expr.getLeft().accept(this, prec);
      return null;
    }
    if (prec > Abstract.BinOpSequenceExpression.PREC) myBuilder.append('(');
    createBinOpLayout(expr.getLeft(), expr.getSequence()).doPrettyPrint(this, noIndent);
    if (prec > Abstract.BinOpSequenceExpression.PREC) myBuilder.append(')');
    return null;
  }

  public void prettyPrintFunctionClause(final Abstract.FunctionClause clause) {
    if (clause == null) return;

    printIndent();
    myBuilder.append("| ");

    if (clause.getExpression() != null) {
      new BinOpLayout(){
        @Override
        void printLeft(PrettyPrintVisitor pp) {
          pp.prettyPrintClause(clause);
        }

        @Override
        void printRight(PrettyPrintVisitor pp) {
          clause.getExpression().accept(pp, Abstract.Expression.PREC);
        }

        @Override
        String getOpText() {
          return "=>";
        }
      }.doPrettyPrint(this, noIndent);
    } else {
      for (int i = 0; i < clause.getPatterns().size(); i++) {
        prettyPrintPattern(clause.getPatterns().get(i), Abstract.Pattern.PREC);
        if (i != clause.getPatterns().size() - 1) {
          myBuilder.append(", ");
        }
      }
    }
  }

  private void prettyPrintClauses(List<? extends Abstract.Expression> expressions, List<? extends Abstract.FunctionClause> clauses, boolean needBraces) {
    if (!expressions.isEmpty()) {
      myBuilder.append(" ");
      for (int i = 0; i < expressions.size(); i++) {
        expressions.get(i).accept(this, Abstract.Expression.PREC);
        if (i != expressions.size() - 1) {
          myBuilder.append(", ");
        }
      }
    }

    if (!clauses.isEmpty()) {
      if (needBraces) myBuilder.append(" {\n"); else myBuilder.append("\n");
      myIndent += INDENT;
      for (int i=0; i<clauses.size(); i++) {
        prettyPrintFunctionClause(clauses.get(i));
        if (i < clauses.size()-1) myBuilder.append('\n');
      }
      myIndent -= INDENT;

      if (needBraces) {
        myBuilder.append('\n');
        printIndent();
        myBuilder.append('}');
      }
    } else if (needBraces) {
      myBuilder.append(" {}");
    }
  }

  @Override
  public Void visitCase(Abstract.CaseExpression expr, Byte prec) {
    if (prec > Abstract.CaseExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\case ");
    new ListLayout<Abstract.Expression>(){
      @Override
      void printListElement(PrettyPrintVisitor ppv, Expression expression) {
        expression.accept(ppv, Abstract.Expression.PREC);
      }

      @Override
      String getSeparator() {
        return ", ";
      }
    }.doPrettyPrint(this, expr.getExpressions(), noIndent);
    myBuilder.append(" \\with");
    prettyPrintClauses(Collections.emptyList(), expr.getClauses(), true);
    myIndent -= INDENT;
    if (prec > Abstract.CaseExpression.PREC) myBuilder.append(')');
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
    myBuilder.append(" ");
    visitClassFieldImpls(expr.getStatements());
    if (prec > Abstract.ClassExtExpression.PREC) myBuilder.append(')');
    return null;
  }

  private void visitClassFieldImpls(Collection<? extends Abstract.ClassFieldImpl> classFieldImpls) {
    myBuilder.append("{\n");
    myIndent += INDENT;
    for (Abstract.ClassFieldImpl classFieldImpl : classFieldImpls) {
      printIndent();
      myBuilder.append("| ");
      visitClassFieldImpl(classFieldImpl);
      myBuilder.append("\n");
    }
    myIndent -= INDENT;
    printIndent();
    myBuilder.append("}");
  }

  private void visitClassFieldImpl(Abstract.ClassFieldImpl classFieldImpl) {
    myBuilder.append(classFieldImpl.getImplementedFieldName()).append(" => ");
    classFieldImpl.getImplementation().accept(this, Abstract.Expression.PREC);
  }

  @Override
  public Void visitNew(Abstract.NewExpression expr, Byte prec) {
    if (prec > Abstract.NewExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\new ");
    expr.getExpression().accept(this, Abstract.NewExpression.PREC);
    if (prec > Abstract.NewExpression.PREC) myBuilder.append(')');
    return null;
  }

  public void prettyPrintLetClause(final Abstract.LetClause letClause, boolean printPipe) {
    if (printPipe) {
      myBuilder.append("| ");
    }
    myBuilder.append(letClause.getName());
    for (Abstract.Parameter arg : letClause.getParameters()) {
      myBuilder.append(" ");
      prettyPrintParameter(arg, Abstract.LetExpression.PREC);
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
          return "=>";
        }
      }.doPrettyPrint(this, noIndent);
    } else {
      myBuilder.append(" => ");
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

  private void prettyPrintNameWithPrecedence(Abstract.Definition def) {
    Abstract.Precedence precedence = def.getPrecedence();
    if (!precedence.equals(Abstract.Precedence.DEFAULT)) {
      myBuilder.append("\\infix");
      if (precedence.associativity == Abstract.Precedence.Associativity.LEFT_ASSOC) myBuilder.append('l');
      if (precedence.associativity == Abstract.Precedence.Associativity.RIGHT_ASSOC) myBuilder.append('r');
      myBuilder.append(' ');
      myBuilder.append(precedence.priority);
      myBuilder.append(' ');
    }

    myBuilder.append(def.getName());
  }

  private void prettyPrintBody(Abstract.FunctionBody body) {
    if (body instanceof Abstract.TermFunctionBody) {
      myBuilder.append("=> ");
      ((Abstract.TermFunctionBody) body).getTerm().accept(this, Abstract.Expression.PREC);
    } else {
      Abstract.ElimFunctionBody elimFunctionBody = (Abstract.ElimFunctionBody) body;
      prettyPrintEliminatedReferences(elimFunctionBody.getEliminatedReferences(), false);
      prettyPrintClauses(Collections.emptyList(), elimFunctionBody.getClauses(), false);
    }
  }

  @Override
  public Void visitFunction(final Abstract.FunctionDefinition def, Void ignored) {
    myBuilder.append("\\function\n");
    printIndent();
    prettyPrintNameWithPrecedence(def);
    myBuilder.append(" ");

    final BinOpLayout l = new BinOpLayout(){
      @Override
      void printLeft(PrettyPrintVisitor pp) {
        pp.prettyPrintParameters(def.getParameters(), Abstract.ReferenceExpression.PREC);
      }

      @Override
      void printRight(PrettyPrintVisitor pp) {
        //noinspection ConstantConditions
        def.getResultType().accept(pp, Abstract.Expression.PREC);
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

    final BinOpLayout r = new BinOpLayout(){
      @Override
      String getOpText() {
        return "";
      }

      @Override
      void printRight(PrettyPrintVisitor pp) {
        pp.prettyPrintBody(def.getBody());
      }

      @Override
      void printLeft(PrettyPrintVisitor pp) {
        if (def.getResultType() != null) {
          l.doPrettyPrint(pp, noIndent);
        } else {
          l.printLeft(pp);
        }
      }

      @Override
      boolean printSpaceBefore() { return true;}

      @Override
      boolean printSpaceAfter() { return false;}
    };

    r.doPrettyPrint(this, noIndent);

    return null;
  }

  @Override
  public Void visitClassField(Abstract.ClassField def, Void params) {
    myBuilder.append("| ");
    prettyPrintNameWithPrecedence(def);
    myBuilder.append(" : ");
    def.getResultType().accept(new PrettyPrintVisitor(myBuilder, myIndent), Abstract.Expression.PREC);

    return null;
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, Void ignored) {
    myBuilder.append("\\data ");
    prettyPrintNameWithPrecedence(def);

    List<? extends Abstract.TypeParameter> parameters = def.getParameters();
    for (Abstract.TypeParameter parameter : parameters) {
      myBuilder.append(' ');
      prettyPrintParameter(parameter, Abstract.ReferenceExpression.PREC);
    }

    Abstract.Expression universe = def.getUniverse();
    if (universe != null) {
      myBuilder.append(" : ");
      universe.accept(this, Abstract.Expression.PREC);
    }
    myIndent += INDENT;

    myBuilder.append(' ');
    prettyPrintEliminatedReferences(def.getEliminatedReferences(), true);

    for (int i=0; i<def.getConstructorClauses().size(); i++) {
      Abstract.ConstructorClause clause = def.getConstructorClauses().get(i);
      if (clause.getPatterns() == null) {
        for (Abstract.Constructor constructor : clause.getConstructors()) {
          myBuilder.append('\n');
          printIndent();
          myBuilder.append("| ");
          constructor.accept(this, null);
        }
      } else {
        myBuilder.append('\n');
        printIndent();
        myBuilder.append("| ");
        new BinOpLayout(){
          @Override
          void printLeft(PrettyPrintVisitor pp) {
            pp.prettyPrintClause(clause);
          }

          @Override
          void printRight(PrettyPrintVisitor pp) {
            new ListLayout<Abstract.Constructor>(){
              @Override
              void printListElement(PrettyPrintVisitor ppv, Constructor constructor) {
                constructor.accept(ppv, null);
              }

              @Override
              String getSeparator() {
                return "\n";
              }
            }.doPrettyPrint(pp, clause.getConstructors(), noIndent);
          }

          @Override
          String getOpText() {
            return "=>";
          }
        }.doPrettyPrint(this, noIndent);
      }
    }
    myIndent -= INDENT;
    return null;
  }

  private void prettyPrintEliminatedReferences(List<? extends Abstract.ReferenceExpression> references, boolean isData) {
    if (references == null) {
      return;
    }
    if (references.isEmpty()) {
      if (isData) myBuilder.append("\\with\n");
      return;
    }

    myBuilder.append("=> \\elim ");
    new ListLayout<Abstract.ReferenceExpression>(){
      @Override
      void printListElement(PrettyPrintVisitor ppv, ReferenceExpression referenceExpression) {
        ppv.myBuilder.append(referenceExpression.getName());
      }

      @Override
      String getSeparator() {
        return ", ";
      }
    }.doPrettyPrint(this, references, noIndent);
  }

  private void prettyPrintConstructorClause(Abstract.ConstructorClause clause) {
    printIndent();
    myBuilder.append("| ");
    prettyPrintClause(clause);
    myBuilder.append(" => ");

    if (clause.getConstructors().size() > 1) {
      myBuilder.append("{ ");
    }
    boolean first = true;
    for (Abstract.Constructor constructor : clause.getConstructors()) {
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

  private void prettyPrintClause(Abstract.Clause clause) {
    if (clause.getPatterns() == null) {
      return;
    }
    boolean first = true;
    for (Abstract.Pattern pattern : clause.getPatterns()) {
      if (first) {
        first = false;
      } else {
        myBuilder.append(", ");
      }
      prettyPrintPattern(pattern, Abstract.Pattern.PREC);
    }
  }

  public void prettyPrintPattern(Abstract.Pattern pattern, byte prec) {
    if (!pattern.isExplicit()) {
      myBuilder.append("{");
    }

    if (pattern instanceof Abstract.NamePattern) {
      String name = ((Abstract.NamePattern) pattern).getName();
      if (name == null) {
        name = "_";
      }
      myBuilder.append(name);
    } else if (pattern instanceof Abstract.EmptyPattern) {
      myBuilder.append("()");
    } else if (pattern instanceof Abstract.ConstructorPattern) {
      Abstract.ConstructorPattern conPattern = (Abstract.ConstructorPattern) pattern;
      if (!conPattern.getPatterns().isEmpty() && prec > Abstract.Pattern.PREC && pattern.isExplicit()) myBuilder.append('(');

      if (!isPrefix(conPattern.getConstructorName())) {
        myBuilder.append('`');
      }
      myBuilder.append(conPattern.getConstructorName());
      for (Abstract.Pattern patternArg : conPattern.getPatterns()) {
        myBuilder.append(' ');
        prettyPrintPattern(patternArg, (byte) (Abstract.Pattern.PREC + 1));
      }

      if (!conPattern.getPatterns().isEmpty() && prec > Abstract.Pattern.PREC && pattern.isExplicit()) myBuilder.append(')');
    }

    if (!pattern.isExplicit()) {
      myBuilder.append("}");
    }
  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, Void ignored) {
    prettyPrintNameWithPrecedence(def);
    for (Abstract.TypeParameter parameter : def.getParameters()) {
      myBuilder.append(' ');
      prettyPrintParameter(parameter, Abstract.ReferenceExpression.PREC);
    }

    if (!def.getEliminatedReferences().isEmpty() || !def.getClauses().isEmpty()) {
      myBuilder.append(' ');
      prettyPrintEliminatedReferences(def.getEliminatedReferences(), false);
      prettyPrintClauses(Collections.emptyList(), def.getClauses(), true);
    }
    return null;
  }

  private void prettyPrintClassDefinitionHeader(Abstract.ClassDefinition def) {
    myBuilder.append("\\class ").append(def.getName());
    prettyPrintParameters(def.getPolyParameters(), Abstract.ReferenceExpression.PREC);
    if (!def.getSuperClasses().isEmpty()) {
      myBuilder.append(" \\extends");
      int i = def.getSuperClasses().size();
      for (Abstract.SuperClass superClass : def.getSuperClasses()) {
        myBuilder.append(" ");
        superClass.getSuperClass().accept(this, Abstract.Expression.PREC);
        if (--i == 0) {
          myBuilder.append(",");
        }
      }
    }
  }

  @Override
  public Void visitClass(Abstract.ClassDefinition def, Void ignored) {
    prettyPrintClassDefinitionHeader(def);

    Collection<? extends Abstract.ClassField> fields = def.getFields();
    Collection<? extends Abstract.Implementation> implementations = def.getImplementations();
    Collection<? extends Abstract.Definition> instanceDefinitions = def.getInstanceDefinitions();

    if (!fields.isEmpty() || !implementations.isEmpty() || !instanceDefinitions.isEmpty()) {
      myBuilder.append(" {");
      myIndent += INDENT;

      if (!fields.isEmpty()) {
        myBuilder.append('\n');
        for (Abstract.ClassField field : fields) {
          printIndent();
          field.accept(this, null);
          myBuilder.append('\n');
        }
      }

      if (!implementations.isEmpty()) {
        myBuilder.append('\n');
        for (Abstract.Implementation implementation : implementations) {
          printIndent();
          implementation.accept(this, null);
          myBuilder.append('\n');
        }
      }

      if (!instanceDefinitions.isEmpty()) {
        myBuilder.append('\n');
        for (Abstract.Definition definition : instanceDefinitions) {
          printIndent();
          definition.accept(this, null);
          myBuilder.append('\n');
        }
      }

      myIndent -= INDENT;
      printIndent();
      myBuilder.append("}");
    }

    return null;
  }

  @Override
  public Void visitImplement(Abstract.Implementation def, Void params) {
    myBuilder.append("\\implement ").append(def.getName()).append(" => ");
    def.getImplementation().accept(this, Abstract.Expression.PREC);
    return null;
  }

  @Override
  public Void visitClassView(Abstract.ClassView def, Void params) {
    myBuilder.append("\\view ").append(def.getName()).append(" \\on ");
    def.getUnderlyingClassReference().accept(this, Abstract.Expression.PREC);
    myBuilder.append(" \\by ").append(def.getClassifyingFieldName()).append(" {");

    if (!def.getFields().isEmpty()) {
      boolean hasImplemented = false;
      for (Abstract.ClassViewField field : def.getFields()) {
        if (!Objects.equals(field.getName(), field.getUnderlyingFieldName())) {
          hasImplemented = true;
          break;
        }
      }

      if (hasImplemented) {
        myIndent += INDENT;
        for (Abstract.ClassViewField field : def.getFields()) {
          myBuilder.append("\n");
          printIndent();
          visitClassViewField(field, null);
        }
        myIndent -= INDENT;
        myBuilder.append("\n");
        printIndent();
      } else {
        for (Abstract.ClassViewField field : def.getFields()) {
          myBuilder.append(" ").append(field.getUnderlyingFieldName());
        }
        myBuilder.append(" ");
      }
    }
    myBuilder.append("}");
    return null;
  }

  @Override
  public Void visitClassViewField(Abstract.ClassViewField def, Void params) {
    myBuilder.append(def.getUnderlyingFieldName()).append(" => ").append(def.getName());
    return null;
  }

  @Override
  public Void visitClassViewInstance(Abstract.ClassViewInstance def, Void params) {
    myBuilder.append("\\instance ");
    prettyPrintNameWithPrecedence(def);
    prettyPrintParameters(def.getParameters(), Abstract.ReferenceExpression.PREC);

    myBuilder.append(" => \\new ");
    def.getClassView().accept(this, Abstract.Expression.PREC);
    myBuilder.append(" ");
    visitClassFieldImpls(def.getClassFieldImpls());

    return null;
  }

  public interface AbstractLayout {
    void doPrettyPrint(PrettyPrintVisitor ppv_default, boolean disabled);
  }

  public static class EmptyLayout implements AbstractLayout {
    public void doPrettyPrint(PrettyPrintVisitor ppv_default, boolean disabled) {}
  }

  public static abstract class ListLayout<T> {
    abstract void printListElement(PrettyPrintVisitor ppv, T t);

    abstract String getSeparator();

    public void doPrettyPrint(PrettyPrintVisitor pp, List<? extends T> l, boolean disabled){
      if (disabled) {
        if (l.size() > 0)
        printListElement(pp, l.get(0));
        if (l.size() > 1)
        for (T t : l.subList(1, l.size())) {
          pp.myBuilder.append(getSeparator());
          printListElement(pp, t);
        }
        return;
      }

      int rem = -1;
      int indent = 0;
      boolean isMultLine = false;
      boolean splitMultiLineArgs;
      for (T t : l) {
        StringBuilder sb = new StringBuilder();
        PrettyPrintVisitor ppv = new PrettyPrintVisitor(sb, 0);
        printListElement(ppv, t);

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
    }
  }

  public static abstract class BinOpLayout implements AbstractLayout {
    abstract void printLeft(PrettyPrintVisitor pp);
    abstract void printRight(PrettyPrintVisitor pp);
    abstract String getOpText();
    boolean printSpaceBefore() {return true;}
    boolean printSpaceAfter() {return true;}

    boolean doHyphenation(int leftLen, int rightLen) {
      if (leftLen == 0) leftLen = 1; if (leftLen > MAX_LEN) leftLen = MAX_LEN;
      if (rightLen == 0) rightLen = 1; if (rightLen > MAX_LEN) rightLen = MAX_LEN;
      double ratio = ((double) rightLen) / leftLen;
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

    public void doPrettyPrint(PrettyPrintVisitor ppv_default, boolean disabled) {
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
