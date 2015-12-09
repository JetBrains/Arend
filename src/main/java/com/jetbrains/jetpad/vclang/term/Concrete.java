package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionPrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.statement.visitor.AbstractStatementVisitor;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.definition.Condition.prettyPrintCondition;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.prettyPrintPattern;

public final class Concrete {
  private Concrete() {}

  public static class Position {
    public int line;
    public int column;

    public Position(int line, int column) {
      this.line = line;
      this.column = column + 1;
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

  public static class Identifier extends SourceNode implements Abstract.Identifier {
    private final Name myName;

    public Identifier(Position position, String name, Abstract.Definition.Fixity fixity) {
      super(position);
      myName = new Name(name, fixity);
    }

    @Override
    public Name getName() {
      return myName;
    }
  }

  public static abstract class Expression extends SourceNode implements Abstract.Expression {
    public Expression(Position position) {
      super(position);
    }

    @Override
    public void setWellTyped(List<com.jetbrains.jetpad.vclang.term.definition.Binding> context, com.jetbrains.jetpad.vclang.term.expr.Expression wellTyped) {
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      accept(new PrettyPrintVisitor(builder, new ArrayList<String>(), 0), Abstract.Expression.PREC);
      return builder.toString();
    }

    @Override
    public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
      accept(new PrettyPrintVisitor(builder, names, 0), prec);
    }
  }

  public static class Argument extends SourceNode implements Abstract.Argument {
    private boolean myExplicit;

    public Argument(Position position, boolean explicit) {
      super(position);
      myExplicit = explicit;
    }

    @Override
    public boolean getExplicit() {
      return myExplicit;
    }

    public void setExplicit(boolean explicit) {
      myExplicit = explicit;
    }

    @Override
    public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
      Utils.prettyPrintArgument(this, builder, names, prec, 0);
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
  }

  public static class ArgumentExpression implements Abstract.ArgumentExpression {
    private final Expression myExpression;
    private final boolean myExplicit;
    private final boolean myHidden;

    public ArgumentExpression(Expression expression, boolean explicit, boolean hidden) {
      myExpression = expression;
      myExplicit = explicit;
      myHidden = hidden;
    }

    @Override
    public Expression getExpression() {
      return myExpression;
    }

    @Override
    public boolean isExplicit() {
      return myExplicit;
    }

    @Override
    public boolean isHidden() {
      return myHidden;
    }

    @Override
    public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
      myExpression.prettyPrint(builder, names, prec);
    }
  }

  public static class AppExpression extends Expression implements Abstract.AppExpression {
    private final Expression myFunction;
    private final ArgumentExpression myArgument;

    public AppExpression(Position position, Expression function, ArgumentExpression argument) {
      super(position);
      myFunction = function;
      myArgument = argument;
    }

    @Override
    public Expression getFunction() {
      return myFunction;
    }

    @Override
    public ArgumentExpression getArgument() {
      return myArgument;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitApp(this, params);
    }
  }

  public static class BinOpSequenceExpression extends Expression implements Abstract.BinOpSequenceExpression {
    private Expression myLeft;
    private final List<Abstract.BinOpSequenceElem> mySequence;

    public BinOpSequenceExpression(Position position, Expression left, List<Abstract.BinOpSequenceElem> sequence) {
      super(position);
      myLeft = left;
      mySequence = sequence;
    }

    @Override
    public Expression getLeft() {
      return myLeft;
    }

    @Override
    public List<Abstract.BinOpSequenceElem> getSequence() {
      return mySequence;
    }

    public BinOpExpression makeBinOp(Abstract.Expression left, ResolvedName name, Abstract.DefCallExpression var, Abstract.Expression right) {
      assert left instanceof Expression && right instanceof Expression && var instanceof Expression;
      return new BinOpExpression(((Expression) var).getPosition(), (Expression) left, name, (Expression) right);
    }

    public Expression makeError(Abstract.SourceNode node) {
      return new Concrete.InferHoleExpression(((SourceNode) node).getPosition());
    }

    public void replace(Abstract.Expression expression) {
      assert expression instanceof Expression;
      myLeft = (Expression) expression;
      mySequence.clear();
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitBinOpSequence(this, params);
    }
  }

  public static class BinOpExpression extends Expression implements Abstract.BinOpExpression {
    private final ResolvedName myName;
    private final Expression myLeft;
    private final Expression myRight;

