package org.arend.term.prettyprint;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.ext.reference.Precedence;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;
import org.arend.term.Fixity;
import org.arend.term.FunctionKind;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.Concrete.BinOpSequenceElem;
import org.arend.term.concrete.Concrete.Constructor;
import org.arend.term.concrete.Concrete.Expression;
import org.arend.term.concrete.Concrete.ReferenceExpression;
import org.arend.term.concrete.ConcreteDefinitionVisitor;
import org.arend.term.concrete.ConcreteExpressionVisitor;
import org.arend.term.concrete.ConcreteLevelExpressionVisitor;

import java.util.*;

public class PrettyPrintVisitor implements ConcreteExpressionVisitor<Precedence, Void>, ConcreteLevelExpressionVisitor<Precedence, Void>, ConcreteDefinitionVisitor<Void, Void> {
  public static final int INDENT = 4;
  public static final int MAX_LEN = 120;
  public static final float SMALL_RATIO = (float) 0.1;

  protected final TextBuilder<?> myBuilder;
  private Map<InferenceLevelVariable, Integer> myPVariables = Collections.emptyMap();
  private Map<InferenceLevelVariable, Integer> myHVariables = Collections.emptyMap();
  protected int myIndent;
  private final boolean noIndent;

  public PrettyPrintVisitor(StringBuilder builder, int indent, boolean doIndent) {
    this(new PlainTextBuilder(builder), indent, doIndent);
  }

  public PrettyPrintVisitor(TextBuilder<?> builder, int indent, boolean doIndent) {
    myBuilder = builder;
    myIndent = indent;
    noIndent = !doIndent;
  }

  public PrettyPrintVisitor(StringBuilder builder, int indent) {
    this(new PlainTextBuilder(builder), indent);
  }

  public PrettyPrintVisitor(TextBuilder<?> builder, int indent) {
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
      visitBinOp(args.get(args.size() - 2).getExpression(), (ReferenceExpression) fun, ((GlobalReferable) ((ReferenceExpression) fun).getReferent()).getPrecedence(), args.subList(0, args.size() - 2), args.get(args.size() - 1).getExpression(), prec);
    } else {
      long braceId = myBuilder.allocBraceId();
      if (prec.priority > Concrete.AppExpression.PREC) myBuilder.openingBrace('(', braceId);
      final Expression finalFun = fun;
      new BinOpLayout() {
        @Override
        void printLeft(PrettyPrintVisitor pp) {
          finalFun.accept(pp, new Precedence(Concrete.AppExpression.PREC));
        }

        @Override
        void printRight(PrettyPrintVisitor pp) {
          printArguments(pp, args, noIndent);
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
      if (prec.priority > Concrete.AppExpression.PREC) myBuilder.closingBrace(')', braceId);
    }

    return null;
  }

  @Override
  public Void visitReference(Concrete.ReferenceExpression expr, Precedence prec) {
    boolean parens = expr.getReferent() instanceof GlobalReferable && ((GlobalReferable) expr.getReferent()).getPrecedence().isInfix || expr.getPLevel() != null || expr.getHLevel() != null;
    long braceId = myBuilder.allocBraceId();
    if (parens) {
      myBuilder.openingBrace('(', braceId);
    }
    myBuilder.reference(expr.getReferent());

    if (expr.getPLevel() != null || expr.getHLevel() != null) {
      myBuilder.whitespace().keyword("\\level").whitespace();
      if (expr.getHLevel() instanceof Concrete.NumberLevelExpression && ((Concrete.NumberLevelExpression) expr.getHLevel()).getNumber() == -1) {
        myBuilder.keyword("\\Prop");
      } else {
        if (expr.getPLevel() != null) {
          expr.getPLevel().accept(this, new Precedence((byte) (Concrete.AppExpression.PREC + 1)));
        } else {
          myBuilder.underscore();
        }
        myBuilder.whitespace();
        if (expr.getHLevel() != null) {
          expr.getHLevel().accept(this, new Precedence((byte) (Concrete.AppExpression.PREC + 1)));
        } else {
          myBuilder.underscore();
        }
      }
    }
    if (parens) {
      myBuilder.closingBrace(')', braceId);
    }
    return null;
  }

  @Override
  public Void visitThis(Concrete.ThisExpression expr, Precedence params) {
    myBuilder.keyword("\\this");
    return null;
  }

  @Override
  public Void visitInferenceReference(Concrete.InferenceReferenceExpression expr, Precedence params) {
    myBuilder.goal("?").goal(expr.getVariable().getName());
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
      myBuilder.error("{!error}");
    }
  }

