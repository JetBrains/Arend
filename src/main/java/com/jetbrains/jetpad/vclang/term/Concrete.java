package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionPrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.prettyPrintArgument;

public final class Concrete {
  private Concrete() {}

  public static class Position {
    public int line;
    public int column;

    public Position(int line, int column) {
      this.line = line;
      this.column = column;
    }
  }

  public static class SourceNode implements Abstract.SourceNode {
    private final Position myPosition;

    public SourceNode(Position position) {
      myPosition = position;
    }

    public Position getPosition() {
      return myPosition;
    }
  }

  public static abstract class Expression extends SourceNode implements Abstract.Expression {
    public Expression(Position position) {
      super(position);
    }

    @Override
    public void setWellTyped(com.jetbrains.jetpad.vclang.term.expr.Expression wellTyped) {
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      accept(new PrettyPrintVisitor(builder, new ArrayList<String>(), 0), Abstract.Expression.PREC);
      return builder.toString();
    }
  }

  public static class Argument extends SourceNode implements Abstract.Argument {
    private final boolean myExplicit;

    public Argument(Position position, boolean explicit) {
      super(position);
      myExplicit = explicit;
    }

    @Override
    public boolean getExplicit() {
      return myExplicit;
    }

    @Override
    public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
      prettyPrintArgument(this, builder, names, prec, 0);
    }
  }

  public static class NameArgument extends Argument implements Abstract.NameArgument {
    private final String myName;

    public NameArgument(Position position, boolean explicit, String name) {
      super(position, explicit);
      myName = name;
    }

    @Override
    public String getName() {
      return myName;
    }
  }

  public static class TypeArgument extends Argument implements Abstract.TypeArgument {
    private final Expression myType;

    public TypeArgument(Position position, boolean explicit, Expression type) {
      super(position, explicit);
      myType = type;
    }

    public TypeArgument(boolean explicit, Expression type) {
      this(type.getPosition(), explicit, type);
    }

    @Override
    public Expression getType() {
      return myType;
    }
  }

  public static class TelescopeArgument extends TypeArgument implements Abstract.TelescopeArgument {
    private final List<String> myNames;

    public TelescopeArgument(Position position, boolean explicit, List<String> names, Expression type) {
      super(position, explicit, type);
      myNames = names;
    }

    @Override
    public List<String> getNames() {
      return myNames;
    }

    @Override
    public String getName(int index) {
      return myNames.get(index);
    }
  }

  public static class AppExpression extends Expression implements Abstract.AppExpression {
    private final Expression myFunction;
    private final Expression myArgument;
    private final boolean myExplicit;

    public AppExpression(Position position, Expression function, Expression argument, boolean isExplicit) {
      super(position);
      myFunction = function;
      myArgument = argument;
      myExplicit = isExplicit;
    }

    @Override
    public Expression getFunction() {
      return myFunction;
    }

    @Override
    public Expression getArgument() {
      return myArgument;
    }

    @Override
    public boolean isExplicit() {
      return myExplicit;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitApp(this, params);
    }
  }

  public static class BinOpExpression extends Expression implements Abstract.BinOpExpression {
    private final Expression myLeft;
    private final Expression myRight;
    private final Abstract.Definition myBinOp;

    public BinOpExpression(Position position, Expression left, Abstract.Definition binOp, Expression right) {
      super(position);
      myLeft = left;
      myRight = right;
      myBinOp = binOp;
    }

    @Override
    public Expression getLeft() {
      return myLeft;
    }

    @Override
    public Expression getRight() {
      return myRight;
    }

    @Override
    public Abstract.Definition getBinOp() {
      return myBinOp;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitBinOp(this, params);
    }
  }

  public static class DefCallExpression extends Expression implements Abstract.DefCallExpression {
    private final Abstract.Definition myDefinition;

    public DefCallExpression(Position position, Abstract.Definition definition) {
      super(position);
      myDefinition = definition;
    }

    @Override
    public Abstract.Definition getDefinition() {
      return myDefinition;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitDefCall(this, params);
    }
  }

  public static class ErrorExpression extends Expression implements Abstract.ErrorExpression {
    public ErrorExpression(Position position) {
      super(position);
    }

    @Override
    public Expression getExpr() {
      return null;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitError(this, params);
    }
  }

  public static class InferHoleExpression extends Expression implements Abstract.InferHoleExpression {
    public InferHoleExpression(Position position) {
      super(position);
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitInferHole(this, params);
    }
  }

  public static class LamExpression extends Expression implements Abstract.LamExpression {
    private final List<Argument> myArguments;
    private final Expression myBody;

    public LamExpression(Position position, List<Argument> arguments, Expression body) {
      super(position);
      myArguments = arguments;
      myBody = body;
    }

    @Override
    public List<Argument> getArguments() {
      return myArguments;
    }

    @Override
    public Argument getArgument(int index) {
      return myArguments.get(index);
    }

    @Override
    public Expression getBody() {
      return myBody;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitLam(this, params);
    }
  }

  public static class NatExpression extends Expression implements Abstract.NatExpression {
    public NatExpression(Position position) {
      super(position);
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitNat(this, params);
    }
  }

  public static class NelimExpression extends Expression implements Abstract.NelimExpression {
    public NelimExpression(Position position) {
      super(position);
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitNelim(this, params);
    }
  }

  public static class PiExpression extends Expression implements Abstract.PiExpression {
    private final List<TypeArgument> myArguments;
    private final Expression myCodomain;

    public PiExpression(Position position, List<TypeArgument> arguments, Expression codomain) {
      super(position);
      myArguments = arguments;
      myCodomain = codomain;
    }

    @Override
    public List<TypeArgument> getArguments() {
      return myArguments;
    }

    @Override
    public TypeArgument getArgument(int index) {
      return myArguments.get(index);
    }

    @Override
    public Expression getCodomain() {
      return myCodomain;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitPi(this, params);
    }
  }

  public static class SigmaExpression extends Expression implements Abstract.SigmaExpression {
    private final List<TypeArgument> myArguments;

    public SigmaExpression(Position position, List<TypeArgument> arguments) {
      super(position);
      myArguments = arguments;
    }

    @Override
    public List<TypeArgument> getArguments() {
      return myArguments;
    }

    @Override
    public TypeArgument getArgument(int index) {
      return myArguments.get(index);
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return null;
    }
  }

  public static class SucExpression extends Expression implements Abstract.SucExpression {
    public SucExpression(Position position) {
      super(position);
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitSuc(this, params);
    }
  }

  public static class TupleExpression extends Expression implements Abstract.TupleExpression {
    private final List<Expression> myFields;

    public TupleExpression(Position position, List<Expression> fields) {
      super(position);
      myFields = fields;
    }

    @Override
    public List<Expression> getFields() {
      return myFields;
    }

    @Override
    public Expression getField(int index) {
      return myFields.get(index);
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return null;
    }
  }

  public static class UniverseExpression extends Expression implements Abstract.UniverseExpression {
    private final Universe myUniverse;

    public UniverseExpression(Position position, Universe universe) {
      super(position);
      myUniverse = universe;
    }

    @Override
    public Universe getUniverse() {
      return myUniverse;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitUniverse(this, params);
    }
  }

  public static class VarExpression extends Expression implements Abstract.VarExpression {
    private final String myName;

    public VarExpression(Position position, String name) {
      super(position);
      myName = name;
    }

    @Override
    public String getName() {
      return myName;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitVar(this, params);
    }
  }

  public static class ZeroExpression extends Expression implements Abstract.ZeroExpression {
    public ZeroExpression(Position position) {
      super(position);
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitZero(this, params);
    }
  }

  public static class ElimExpression extends Expression implements Abstract.ElimExpression {
    private final ElimType myElimType;
    private final Expression myExpression;
    private final List<Clause> myClauses;

    public ElimExpression(Position position, ElimType elimType, Expression expression, List<Clause> clauses) {
      super(position);
      myElimType = elimType;
      myExpression = expression;
      myClauses = clauses;
    }

    @Override
    public ElimType getElimType() {
      return myElimType;
    }

    @Override
    public Expression getExpression() {
      return myExpression;
    }

    @Override
    public List<Clause> getClauses() {
      return myClauses;
    }

    @Override
    public Clause getClause(int index) {
      return myClauses.get(index);
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitElim(this, params);
    }
  }

  public static class Clause extends SourceNode implements Abstract.Clause {
    private final String myName;
    private final List<Argument> myArguments;
    private final Definition.Arrow myArrow;
    private final Expression myExpression;

    public Clause(Position position, String name, List<Argument> arguments, Abstract.Definition.Arrow arrow, Expression expression) {
      super(position);
      myName = name;
      myArguments = arguments;
      myArrow = arrow;
      myExpression = expression;
    }

    @Override
    public String getName() {
      return myName;
    }

    @Override
    public List<Argument> getArguments() {
      return myArguments;
    }

    @Override
    public Argument getArgument(int index) {
      return myArguments.get(index);
    }

    @Override
    public Definition.Arrow getArrow() {
      return myArrow;
    }

    @Override
    public Expression getExpression() {
      return myExpression;
    }
  }

  public static abstract class Binding extends SourceNode implements Abstract.Binding {
    private final String myName;

    public Binding(Position position, String name) {
      super(position);
      myName = name;
    }

    @Override
    public String getName() {
      return myName;
    }
  }

  public static abstract class Definition extends Binding implements Abstract.Definition {
    private final Precedence myPrecedence;
    private final Fixity myFixity;
    private final Universe myUniverse;

    public Definition(Position position, String name, Precedence precedence, Fixity fixity, Universe universe) {
      super(position, name);
      myPrecedence = precedence;
      myFixity = fixity;
      myUniverse = universe;
    }

    @Override
    public Precedence getPrecedence() {
      return myPrecedence;
    }

    @Override
    public Fixity getFixity() {
      return myFixity;
    }

    @Override
    public Universe getUniverse() {
      return myUniverse;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      accept(new DefinitionPrettyPrintVisitor(builder, new ArrayList<String>()), Abstract.Expression.PREC);
      return builder.toString();
    }
  }

  public static class FunctionDefinition extends Definition implements Abstract.FunctionDefinition {
    private final Expression myTerm;
    private final Abstract.Definition.Arrow myArrow;
    private final List<TelescopeArgument> myArguments;
    private final Expression myResultType;

    public FunctionDefinition(Position position, String name, Precedence precedence, Fixity fixity, List<TelescopeArgument> arguments, Expression resultType, Abstract.Definition.Arrow arrow, Expression term) {
      super(position, name, precedence, fixity, null);
      myArguments = arguments;
      myResultType = resultType;
      myArrow = arrow;
      myTerm = term;
    }

    @Override
    public Abstract.Definition.Arrow getArrow() {
      return myArrow;
    }

    @Override
    public Expression getTerm() {
      return myTerm;
    }

    @Override
    public List<TelescopeArgument> getArguments() {
      return myArguments;
    }

    @Override
    public TelescopeArgument getArgument(int index) {
      return myArguments.get(index);
    }

    @Override
    public Expression getResultType() {
      return myResultType;
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitFunction(this, params);
    }
  }

  public static class DataDefinition extends Definition implements Abstract.DataDefinition {
    private final List<Constructor> myConstructors;
    private final List<TypeArgument> myParameters;

    public DataDefinition(Position position, String name, Precedence precedence, Fixity fixity, Universe universe, List<TypeArgument> parameters, List<Constructor> constructors) {
      super(position, name, precedence, fixity, universe);
      myParameters = parameters;
      myConstructors = constructors;
    }

    @Override
    public List<TypeArgument> getParameters() {
      return myParameters;
    }

    @Override
    public TypeArgument getParameter(int index) {
      return myParameters.get(index);
    }

    @Override
    public List<Constructor> getConstructors() {
      return myConstructors;
    }

    @Override
    public Constructor getConstructor(int index) {
      return myConstructors.get(index);
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitData(this, params);
    }
  }

  public static class Constructor extends Definition implements Abstract.Constructor {
    private final DataDefinition myDataType;
    private final List<TypeArgument> myArguments;

    public Constructor(Position position, String name, Precedence precedence, Fixity fixity, Universe universe, List<TypeArgument> arguments, DataDefinition dataType) {
      super(position, name, precedence, fixity, universe);
      myArguments = arguments;
      myDataType = dataType;
    }

    @Override
    public List<? extends Abstract.TypeArgument> getArguments() {
      return myArguments;
    }

    @Override
    public Abstract.TypeArgument getArgument(int index) {
      return myArguments.get(index);
    }

    @Override
    public DataDefinition getDataType() {
      return myDataType;
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitConstructor(this, params);
    }
  }
}
