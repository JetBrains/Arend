package com.jetbrains.jetpad.vclang.term.prettyprint;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.definition.Name;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.AbstractLevelExpressionVisitor;

import java.util.*;

public class PrettyPrintVisitor implements AbstractExpressionVisitor<Byte, Void>, AbstractLevelExpressionVisitor<Byte, Void>, AbstractDefinitionVisitor<Void, Void> {
  private final StringBuilder myBuilder;
  private Map<InferenceLevelVariable, Integer> myPVariables = Collections.emptyMap();
  private Map<InferenceLevelVariable, Integer> myHVariables = Collections.emptyMap();
  private int myIndent;
  public static final int INDENT = 4;
  public static final int MAX_LEN = 120;
  public static final float SMALL_RATIO = (float) 0.1;

  public PrettyPrintVisitor(StringBuilder builder, int indent) {
    myBuilder = builder;
    myIndent = indent;
  }

  public static String prettyPrint(Abstract.SourceNode node, int indent) {
    StringBuilder builder = new StringBuilder();
    return new PrettyPrintVisitor(builder, indent).prettyPrint(node, Abstract.Expression.PREC) ? builder.toString() : null;
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
    }.doPrettyPrint(this);

    if (prec > Abstract.AppExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitReference(Abstract.ReferenceExpression expr, Byte prec) {
    if (expr.getExpression() != null) {
      expr.getExpression().accept(this, Abstract.ReferenceExpression.PREC);
      myBuilder.append(".");
    }
    myBuilder.append(new Name(expr.getName()));
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
      }.doPrettyPrint(this, parameters);
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
    }.doPrettyPrint(this);

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
    }.doPrettyPrint(this, expr.getFields());

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
    new BinOpLayout() {
      @Override
      void printLeft(PrettyPrintVisitor pp) {
        if (prec > expr.getReferent().getPrecedence().priority) pp.myBuilder.append('(');
        expr.getLeft().accept(pp, (byte) (expr.getReferent().getPrecedence().priority + (expr.getReferent().getPrecedence().associativity == Abstract.Precedence.Associativity.LEFT_ASSOC ? 0 : 1)));
      }

      @Override
      void printRight(PrettyPrintVisitor pp) {
        expr.getRight().accept(pp, (byte) (expr.getReferent().getPrecedence().priority + (expr.getReferent().getPrecedence().associativity == Abstract.Precedence.Associativity.RIGHT_ASSOC ? 0 : 1)));
        if (prec > expr.getReferent().getPrecedence().priority) pp.myBuilder.append(')');
      }

      @Override
      String getOpText() {
        return new Name(expr.getReferent().getName()).getInfixName();
      }

      @Override
      boolean increaseIndent(List<String> right_strings) {
        Abstract.Expression r = expr.getRight();
        if (r instanceof Abstract.BinOpExpression) {
          Abstract.Definition referable = ((Abstract.BinOpExpression) r).getReferent();
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
    if (expr.getSequence().isEmpty()) {
      expr.getLeft().accept(this, prec);
      return null;
    }
    if (prec > Abstract.BinOpSequenceExpression.PREC) myBuilder.append('(');
    expr.getLeft().accept(this, (byte) 10);
    for (Abstract.BinOpSequenceElem elem : expr.getSequence()) {
      myBuilder.append(' ').append(new Name(elem.binOp.getName()).getInfixName()).append(' ');
      elem.argument.accept(this, (byte) 10);
    }
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
      }.doPrettyPrint(this);
    } else {
      for (int i = 0; i < clause.getPatterns().size(); i++) {
        prettyPrintPattern(clause.getPatterns().get(i), Abstract.Pattern.PREC);
        if (i != clause.getPatterns().size() - 1) {
          myBuilder.append(", ");
        }
      }
    }

    myBuilder.append('\n');
  }

  private void prettyPrintClauses(List<? extends Abstract.Expression> expressions, List<? extends Abstract.FunctionClause> clauses) {
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
      myBuilder.append(" {\n");
      myIndent += INDENT;
      for (Abstract.FunctionClause clause : clauses) {
        prettyPrintFunctionClause(clause);
      }

      printIndent();
      myBuilder.append('}');
    }
  }

  @Override
  public Void visitCase(Abstract.CaseExpression expr, Byte prec) {
    if (prec > Abstract.CaseExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\case");
    prettyPrintClauses(expr.getExpressions(), expr.getClauses());
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
    for (Abstract.ClassFieldImpl statement : classFieldImpls) {
      printIndent();
      myBuilder.append("| ").append(new Name(statement.getImplementedFieldName()).getPrefixName()).append(" => ");
      statement.getImplementation().accept(this, Abstract.Expression.PREC);
      myBuilder.append("\n");
    }
    myIndent -= INDENT;
    printIndent();
    myBuilder.append("}");
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
      }.doPrettyPrint(this);
    } else {
      myBuilder.append("=>");
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

  public static void printIndent(StringBuilder builder, int indent) {
    for (int i = 0; i < indent; i++) {
      builder.append(' ');
    }
  }

  private void prettyPrintNameWithPrecedence(Abstract.Definition def) {
    Abstract.Precedence precedence = def.getPrecedence();
    if (precedence != null && !precedence.equals(Abstract.Precedence.DEFAULT)) {
      myBuilder.append("\\infix");
      if (precedence.associativity == Abstract.Precedence.Associativity.LEFT_ASSOC) myBuilder.append('l');
      if (precedence.associativity == Abstract.Precedence.Associativity.RIGHT_ASSOC) myBuilder.append('r');
      myBuilder.append(' ');
      myBuilder.append(precedence.priority);
      myBuilder.append(' ');
    }

    myBuilder.append(new Name(def.getName()).getPrefixName());
  }

  private void prettyPrintBody(Abstract.FunctionBody body) {
    if (body instanceof Abstract.TermFunctionBody) {
      myBuilder.append(" => ");
      ((Abstract.TermFunctionBody) body).getTerm().accept(this, Abstract.Expression.PREC);
    } else {
      Abstract.ElimFunctionBody elimFunctionBody = (Abstract.ElimFunctionBody) body;
      prettyPrintEliminatedReferences(elimFunctionBody.getEliminatedReferences());
      prettyPrintClauses(Collections.emptyList(), elimFunctionBody.getClauses());
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
        if (def.getBody() != null) {
          pp.prettyPrintBody(def.getBody());
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

    r.doPrettyPrint(this);

    if (!def.getGlobalDefinitions().isEmpty()) {
      myBuilder.append("\n");
      printIndent();
      visitWhere(def.getGlobalDefinitions());
    }
    return null;
  }

  @Override
  public Void visitClassField(Abstract.ClassField def, Void params) {
    myBuilder.append("\\field ");
    prettyPrintNameWithPrecedence(def);

    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      myBuilder.append(" : ");
      resultType.accept(new PrettyPrintVisitor(myBuilder, myIndent), Abstract.Expression.PREC);
    }

    return null;
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, Void ignored) {
    myBuilder.append("\\data ");
    prettyPrintNameWithPrecedence(def);

    List<? extends Abstract.TypeParameter> parameters = def.getParameters();
    if (parameters != null) {
      for (Abstract.TypeParameter parameter : parameters) {
        myBuilder.append(' ');
        prettyPrintParameter(parameter, Abstract.ReferenceExpression.PREC);
      }
    } else {
      myBuilder.append("{!error}");
    }

    Abstract.Expression universe = def.getUniverse();
    if (universe != null) {
      myBuilder.append(" : ");
      universe.accept(this, Abstract.Expression.PREC);
    }
    myIndent += INDENT;

    prettyPrintEliminatedReferences(def.getEliminatedReferences());

    for (Abstract.ConstructorClause clause : def.getConstructorClauses()) {
      if (clause.getPatterns() == null) {
        for (Abstract.Constructor constructor : clause.getConstructors()) {
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

  private void prettyPrintEliminatedReferences(List<? extends Abstract.ReferenceExpression> references) {
    if (references != null) {
      if (references.isEmpty()) {
        myBuilder.append(" \\with");
      } else {
        myBuilder.append(" => \\elim ");
        boolean first = true;
        for (Abstract.ReferenceExpression ref : references) {
          if (first) {
            first = false;
          } else {
            myBuilder.append(", ");
          }
          myBuilder.append(ref.getName());
        }
      }
    }
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
      if (!conPattern.getArguments().isEmpty() && prec > Abstract.Pattern.PREC && pattern.isExplicit()) myBuilder.append('(');

      myBuilder.append(new Name(conPattern.getConstructorName()).getPrefixName());
      for (Abstract.Pattern patternArg : conPattern.getArguments()) {
        myBuilder.append(' ');
        prettyPrintPattern(patternArg, (byte) (Abstract.Pattern.PREC + 1));
      }

      if (!conPattern.getArguments().isEmpty() && prec > Abstract.Pattern.PREC && pattern.isExplicit()) myBuilder.append(')');
    }

    if (!pattern.isExplicit()) {
      myBuilder.append("}");
    }
  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, Void ignored) {
    prettyPrintNameWithPrecedence(def);
    List<? extends Abstract.TypeParameter> arguments = def.getParameters();
    if (arguments == null) {
      myBuilder.append("{!error}");
    } else {
      for (Abstract.TypeParameter argument : arguments) {
        myBuilder.append(' ');
        prettyPrintParameter(argument, Abstract.ReferenceExpression.PREC);
      }
    }

    if (def.getEliminatedReferences() != null) {
      prettyPrintEliminatedReferences(def.getEliminatedReferences());
      prettyPrintClauses(Collections.emptyList(), def.getClauses());
    }
    return null;
  }

  private void visitWhere(Collection<? extends Abstract.Definition> definitions) {
    myBuilder.append("\\where {");
    myIndent += INDENT;
    for (Abstract.Definition definition : definitions) {
      myBuilder.append("\n");
      printIndent();
      definition.accept(this, null);
      myBuilder.append("\n");
    }
    myIndent -= INDENT;
    myBuilder.append("}");
  }

  private void prettyPrintClassDefinitionHeader(Abstract.ClassDefinition def) {
    myBuilder.append("\\class ").append(def.getName());
    prettyPrintParameters(def.getPolyParameters(), Abstract.ReferenceExpression.PREC);
    if (def.getSuperClasses() != null && !def.getSuperClasses().isEmpty()) {
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
    Collection<? extends Abstract.Definition> globalDefinitions = def.getGlobalDefinitions();
    Collection<? extends Abstract.Definition> instanceDefinitions = def.getInstanceDefinitions();

    if (fields != null && !fields.isEmpty() || implementations != null && !implementations.isEmpty() || instanceDefinitions != null && !instanceDefinitions.isEmpty()) {
      myBuilder.append(" {");
      myIndent += INDENT;

      if (fields != null) {
        for (Abstract.ClassField field : fields) {
          myBuilder.append('\n');
          printIndent();
          field.accept(this, null);
          myBuilder.append('\n');
        }
      }

      if (implementations != null) {
        for (Abstract.Implementation implementation : implementations) {
          myBuilder.append('\n');
          printIndent();
          implementation.accept(this, null);
          myBuilder.append('\n');
        }
      }

      if (instanceDefinitions != null) {
        for (Abstract.Definition definition : instanceDefinitions) {
          myBuilder.append('\n');
          printIndent();
          definition.accept(this, null);
          myBuilder.append('\n');
        }
      }

      myIndent -= INDENT;
      printIndent();
      myBuilder.append("}");
    }

    if (!globalDefinitions.isEmpty()) {
      myBuilder.append(" ");
      visitWhere(globalDefinitions);
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
        if (!field.getName().equals(field.getUnderlyingFieldName())) {
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

  public static abstract class ListLayout<T>{
    abstract void printListElement(PrettyPrintVisitor ppv, T t);

    abstract String getSeparator();

    public void doPrettyPrint(PrettyPrintVisitor pp, List<? extends T> l){
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
      pp.myIndent -= indent;
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