  private void prettyPrintParameter(Concrete.Parameter parameter, byte prec) {
    if (parameter instanceof Concrete.NameParameter) {
      Referable referable = ((Concrete.NameParameter) parameter).getReferable();
      if (parameter.isExplicit()) myBuilder.referenceNull(referable);
      else {
        long braceId = myBuilder.allocBraceId();
        myBuilder.openingBrace('{', braceId)
            .referenceNull(referable)
            .closingBrace('}', braceId);
      }
    } else
    if (parameter instanceof Concrete.TelescopeParameter) {
      long braceId = myBuilder.allocBraceId();
      myBuilder.openingBrace(parameter.isExplicit() ? '(' : '{', braceId);
      for (Referable referable : parameter.getReferableList()) {
        myBuilder.referenceNull(referable).whitespace();
      }

      myBuilder.colon().whitespace();
      ((Concrete.TypeParameter) parameter).getType().accept(this, new Precedence(Concrete.Expression.PREC));
      myBuilder.closingBrace(parameter.isExplicit() ? ')' : '}', braceId);
    } else
    if (parameter instanceof Concrete.TypeParameter) {
      Concrete.Expression type = ((Concrete.TypeParameter) parameter).getType();
      if (parameter.isExplicit()) {
        type.accept(this, new Precedence((byte) (ReferenceExpression.PREC + 1)));
      } else {
        long braceId = myBuilder.allocBraceId();
        myBuilder.openingBrace('{', braceId);
        type.accept(this, new Precedence(Concrete.Expression.PREC));
        myBuilder.closingBrace('}', braceId);
      }
    }
  }

  @Override
  public Void visitLam(final Concrete.LamExpression expr, Precedence prec) {
    long braceId = myBuilder.allocBraceId();
    if (prec.priority > Concrete.LamExpression.PREC) myBuilder.openingBrace('(', braceId);
    myBuilder.keyword("\\lam").whitespace();

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

    if (prec.priority > Concrete.LamExpression.PREC) myBuilder.closingBrace(')', braceId);
    return null;
  }

