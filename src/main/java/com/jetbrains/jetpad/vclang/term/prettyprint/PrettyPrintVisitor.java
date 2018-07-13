package com.jetbrains.jetpad.vclang.term.prettyprint;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Fixity;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete.BinOpSequenceElem;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete.Constructor;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete.Expression;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete.ReferenceExpression;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteLevelExpressionVisitor;

import java.util.*;

public class PrettyPrintVisitor implements ConcreteExpressionVisitor<Precedence, Void>, ConcreteLevelExpressionVisitor<Precedence, Void>, ConcreteDefinitionVisitor<Void, Void> {
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

  public static void prettyPrint(StringBuilder builder, Concrete.SourceNode node) {
    if (!new PrettyPrintVisitor(builder, 0).prettyPrint(node, Concrete.Expression.PREC)) {
      builder.append(node);
    }
  }

  public boolean prettyPrint(Concrete.SourceNode node, byte prec) {
    if (node instanceof Concrete.Expression) {
      ((Concrete.Expression) node).accept(this, new Precedence(prec));
      return true;
    }
    if (node instanceof Concrete.Parameter) {
      prettyPrintParameter((Concrete.Parameter) node, prec);
      return true;
    }
    if (node instanceof Concrete.Definition) {
      ((Concrete.Definition) node).accept(this, null);
      return true;
    }
    if (node instanceof Concrete.ClassFieldImpl) {
      visitClassFieldImpl((Concrete.ClassFieldImpl) node);
      return true;
    }
    if (node instanceof Concrete.FunctionClause) {
      prettyPrintFunctionClause((Concrete.FunctionClause) node);
      return true;
    }
    if (node instanceof Concrete.ConstructorClause) {
      prettyPrintConstructorClause((Concrete.ConstructorClause) node);
      return true;
    }
    if (node instanceof Concrete.Clause) {
      prettyPrintClause((Concrete.Clause) node);
      return true;
    }
    if (node instanceof Concrete.LetClause) {
      prettyPrintLetClause((Concrete.LetClause) node, false);
      return true;
    }
    if (node instanceof Concrete.Pattern) {
      prettyPrintPattern((Concrete.Pattern) node, prec);
      return true;
    }
    if (node instanceof Concrete.LevelExpression) {
      ((Concrete.LevelExpression) node).accept(this, new Precedence(Concrete.Expression.PREC));
      return true;
    }
    return false;
  }

