package org.arend.term.prettyprint;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Constructor;
import org.arend.ext.concrete.expr.SigmaFieldKind;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.reference.Precedence;
import org.arend.ext.variable.Variable;
import org.arend.extImpl.AbstractedExpressionImpl;
import org.arend.naming.reference.*;
import org.arend.naming.renamer.MapReferableRenamer;
import org.arend.term.Fixity;
import org.arend.ext.concrete.definition.FunctionKind;
import org.arend.term.concrete.*;
import org.arend.term.concrete.Concrete.BinOpSequenceElem;
import org.arend.term.concrete.Concrete.Expression;
import org.arend.term.concrete.Concrete.ReferenceExpression;
import org.arend.util.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PrettyPrintVisitor implements ConcreteExpressionVisitor<Precedence, Void>, ConcreteLevelExpressionVisitor<Precedence, Void>, ConcreteResolvableDefinitionVisitor<Void, Void> {
  public static final int INDENT = 2;
  public static final int MAX_LEN = 120;
  public static final float SMALL_RATIO = (float) 0.1;

  protected final StringBuilder myBuilder;
  private Map<InferenceLevelVariable, Integer> myPVariables = Collections.emptyMap();
  private Map<InferenceLevelVariable, Integer> myHVariables = Collections.emptyMap();
  protected int myIndent;
  private final boolean noIndent;

  public PrettyPrintVisitor(StringBuilder builder, int indent, boolean doIndent) {
    myBuilder = builder;
    myIndent = indent;
    noIndent = !doIndent;
  }

  public PrettyPrintVisitor(StringBuilder builder, int indent) {
    this(builder, indent, true);
  }

  @Override
  public Void visitApp(Concrete.AppExpression expr, Precedence prec) {
    List<String> tail = null;

    Concrete.Expression it = expr;
    do {
      expr = (Concrete.AppExpression) it;
      TCDefReferable tcRef = expr.getFunction() instanceof Concrete.ReferenceExpression && ((ReferenceExpression) expr.getFunction()).getReferent() instanceof TCDefReferable ? (TCDefReferable) ((ReferenceExpression) expr.getFunction()).getReferent() : null;
      Constructor constructor = tcRef == null ? null : tcRef.getTypechecked() instanceof Constructor ? (Constructor) tcRef.getTypechecked() : null;
      if (constructor == null || constructor.getRecursiveParameter() < 0) {
        break;
      }

      List<Concrete.Argument> args = expr.getArguments();
      if (tcRef.getRepresentablePrecedence().isInfix && isInfix(args)) {
        int explicitIndex = -1;
        int i = 0;
        for (DependentLink link = constructor.getParameters(); link.hasNext(); link = link.getNext(), i++) {
          if (link.isExplicit()) {
            if (link.getNext().hasNext() && link.getNext().isExplicit()) {
              explicitIndex = i;
            }
            break;
          }
        }
        if (explicitIndex == -1 || explicitIndex != constructor.getRecursiveParameter() && explicitIndex + 1 != constructor.getRecursiveParameter()) {
          break;
        }

        StringBuilder tailBuilder = new StringBuilder();
        boolean leftParamIsRecursive = explicitIndex == constructor.getRecursiveParameter();
        prec = visitBinOp(leftParamIsRecursive ? null : args.get(args.size() - 2).getExpression(), (Concrete.ReferenceExpression) expr.getFunction(), args.subList(0, args.size() - 2), leftParamIsRecursive ? args.get(args.size() - 1).getExpression() : null, prec, tailBuilder);
        if (tailBuilder.length() != 0) {
          if (tail == null) {
            tail = new ArrayList<>();
          }
          tail.add(tailBuilder.toString());
        }
        it = args.get(args.size() - (leftParamIsRecursive ? 2 : 1)).getExpression();
      } else {
        int recursiveArg = 0;
        int recursiveParam = 0;
        for (DependentLink link = constructor.getParameters(); link.hasNext() && recursiveArg < args.size(); link = link.getNext(), recursiveArg++, recursiveParam++) {
          while (link.hasNext() && recursiveArg < args.size() && link.isExplicit() != args.get(recursiveArg).isExplicit()) {
            if (link.isExplicit()) {
              recursiveArg++;
            } else {
              link = link.getNext();
              recursiveParam++;
            }
          }
          if (recursiveParam == constructor.getRecursiveParameter()) {
            break;
          }
        }
        if (recursiveArg == args.size()) {
          break;
        }

        if (prec.priority > Concrete.AppExpression.PREC) myBuilder.append('(');
        expr.getFunction().accept(this, new Precedence(Concrete.AppExpression.PREC));
        for (int i = 0; i < recursiveArg; i++) {
          myBuilder.append(' ');
          printArgument(args.get(i));
        }
        myBuilder.append(' ');
        if (!args.get(recursiveArg).isExplicit()) {
          myBuilder.append('{');
        }
        if (!args.get(recursiveArg).isExplicit() || recursiveArg < args.size() - 1 || prec.priority > Concrete.AppExpression.PREC) {
          if (tail == null) {
            tail = new ArrayList<>();
          }
          PrettyPrintVisitor ppVisitor = new PrettyPrintVisitor(new StringBuilder(), myIndent, !noIndent);
          if (!args.get(recursiveArg).isExplicit()) {
            ppVisitor.printClosingBrace();
          }
          for (int i = recursiveArg + 1; i < args.size(); i++) {
            ppVisitor.myBuilder.append(' ');
            ppVisitor.printArgument(args.get(i));
          }
          if (prec.priority > Concrete.AppExpression.PREC) ppVisitor.myBuilder.append(')');
          tail.add(ppVisitor.myBuilder.toString());
        }
        it = args.get(recursiveArg).getExpression();
        prec = args.get(recursiveArg).isExplicit() ? new Precedence((byte) (Concrete.AppExpression.PREC + 1)) : new Precedence(Concrete.Expression.PREC);
      }
    } while (it instanceof Concrete.AppExpression);

    if (it instanceof Concrete.AppExpression) {
      visitAppImpl((Concrete.AppExpression) it, prec);
    } else {
      it.accept(this, prec);
    }

    if (tail != null) {
      for (int i = tail.size() - 1; i >= 0; i--) {
        myBuilder.append(tail.get(i));
      }
    }

    return null;
  }

  private void printArgument(Concrete.Argument arg) {
    if (arg.isExplicit()) {
      arg.getExpression().accept(this, new Precedence((byte) (Concrete.AppExpression.PREC + 1)));
    } else {
      myBuilder.append("{");
      arg.getExpression().accept(this, new Precedence(Concrete.Expression.PREC));
      printClosingBrace();
    }
  }

  private boolean isInfix(List<Concrete.Argument> args) {
    for (int i = 0; i < args.size(); i++) {
      if (args.get(i).isExplicit()) {
        return i == args.size() - 2 && args.get(i + 1).isExplicit();
      }
    }
    return false;
  }

  private void visitAppImpl(final Concrete.AppExpression expr, Precedence prec) {
    Concrete.Expression fun = expr.getFunction();
    List<Concrete.Argument> args = expr.getArguments();

    if (fun instanceof Concrete.ReferenceExpression && ((ReferenceExpression) fun).getReferent() instanceof GlobalReferable && ((GlobalReferable) ((ReferenceExpression) fun).getReferent()).getRepresentablePrecedence().isInfix && isInfix(args)) {
      visitBinOp(args.get(args.size() - 2).getExpression(), (ReferenceExpression) fun, args.subList(0, args.size() - 2), args.get(args.size() - 1).getExpression(), prec, null);
    } else {
      if (prec.priority > Concrete.AppExpression.PREC) myBuilder.append('(');
      new BinOpLayout() {
        @Override
        void printLeft(PrettyPrintVisitor pp) {
          fun.accept(pp, new Precedence(Concrete.AppExpression.PREC));
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
      if (prec.priority > Concrete.AppExpression.PREC) myBuilder.append(')');
    }
  }

  private void printReferenceName(Concrete.ReferenceExpression expr, Precedence prec) {
    Referable ref = expr.getReferent();
    if (ref instanceof CoreReferable && ((CoreReferable) ref).printExpression()) {
      ToAbstractVisitor.convert(((CoreReferable) ref).result.expression, PrettyPrinterConfig.DEFAULT).accept(this, prec == null ? new Precedence(ReferenceExpression.PREC) : prec);
    } else if (ref instanceof AbstractedReferable) {
      AbstractedReferable abs = (AbstractedReferable) ref;
      List<Binding> bindings = new ArrayList<>();
      org.arend.core.expr.Expression core = AbstractedExpressionImpl.getExpression(abs.expression, bindings);
      Map<Variable, Concrete.Expression> mapper = new HashMap<>();
      for (int i = 0; i < bindings.size() && i < abs.arguments.size(); i++) {
        if (abs.arguments.get(i) instanceof Concrete.Expression) {
          mapper.put(bindings.get(i), (Concrete.Expression) abs.arguments.get(i));
        }
      }
      ToAbstractVisitor.convert(core, PrettyPrinterConfig.DEFAULT, new MapReferableRenamer(mapper)).accept(this, prec == null ? new Precedence(ReferenceExpression.PREC) : prec);
    } else {
      String name = null;
      if (expr instanceof Concrete.LongReferenceExpression) {
        name = ((Concrete.LongReferenceExpression) expr).getLongName().toString();
      } else {
        if (ref instanceof GlobalReferable) {
          String alias = ((GlobalReferable) ref).getAliasName();
          if (alias != null) {
            name = alias;
          }
        }
        if (name == null) {
          name = ref.textRepresentation();
        }
      }
      if (!name.isEmpty() && name.charAt(0) == '-' && myBuilder.length() != 0 && myBuilder.charAt(myBuilder.length() - 1) == '{') {
        myBuilder.append(' ');
      }
      myBuilder.append(name);
    }
  }

  private void printLevels(List<Concrete.LevelExpression> levels) {
    if (levels != null) {
      if (levels.size() == 1) {
        levels.get(0).accept(this, new Precedence((byte) (Concrete.AppExpression.PREC + 1)));
      } else {
        myBuilder.append('(');
        boolean first = true;
        for (Concrete.LevelExpression level : levels) {
          if (first) {
            first = false;
          } else {
            myBuilder.append(", ");
          }
          level.accept(this, new Precedence(Expression.PREC));
        }
        myBuilder.append(')');
      }
    } else {
      myBuilder.append('_');
    }
  }

  private void visitReference(Concrete.ReferenceExpression expr, Precedence prec, boolean printLevelsKeyword) {
    boolean parens = expr.getReferent() instanceof GlobalReferable && ((GlobalReferable) expr.getReferent()).getRepresentablePrecedence().isInfix || ((expr.getPLevels() != null || expr.getHLevels() != null) && prec.priority > Concrete.AppExpression.PREC);
    if (parens) {
      myBuilder.append('(');
    }
    printReferenceName(expr, prec);

    if (expr.getPLevels() != null || expr.getHLevels() != null) {
      myBuilder.append(printLevelsKeyword ? " \\levels " : " ");
      printLevels(expr.getPLevels());
      if (printLevelsKeyword || expr.getHLevels() != null) {
        myBuilder.append(' ');
        printLevels(expr.getHLevels());
      }
    }
    if (parens) {
      myBuilder.append(')');
    }
  }

  @Override
  public Void visitReference(Concrete.ReferenceExpression expr, Precedence prec) {
    visitReference(expr, prec, true);
    return null;
  }

  @Override
  public Void visitThis(Concrete.ThisExpression expr, Precedence params) {
    myBuilder.append("\\this");
    return null;
  }

  public void prettyPrintParameters(List<? extends Concrete.Parameter> parameters) {
    if (parameters != null) {
      new ListLayout<Concrete.Parameter>(){
        @Override
        void printListElement(PrettyPrintVisitor ppv, Concrete.Parameter parameter) {
          ppv.prettyPrintParameter(parameter);
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

  public void prettyPrintParameter(Concrete.Parameter parameter) {
    if (parameter instanceof Concrete.NameParameter) {
      Referable referable = ((Concrete.NameParameter) parameter).getReferable();
      String name = referable == null ? null : referable.textRepresentation();
      if (name == null) {
        name = "_";
      }
      if (parameter.isExplicit()) {
        myBuilder.append(name);
      } else {
        myBuilder.append('{').append(name);
        printClosingBrace();
      }
    } else if (parameter instanceof Concrete.TelescopeParameter) {
      myBuilder.append(parameter.isExplicit() ? '(' : '{');
      if (parameter instanceof Concrete.SigmaTelescopeParameter) {
        SigmaFieldKind kind = ((Concrete.SigmaTelescopeParameter) parameter).getKind();
        myBuilder.append(getKindRepresentation(kind));
      }
      if (parameter.isStrict()) {
        myBuilder.append("\\strict ");
      }
      for (Referable referable : parameter.getReferableList()) {
        myBuilder.append(referable == null ? "_" : referable.textRepresentation()).append(' ');
      }

      myBuilder.append(": ");
      ((Concrete.TypeParameter) parameter).getType().accept(this, new Precedence(Concrete.Expression.PREC));
      if (parameter.isExplicit()) {
        myBuilder.append(')');
      } else {
        printClosingBrace();
      }
    } else {
      Concrete.Expression type = parameter.getType();
      if (type != null) {
        if (parameter.isExplicit()) {
          if (parameter.isStrict()) {
            myBuilder.append("(\\strict ");
          }
          if (parameter instanceof Concrete.SigmaTypeParameter && ((Concrete.SigmaTypeParameter) parameter).getKind() != SigmaFieldKind.ANY) {
            myBuilder.append("(").append(getKindRepresentation(((Concrete.SigmaTypeParameter) parameter).getKind()));
          }
          type.accept(this, new Precedence((byte) (ReferenceExpression.PREC + 1)));
          if (parameter.isStrict())
            myBuilder.append(")");
          }
          if (parameter instanceof Concrete.SigmaTypeParameter && ((Concrete.SigmaTypeParameter) parameter).getKind() != SigmaFieldKind.ANY) {
            myBuilder.append(")");
          }
        } else {
          myBuilder.append('{');
          if (parameter.isStrict()) {
            myBuilder.append("\\strict ");
          }
          type.accept(this, new Precedence(Concrete.Expression.PREC));
          printClosingBrace();
        }
      }
    }

  @NotNull
  private static String getKindRepresentation(SigmaFieldKind kind) {
    return kind == SigmaFieldKind.PROPERTY ? "\\property " : kind == SigmaFieldKind.FIELD ? "\\field " : "";
  }

  @Override
  public Void visitLam(final Concrete.LamExpression expr, Precedence prec) {
    if (prec.priority > Concrete.LamExpression.PREC) myBuilder.append("(");
    myBuilder.append("\\lam ");

    new BinOpLayout(){
      @Override
      void printLeft(PrettyPrintVisitor pp) {
        pp.prettyPrintParameters(expr.getParameters());
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
        if (expr.getParameters().size() == 1 && !(expr.getParameters().get(0) instanceof Concrete.TelescopeParameter)) {
          expr.getParameters().get(0).getType().accept(pp, new Precedence((byte) (Concrete.PiExpression.PREC + 1)));
          pp.myBuilder.append(' ');
        } else {
          pp.myBuilder.append("\\Pi ");
          for (Concrete.Parameter parameter : expr.getParameters()) {
            pp.prettyPrintParameter(parameter);
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
    myBuilder.append("\\oo");
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
  public Void visitId(Concrete.IdLevelExpression expr, Precedence param) {
    myBuilder.append(expr.getReferent().getRefName());
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

  public void prettyPrintLevelVar(LevelVariable variable) {
    myBuilder.append(variable);
    if (variable instanceof InferenceLevelVariable) {
      myBuilder.append(getVariableNumber((InferenceLevelVariable) variable));
    }
  }

  public String getInferLevelVarText(InferenceLevelVariable variable) {
    return variable.toString() + getVariableNumber(variable);
  }

  @Override
  public Void visitVar(Concrete.VarLevelExpression expr, Precedence param) {
    LevelVariable variable = expr.getVariable();
    prettyPrintLevelVar(variable);
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
      myBuilder.append("\\hType");
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
    myBuilder.append(expr.isErrorHole() ? "{?}" : "_");
    return null;
  }

  @Override
  public Void visitApplyHole(Concrete.ApplyHoleExpression expr, Precedence params) {
    myBuilder.append("__");
    return null;
  }

  @Override
  public Void visitGoal(Concrete.GoalExpression expr, Precedence prec) {
    myBuilder.append("{?");
    if (expr.getName() != null) {
      myBuilder.append(expr.getName());
    }
    printClosingBrace();
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
    myBuilder.append("\\Sigma");
    if (!expr.getParameters().isEmpty()) {
      myBuilder.append(' ');
    }

    prettyPrintParameters(expr.getParameters());

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
      if (elems.get(i).isPostfixReference()) {
        lhs = Concrete.AppExpression.make(elems.get(i).expression.getData(), elems.get(i).expression, lhs, true);
      } else if (elems.get(i).isInfixReference()) {
        break;
      } else {
        lhs = Concrete.AppExpression.make(lhs.getData(), lhs, elems.get(i).expression, elems.get(i).isExplicit);
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
              printClosingBrace();
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

  private Precedence visitBinOp(Concrete.Expression left, Concrete.ReferenceExpression infix, List<Concrete.Argument> implicitArgs, Concrete.Expression right, Precedence prec, StringBuilder builder) {
    Precedence infixPrec = ((GlobalReferable) infix.getReferent()).getRepresentablePrecedence();
    boolean needParens = prec.priority > infixPrec.priority || prec.priority == infixPrec.priority && (prec.associativity != infixPrec.associativity || prec.associativity == Precedence.Associativity.NON_ASSOC);
    if (needParens) myBuilder.append('(');
    PrettyPrintVisitor ppVisitor;
    Precedence leftPrec = infixPrec.associativity != Precedence.Associativity.LEFT_ASSOC ? new Precedence(Precedence.Associativity.NON_ASSOC, infixPrec.priority, infixPrec.isInfix) : infixPrec;
    if (left != null) {
      left.accept(this, leftPrec);
      ppVisitor = this;
    } else {
      ppVisitor = new PrettyPrintVisitor(builder, myIndent, !noIndent);
    }
    ppVisitor.myBuilder.append(' ');
    ppVisitor.printReferenceName(infix, null);
    for (Concrete.Argument arg : implicitArgs) {
      ppVisitor.myBuilder.append(" {");
      arg.expression.accept(ppVisitor, new Precedence(Expression.PREC));
      ppVisitor.printClosingBrace();
    }
    ppVisitor.myBuilder.append(' ');
    Precedence rightPrec = infixPrec.associativity != Precedence.Associativity.RIGHT_ASSOC ? new Precedence(Precedence.Associativity.NON_ASSOC, infixPrec.priority, infixPrec.isInfix) : infixPrec;
    if (right != null) {
      right.accept(ppVisitor, rightPrec);
      if (needParens) ppVisitor.myBuilder.append(')');
      return leftPrec;
    } else {
      if (needParens) builder.append(')');
      return rightPrec;
    }
  }

  public void prettyPrintFunctionClauses(Concrete.FunctionClauses clauses) {
    myBuilder.append("\\with {");
    if (!clauses.getClauseList().isEmpty()) {
      myIndent += INDENT;
      for (Concrete.FunctionClause clause : clauses.getClauseList()) {
        myBuilder.append("\n");
        prettyPrintFunctionClause(clause);
      }
      myBuilder.append("\n");
      myIndent -= INDENT;
    }
    myBuilder.append("}");
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
        prettyPrintPattern(clause.getPatterns().get(i), Concrete.Pattern.PREC, false);
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
    myBuilder.append(expr.isSCase() ? "\\scase " : "\\case ");
    new ListLayout<Concrete.CaseArgument>() {
      @Override
      void printListElement(PrettyPrintVisitor ppv, Concrete.CaseArgument caseArg) {
        caseArg.expression.accept(ppv, new Precedence(Concrete.Expression.PREC));
        if (caseArg.referable != null) {
          ppv.myBuilder.append(" \\as ").append(caseArg.referable.textRepresentation());
        }
        if (caseArg.type != null) {
          ppv.myBuilder.append(" : ");
          caseArg.type.accept(ppv, new Precedence(Expression.PREC));
        }
      }

      @Override
      String getSeparator() {
        return ", ";
      }
    }.doPrettyPrint(this, expr.getArguments(), noIndent);
    if (expr.getResultType() != null) {
      myBuilder.append(" \\return ");
      printTypeLevel(expr.getResultType(), expr.getResultTypeLevel());
    }
    myBuilder.append(" \\with");
    prettyPrintClauses(Collections.emptyList(), expr.getClauses(), true);
    myIndent -= INDENT;
    if (prec.priority > Concrete.CaseExpression.PREC) myBuilder.append(')');
    return null;
  }

  @Override
  public Void visitEval(Concrete.EvalExpression expr, Precedence prec) {
    if (prec.priority > Concrete.EvalExpression.PREC) myBuilder.append('(');
    myBuilder.append(expr.isPEval() ? "\\peval " : "\\eval ");
    expr.getExpression().accept(this, new Precedence(Concrete.Expression.PREC));
    if (prec.priority > Concrete.EvalExpression.PREC) myBuilder.append(')');
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
    myBuilder.append(" ");
    prettyprintCoclauses(expr.getCoclauses());
    if (prec.priority > Concrete.ClassExtExpression.PREC) myBuilder.append(')');
    return null;
  }

  public void prettyprintCoclauses(Concrete.Coclauses coclauses) {
    myBuilder.append("{");
    if (coclauses != null && !coclauses.getCoclauseList().isEmpty()) {
      myIndent += INDENT;
      for (Concrete.ClassFieldImpl classFieldImpl : coclauses.getCoclauseList()) {
        myBuilder.append("\n");
        printIndent();
        myBuilder.append("| ");
        prettyPrintClassFieldImpl(classFieldImpl);
      }
      myIndent -= INDENT;
      myBuilder.append("\n");
      printIndent();
    }
    myBuilder.append("}");
  }

  public void prettyPrintClassFieldImpl(Concrete.ClassFieldImpl classFieldImpl) {
    String name = classFieldImpl.getImplementedField() == null ? "_" : classFieldImpl.getImplementedField().textRepresentation();
    myBuilder.append(name);
    if (classFieldImpl.implementation == null) {
      myBuilder.append(" ");
      prettyprintCoclauses(classFieldImpl.getSubCoclauses());
    } else {
      myBuilder.append(" => ");
      classFieldImpl.implementation.accept(this, new Precedence(Expression.PREC));
    }
  }

  @Override
  public Void visitNew(Concrete.NewExpression expr, Precedence prec) {
    if (prec.priority > Concrete.NewExpression.PREC) myBuilder.append('(');
    myBuilder.append("\\new ");
    expr.getExpression().accept(this, new Precedence(Concrete.NewExpression.PREC));
    if (prec.priority > Concrete.NewExpression.PREC) myBuilder.append(')');
    return null;
  }

  public void prettyPrintLetClause(Concrete.LetClause letClause, boolean printPipe) {
    if (printPipe) {
      myBuilder.append("| ");
    }
    prettyPrintPattern(letClause.getPattern(), Concrete.Pattern.PREC, true);
    for (Concrete.Parameter arg : letClause.getParameters()) {
      myBuilder.append(" ");
      prettyPrintParameter(arg);
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
    String let = expr.isHave() ? (expr.isStrict() ? "\\have! " : "\\have ") : (expr.isStrict() ? "\\let! " : "\\let ");
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

  @Override
  public Void visitStringLiteral(Concrete.StringLiteral expr, Precedence params) {
    var unescapedString = expr.getUnescapedString();
    myBuilder.append("\"");
    StringEscapeUtils.escapeStringCharacters(unescapedString.length(), unescapedString, "\"", myBuilder);
    myBuilder.append("\"");
    return null;
  }

  @Override
  public Void visitTyped(Concrete.TypedExpression expr, Precedence prec) {
    if (prec.priority > Concrete.TypedExpression.PREC) myBuilder.append('(');
    expr.expression.accept(this, new Precedence(Concrete.TypedExpression.PREC));
    myBuilder.append(" : ");
    expr.type.accept(this, new Precedence(Concrete.TypedExpression.PREC));
    if (prec.priority > Concrete.TypedExpression.PREC) myBuilder.append(')');
    return null;
  }

  public void printIndent() {
    myBuilder.append(" ".repeat(Math.max(0, myIndent)));
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

  public void prettyPrintBody(Concrete.FunctionBody body, boolean isFunction) {
    if (body instanceof Concrete.TermFunctionBody) {
      myBuilder.append("=> ");
      ((Concrete.TermFunctionBody) body).getTerm().accept(this, new Precedence(Concrete.Expression.PREC));
    } else if (body instanceof Concrete.CoelimFunctionBody) {
      if (isFunction) {
        myBuilder.append("\\cowith");
      }

      myIndent += INDENT;
      for (Concrete.CoClauseElement element : body.getCoClauseElements()) {
        myBuilder.append("\n");
        printIndent();
        if (element instanceof Concrete.CoClauseFunctionReference) {
          TCReferable ref = ((Concrete.CoClauseFunctionReference) element).getFunctionReference();
          prettyPrintNameWithPrecedence(ref);
          myBuilder.append(" => ").append(ref.textRepresentation());
        } else if (element instanceof Concrete.ClassFieldImpl) {
          myBuilder.append("| ");
          prettyPrintClassFieldImpl((Concrete.ClassFieldImpl) element);
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
      myBuilder.append("\\level ");
      type.accept(this, prec);
      myBuilder.append(" ");
      typeLevel.accept(this, prec);
    } else {
      type.accept(this, new Precedence(Concrete.Expression.PREC));
    }
  }

  @Override
  public Void visitFunction(final Concrete.BaseFunctionDefinition def, Void ignored) {
    printIndent();
    switch (def.getKind()) {
      case FUNC: myBuilder.append("\\func "); break;
      case FUNC_COCLAUSE: myBuilder.append("| "); break;
      case CLASS_COCLAUSE: myBuilder.append("\\default "); break;
      case TYPE: myBuilder.append("\\type "); break;
      case LEMMA: myBuilder.append("\\lemma "); break;
      case LEVEL: myBuilder.append("\\use \\level "); break;
      case COERCE: myBuilder.append("\\use \\coerce "); break;
      case INSTANCE: myBuilder.append("\\instance "); break;
    }

    prettyPrintNameWithPrecedence(def.getData());

    if (def.getPLevelParameters() != null) {
      myBuilder.append(" ");
      prettyPrintLevelParameters(def.getPLevelParameters());
    }
    if (def.getHLevelParameters() != null) {
      myBuilder.append(" ");
      prettyPrintLevelParameters(def.getHLevelParameters());
    }

    myBuilder.append(" ");
    final BinOpLayout l = new BinOpLayout(){
      @Override
      void printLeft(PrettyPrintVisitor pp) {
        pp.prettyPrintParameters(def.getParameters());
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
        pp.prettyPrintBody(def.getBody(), !def.getKind().isCoclause() && def.getKind() != FunctionKind.INSTANCE && def.getKind() != FunctionKind.CONS);
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

    if (def.getPLevelParameters() != null) {
      myBuilder.append(" ");
      prettyPrintLevelParameters(def.getPLevelParameters());
    }
    if (def.getHLevelParameters() != null) {
      myBuilder.append(" ");
      prettyPrintLevelParameters(def.getHLevelParameters());
    }

    List<? extends Concrete.TypeParameter> parameters = def.getParameters();
    for (Concrete.TypeParameter parameter : parameters) {
      myBuilder.append(' ');
      prettyPrintParameter(parameter);
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
          prettyPrintConstructor(constructor);
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
              void printListElement(PrettyPrintVisitor ppv, Concrete.Constructor constructor) {
                ppv.prettyPrintConstructor(constructor);
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
      if (printWith) myBuilder.append("\\with");
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

  public void prettyPrintConstructorClause(Concrete.ConstructorClause clause) {
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
      prettyPrintConstructor(constructor);
    }
    if (clause.getConstructors().size() > 1) {
      myBuilder.append(" }");
    }
  }

  public void prettyPrintClause(Concrete.Clause clause) {
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
      prettyPrintPattern(pattern, Concrete.Pattern.PREC, false);
    }
  }

  public void prettyPrintPattern(Concrete.Pattern pattern, byte prec, boolean withParens) {
    if (!pattern.isExplicit()) {
      myBuilder.append("{");
    }

    if (pattern instanceof Concrete.NamePattern) {
      Concrete.NamePattern namePattern = (Concrete.NamePattern) pattern;
      Referable referable = namePattern.getReferable();
      String name = referable == null ? null : referable.textRepresentation();
      if (name == null) {
        name = "_";
      }
      myBuilder.append(name);
      if (namePattern.type != null && !name.equals("_")) {
        myBuilder.append(" : ");
        namePattern.type.accept(this, new Precedence(Expression.PREC));
      }
    } else if (pattern instanceof Concrete.NumberPattern) {
      myBuilder.append(((Concrete.NumberPattern) pattern).getNumber());
    } else if (pattern instanceof Concrete.TuplePattern) {
      myBuilder.append('(');
      boolean first = true;
      for (Concrete.Pattern arg : ((Concrete.TuplePattern) pattern).getPatterns()) {
        if (first) {
          first = false;
        } else {
          myBuilder.append(',');
        }
        prettyPrintPattern(arg, Concrete.Pattern.PREC, false);
      }
      myBuilder.append(')');
    } else if (pattern instanceof Concrete.ConstructorPattern) {
      Concrete.ConstructorPattern conPattern = (Concrete.ConstructorPattern) pattern;
      if ((withParens || !conPattern.getPatterns().isEmpty() && prec > Concrete.Pattern.PREC) && pattern.isExplicit()) myBuilder.append('(');

      myBuilder.append(conPattern.getConstructor().textRepresentation());
      for (Concrete.Pattern patternArg : conPattern.getPatterns()) {
        myBuilder.append(' ');
        prettyPrintPattern(patternArg, (byte) (Concrete.Pattern.PREC + 1), false);
      }

      if ((withParens || !conPattern.getPatterns().isEmpty() && prec > Concrete.Pattern.PREC) && pattern.isExplicit()) myBuilder.append(')');
    }

    if (pattern.getAsReferable() != null) {
      myBuilder.append(" \\as ");
      prettyPrintTypedReferable(pattern.getAsReferable());
    }

    if (!pattern.isExplicit()) {
      printClosingBrace();
    }
  }

  private void printClosingBrace() {
    if (myBuilder.length() != 0 && myBuilder.charAt(myBuilder.length() - 1) == '-') {
      myBuilder.append(' ');
    }
    myBuilder.append('}');
  }

  public void prettyPrintTypedReferable(Concrete.TypedReferable typedReferable) {
    myBuilder.append(typedReferable.referable.textRepresentation());
    if (typedReferable.type != null) {
      myBuilder.append(" : ");
      typedReferable.type.accept(this, new Precedence(Expression.PREC));
    }
  }

  public void prettyPrintConstructor(Concrete.Constructor def) {
    prettyPrintNameWithPrecedence(def.getData());
    for (Concrete.TypeParameter parameter : def.getParameters()) {
      myBuilder.append(' ');
      prettyPrintParameter(parameter);
    }

    if (def.getResultType() != null) {
      myBuilder.append(" : ");
      def.getResultType().accept(this, new Precedence(Expression.PREC));
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
        visitReference(superClass, new Precedence(Concrete.Expression.PREC), false);
      }
    }
  }

  public void prettyPrintClassField(Concrete.ClassField field) {
    switch (field.getKind()) {
      case FIELD: myBuilder.append("\\field "); break;
      case PROPERTY: myBuilder.append("\\property "); break;
      default: myBuilder.append("| ");
    }
    prettyPrintNameWithPrecedence(field.getData());
    if (!field.getParameters().isEmpty()) {
      myBuilder.append(" ");
      prettyPrintParameters(field.getParameters());
    }
    myBuilder.append(" : ");
    printTypeLevel(field.getResultType(), field.getResultTypeLevel());
  }

  public void prettyPrintOverridden(Concrete.OverriddenField field) {
    myBuilder.append("\\override ").append(field.getOverriddenField().textRepresentation());
    if (!field.getParameters().isEmpty()) {
      myBuilder.append(" ");
      prettyPrintParameters(field.getParameters());
    }
    myBuilder.append(" : ");
    printTypeLevel(field.getResultType(), field.getResultTypeLevel());
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Void ignored) {
    prettyPrintClassDefinitionHeader(def, def.getSuperClasses());

    if (def.getPLevelParameters() != null) {
      myBuilder.append(" ");
      prettyPrintLevelParameters(def.getPLevelParameters());
    }
    if (def.getHLevelParameters() != null) {
      myBuilder.append(" ");
      prettyPrintLevelParameters(def.getHLevelParameters());
    }

    if (!def.getElements().isEmpty()) {
      myBuilder.append(" {");
      myIndent += INDENT;

      for (Concrete.ClassElement element : def.getElements()) {
        myBuilder.append('\n');
        printIndent();
        if (element instanceof Concrete.ClassField) {
          prettyPrintClassField((Concrete.ClassField) element);
        } else if (element instanceof Concrete.ClassFieldImpl) {
          myBuilder.append("| ");
          prettyPrintClassFieldImpl((Concrete.ClassFieldImpl) element);
        } else if (element instanceof Concrete.OverriddenField) {
          prettyPrintOverridden((Concrete.OverriddenField) element);
        } else {
          throw new IllegalStateException();
        }
      }

      myIndent -= INDENT;
      myBuilder.append('\n');
      printIndent();
      myBuilder.append("}");
    }

    return null;
  }

  @Override
  public Void visitMeta(DefinableMetaDefinition def, Void params) {
    myBuilder.append("\\meta ");
    prettyPrintNameWithPrecedence(def.getData());
    for (var parameter : def.getParameters()) {
      myBuilder.append(" ").append(
        Objects.requireNonNull(parameter.getReferable()).textRepresentation());
    }
    if (def.body == null) {
      return null;
    }
    myBuilder.append(" => ");

    final BinOpLayout l = new BinOpLayout(){
      @Override
      void printLeft(PrettyPrintVisitor pp) {
        pp.prettyPrintParameters(def.getParameters());
      }

      @Override
      void printRight(PrettyPrintVisitor pp) {
      }

      @Override
      boolean printSpaceBefore() {
        return def.getParameters().size() > 0;
      }

      @Override
      String getOpText() {
        return "";
      }
    };

    final BinOpLayout r = new BinOpLayout(){
      @Override
      String getOpText() {
        return "";
      }

      @Override
      void printRight(PrettyPrintVisitor pp) {
        def.body.accept(pp, new Precedence(Concrete.Expression.PREC));
      }

      @Override
      void printLeft(PrettyPrintVisitor pp) {
        l.printLeft(pp);
      }

      @Override
      boolean printSpaceBefore() { return true; }

      @Override
      boolean printSpaceAfter() { return false; }
    };

    r.doPrettyPrint(this, noIndent);
    return null;
  }

  public void prettyPrintLevelParameters(Concrete.LevelParameters parameters) {
    List<LevelReferable> referables = parameters.referables;
    for (int i = 0; i < referables.size(); i++) {
      if (i > 0 && i < referables.size() - 1) {
        myBuilder.append(parameters.isIncreasing ? " <= " : " >= ");
      }
      myBuilder.append(referables.get(i).getRefName());
    }
  }

  static public void printArguments(PrettyPrintVisitor pp, List<Concrete.Argument> args, boolean noIndent) {
    new ListLayout<Concrete.Argument>() {
      @Override
      void printListElement(PrettyPrintVisitor ppv, Concrete.Argument arg) {
        ppv.printArgument(arg);
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