  @Override
  public Void visitPi(final Concrete.PiExpression expr, Precedence prec) {
    long braceId = myBuilder.allocBraceId();
    if (prec.priority > Concrete.PiExpression.PREC) myBuilder.openingBrace('(', braceId);

    new BinOpLayout(){
      @Override
      void printLeft(PrettyPrintVisitor pp) {
        byte domPrec = (byte) (expr.getParameters().size() > 1 ? Concrete.AppExpression.PREC + 1 : Concrete.PiExpression.PREC + 1);
        if (expr.getParameters().size() == 1 && !(expr.getParameters().get(0) instanceof Concrete.TelescopeParameter)) {
          expr.getParameters().get(0).getType().accept(pp, new Precedence((byte) (Concrete.PiExpression.PREC + 1)));
          pp.myBuilder.whitespace();
        } else {
          pp.myBuilder.keyword("\\Pi").whitespace();
          for (Concrete.Parameter parameter : expr.getParameters()) {
            pp.prettyPrintParameter(parameter, domPrec);
            pp.myBuilder.whitespace();
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

    if (prec.priority > Concrete.PiExpression.PREC) myBuilder.closingBrace(')', braceId);
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
    myBuilder.keyword("\\oo");
    return null;
  }

  @Override
  public Void visitLP(Concrete.PLevelExpression expr, Precedence param) {
    myBuilder.keyword("\\lp");
    return null;
  }

  @Override
  public Void visitLH(Concrete.HLevelExpression expr, Precedence param) {
    myBuilder.keyword("\\lh");
    return null;
  }

  @Override
  public Void visitNumber(Concrete.NumberLevelExpression expr, Precedence param) {
    myBuilder.integer(expr.getNumber());
    return null;
  }

  @Override
  public Void visitSuc(Concrete.SucLevelExpression expr, Precedence prec) {
    long braceId = myBuilder.allocBraceId();
    if (prec.priority > Concrete.AppExpression.PREC) myBuilder.openingBrace('(', braceId);
    myBuilder.keyword("\\suc").whitespace();
    expr.getExpression().accept(this, new Precedence((byte) (Concrete.AppExpression.PREC + 1)));
    if (prec.priority > Concrete.AppExpression.PREC) myBuilder.closingBrace(')', braceId);
    return null;
  }

  @Override
  public Void visitMax(Concrete.MaxLevelExpression expr, Precedence prec) {
    long braceId = myBuilder.allocBraceId();
    if (prec.priority > Concrete.AppExpression.PREC) myBuilder.openingBrace('(', braceId);
    myBuilder.keyword("\\max").whitespace();
    expr.getLeft().accept(this, new Precedence((byte) (Concrete.AppExpression.PREC + 1)));
    myBuilder.whitespace();
    expr.getRight().accept(this, new Precedence((byte) (Concrete.AppExpression.PREC + 1)));
    if (prec.priority > Concrete.AppExpression.PREC) myBuilder.closingBrace(')', braceId);
    return null;
  }

  public void prettyPrintInferLevelVar(InferenceLevelVariable variable) {
    myBuilder.goal(variable.toString())
        .goal(String.valueOf(getVariableNumber(variable)));
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
      myBuilder.keyword("\\Prop");
      return null;
    }

    boolean hParens = !(expr.getHLevel() instanceof Concrete.InfLevelExpression || expr.getHLevel() instanceof Concrete.NumberLevelExpression || expr.getHLevel() == null);
    boolean parens = prec.priority > Concrete.AppExpression.PREC && (hParens || !(expr.getPLevel() instanceof Concrete.NumberLevelExpression || expr.getPLevel() == null));
    long braceId = myBuilder.allocBraceId();
    if (parens) myBuilder.openingBrace('(', braceId);

    if (expr.getHLevel() instanceof Concrete.InfLevelExpression) {
      myBuilder.keyword("\\hType");
    } else
    if (expr.getHLevel() instanceof Concrete.NumberLevelExpression) {
      int hLevel = ((Concrete.NumberLevelExpression) expr.getHLevel()).getNumber();
      if (hLevel == 0) {
        myBuilder.keyword("\\Set");
      } else {
        myBuilder.keyword("\\").keyword(hLevel).keyword("-Type");
      }
    } else {
      myBuilder.keyword("\\Type");
    }

    if (expr.getPLevel() instanceof Concrete.NumberLevelExpression) {
      myBuilder.keyword(((Concrete.NumberLevelExpression) expr.getPLevel()).getNumber());
    } else if (expr.getPLevel() != null) {
      myBuilder.whitespace();
      expr.getPLevel().accept(this, new Precedence((byte) (Concrete.AppExpression.PREC + 1)));
    }

    if (hParens) {
      myBuilder.whitespace();
      expr.getHLevel().accept(this, new Precedence((byte) (Concrete.AppExpression.PREC + 1)));
    }

    if (parens) myBuilder.closingBrace(')', braceId);
    return null;
  }

  @Override
  public Void visitHole(Concrete.HoleExpression expr, Precedence prec) {
    if (expr.isErrorHole()) myBuilder.error("{?}");
    else myBuilder.underscore();
    return null;
  }

  @Override
  public Void visitGoal(Concrete.GoalExpression expr, Precedence prec) {
    myBuilder.goal("{?");
    if (expr.getName() != null) {
      myBuilder.goal(expr.getName());
    }
    myBuilder.goal("}");
    return null;
  }

  @Override
  public Void visitTuple(Concrete.TupleExpression expr, Precedence prec) {
    long braceId = myBuilder.allocBraceId();
    myBuilder.openingBrace('(', braceId);

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

    myBuilder.closingBrace(')', braceId);
    return null;
  }

  @Override
  public Void visitSigma(Concrete.SigmaExpression expr, Precedence prec) {
    long braceId = myBuilder.allocBraceId();
    if (prec.priority > Concrete.SigmaExpression.PREC) myBuilder.openingBrace('(', braceId);
    myBuilder.keyword("\\Sigma");
    if (!expr.getParameters().isEmpty()) {
      myBuilder.whitespace();
    }

    prettyPrintParameters(expr.getParameters(), (byte) (Concrete.AppExpression.PREC + 1));

    if (prec.priority > Concrete.SigmaExpression.PREC) myBuilder.closingBrace(')', braceId);
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
    long braceId = myBuilder.allocBraceId();
    if (prec.priority > Concrete.BinOpSequenceExpression.PREC) myBuilder.openingBrace('(', braceId);
    createBinOpLayout(expr.getSequence()).doPrettyPrint(this, noIndent);
    if (prec.priority > Concrete.BinOpSequenceExpression.PREC) myBuilder.closingBrace('(', braceId);
    return null;
  }

  private void visitBinOp(Concrete.Expression left, Concrete.ReferenceExpression infix, Precedence infixPrec, List<Concrete.Argument> implicitArgs, Concrete.Expression right, Precedence prec) {
    boolean needParens = prec.priority > infixPrec.priority || prec.priority == infixPrec.priority && (prec.associativity != infixPrec.associativity || prec.associativity == Precedence.Associativity.NON_ASSOC);
    long braceId = myBuilder.allocBraceId();
    if (needParens) myBuilder.openingBrace('(', braceId);
    left.accept(this, infixPrec.associativity != Precedence.Associativity.LEFT_ASSOC ? new Precedence(Precedence.Associativity.NON_ASSOC, infixPrec.priority, infixPrec.isInfix) : infixPrec);
    myBuilder.whitespace()
        .reference(infix.getReferent());
    for (Concrete.Argument arg : implicitArgs) {
      long braceIdArg = myBuilder.allocBraceId();
      myBuilder.whitespace().openingBrace('{', braceIdArg);
      arg.expression.accept(this, new Precedence(Expression.PREC));
      myBuilder.closingBrace('}', braceIdArg);
    }
    myBuilder.whitespace();
    right.accept(this, infixPrec.associativity != Precedence.Associativity.RIGHT_ASSOC ? new Precedence(Precedence.Associativity.NON_ASSOC, infixPrec.priority, infixPrec.isInfix) : infixPrec);
    if (needParens) myBuilder.closingBrace('(', braceId);
  }

  public void prettyPrintFunctionClause(final Concrete.FunctionClause clause) {
    prettyPrintFunctionClause(clause, myBuilder.allocBraceId());
  }

  private void prettyPrintFunctionClause(final Concrete.FunctionClause clause, long barId) {
    if (clause == null) return;

    printIndent();
    myBuilder.bar(barId).whitespace();

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
          myBuilder.comma().whitespace();
        }
      }
    }
  }