    public BinOpExpression(Position position, Expression left, ResolvedName name, Expression right) {
      super(position);
      myLeft = left;
      myName = name;
      myRight = right;
    }

    @Override
    public ResolvedName getResolvedBinOpName() {
      return myName;
    }

    @Override
    public Concrete.Expression getLeft() {
      return myLeft;
    }

    @Override
    public Concrete.Expression getRight() {
      return myRight;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitBinOp(this, params);
    }
  }

  public static class DefCallExpression extends Expression implements Abstract.DefCallExpression {
    private Expression myExpression;
    private Name myName;
    private ResolvedName myResolvedName;

    public DefCallExpression(Position position, Expression expression, Name name) {
      super(position);
      myExpression = expression;
      myName = name;
      myResolvedName = null;
    }

    public DefCallExpression(Position position, ResolvedName resolvedName) {
      super(position);
      myExpression = null;
      myName = resolvedName.name;
      myResolvedName = resolvedName;
    }

    public DefCallExpression(Position position, com.jetbrains.jetpad.vclang.term.definition.Definition definition) {
      super(position);
      myExpression = null;
      myName = definition.getName();
      myResolvedName = new ResolvedName(definition.getParentNamespace(), definition.getName());
    }

    @Override
    public Expression getExpression() {
      return myExpression;
    }

    @Override
    public ResolvedName getResolvedName() {
      return myResolvedName;
    }

    public void setResolvedName(ResolvedName name) {
      myResolvedName = name;
    }

    @Override
    public Name getName() {
      return myName;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitDefCall(this, params);
    }
  }

  public static class ClassExtExpression extends Expression implements Abstract.ClassExtExpression {
    private final Expression myBaseClassExpression;
    private final List<ImplementStatement> myDefinitions;

    public ClassExtExpression(Position position, Expression baseClassExpression, List<ImplementStatement> definitions) {
      super(position);
      myBaseClassExpression = baseClassExpression;
      myDefinitions = definitions;
    }

    @Override
    public Expression getBaseClassExpression() {
      return myBaseClassExpression;
    }

    @Override
    public List<ImplementStatement> getStatements() {
      return myDefinitions;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClassExt(this, params);
    }
  }

  public static class ImplementStatement extends SourceNode implements Abstract.ImplementStatement {
    private final Identifier myIdentifier;
    private final Expression myExpression;

    public ImplementStatement(Identifier identifier, Expression expression) {
      super(identifier.getPosition());
      myIdentifier = identifier;
      myExpression = expression;
    }

    @Override
    public Identifier getIdentifier() {
      return myIdentifier;
    }

    @Override
    public Expression getExpression() {
      return myExpression;
    }
  }

  public static class NewExpression extends Expression implements Abstract.NewExpression {
    private final Expression myExpression;

    public NewExpression(Position position, Expression expression) {
      super(position);
      myExpression = expression;
    }

    @Override
    public Expression getExpression() {
      return myExpression;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitNew(this, params);
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
    public Expression getBody() {
      return myBody;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitLam(this, params);
    }
  }

  public static class LetClause extends Binding implements Abstract.LetClause {
    private final List<Argument> myArguments;
    private final Expression myResultType;
    private final Abstract.Definition.Arrow myArrow;
    private final Expression myTerm;

    public LetClause(Position position, String name, List<Argument> arguments, Expression resultType, Abstract.Definition.Arrow arrow, Expression term) {
      super(position, name);
      myArguments = arguments;
      myResultType = resultType;
      myArrow = arrow;
      myTerm = term;
    }

    @Override
    public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
      Utils.prettyPrintLetClause(this, builder, names, 0);
    }

    @Override
    public Abstract.Definition.Arrow getArrow() {
      return myArrow;
    }

    @Override
    public Abstract.Expression getTerm() {
      return myTerm;
    }

    @Override
    public List<Argument> getArguments() {
      return myArguments;
    }

    @Override
    public Abstract.Expression getResultType() {
      return myResultType;
    }
  }

  public static class LetExpression extends Expression implements Abstract.LetExpression {
    private final List<LetClause> myClauses;
    private final Expression myExpression;

    public LetExpression(Position position, List<LetClause> clauses, Expression expression) {
      super(position);
      myClauses = clauses;
      myExpression = expression;
    }

    @Override
    public List<LetClause> getClauses() {
      return myClauses;
    }