  @Override
  public Void visitApp(final Concrete.AppExpression expr, Precedence prec) {
    if (prec.priority > Concrete.AppExpression.PREC) myBuilder.append('(');

    Concrete.Expression fun = expr.getFunction();
    List<Concrete.Argument> args = expr.getArguments();

    boolean infix = false;
    if (fun instanceof Concrete.ReferenceExpression && ((ReferenceExpression) fun).getReferent() instanceof GlobalReferable && ((GlobalReferable) ((ReferenceExpression) fun).getReferent()).getPrecedence().isInfix) {
      for (int i = 0; i < args.size(); i++) {
        if (args.get(i).isExplicit()) {
          infix = i == args.size() - 2 && args.get(i + 1).isExplicit();
          break;
        }
      }
    }

    if (infix) {
      List<Concrete.BinOpSequenceElem> sequence = new ArrayList<>();
      // print first explicit argument
      sequence.add(new Concrete.BinOpSequenceElem(args.get(args.size() - 2).getExpression(), Fixity.NONFIX, true));
      // print operation
      sequence.add(new Concrete.BinOpSequenceElem(fun, Fixity.UNKNOWN, true));

      // print implicit arguments
      for (int i = 0; i < args.size() - 2; i++) {
        sequence.add(new Concrete.BinOpSequenceElem(args.get(i).getExpression(), Fixity.NONFIX, false));
      }

      // print second explicit argument
      sequence.add(new Concrete.BinOpSequenceElem(args.get(args.size() - 1).getExpression(), Fixity.NONFIX, true));
      new Concrete.BinOpSequenceExpression(null, sequence).accept(this, new Precedence(Concrete.Expression.PREC));
    } else {
      final Expression finalFun = fun;
      new BinOpLayout() {
        @Override
        void printLeft(PrettyPrintVisitor pp) {
          finalFun.accept(pp, new Precedence(Concrete.AppExpression.PREC));
        }

        @Override
        void printRight(PrettyPrintVisitor pp) {
          new ListLayout<Concrete.Argument>() {
            @Override
            void printListElement(PrettyPrintVisitor ppv, Concrete.Argument arg) {
              if (arg.isExplicit()) {
                arg.getExpression().accept(ppv, new Precedence((byte) (Concrete.AppExpression.PREC + 1)));
              } else {
                ppv.myBuilder.append("{");
                arg.getExpression().accept(ppv, new Precedence(Concrete.Expression.PREC));
                ppv.myBuilder.append('}');
              }
            }

            @Override
            String getSeparator() {
              return " ";
            }
          }.doPrettyPrint(pp, args, noIndent);
        }

        @Override
        boolean printSpaceBefore() {
          return false;
        }

        @Override
        String getOpText() {
          return " ";
        }
      }.doPrettyPrint(this, noIndent);
    }

    if (prec.priority > Concrete.AppExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitReference(Concrete.ReferenceExpression expr, Precedence prec) {
    boolean parens = expr.getReferent() instanceof GlobalReferable && ((GlobalReferable) expr.getReferent()).getPrecedence().isInfix;
    if (parens) {
      myBuilder.append('(');
    }
    myBuilder.append(expr.getReferent().textRepresentation());

    if (expr.getPLevel() != null || expr.getHLevel() != null) {
      myBuilder.append(" \\levels ");
      if (expr.getHLevel() instanceof Concrete.NumberLevelExpression && ((Concrete.NumberLevelExpression) expr.getHLevel()).getNumber() == -1) {
        myBuilder.append("\\Prop");
      } else {
        if (expr.getPLevel() != null) {
          expr.getPLevel().accept(this, new Precedence(Expression.PREC));
        } else {
          myBuilder.append('_');
        }
        myBuilder.append(' ');
        if (expr.getHLevel() != null) {
          expr.getHLevel().accept(this, new Precedence(Expression.PREC));
        } else {
          myBuilder.append('_');
        }
      }
    }
    if (parens) {
      myBuilder.append(')');
    }
    return null;
  }

  @Override
  public Void visitInferenceReference(Concrete.InferenceReferenceExpression expr, Precedence params) {
    myBuilder.append("?").append(expr.getVariable().getName());
    return null;
  }

  public void prettyPrintParameters(List<? extends Concrete.Parameter> parameters, final byte prec) {
    if (parameters != null) {
      new ListLayout<Concrete.Parameter>(){
        @Override
        void printListElement(PrettyPrintVisitor ppv, Concrete.Parameter parameter) {
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

  private void prettyPrintParameter(Concrete.Parameter parameter, byte prec) {
    if (parameter instanceof Concrete.NameParameter) {
      Referable referable = ((Concrete.NameParameter) parameter).getReferable();
      String name = referable == null ? null : referable.textRepresentation();
      if (name == null) {
        name = "_";
      }
      myBuilder.append(parameter.getExplicit() ? name : '{' + name + '}');
    } else
    if (parameter instanceof Concrete.TelescopeParameter) {
      myBuilder.append(parameter.getExplicit() ? '(' : '{');
      for (Referable referable : ((Concrete.TelescopeParameter) parameter).getReferableList()) {
        myBuilder.append(referable == null ? "_" : referable.textRepresentation()).append(' ');
      }

      myBuilder.append(": ");
      ((Concrete.TypeParameter) parameter).getType().accept(this, new Precedence(Concrete.Expression.PREC));
      myBuilder.append(parameter.getExplicit() ? ')' : '}');
    } else
    if (parameter instanceof Concrete.TypeParameter) {
      Concrete.Expression type = ((Concrete.TypeParameter) parameter).getType();
      if (parameter.getExplicit()) {
        type.accept(this, new Precedence((byte) (ReferenceExpression.PREC + 1)));
      } else {
        myBuilder.append('{');
        type.accept(this, new Precedence(Concrete.Expression.PREC));
        myBuilder.append('}');
      }
    }
  }

  @Override
  public Void visitLam(final Concrete.LamExpression expr, Precedence prec) {
    if (prec.priority > Concrete.LamExpression.PREC) myBuilder.append("(");
    myBuilder.append("\\lam ");

    new BinOpLayout(){
      @Override
      void printLeft(PrettyPrintVisitor pp) {
        pp.prettyPrintParameters(expr.getParameters(), Concrete.Expression.PREC);
      }

      @Override
      void printRight(PrettyPrintVisitor pp) {
        expr.getBody().accept(pp, new Precedence(Concrete.LamExpression.PREC));
      }

      @Override
      String getOpText() {
        return "=>";
      }
    }.doPrettyPrint(this, noIndent);

    if (prec.priority > Concrete.LamExpression.PREC) myBuilder.append(")");
    return null;
  }

  @Override
  public Void visitPi(final Concrete.PiExpression expr, Precedence prec) {
    if (prec.priority > Concrete.PiExpression.PREC) myBuilder.append('(');

    new BinOpLayout(){
      @Override
      void printLeft(PrettyPrintVisitor pp) {
        byte domPrec = (byte) (expr.getParameters().size() > 1 ? Concrete.AppExpression.PREC + 1 : Concrete.PiExpression.PREC + 1);
        if (expr.getParameters().size() == 1 && !(expr.getParameters().get(0) instanceof Concrete.TelescopeParameter)) {
          expr.getParameters().get(0).getType().accept(pp, new Precedence((byte) (Concrete.PiExpression.PREC + 1)));
          pp.myBuilder.append(' ');
        } else {
          pp.myBuilder.append("\\Pi ");
          for (Concrete.Parameter parameter : expr.getParameters()) {
            pp.prettyPrintParameter(parameter, domPrec);
            pp.myBuilder.append(' ');
          }
        }
      }

      @Override
      void printRight(PrettyPrintVisitor ppv_right) {
        expr.getCodomain().accept(ppv_right, new Precedence(Concrete.PiExpression.PREC));
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

    if (prec.priority > Concrete.PiExpression.PREC) myBuilder.append(')');
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
  public Void visitInf(Concrete.InfLevelExpression expr, Precedence param) {
    myBuilder.append("\\inf");
    return null;
  }

  @Override
  public Void visitLP(Concrete.PLevelExpression expr, Precedence param) {
    myBuilder.append("\\lp");
    return null;
  }

  @Override
  public Void visitLH(Concrete.HLevelExpression expr, Precedence param) {
    myBuilder.append("\\lh");
    return null;
  }

  @Override
  public Void visitNumber(Concrete.NumberLevelExpression expr, Precedence param) {
    myBuilder.append(expr.getNumber());
    return null;
  }

  @Override
  public Void visitSuc(Concrete.SucLevelExpression expr, Precedence prec) {
    if (prec.priority > Concrete.AppExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\suc ");
    expr.getExpression().accept(this, new Precedence((byte) (Concrete.AppExpression.PREC + 1)));
    if (prec.priority > Concrete.AppExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitMax(Concrete.MaxLevelExpression expr, Precedence prec) {
    if (prec.priority > Concrete.AppExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\max ");
    expr.getLeft().accept(this, new Precedence((byte) (Concrete.AppExpression.PREC + 1)));
    myBuilder.append(" ");
    expr.getRight().accept(this, new Precedence((byte) (Concrete.AppExpression.PREC + 1)));
    if (prec.priority > Concrete.AppExpression.PREC) myBuilder.append(')');
    return null;
  }

  public void prettyPrintInferLevelVar(InferenceLevelVariable variable) {
    myBuilder.append(variable).append(getVariableNumber(variable));
  }

  public String getInferLevelVarText(InferenceLevelVariable variable) {
    return variable.toString() + getVariableNumber(variable);
  }

  @Override
  public Void visitVar(Concrete.InferVarLevelExpression expr, Precedence param) {
    InferenceLevelVariable variable = expr.getVariable();
    prettyPrintInferLevelVar(variable);
    return null;
  }

  @Override
  public Void visitUniverse(Concrete.UniverseExpression expr, Precedence prec) {
    if (expr.getHLevel() instanceof Concrete.NumberLevelExpression && ((Concrete.NumberLevelExpression) expr.getHLevel()).getNumber() == -1) {
      myBuilder.append("\\Prop");
      return null;
    }

    boolean hParens = !(expr.getHLevel() instanceof Concrete.InfLevelExpression || expr.getHLevel() instanceof Concrete.NumberLevelExpression || expr.getHLevel() == null);
    boolean parens = prec.priority > Concrete.AppExpression.PREC && (hParens || !(expr.getPLevel() instanceof Concrete.NumberLevelExpression || expr.getPLevel() == null));
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
      expr.getPLevel().accept(this, new Precedence((byte) (Concrete.AppExpression.PREC + 1)));
    }

    if (hParens) {
      myBuilder.append(" ");
      expr.getHLevel().accept(this, new Precedence((byte) (Concrete.AppExpression.PREC + 1)));
    }

    if (parens) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitHole(Concrete.HoleExpression expr, Precedence prec) {
    myBuilder.append(expr.getError() == null ? "_" : "{?}");
    return null;
  }

  @Override
  public Void visitGoal(Concrete.GoalExpression expr, Precedence prec) {
    myBuilder.append("{?");
    if (expr.getName() != null) {
      myBuilder.append(expr.getName());
    }
    myBuilder.append('}');
    return null;
  }

  @Override
  public Void visitTuple(Concrete.TupleExpression expr, Precedence prec) {
    myBuilder.append('(');

    new ListLayout<Concrete.Expression>(){
      @Override
      void printListElement(PrettyPrintVisitor ppv, Concrete.Expression o) {
        o.accept(ppv, new Precedence(Concrete.Expression.PREC));
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
  public Void visitSigma(Concrete.SigmaExpression expr, Precedence prec) {
    if (prec.priority > Concrete.SigmaExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\Sigma ");

    prettyPrintParameters(expr.getParameters(), (byte) (Concrete.AppExpression.PREC + 1));

    if (prec.priority > Concrete.SigmaExpression.PREC) myBuilder.append(')');
    return null;
  }

  private AbstractLayout createBinOpLayout(List<BinOpSequenceElem> elems) {
    Concrete.Expression lhs = elems.get(0).expression;
    if (lhs instanceof Concrete.AppExpression && elems.size() > 1) {
      lhs = Concrete.AppExpression.make(lhs.getData(), ((Concrete.AppExpression) lhs).getFunction(), new ArrayList<>(((Concrete.AppExpression) lhs).getArguments()));
    }

    int i = 1;
    for (; i < elems.size(); i++) {
      if (!elems.get(i).isReference() || !elems.get(i).isExplicit || elems.get(i).fixity == Fixity.NONFIX || elems.get(i).fixity == Fixity.UNKNOWN && !elems.get(i).isInfixReference()) {
        lhs = Concrete.AppExpression.make(lhs.getData(), lhs, elems.get(i).expression, elems.get(i).isExplicit);
      } else {
        break;
      }
    }

    if (i == elems.size()) {
      if (lhs != null) {
        final Expression finalLhs = lhs;
        return (ppv_default, disabled) -> finalLhs.accept(ppv_default, new Precedence((byte) 10));
      } else {
        return new EmptyLayout();
      }
    }

    // TODO[pretty]
    List<BinOpSequenceElem> ops = new ArrayList<>();
    for (; i < elems.size(); i++) {
      if (!elems.get(i).isExplicit || elems.get(i).fixity == Fixity.INFIX || elems.get(i).fixity == Fixity.POSTFIX || elems.get(i).isInfixReference()) {
        ops.add(elems.get(i));
      } else {
        break;
      }
    }

    final AbstractLayout layout = i == elems.size() ? null : createBinOpLayout(elems.subList(i, elems.size()));
    final Expression finalLhs = lhs;
    return new BinOpLayout(){
      @Override
      void printLeft(PrettyPrintVisitor pp) {
        if (finalLhs != null) finalLhs.accept(pp, new Precedence((byte) 10));
      }

      @Override
      void printRight(PrettyPrintVisitor pp) {
        if (layout != null) {
          layout.doPrettyPrint(pp, noIndent);
        }
      }

      @Override
      String getOpText() {
        StringBuilder builder = new StringBuilder();
        for (BinOpSequenceElem elem : ops) {
          if (elem.fixity == Fixity.INFIX || elem.fixity == Fixity.POSTFIX && elem.expression instanceof Concrete.ReferenceExpression) {
            builder.append('`').append(((ReferenceExpression) elem.expression).getReferent().textRepresentation());
            if (elem.fixity == Fixity.INFIX) {
              builder.append('`');
            }
          } else {
            if (!elem.isExplicit) {
              builder.append(" {");
            }
            if (elem.expression instanceof Concrete.ReferenceExpression) {
              builder.append(((ReferenceExpression) elem.expression).getReferent().textRepresentation());
            } else {
              elem.expression.accept(new PrettyPrintVisitor(builder, myIndent, !noIndent), new Precedence(Expression.PREC));
            }
            if (!elem.isExplicit) {
              builder.append('}');
            }
          }
        }
        return builder.toString();
      }
    };
  }

  @Override
  public Void visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Precedence prec) {
    if (expr.getSequence().size() == 1) {
      expr.getSequence().get(0).expression.accept(this, prec);
      return null;
    }
    if (prec.priority > Concrete.BinOpSequenceExpression.PREC) myBuilder.append('(');
    createBinOpLayout(expr.getSequence()).doPrettyPrint(this, noIndent);
    if (prec.priority > Concrete.BinOpSequenceExpression.PREC) myBuilder.append(')');
    return null;
  }

  public void prettyPrintFunctionClause(final Concrete.FunctionClause clause) {
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
          clause.getExpression().accept(pp, new Precedence(Concrete.Expression.PREC));
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
  }

  private void prettyPrintClauses(List<? extends Concrete.Expression> expressions, List<? extends Concrete.FunctionClause> clauses, boolean needBraces) {
    if (!expressions.isEmpty()) {
      myBuilder.append(" ");
      for (int i = 0; i < expressions.size(); i++) {
        expressions.get(i).accept(this, new Precedence(Concrete.Expression.PREC));
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
  public Void visitCase(Concrete.CaseExpression expr, Precedence prec) {
    if (prec.priority > Concrete.CaseExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\case ");
    new ListLayout<Concrete.Expression>(){
      @Override
      void printListElement(PrettyPrintVisitor ppv, Expression expression) {
        expression.accept(ppv, new Precedence(Concrete.Expression.PREC));
      }

      @Override
      String getSeparator() {
        return ", ";
      }
    }.doPrettyPrint(this, expr.getExpressions(), noIndent);
    myBuilder.append(" \\with");
    prettyPrintClauses(Collections.emptyList(), expr.getClauses(), true);
    myIndent -= INDENT;
    if (prec.priority > Concrete.CaseExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitProj(Concrete.ProjExpression expr, Precedence prec) {
    if (prec.priority > Concrete.ProjExpression.PREC) myBuilder.append('(');
    expr.getExpression().accept(this, new Precedence(Concrete.ProjExpression.PREC));
    myBuilder.append('.').append(expr.getField() + 1);
    if (prec.priority > Concrete.ProjExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitClassExt(Concrete.ClassExtExpression expr, Precedence prec) {
    if (prec.priority > Concrete.ClassExtExpression.PREC) myBuilder.append('(');
    expr.getBaseClassExpression().accept(this, new Precedence(Concrete.ClassExtExpression.PREC));
    myBuilder.append(" {");
    visitClassFieldImpls(expr.getStatements());
    myBuilder.append("\n");
    printIndent();
    myBuilder.append("}");
    if (prec.priority > Concrete.ClassExtExpression.PREC) myBuilder.append(')');
    return null;
  }

  private void visitClassFieldImpls(Collection<? extends Concrete.ClassFieldImpl> classFieldImpls) {
    myIndent += INDENT;
    for (Concrete.ClassFieldImpl classFieldImpl : classFieldImpls) {
      myBuilder.append("\n");
      printIndent();
      myBuilder.append("| ");
      visitClassFieldImpl(classFieldImpl);
    }
    myIndent -= INDENT;
  }

  private void visitClassFieldImpl(Concrete.ClassFieldImpl classFieldImpl) {
    String name = classFieldImpl.getImplementedField() == null ? "_" : classFieldImpl.getImplementedField().textRepresentation();
    myBuilder.append(name);
    if (classFieldImpl.implementation instanceof Concrete.NewExpression) {
      Concrete.Expression expr = ((Concrete.NewExpression) classFieldImpl.implementation).getExpression();
      if (expr instanceof Concrete.ClassExtExpression) {
        myBuilder.append(" {");
        visitClassFieldImpls(((Concrete.ClassExtExpression) expr).getStatements());
        myBuilder.append("\n");
        printIndent();
        myBuilder.append("}");
      }
    }

    myBuilder.append(" => ");
    classFieldImpl.implementation.accept(this, new Precedence(Expression.PREC));
  }

  @Override
  public Void visitNew(Concrete.NewExpression expr, Precedence prec) {
    if (prec.priority > Concrete.NewExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\new ");
    expr.getExpression().accept(this, new Precedence(Concrete.NewExpression.PREC));
    if (prec.priority > Concrete.NewExpression.PREC) myBuilder.append(')');
    return null;
  }

  public void prettyPrintLetClause(final Concrete.LetClause letClause, boolean printPipe) {
    if (printPipe) {
      myBuilder.append("| ");
    }
    myBuilder.append(letClause.getData().textRepresentation());
    for (Concrete.Parameter arg : letClause.getParameters()) {
      myBuilder.append(" ");
      prettyPrintParameter(arg, Concrete.LetExpression.PREC);
    }

    if (letClause.getResultType()!=null) {
      new BinOpLayout() {
        @Override
        void printLeft(PrettyPrintVisitor pp) {
          myBuilder.append(" : ");
          letClause.getResultType().accept(pp, new Precedence(Concrete.Expression.PREC));
        }

        @Override
        void printRight(PrettyPrintVisitor pp) {
          letClause.getTerm().accept(pp, new Precedence(Concrete.LetExpression.PREC));
        }

        @Override
        String getOpText() {
          return "=>";
        }
      }.doPrettyPrint(this, noIndent);
    } else {
      myBuilder.append(" => ");
      letClause.getTerm().accept(this, new Precedence(Concrete.LetExpression.PREC));
    }
  }

  @Override
  public Void visitLet(Concrete.LetExpression expr, Precedence prec) {
    if (prec.priority > Concrete.LetExpression.PREC) myBuilder.append('(');
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
    expr.getExpression().accept(this, new Precedence(Concrete.LetExpression.PREC));
    myIndent -= INDENT1;
    myIndent -= INDENT;

    if (prec.priority > Concrete.LetExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitNumericLiteral(Concrete.NumericLiteral expr, Precedence params) {
    myBuilder.append(expr.getNumber());
    return null;
  }

  public void printIndent() {
    for (int i = 0; i < myIndent; ++i) {
      myBuilder.append(' ');
    }
  }

  private void prettyPrintNameWithPrecedence(GlobalReferable def) {
    Precedence precedence = def.getPrecedence();
    if (!precedence.equals(Precedence.DEFAULT)) {
      myBuilder.append("\\infix");
      if (precedence.associativity == Precedence.Associativity.LEFT_ASSOC) myBuilder.append('l');
      if (precedence.associativity == Precedence.Associativity.RIGHT_ASSOC) myBuilder.append('r');
      myBuilder.append(' ');
      myBuilder.append(precedence.priority);
      myBuilder.append(' ');
    }

    myBuilder.append(def.textRepresentation());
  }

  private void prettyPrintBody(Concrete.FunctionBody body) {
    if (body instanceof Concrete.TermFunctionBody) {
      myBuilder.append("=> ");
      ((Concrete.TermFunctionBody) body).getTerm().accept(this, new Precedence(Concrete.Expression.PREC));
    } else {
      Concrete.ElimFunctionBody elimFunctionBody = (Concrete.ElimFunctionBody) body;
      prettyPrintEliminatedReferences(elimFunctionBody.getEliminatedReferences(), false);
      prettyPrintClauses(Collections.emptyList(), elimFunctionBody.getClauses(), false);
    }
  }

  @Override
  public Void visitFunction(final Concrete.FunctionDefinition def, Void ignored) {
    myBuilder.append("\\func ");
    printIndent();
    prettyPrintNameWithPrecedence(def.getData());
    myBuilder.append(" ");

    final BinOpLayout l = new BinOpLayout(){
      @Override
      void printLeft(PrettyPrintVisitor pp) {
        pp.prettyPrintParameters(def.getParameters(), Concrete.ReferenceExpression.PREC);
      }

      @Override
      void printRight(PrettyPrintVisitor pp) {
        //noinspection ConstantConditions
        def.getResultType().accept(pp, new Precedence(Concrete.Expression.PREC));
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
  public Void visitData(Concrete.DataDefinition def, Void ignored) {
    myBuilder.append("\\data ");
    prettyPrintNameWithPrecedence(def.getData());

    List<? extends Concrete.TypeParameter> parameters = def.getParameters();
    for (Concrete.TypeParameter parameter : parameters) {
      myBuilder.append(' ');
      prettyPrintParameter(parameter, Concrete.ReferenceExpression.PREC);
    }

    Concrete.Expression universe = def.getUniverse();
    if (universe != null) {
      myBuilder.append(" : ");
      universe.accept(this, new Precedence(Concrete.Expression.PREC));
    }
    myIndent += INDENT;

    myBuilder.append(' ');
    prettyPrintEliminatedReferences(def.getEliminatedReferences(), true);

    for (int i=0; i<def.getConstructorClauses().size(); i++) {
      Concrete.ConstructorClause clause = def.getConstructorClauses().get(i);
      if (clause.getPatterns() == null) {
        for (Concrete.Constructor constructor : clause.getConstructors()) {
          myBuilder.append('\n');
          printIndent();
          myBuilder.append("| ");
          visitConstructor(constructor);
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
            new ListLayout<Concrete.Constructor>(){
              @Override
              void printListElement(PrettyPrintVisitor ppv, Constructor constructor) {
                ppv.visitConstructor(constructor);
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

  private void prettyPrintEliminatedReferences(List<? extends Concrete.ReferenceExpression> references, boolean isData) {
    if (references == null) {
      return;
    }
    if (references.isEmpty()) {
      if (isData) myBuilder.append("\\with\n");
      return;
    }

    myBuilder.append("\\elim ");
    new ListLayout<Concrete.ReferenceExpression>(){
      @Override
      void printListElement(PrettyPrintVisitor ppv, ReferenceExpression referenceExpression) {
        ppv.myBuilder.append(referenceExpression.getReferent().textRepresentation());
      }

      @Override
      String getSeparator() {
        return ", ";
      }
    }.doPrettyPrint(this, references, noIndent);
  }

  private void prettyPrintConstructorClause(Concrete.ConstructorClause clause) {
    printIndent();
    myBuilder.append("| ");
    prettyPrintClause(clause);
    myBuilder.append(" => ");

    if (clause.getConstructors().size() > 1) {
      myBuilder.append("{ ");
    }
    boolean first = true;
    for (Concrete.Constructor constructor : clause.getConstructors()) {
      if (first) {
        first = false;
      } else {
        myBuilder.append(" | ");
      }
      visitConstructor(constructor);
    }
    if (clause.getConstructors().size() > 1) {
      myBuilder.append(" }");
    }
  }

  private void prettyPrintClause(Concrete.Clause clause) {
    if (clause.getPatterns() == null) {
      return;
    }
    boolean first = true;
    for (Concrete.Pattern pattern : clause.getPatterns()) {
      if (first) {
        first = false;
      } else {
        myBuilder.append(", ");
      }
      prettyPrintPattern(pattern, Concrete.Pattern.PREC);
    }
  }

  public void prettyPrintPattern(Concrete.Pattern pattern, byte prec) {
    if (!pattern.isExplicit()) {
      myBuilder.append("{");
    }

    if (pattern instanceof Concrete.NamePattern) {
      Referable referable = ((Concrete.NamePattern) pattern).getReferable();
      String name = referable == null ? null : referable.textRepresentation();
      if (name == null) {
        name = "_";
      }
      myBuilder.append(name);
    } else if (pattern instanceof Concrete.TuplePattern) {
      myBuilder.append('(');
      boolean first = true;
      for (Concrete.Pattern arg : ((Concrete.TuplePattern) pattern).getPatterns()) {
        if (first) {
          first = false;
        } else {
          myBuilder.append(',');
        }
        prettyPrintPattern(arg, Concrete.Pattern.PREC);
      }
      myBuilder.append(')');
    } else if (pattern instanceof Concrete.ConstructorPattern) {
      Concrete.ConstructorPattern conPattern = (Concrete.ConstructorPattern) pattern;
      if (!conPattern.getPatterns().isEmpty() && prec > Concrete.Pattern.PREC && pattern.isExplicit()) myBuilder.append('(');

      myBuilder.append(conPattern.getConstructor().textRepresentation());
      for (Concrete.Pattern patternArg : conPattern.getPatterns()) {
        myBuilder.append(' ');
        prettyPrintPattern(patternArg, (byte) (Concrete.Pattern.PREC + 1));
      }

      if (!conPattern.getPatterns().isEmpty() && prec > Concrete.Pattern.PREC && pattern.isExplicit()) myBuilder.append(')');
    }

    if (!pattern.isExplicit()) {
      myBuilder.append("}");
    }
  }

  private void visitConstructor(Concrete.Constructor def) {
    prettyPrintNameWithPrecedence(def.getData());
    for (Concrete.TypeParameter parameter : def.getParameters()) {
      myBuilder.append(' ');
      prettyPrintParameter(parameter, Concrete.ReferenceExpression.PREC);
    }

    if (!def.getEliminatedReferences().isEmpty() || !def.getClauses().isEmpty()) {
      myBuilder.append(' ');
      prettyPrintEliminatedReferences(def.getEliminatedReferences(), false);
      prettyPrintClauses(Collections.emptyList(), def.getClauses(), true);
    }
  }

  private void prettyPrintClassDefinitionHeader(Concrete.Definition def, List<Concrete.ReferenceExpression> superClasses) {
    myBuilder.append("\\class ").append(def.getData().textRepresentation());
    if (!superClasses.isEmpty()) {
      myBuilder.append(" \\extends ");
      boolean first = true;
      for (Concrete.ReferenceExpression superClass : superClasses) {
        if (first) {
          first = false;
        } else {
          myBuilder.append(", ");
        }
        visitReference(superClass, new Precedence(Concrete.Expression.PREC));
      }
    }
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Void ignored) {
    prettyPrintClassDefinitionHeader(def, def.getSuperClasses());

    Collection<? extends Concrete.ClassField> fields = def.getFields();
    Collection<? extends Concrete.ClassFieldImpl> implementations = def.getImplementations();

    if (!fields.isEmpty() || !implementations.isEmpty()) {
      myBuilder.append(" {");
      myIndent += INDENT;

      if (!fields.isEmpty()) {
        myBuilder.append('\n');
        for (Concrete.ClassField field : fields) {
          printIndent();
          myBuilder.append("| ");
          prettyPrintNameWithPrecedence(field.getData());
          myBuilder.append(" : ");
          field.getResultType().accept(this, new Precedence(Concrete.Expression.PREC));
          myBuilder.append('\n');
        }
      }

      myIndent -= INDENT;
      if (!implementations.isEmpty()) {
        visitClassFieldImpls(implementations);
        myBuilder.append('\n');
      }

      printIndent();
      myBuilder.append("}");
    }

    return null;
  }

  @Override
  public Void visitInstance(Concrete.Instance def, Void params) {
    myBuilder.append("\\instance ");
    prettyPrintNameWithPrecedence(def.getData());
    if (!def.getParameters().isEmpty()) {
      myBuilder.append(" ");
      prettyPrintParameters(def.getParameters(), Concrete.ReferenceExpression.PREC);
    }

    visitClassFieldImpls(def.getClassFieldImpls());
    return null;
  }

  public interface AbstractLayout {
    void doPrettyPrint(PrettyPrintVisitor ppv_default, boolean disabled);
  }

  public static class EmptyLayout implements AbstractLayout {
    public void doPrettyPrint(PrettyPrintVisitor ppv_default, boolean disabled) {}
  }

  public static abstract class ListLayout<E> {
    abstract void printListElement(PrettyPrintVisitor ppv, E e);

    abstract String getSeparator();

    public void doPrettyPrint(PrettyPrintVisitor pp, List<? extends E> l, boolean disabled){
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
        PrettyPrintVisitor ppv = new PrettyPrintVisitor(sb, 0, !pp.noIndent);
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
      PrettyPrintVisitor ppv_left = new PrettyPrintVisitor(lhs, 0, !ppv_default.noIndent);
      PrettyPrintVisitor ppv_right = new PrettyPrintVisitor(rhs, 0, !ppv_default.noIndent);

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
  */
}