  private void prettyPrintClauses(List<? extends Concrete.Expression> expressions, List<? extends Concrete.FunctionClause> clauses, boolean needBraces) {
    if (!expressions.isEmpty()) {
      myBuilder.whitespace();
      for (int i = 0; i < expressions.size(); i++) {
        expressions.get(i).accept(this, new Precedence(Concrete.Expression.PREC));
        if (i != expressions.size() - 1) {
          myBuilder.comma().whitespace();
        }
      }
    }

    if (!clauses.isEmpty()) {
      long braceId = myBuilder.allocBraceId();
      if (needBraces) myBuilder.whitespace().openingBrace('{', braceId).eol();
      else myBuilder.eol();
      myIndent += INDENT;
      long barId = myBuilder.allocBraceId();
      for (int i=0; i<clauses.size(); i++) {
        prettyPrintFunctionClause(clauses.get(i), barId);
        if (i < clauses.size()-1) myBuilder.eol();
      }
      myIndent -= INDENT;

      if (needBraces) {
        myBuilder.eol();
        printIndent();
        myBuilder.closingBrace('}', braceId);
      }
    } else if (needBraces) {
      long braceId = myBuilder.allocBraceId();
      myBuilder.whitespace()
          .openingBrace('{', braceId)
          .closingBrace('}', braceId);
    }
  }

  @Override
  public Void visitCase(Concrete.CaseExpression expr, Precedence prec) {
    long braceId = myBuilder.allocBraceId();
    if (prec.priority > Concrete.CaseExpression.PREC) myBuilder.openingBrace('(', braceId);
    myBuilder.keyword("\\case").whitespace();
    new ListLayout<Concrete.CaseArgument>() {
      @Override
      void printListElement(PrettyPrintVisitor ppv, Concrete.CaseArgument caseArg) {
        caseArg.expression.accept(ppv, new Precedence(Concrete.Expression.PREC));
        if (caseArg.referable != null) {
          ppv.myBuilder.whitespace()
              .keyword("\\as")
              .whitespace()
              .reference(caseArg.referable);
        }
        if (caseArg.type != null) {
          ppv.myBuilder.wsColonWs();
          caseArg.type.accept(ppv, new Precedence(Expression.PREC));
        }
      }

      @Override
      String getSeparator() {
        return ", ";
      }
    }.doPrettyPrint(this, expr.getArguments(), noIndent);
    if (expr.getResultType() != null) {
      myBuilder.whitespace().keyword("\\return").whitespace();
      printTypeLevel(expr.getResultType(), expr.getResultTypeLevel());
    }
    myBuilder.whitespace().keyword(" \\with");
    prettyPrintClauses(Collections.emptyList(), expr.getClauses(), true);
    myIndent -= INDENT;
    if (prec.priority > Concrete.CaseExpression.PREC) myBuilder.closingBrace('(', braceId);
    return null;
  }

  @Override
  public Void visitEval(Concrete.EvalExpression expr, Precedence prec) {
    long braceId = myBuilder.allocBraceId();
    if (prec.priority > Concrete.EvalExpression.PREC) myBuilder.openingBrace('(', braceId);
    myBuilder.keyword(expr.isPEval() ? "\\peval" : "\\eval").whitespace();
    expr.getExpression().accept(this, new Precedence(Concrete.Expression.PREC));
    if (prec.priority > Concrete.EvalExpression.PREC) myBuilder.closingBrace('(', braceId);
    return null;
  }

  @Override
  public Void visitProj(Concrete.ProjExpression expr, Precedence prec) {
    long braceId = myBuilder.allocBraceId();
    if (prec.priority > Concrete.ProjExpression.PREC) myBuilder.openingBrace('(', braceId);
    expr.getExpression().accept(this, new Precedence(Concrete.ProjExpression.PREC));
    myBuilder.dot().integer(expr.getField() + 1);
    if (prec.priority > Concrete.ProjExpression.PREC) myBuilder.closingBrace('(', braceId);
    return null;
  }

  @Override
  public Void visitClassExt(Concrete.ClassExtExpression expr, Precedence prec) {
    long parenId = myBuilder.allocBraceId();
    long braceId = myBuilder.allocBraceId();
    if (prec.priority > Concrete.ClassExtExpression.PREC) myBuilder.openingBrace('(', parenId);
    expr.getBaseClassExpression().accept(this, new Precedence(Concrete.ClassExtExpression.PREC));
    myBuilder.whitespace().openingBrace('{', braceId);
    visitClassFieldImpls(expr.getStatements());
    myBuilder.eol();
    printIndent();
    myBuilder.closingBrace('}', braceId);
    if (prec.priority > Concrete.ClassExtExpression.PREC) myBuilder.closingBrace('(', parenId);
    return null;
  }

  private void visitClassFieldImpls(Collection<? extends Concrete.ClassFieldImpl> classFieldImpls) {
    myIndent += INDENT;
    long barId = myBuilder.allocBraceId();
    for (Concrete.ClassFieldImpl classFieldImpl : classFieldImpls) {
      myBuilder.eol();
      printIndent();
      myBuilder.bar(barId).whitespace();
      visitClassFieldImpl(classFieldImpl);
    }
    myIndent -= INDENT;
  }

  private void visitClassFieldImpl(Concrete.ClassFieldImpl classFieldImpl) {
    myBuilder.referenceNull(classFieldImpl.getImplementedField());
    if (classFieldImpl.implementation == null) {
      long braceId = myBuilder.allocBraceId();
      myBuilder.whitespace().openingBrace('{', braceId);
      visitClassFieldImpls(classFieldImpl.subClassFieldImpls);
      myBuilder.eol();
      printIndent();
      myBuilder.closingBrace('}', braceId);
    } else {
      myBuilder.whitespace().keyword("=>").whitespace();
      classFieldImpl.implementation.accept(this, new Precedence(Expression.PREC));
    }
  }