    @Override
    public Expression getExpression() {
      return myExpression;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitLet(this, params);
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
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitSigma(this, params);
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
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitTuple(this, params);
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

  public static class IndexExpression extends Expression implements Abstract.IndexExpression {
    private final int myIndex;

    public IndexExpression(Position position, int index) {
      super(position);
      myIndex = index;
    }

    @Override
    public int getIndex() {
      return myIndex;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitIndex(this, params);
    }
  }

  public static class ProjExpression extends Expression implements Abstract.ProjExpression {
    private final Expression myExpression;
    private final int myField;

    public ProjExpression(Position position, Expression expression, int field) {
      super(position);
      myExpression = expression;
      myField = field;
    }

    @Override
    public Expression getExpression() {
      return myExpression;
    }

    @Override
    public int getField() {
      return myField;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitProj(this, params);
    }
  }

  public static abstract class ElimCaseExpression extends Expression implements Abstract.ElimCaseExpression {
    private final List<Expression> myExpressions;
    private final List<Clause> myClauses;

    public ElimCaseExpression(Position position, List<Expression> expressions, List<Clause> clauses) {
      super(position);
      myExpressions = expressions;
      myClauses = clauses;
    }

    @Override
    public List<Expression> getExpressions() {
      return myExpressions;
    }

    @Override
    public List<Clause> getClauses() {
      return myClauses;
    }
  }

  public static class ElimExpression extends ElimCaseExpression implements Abstract.ElimExpression {
    public ElimExpression(Position position, List<Expression> expressions, List<Clause> clauses) {
      super(position, expressions, clauses);
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitElim(this, params);
    }
  }

  public static class CaseExpression extends ElimCaseExpression implements Abstract.CaseExpression {
    public CaseExpression(Position position, List<Expression> expressions, List<Clause> clauses) {
      super(position, expressions, clauses);
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitCase(this, params);
    }
  }

  public static class PatternContainerHelper {
    public static void replacePatternWithConstructor(List<Pattern> patterns, int index) {
      Pattern pattern = patterns.get(index);
      patterns.set(index, new ConstructorPattern(pattern.getPosition(), new Name(pattern.getName()), new ArrayList<Pattern>(0)));
      patterns.get(index).setExplicit(pattern.getExplicit());
    }
  }

  public static class Clause extends SourceNode implements Abstract.Clause {
    private final List<Pattern> myPatterns;
    private final Definition.Arrow myArrow;
    private final Expression myExpression;

    public Clause(Position position, List<Pattern> patterns, Abstract.Definition.Arrow arrow, Expression expression) {
      super(position);
      myPatterns = patterns;
      myArrow = arrow;
      myExpression = expression;
    }

    @Override
    public List<Pattern> getPatterns() {
      return myPatterns;
    }

    @Override
    public Definition.Arrow getArrow() {
      return myArrow;
    }

    @Override
    public Expression getExpression() {
      return myExpression;
    }

    @Override
    public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
      Utils.prettyPrintClause(null, this, builder, names, 0);
    }

    @Override
    public void replacePatternWithConstructor(int index) {
      PatternContainerHelper.replacePatternWithConstructor(myPatterns, index);
    }
  }

  public static class NumericLiteral extends Expression implements Abstract.NumericLiteral {
    private final int myNumber;

    public NumericLiteral(Position position, int number) {
      super(position);
      myNumber = number;
    }

    @Override
    public int getNumber() {
      return myNumber;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitNumericLiteral(this, params);
    }
  }

  public static abstract class Binding extends SourceNode implements Abstract.Binding {
    private final Name myName;

    public Binding(Position position, Name name) {
      super(position);
      myName = name;
    }

    public Binding(Position position, String name) {
      super(position);
      myName = new Name(name, Abstract.Definition.Fixity.PREFIX);
    }

    @Override
    public Name getName() {
      return myName;
    }
  }

  public static abstract class Statement extends SourceNode implements Abstract.Statement {
    public Statement(Position position) {
      super(position);
    }
  }

  public static class DefineStatement extends Statement implements Abstract.DefineStatement {
    private final boolean myStatic;
    private final Definition myDefinition;
    private Definition myParent;

    public DefineStatement(Position position, boolean isStatic, Definition definition) {
      super(position);
      myStatic = isStatic;
      myDefinition = definition;
    }

    @Override
    public boolean isStatic() {
      return myStatic;
    }