  @Override
  public Void visitNew(Concrete.NewExpression expr, Precedence prec) {
    long braceId = myBuilder.allocBraceId();
    if (prec.priority > Concrete.NewExpression.PREC) myBuilder.openingBrace('(', braceId);
    myBuilder.keyword("\\new").whitespace();
    expr.getExpression().accept(this, new Precedence(Concrete.NewExpression.PREC));
    if (prec.priority > Concrete.NewExpression.PREC) myBuilder.closingBrace('(', braceId);
    return null;
  }

  public void prettyPrintLetClausePattern(Concrete.LetClausePattern pattern) {
    if (pattern.getReferable() != null || pattern.isIgnored()) {
      if (pattern.isIgnored()) myBuilder.underscore();
      else myBuilder.reference(pattern.getReferable());
      if (pattern.type != null) {
        myBuilder.wsColonWs();
        pattern.type.accept(this, new Precedence(Concrete.Expression.PREC));
      }
    } else {
      long braceId = myBuilder.allocBraceId();
      myBuilder.openingBrace('(', braceId);
      boolean first = true;
      for (Concrete.LetClausePattern subPattern : pattern.getPatterns()) {
        if (first) {
          first = false;
        } else {
          myBuilder.comma().whitespace();
        }
        prettyPrintLetClausePattern(subPattern);
      }
      myBuilder.closingBrace('(', braceId);
    }
  }

  public void prettyPrintLetClause(Concrete.LetClause letClause, boolean printPipe) {
    prettyPrintLetClause(letClause, printPipe, 0);
  }