    @Override
    public Definition getDefinition() {
      return myDefinition;
    }

    @Override
    public Definition getParentDefinition() {
      return myParent;
    }

    public void setParentDefinition(Definition parent) {
      myParent = parent;
    }

    @Override
    public <P, R> R accept(AbstractStatementVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitDefine(this, params);
    }
  }

  public static abstract class Definition extends Binding implements Abstract.Definition {
    private final Precedence myPrecedence;
    private DefineStatement myParent;

    public Definition(Position position, Name name, Precedence precedence) {
      super(position, name);
      myPrecedence = precedence;
    }

    @Override
    public Precedence getPrecedence() {
      return myPrecedence;
    }

    @Override
    public DefineStatement getParentStatement() {
      return myParent;
    }

    public void setParentStatement(DefineStatement parent) {
      myParent = parent;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      accept(new DefinitionPrettyPrintVisitor(builder, new ArrayList<String>(), 0), null);
      return builder.toString();
    }
  }

  public static abstract class SignatureDefinition extends Definition {
    private final List<Argument> myArguments;
    private final Expression myResultType;

    public SignatureDefinition(Position position, Name name, Precedence precedence, List<Argument> arguments, Expression resultType) {
      super(position, name, precedence);
      myArguments = arguments;
      myResultType = resultType;
    }

    public List<? extends Argument> getArguments() {
      return myArguments;
    }

    public Expression getResultType() {
      return myResultType;
    }
  }

  public static class AbstractDefinition extends SignatureDefinition implements Abstract.AbstractDefinition {
    public AbstractDefinition(Position position, Name name, Precedence precedence, List<Argument> arguments, Expression resultType) {
      super(position, name, precedence, arguments, resultType);
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitAbstract(this, params);
    }
  }

  public static class FunctionDefinition extends SignatureDefinition implements Abstract.FunctionDefinition {
    private final Abstract.Definition.Arrow myArrow;
    private final boolean myOverridden;
    private final Name myOriginalName;
    private final Expression myTerm;
    private final List<Statement> myStatements;

    public FunctionDefinition(Position position, Name name, Precedence precedence, List<Argument> arguments, Expression resultType, Abstract.Definition.Arrow arrow, Expression term, boolean overridden, Name originalName, List<Statement> statements) {
      super(position, name, precedence, arguments, resultType);
      myArrow = arrow;
      myTerm = term;
      myOverridden = overridden;
      myOriginalName = originalName;
      myStatements = statements;
    }

    @Override
    public boolean isAbstract() {
      return false;
    }

    @Override
    public Definition.Arrow getArrow() {
      return myArrow;
    }

    @Override
    public boolean isOverridden() {
      return myOverridden;
    }

    @Override
    public Name getOriginalName() {
      return myOriginalName;
    }

    @Override
    public List<Statement> getStatements() {
      return myStatements;
    }

    @Override
    public Expression getTerm() {
      return myTerm;
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitFunction(this, params);
    }
  }

  public static class Condition extends SourceNode implements Abstract.Condition {
    private final Name myConstructorName;
    private final List<Pattern> myPatterns;
    private final Expression myTerm;

    public Condition(Position position, Name constructorName, List<Pattern> patterns, Expression term) {
      super(position);
      myConstructorName = constructorName;
      myPatterns = patterns;
      myTerm = term;
    }

    @Override
    public Name getConstructorName() {
      return myConstructorName;
    }

    @Override
    public List<Pattern> getPatterns() {
      return myPatterns;
    }

    @Override
    public void replacePatternWithConstructor(int index) {
      PatternContainerHelper.replacePatternWithConstructor(myPatterns, index);
    }

    @Override
    public Expression getTerm() {
      return myTerm;
    }

    @Override
    public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
      prettyPrintCondition(this, builder, names);
    }
  }

  public static class DataDefinition extends Definition implements Abstract.DataDefinition {
    private final List<Constructor> myConstructors;
    private final List<TypeArgument> myParameters;
    private final List<Condition> myConditions;
    private final Universe myUniverse;

    public DataDefinition(Position position, Name name, Precedence precedence, List<TypeArgument> parameters, Universe universe, List<Concrete.Constructor> constructors, List<Condition> conditions) {
      super(position, name, precedence);
      myParameters = parameters;
      myConstructors = constructors;
      myConditions = conditions;
      myUniverse = universe;
    }