  private void prettyPrintLetClause(Concrete.LetClause letClause, boolean printPipe, long barId) {
    if (printPipe) {
      myBuilder.bar(barId).whitespace();
    }
    prettyPrintLetClausePattern(letClause.getPattern());
    for (Concrete.Parameter arg : letClause.getParameters()) {
      myBuilder.whitespace();
      prettyPrintParameter(arg, Concrete.LetExpression.PREC);
    }

    if (letClause.getResultType()!=null) {
      new BinOpLayout() {
        @Override
        void printLeft(PrettyPrintVisitor pp) {
          myBuilder.wsColonWs();
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
      myBuilder.whitespace().keyword("=>").whitespace();
      letClause.getTerm().accept(this, new Precedence(Concrete.LetExpression.PREC));
    }
  }

  @Override
  public Void visitLet(Concrete.LetExpression expr, Precedence prec) {
    long braceId = myBuilder.allocBraceId();
    if (prec.priority > Concrete.LetExpression.PREC) myBuilder.openingBrace('(', braceId);
    myBuilder.eol();
    myIndent += INDENT;
    printIndent();
    String let = "\\let";
    myBuilder.keyword(let).whitespace();

    final int INDENT0 = let.length() + 1;
    myIndent += INDENT0;
    long barId = myBuilder.allocBraceId();
    for (int i = 0; i < expr.getClauses().size(); ++i) {
      prettyPrintLetClause(expr.getClauses().get(i), expr.getClauses().size() > 1, barId);
      myBuilder.eol();
      if (i == expr.getClauses().size() - 1) {
        myIndent -= INDENT0;
      }
      printIndent();
    }

    String in = "\\in";
    myBuilder.keyword(in).whitespace();
    final int INDENT1 = in.length() + 1;
    myIndent += INDENT1;
    expr.getExpression().accept(this, new Precedence(Concrete.LetExpression.PREC));
    myIndent -= INDENT1;
    myIndent -= INDENT;

    if (prec.priority > Concrete.LetExpression.PREC) myBuilder.closingBrace('(', braceId);
    return null;
  }

  @Override
  public Void visitNumericLiteral(Concrete.NumericLiteral expr, Precedence params) {
    myBuilder.integer(expr.getNumber());
    return null;
  }

  @Override
  public Void visitTyped(Concrete.TypedExpression expr, Precedence prec) {
    long braceId = myBuilder.allocBraceId();
    if (prec.priority > Concrete.TypedExpression.PREC) myBuilder.openingBrace('(', braceId);
    expr.expression.accept(this, new Precedence(Concrete.TypedExpression.PREC));
    myBuilder.wsColonWs();
    expr.type.accept(this, new Precedence(Concrete.TypedExpression.PREC));
    if (prec.priority > Concrete.TypedExpression.PREC) myBuilder.closingBrace('(', braceId);
    return null;
  }

  public void printIndent() {
    myBuilder.whitespaces(myIndent);
  }

  private void prettyPrintNameWithPrecedence(GlobalReferable def) {
    Precedence precedence = def.getPrecedence();
    if (!precedence.equals(Precedence.DEFAULT)) {
      myBuilder.keyword("\\infix");
      if (precedence.associativity == Precedence.Associativity.LEFT_ASSOC) myBuilder.keyword('l');
      if (precedence.associativity == Precedence.Associativity.RIGHT_ASSOC) myBuilder.keyword('r');
      myBuilder.whitespace();
      myBuilder.integer(precedence.priority);
      myBuilder.whitespace();
    }

    myBuilder.reference(def);
  }

  private void prettyPrintBody(Concrete.FunctionBody body, boolean isFunction) {
    if (body instanceof Concrete.TermFunctionBody) {
      myBuilder.keyword("=>").whitespace();
      ((Concrete.TermFunctionBody) body).getTerm().accept(this, new Precedence(Concrete.Expression.PREC));
    } else if (body instanceof Concrete.CoelimFunctionBody) {
      if (isFunction) {
        myBuilder.keyword("\\cowith");
      }

      myIndent += INDENT;
      long barId = myBuilder.allocBraceId();
      for (Concrete.CoClauseElement element : body.getCoClauseElements()) {
        myBuilder.eol();
        printIndent();
        if (element instanceof Concrete.ClassFieldImpl) {
          myBuilder.bar(barId).whitespace();
          visitClassFieldImpl((Concrete.ClassFieldImpl) element);
        } else if (element instanceof Concrete.CoClauseFunctionReference) {
          TCReferable ref = ((Concrete.CoClauseFunctionReference) element).getFunctionReference();
          prettyPrintNameWithPrecedence(ref);
          myBuilder.whitespace().keyword("=>").whitespace().reference(ref);
        }
      }
      myIndent -= INDENT;
    } else {
      prettyPrintEliminatedReferences(body.getEliminatedReferences(), !isFunction);
      prettyPrintClauses(Collections.emptyList(), body.getClauses(), false);
    }
  }

  private void printTypeLevel(Concrete.Expression type, Concrete.Expression typeLevel) {
    if (typeLevel != null) {
      Precedence prec = new Precedence((byte) (Concrete.AppExpression.PREC + 1));
      myBuilder.keyword("\\level").whitespace();
      type.accept(this, prec);
      myBuilder.whitespace();
      typeLevel.accept(this, prec);
    } else {
      type.accept(this, new Precedence(Concrete.Expression.PREC));
    }
  }

  @Override
  public Void visitFunction(final Concrete.BaseFunctionDefinition def, Void ignored) {
    printIndent();
    switch (def.getKind()) {
      case FUNC: myBuilder.keyword("\\func").whitespace(); break;
      case COCLAUSE_FUNC: myBuilder.bar(0).whitespace(); break;
      case LEMMA: myBuilder.keyword("\\lemma").whitespace(); break;
      case LEVEL: myBuilder.keyword("\\use").whitespace().keyword("\\level").whitespace(); break;
      case COERCE: myBuilder.keyword("\\use").whitespace().keyword("\\coerce").whitespace(); break;
      case INSTANCE: myBuilder.keyword("\\instance").whitespace(); break;
    }

    prettyPrintNameWithPrecedence(def.getData());
    myBuilder.whitespace();

    final BinOpLayout l = new BinOpLayout(){
      @Override
      void printLeft(PrettyPrintVisitor pp) {
        pp.prettyPrintParameters(def.getParameters(), Concrete.ReferenceExpression.PREC);
      }

      @Override
      void printRight(PrettyPrintVisitor pp) {
        pp.printTypeLevel(def.getResultType(), def.getResultTypeLevel());
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
        pp.prettyPrintBody(def.getBody(), def.getKind() != FunctionKind.COCLAUSE_FUNC && def.getKind() != FunctionKind.INSTANCE);
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
    myBuilder.keyword("\\data").whitespace();
    prettyPrintNameWithPrecedence(def.getData());

    List<? extends Concrete.TypeParameter> parameters = def.getParameters();
    for (Concrete.TypeParameter parameter : parameters) {
      myBuilder.whitespace();
      prettyPrintParameter(parameter, Concrete.ReferenceExpression.PREC);
    }

    Concrete.Expression universe = def.getUniverse();
    if (universe != null) {
      myBuilder.wsColonWs();
      universe.accept(this, new Precedence(Concrete.Expression.PREC));
    }
    myIndent += INDENT;

    myBuilder.whitespace();
    prettyPrintEliminatedReferences(def.getEliminatedReferences(), true);

    long barId = myBuilder.allocBraceId();
    for (int i=0; i<def.getConstructorClauses().size(); i++) {
      Concrete.ConstructorClause clause = def.getConstructorClauses().get(i);
      if (clause.getPatterns() == null) {
        for (Concrete.Constructor constructor : clause.getConstructors()) {
          myBuilder.eol();
          printIndent();
          myBuilder.bar(barId).whitespace();
          visitConstructor(constructor);
        }
      } else {
        myBuilder.eol();
        printIndent();
        myBuilder.bar(barId).whitespace();
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

  private void prettyPrintEliminatedReferences(List<? extends Concrete.ReferenceExpression> references, boolean printWith) {
    if (references == null) {
      return;
    }
    if (references.isEmpty()) {
      if (printWith) myBuilder.keyword("\\with").eol();
      return;
    }

    myBuilder.keyword("\\elim").whitespace();
    new ListLayout<Concrete.ReferenceExpression>(){
      @Override
      void printListElement(PrettyPrintVisitor ppv, ReferenceExpression referenceExpression) {
        ppv.myBuilder.reference(referenceExpression.getReferent());
      }

      @Override
      String getSeparator() {
        return ", ";
      }
    }.doPrettyPrint(this, references, noIndent);
  }

  private void prettyPrintConstructorClause(Concrete.ConstructorClause clause) {
    printIndent();
    myBuilder.bar(0).whitespace();
    prettyPrintClause(clause);
    myBuilder.whitespace().keyword("=>").whitespace();

    long braceId = myBuilder.allocBraceId();
    if (clause.getConstructors().size() > 1) {
      myBuilder.openingBrace('{', braceId).whitespace();
    }
    boolean first = true;
    long barId = myBuilder.allocBraceId();
    for (Concrete.Constructor constructor : clause.getConstructors()) {
      if (first) {
        first = false;
      } else {
        myBuilder.whitespace().bar(barId).whitespace();
      }
      visitConstructor(constructor);
    }
    if (clause.getConstructors().size() > 1) {
      myBuilder.whitespace().closingBrace( '}', braceId);
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
        myBuilder.comma().whitespace();
      }
      prettyPrintPattern(pattern, Concrete.Pattern.PREC);
    }
  }

  public void prettyPrintPattern(Concrete.Pattern pattern, byte prec) {
    long braceId = myBuilder.allocBraceId();
    if (!pattern.isExplicit()) {
      myBuilder.openingBrace('{', braceId);
    }

    int numberOfAs = pattern.getAsReferables().size();
    long[] parenIds = new long[numberOfAs - 1];
    for (int i = 0; i < numberOfAs - 1; i++) {
      parenIds[i] = myBuilder.allocBraceId();
      myBuilder.openingBrace('(', parenIds[i]);
    }

    if (pattern instanceof Concrete.NamePattern) {
      Concrete.NamePattern namePattern = (Concrete.NamePattern) pattern;
      Referable referable = namePattern.getReferable();
      myBuilder.referenceNull(referable);
      String name = referable == null ? "_" : referable.textRepresentation();
      if (namePattern.type != null && !name.equals("_")) {
        myBuilder.wsColonWs();
        namePattern.type.accept(this, new Precedence(Expression.PREC));
      }
    } else if (pattern instanceof Concrete.NumberPattern) {
      myBuilder.integer(((Concrete.NumberPattern) pattern).getNumber());
    } else if (pattern instanceof Concrete.TuplePattern) {
      long tupleBraceId = myBuilder.allocBraceId();
      myBuilder.openingBrace('(', tupleBraceId);
      boolean first = true;
      for (Concrete.Pattern arg : ((Concrete.TuplePattern) pattern).getPatterns()) {
        if (first) {
          first = false;
        } else {
          myBuilder.comma();
        }
        prettyPrintPattern(arg, Concrete.Pattern.PREC);
      }
      myBuilder.closingBrace(')', tupleBraceId);
    } else if (pattern instanceof Concrete.ConstructorPattern) {
      Concrete.ConstructorPattern conPattern = (Concrete.ConstructorPattern) pattern;
      long parenId = myBuilder.allocBraceId();
      if (!conPattern.getPatterns().isEmpty() && prec > Concrete.Pattern.PREC && pattern.isExplicit()) myBuilder.openingBrace('(', parenId);

      myBuilder.reference(conPattern.getConstructor());
      for (Concrete.Pattern patternArg : conPattern.getPatterns()) {
        myBuilder.whitespace();
        prettyPrintPattern(patternArg, (byte) (Concrete.Pattern.PREC + 1));
      }

      if (!conPattern.getPatterns().isEmpty() && prec > Concrete.Pattern.PREC && pattern.isExplicit()) myBuilder.closingBrace(')', parenId);
    }

    int asPatIndex = numberOfAs;
    for (Concrete.TypedReferable typedReferable : pattern.getAsReferables()) {
      if (asPatIndex < numberOfAs)
        myBuilder.closingBrace(')', parenIds[asPatIndex]);
      asPatIndex--;

      myBuilder.whitespace().keyword("\\as").whitespace();
      prettyPrintTypedReferable(typedReferable);
    }

    if (!pattern.isExplicit()) {
      myBuilder.closingBrace('}', braceId);
    }
  }

  public void prettyPrintTypedReferable(Concrete.TypedReferable typedReferable) {
    myBuilder.reference(typedReferable.referable);
    if (typedReferable.type != null) {
      myBuilder.wsColonWs();
      typedReferable.type.accept(this, new Precedence(Expression.PREC));
    }
  }

  private void visitConstructor(Concrete.Constructor def) {
    prettyPrintNameWithPrecedence(def.getData());
    for (Concrete.TypeParameter parameter : def.getParameters()) {
      myBuilder.whitespace();
      prettyPrintParameter(parameter, Concrete.ReferenceExpression.PREC);
    }

    if (def.getResultType() != null) {
      myBuilder.wsColonWs();
      def.getResultType().accept(this, new Precedence(Expression.PREC));
    }

    if (!def.getEliminatedReferences().isEmpty() || !def.getClauses().isEmpty()) {
      myBuilder.whitespace();
      prettyPrintEliminatedReferences(def.getEliminatedReferences(), false);
      prettyPrintClauses(Collections.emptyList(), def.getClauses(), true);
    }
  }

  private void prettyPrintClassDefinitionHeader(Concrete.Definition def, List<Concrete.ReferenceExpression> superClasses) {
    myBuilder.keyword("\\class").whitespace().reference(def.getData());
    if (!superClasses.isEmpty()) {
      myBuilder.whitespace().keyword("\\extends").whitespace();
      boolean first = true;
      for (Concrete.ReferenceExpression superClass : superClasses) {
        if (first) {
          first = false;
        } else {
          myBuilder.comma().whitespace();
        }
        visitReference(superClass, new Precedence(Concrete.Expression.PREC));
      }
    }
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Void ignored) {
    prettyPrintClassDefinitionHeader(def, def.getSuperClasses());

    if (!def.getElements().isEmpty()) {
      long braceId = myBuilder.allocBraceId();
      myBuilder.whitespace().openingBrace('{', braceId);
      myIndent += INDENT;

      long barId = myBuilder.allocBraceId();
      for (Concrete.ClassElement element : def.getElements()) {
        myBuilder.eol();
        printIndent();
        if (element instanceof Concrete.ClassField) {
          Concrete.ClassField field = (Concrete.ClassField) element;
          switch (field.getKind()) {
            case FIELD: myBuilder.keyword("\\field"); break;
            case PROPERTY: myBuilder.keyword("\\property"); break;
            default: myBuilder.bar(barId);
          }
          myBuilder.whitespace();
          prettyPrintNameWithPrecedence(field.getData());
          if (!field.getParameters().isEmpty()) {
            myBuilder.whitespace();
            prettyPrintParameters(field.getParameters(), Concrete.ReferenceExpression.PREC);
          }
          myBuilder.wsColonWs();
          printTypeLevel(field.getResultType(), field.getResultTypeLevel());
        } else if (element instanceof Concrete.ClassFieldImpl) {
          myBuilder.bar(barId).whitespace();
          visitClassFieldImpl((Concrete.ClassFieldImpl) element);
        } else if (element instanceof Concrete.OverriddenField) {
          Concrete.OverriddenField field = (Concrete.OverriddenField) element;
          myBuilder
            .keyword("\\override").whitespace()
            .reference(field.getOverriddenField());
          if (!field.getParameters().isEmpty()) {
            myBuilder.whitespace();
            prettyPrintParameters(field.getParameters(), Concrete.ReferenceExpression.PREC);
          }
          myBuilder.wsColonWs();
          printTypeLevel(field.getResultType(), field.getResultTypeLevel());
        } else {
          throw new IllegalStateException();
        }
      }

      myIndent -= INDENT;
      myBuilder.eol();
      printIndent();
      myBuilder.closingBrace('}', braceId);
    }

    return null;
  }

  static public void printArguments(PrettyPrintVisitor pp, List<Concrete.Argument> args, boolean noIndent) {
    new ListLayout<Concrete.Argument>() {
      @Override
      void printListElement(PrettyPrintVisitor ppv, Concrete.Argument arg) {
        if (arg.isExplicit()) {
          arg.getExpression().accept(ppv, new Precedence((byte) (Concrete.AppExpression.PREC + 1)));
        } else {
          long braceId = ppv.myBuilder.allocBraceId();
          ppv.myBuilder.openingBrace('{', braceId);
          arg.getExpression().accept(ppv, new Precedence(Concrete.Expression.PREC));
          ppv.myBuilder.closingBrace('}', braceId);
        }
      }

      @Override
      String getSeparator() {
        return " ";
      }
    }.doPrettyPrint(pp, args, noIndent);
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
          pp.myBuilder.plainText(getSeparator());
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

          pp.myBuilder.plainText(separator.trim());
          if (rem + strs[0].length() + separator.length() > MAX_LEN || splitMultiLineArgs) {
            if (indent == 0) pp.myIndent += INDENT;
            indent = INDENT;
            pp.myBuilder.eol();
            rem = 0;
          } else {
            pp.myBuilder.whitespace();
            rem++;
          }
        }

        for (int i = 0; i < sz; i++) {
          String s = strs[i];
          if (rem == 0) pp.printIndent();
          pp.myBuilder.plainText(s);
          rem += s.trim().length();
          if (i < sz - 1) {
            pp.myBuilder.eol();
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
        if (printSpaceBefore()) ppv_default.myBuilder.whitespace();
        ppv_default.myBuilder.plainText(getOpText().trim());
        if (printSpaceAfter()) ppv_default.myBuilder.whitespace();
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
        if (i>0) ppv_default.printIndent(); ppv_default.myBuilder.plainText(s);
        if (i<lhs_sz-1) ppv_default.myBuilder.eol();
      }

      if (printSpaceBefore()) ppv_default.myBuilder.whitespace();
      ppv_default.myBuilder.plainText(getOpText().trim());

      if (hyph) {
        ppv_default.myBuilder.eol();
      } else {
        if (printSpaceAfter()) ppv_default.myBuilder.whitespace();
      }

      boolean ii = increaseIndent(rhs_strings);

      if (ii) ppv_default.myIndent+=INDENT;

      for (int i=0; i<rhs_sz; i++) {
        String s = rhs_strings.get(i);

        if (i>0 || hyph) {
          ppv_default.printIndent();
        }

        ppv_default.myBuilder.plainText(s);

        if (i<rhs_strings.size()-1) ppv_default.myBuilder.eol();
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