    @Override
    public List<TypeArgument> getParameters() {
      return myParameters;
    }

    @Override
    public List<Constructor> getConstructors() {
      return myConstructors;
    }

    @Override
    public List<? extends Abstract.Condition> getConditions() {
      return myConditions;
    }

    @Override
    public Universe getUniverse() {
      return myUniverse;
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitData(this, params);
    }
  }

  public static class ClassDefinition extends Definition implements Abstract.ClassDefinition {
    private final List<Statement> myFields;

    public ClassDefinition(Position position, String name, List<Statement> fields) {
      super(position, new Name(name, Fixity.PREFIX), DEFAULT_PRECEDENCE);
      myFields = fields;
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClass(this, params);
    }

    @Override
    public List<Statement> getStatements() {
      return myFields;
    }
  }

  public static abstract class Pattern extends SourceNode implements Abstract.Pattern {
    private boolean myExplicit;

    public Pattern(Position position) {
      super(position);
      myExplicit = true;
    }

    @Override
    public boolean getExplicit() {
      return myExplicit;
    }

    public void setExplicit(boolean isExplicit) {
      myExplicit = isExplicit;
    }

    @Override
    public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
      prettyPrintPattern(this, builder, names);
    }

    @Override
    public void setWellTyped(com.jetbrains.jetpad.vclang.term.pattern.Pattern pattern) {

    }

    public abstract String getName();
  }

  public static class NamePattern extends Pattern implements Abstract.NamePattern {
    private final String myName;
    public NamePattern(Position position, String name) {
      super(position);
      myName = name;
    }

    @Override
    public String getName() {
      return myName;
    }
  }

  public static class ConstructorPattern extends Pattern implements Abstract.ConstructorPattern {
    private final Name myConstructorName;
    private final List<Pattern> myArguments;

    public ConstructorPattern(Position position, Name constructorName, List<Pattern> arguments) {
      super(position);
      myConstructorName = constructorName;
      myArguments = arguments;
    }

    @Override
    public Name getConstructorName() {
      return myConstructorName;
    }

    @Override
    public List<Concrete.Pattern> getPatterns() {
      return myArguments;
    }

    @Override
    public void replacePatternWithConstructor(int index) {
      PatternContainerHelper.replacePatternWithConstructor(myArguments, index);
    }

    @Override
    public String getName() {
      return myConstructorName.name;
    }
  }

  public static class AnyConstructorPattern extends Pattern implements Abstract.AnyConstructorPattern {
    public AnyConstructorPattern(Position position) {
      super(position);
    }

    @Override
    public String getName() {
      return null;
    }
  }

  public static class Constructor extends Definition implements Abstract.Constructor {
    private final DataDefinition myDataType;
    private final List<TypeArgument> myArguments;
    private final List<Pattern> myPatterns;

    public Constructor(Position position, Name name, Precedence precedence, List<TypeArgument> arguments, DataDefinition dataType, List<Pattern> patterns) {
      super(position, name, precedence);
      myArguments = arguments;
      myDataType = dataType;
      myPatterns = patterns;
    }

    @Override
    public List<Pattern> getPatterns() {
      return myPatterns;
    }

    @Override
    public void replacePatternWithConstructor(int index) {
      PatternContainerHelper.replacePatternWithConstructor(myPatterns, index);
    }

    @Override
    public List<TypeArgument> getArguments() {
      return myArguments;
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

  public static class NamespaceCommandStatement extends Statement implements Abstract.NamespaceCommandStatement {
    private final Kind myKind;
    private ResolvedName myResolvedPath;
    private final List<Identifier> myPath;
    private final List<Identifier> myNames;

    public NamespaceCommandStatement(Position position, Kind kind, List<Identifier> path, List<Identifier> names) {
      super(position);
      myKind = kind;
      myResolvedPath = null;
      myPath = path;
      myNames = names;
    }

    @Override
    public Kind getKind() {
      return myKind;
    }

    @Override
    public List<Identifier> getPath() {
      return myPath;
    }

    @Override
    public void setResolvedPath(ResolvedName path) {
      myResolvedPath = path;
    }

    @Override
    public ResolvedName getResolvedPath() {
      return myResolvedPath;
    }

    @Override
    public List<Identifier> getNames() {
      return myNames;
    }

    @Override
    public <P, R> R accept(AbstractStatementVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitNamespaceCommand(this, params);
    }
  }
}
