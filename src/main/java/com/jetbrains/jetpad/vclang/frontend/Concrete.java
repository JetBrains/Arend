package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.AbstractStatementVisitor;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;

import java.util.Collections;
import java.util.List;

public final class Concrete {
  private Concrete() {}

  public static class Position {
    public final SourceId module;
    public final int line;
    public final int column;

    public Position(SourceId module, int line, int column) {
      this.module = module;
      this.line = line;
      this.column = column + 1;
    }

    @Override
    public String toString() {
      return line + ":" + column;
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

  // Arguments

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

  // Expressions

  public static abstract class Expression extends SourceNode implements Abstract.Expression {
    public Expression(Position position) {
      super(position);
    }

    @Override
    public void setWellTyped(List<Binding> context, com.jetbrains.jetpad.vclang.core.expr.Expression wellTyped) {
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      accept(new PrettyPrintVisitor(builder, 0), Abstract.Expression.PREC);
      return builder.toString();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof Expression)) return false;
      return accept(new AbstractCompareVisitor(), (Expression) obj);
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

    public BinOpExpression makeBinOp(Abstract.Expression left, Abstract.Definition binOp, Abstract.DefCallExpression var, Abstract.Expression right) {
      assert left instanceof Expression && right instanceof Expression && var instanceof Expression;
      return new BinOpExpression(((Expression) var).getPosition(), (Expression) left, binOp, (Expression) right);
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
    private final Abstract.Definition myBinOp;
    private final Expression myLeft;
    private final Expression myRight;

    public BinOpExpression(Position position, Expression left, Abstract.Definition binOp, Expression right) {
      super(position);
      myLeft = left;
      myBinOp = binOp;
      myRight = right;
    }

    @Override
    public String getName() {
      return myBinOp.getName();
    }

    @Override
    public Abstract.Expression getExpression() {
      return null;
    }

    @Override
    public Abstract.Definition getReferent() {
      return myBinOp;
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
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitBinOp(this, params);
    }
  }

  public static class DefCallExpression extends Expression implements Abstract.DefCallExpression {
    private final Expression myExpression;
    private final String myName;
    private Abstract.Definition myDefinition;

    public DefCallExpression(Position position, Expression expression, String name) {
      super(position);
      myExpression = expression;
      myName = name;
      myDefinition = null;
    }

    public DefCallExpression(Position position, Abstract.Definition definition) {
      super(position);
      myExpression = null;
      myName = definition.getName();
      myDefinition = definition;
    }

    @Override
    public Expression getExpression() {
      return myExpression;
    }

    @Override
    public Abstract.Definition getReferent() {
      return myDefinition;
    }

    public void setResolvedDefinition(Abstract.Definition definition) {
      myDefinition = definition;
    }

    @Override
    public String getName() {
      return myName;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitDefCall(this, params);
    }
  }

  public static class ModuleCallExpression extends Expression implements Abstract.ModuleCallExpression {
    private final ModulePath myPath;
    private Abstract.Definition myModule;

    public ModuleCallExpression(Position position, List<String> path) {
      super(position);
      this.myPath = new ModulePath(path);
    }

    @Override
    public ModulePath getPath() {
      return myPath;
    }

    @Override
    public Abstract.Definition getModule() {
      return myModule;
    }

    public void setModule(Abstract.Definition module) {
      myModule = module;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitModuleCall(this, params);
    }
  }

  public static class ClassExtExpression extends Expression implements Abstract.ClassExtExpression {
    private final Expression myBaseClassExpression;
    private final List<ClassFieldImpl> myDefinitions;

    public ClassExtExpression(Position position, Expression baseClassExpression, List<ClassFieldImpl> definitions) {
      super(position);
      myBaseClassExpression = baseClassExpression;
      myDefinitions = definitions;
    }

    @Override
    public Expression getBaseClassExpression() {
      return myBaseClassExpression;
    }

    @Override
    public List<ClassFieldImpl> getStatements() {
      return myDefinitions;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClassExt(this, params);
    }
  }

  public static class ClassFieldImpl extends SourceNode implements Abstract.ClassFieldImpl {
    private final String myName;
    private Abstract.ClassField myImplementedField;
    private final Expression myExpression;

    public ClassFieldImpl(Position position, String identifier, Expression expression) {
      super(position);
      myName = identifier;
      myExpression = expression;
    }

    @Override
    public String getImplementedFieldName() {
      return myName;
    }

    @Override
    public Abstract.ClassField getImplementedField() {
      return myImplementedField;
    }

    public void setImplementedField(Abstract.ClassField newImplementedField) {
      myImplementedField = newImplementedField;
    }

    @Override
    public Expression getImplementation() {
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

  public static class LetClause extends ReferableSourceNode implements Abstract.LetClause {
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

  public static class LvlExpression extends Expression implements Abstract.LvlExpression {
    public LvlExpression(Position position) {
      super(position);
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitLvl(this, params);
    }
  }

  public static class PolyUniverseExpression extends Expression implements Abstract.PolyUniverseExpression {
    private final List<? extends Abstract.Expression> myPLevel;
    private final int myHLevel;

    public PolyUniverseExpression(Position position, List<? extends Abstract.Expression> pLevel, int hLevel) {
      super(position);
      myPLevel = pLevel;
      myHLevel = hLevel;
    }

    @Override
    public List<? extends Abstract.Expression> getPLevel() {
      return myPLevel;
    }

    @Override
    public int getHLevel() {
      return myHLevel;
    }

    @Override
    public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitPolyUniverse(this, params);
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

  public interface PatternContainer extends Abstract.PatternContainer {
    void replaceWithConstructor(int index);
  }

  public static class Clause extends SourceNode implements PatternContainer, Abstract.Clause {
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
    public void replaceWithConstructor(int index) {
      Pattern old = myPatterns.get(index);
      myPatterns.set(index, new ConstructorPattern(old.getPosition(), old.getName(), Collections.<PatternArgument>emptyList()));
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

  // Definitions

  public static class ReferableSourceNode extends SourceNode implements Abstract.ReferableSourceNode {
    private final String myName;

    public ReferableSourceNode(Position position, String name) {
      super(position);
      myName = name;
    }

    @Override
    public String getName() {
      return myName;
    }
  }

  public static abstract class Definition extends ReferableSourceNode implements Abstract.Definition {
    private final Abstract.Precedence myPrecedence;
    private Definition myParent;
    private boolean myStatic;

    public Definition(Position position, String name, Abstract.Precedence precedence) {
      super(position, name);
      myStatic = true;
      myPrecedence = precedence;
    }

    @Override
    public Abstract.Precedence getPrecedence() {
      return myPrecedence;
    }

    @Override
    public Definition getParent() {
      return myParent;
    }

    public void setParent(Definition parent) {
      myParent = parent;
    }

    @Override
    public boolean isStatic() {
      return myStatic;
    }

    public void setIsStatic(boolean isStatic) {
      myStatic = isStatic;
    }

    @Override
    public String toString() {
      return getName();
    }
  }

  public static abstract class SignatureDefinition extends Definition {
    private final List<Argument> myArguments;
    private final Expression myResultType;

    public SignatureDefinition(Position position, String name, Abstract.Precedence precedence, List<Argument> arguments, Expression resultType) {
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

  public static class SuperClass extends SourceNode implements Abstract.SuperClass {
    private Expression mySuperClass;

    public SuperClass(Position position, Expression superClass) {
      super(position);
      mySuperClass = superClass;
    }

    @Override
    public Expression getSuperClass() {
      return mySuperClass;
    }
  }

  public static class ClassDefinition extends Definition implements Abstract.ClassDefinition {
    private final List<TypeArgument> myPolyParameters;
    private final List<SuperClass> mySuperClasses;
    private final List<ClassField> myFields;
    private final List<Implementation> myImplementations;
    private final List<Statement> myGlobalStatements;
    private final List<Definition> myInstanceDefinitions;

    public ClassDefinition(Position position, String name, List<TypeArgument> polyParams, List<SuperClass> superClasses, List<ClassField> fields, List<Implementation> implementations, List<Statement> globalStatements, List<Definition> instanceDefinitions) {
      super(position, name, Abstract.Precedence.DEFAULT);
      myPolyParameters = polyParams;
      mySuperClasses = superClasses;
      myFields = fields;
      myImplementations = implementations;
      myGlobalStatements = globalStatements;
      myInstanceDefinitions = instanceDefinitions;
    }

    public ClassDefinition(Position position, String name, List<Statement> globalStatements) {
      this(position, name, Collections.<TypeArgument>emptyList(), Collections.<SuperClass>emptyList(), Collections.<ClassField>emptyList(), Collections.<Implementation>emptyList(), globalStatements, Collections.<Definition>emptyList());
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClass(this, params);
    }

    @Override
    public List<TypeArgument> getPolyParameters() {
      return myPolyParameters;
    }

    @Override
    public List<SuperClass> getSuperClasses() {
      return mySuperClasses;
    }

    @Override
    public List<ClassField> getFields() {
      return myFields;
    }

    @Override
    public List<Implementation> getImplementations() {
      return myImplementations;
    }

    @Override
    public List<Statement> getGlobalStatements() {
      return myGlobalStatements;
    }

    @Override
    public List<Definition> getInstanceDefinitions() {
      return myInstanceDefinitions;
    }
  }

  public static class ClassField extends SignatureDefinition implements Abstract.ClassField {
    public ClassField(Position position, String name, Abstract.Precedence precedence, List<Argument> arguments, Expression resultType) {
      super(position, name, precedence, arguments, resultType);
      setIsStatic(false);
    }

    @Override
    public ClassDefinition getParent() {
      return (ClassDefinition) super.getParent();
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClassField(this, params);
    }
  }

  public static class Implementation extends Definition implements Abstract.Implementation {
    private Abstract.ClassField myImplemented;
    private final Expression myExpression;

    public Implementation(Position position, String name, Expression expression) {
      super(position, name, Abstract.Precedence.DEFAULT);
      myExpression = expression;
      setIsStatic(false);
    }

    @Override
    public Abstract.ClassField getImplementedField() {
      return myImplemented;
    }

    public void setImplemented(Abstract.ClassField implemented) {
      myImplemented = implemented;
    }

    @Override
    public Expression getImplementation() {
      return myExpression;
    }

    @Override
    public ClassDefinition getParent() {
      return (ClassDefinition) super.getParent();
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitImplement(this, params);
    }
  }

  public static class FunctionDefinition extends SignatureDefinition implements Abstract.FunctionDefinition {
    private final Abstract.Definition.Arrow myArrow;
    private final Expression myTerm;
    private final List<Statement> myStatements;

    public FunctionDefinition(Position position, String name, Abstract.Precedence precedence, List<Argument> arguments, Expression resultType, Abstract.Definition.Arrow arrow, Expression term, List<Statement> statements) {
      super(position, name, precedence, arguments, resultType);
      myArrow = arrow;
      myTerm = term;
      myStatements = statements;
    }

    @Override
    public Definition.Arrow getArrow() {
      return myArrow;
    }

    @Override
    public List<Statement> getGlobalStatements() {
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

  public static class DataDefinition extends Definition implements Abstract.DataDefinition {
    private final List<Constructor> myConstructors;
    private final List<TypeArgument> myParameters;
    private final List<Condition> myConditions;
    private final boolean myIsTruncated;
    private final Expression myUniverse;

    public DataDefinition(Position position, String name, Abstract.Precedence precedence, List<TypeArgument> parameters, boolean isTruncated, Expression universe, List<Concrete.Constructor> constructors, List<Condition> conditions) {
      super(position, name, precedence);
      myParameters = parameters;
      myConstructors = constructors;
      myConditions = conditions;
      myIsTruncated = isTruncated;
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
    public boolean isTruncated() {
      return myIsTruncated;
    }

    @Override
    public Expression getUniverse() {
      return myUniverse;
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitData(this, params);
    }
  }

  public static class Constructor extends Definition implements Abstract.Constructor {
    private final DataDefinition myDataType;
    private final List<TypeArgument> myArguments;
    private final List<PatternArgument> myPatterns;

    public Constructor(Position position, String name, Abstract.Precedence precedence, List<TypeArgument> arguments, DataDefinition dataType, List<PatternArgument> patterns) {
      super(position, name, precedence);
      myArguments = arguments;
      myDataType = dataType;
      myPatterns = patterns;
    }

    @Override
    public List<PatternArgument> getPatterns() {
      return myPatterns;
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

  public static class Condition extends SourceNode implements Abstract.Condition {
    private final String myConstructorName;
    private final List<PatternArgument> myPatterns;
    private final Expression myTerm;

    public Condition(Position position, String constructorName, List<PatternArgument> patterns, Expression term) {
      super(position);
      myConstructorName = constructorName;
      myPatterns = patterns;
      myTerm = term;
    }

    @Override
    public String getConstructorName() {
      return myConstructorName;
    }

    @Override
    public List<PatternArgument> getPatterns() {
      return myPatterns;
    }

    @Override
    public Expression getTerm() {
      return myTerm;
    }

    @Override
    public void setWellTyped(com.jetbrains.jetpad.vclang.core.definition.Condition condition) {

    }
  }

  // ClassViews

  public static class ClassView extends Definition implements Abstract.ClassView {
    private final DefCallExpression myUnderlyingClass;
    private final String myClassifyingFieldName;
    private Abstract.ClassField myClassifyingField;
    private final List<ClassViewField> myFields;

    public ClassView(Position position, String name, DefCallExpression underlyingClass, String classifyingFieldName, List<ClassViewField> fields) {
      super(position, name, Abstract.Precedence.DEFAULT);
      myUnderlyingClass = underlyingClass;
      myFields = fields;
      myClassifyingFieldName = classifyingFieldName;
    }

    @Override
    public DefCallExpression getUnderlyingClassDefCall() {
      return myUnderlyingClass;
    }

    @Override
    public String getClassifyingFieldName() {
      return myClassifyingFieldName;
    }

    @Override
    public Abstract.ClassField getClassifyingField() {
      return myClassifyingField;
    }

    public void setClassifyingField(Abstract.ClassField classifyingField) {
      myClassifyingField = classifyingField;
    }

    @Override
    public List<ClassViewField> getFields() {
      return myFields;
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClassView(this, params);
    }
  }

  public static class ClassViewField extends Definition implements Abstract.ClassViewField {
    private final String myUnderlyingFieldName;
    private Abstract.ClassField myUnderlyingField;
    private final ClassView myOwnView;

    public ClassViewField(Position position, String name, Abstract.Precedence precedence, String underlyingFieldName, ClassView ownView) {
      super(position, name, precedence);
      myUnderlyingFieldName = underlyingFieldName;
      myOwnView = ownView;
    }

    @Override
    public String getUnderlyingFieldName() {
      return myUnderlyingFieldName;
    }

    @Override
    public Abstract.ClassField getUnderlyingField() {
      return myUnderlyingField;
    }

    @Override
    public ClassView getOwnView() {
      return myOwnView;
    }

    public void setUnderlyingField(Abstract.ClassField underlyingField) {
      myUnderlyingField = underlyingField;
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClassViewField(this, params);
    }
  }

  public static class ClassViewInstance extends Definition implements Abstract.ClassViewInstance {
    private final boolean myDefault;
    private final List<Argument> myArguments;
    private final DefCallExpression myClassView;
    private final List<ClassFieldImpl> myClassFieldImpls;
    private Abstract.Definition myClassifyingDefinition;

    public ClassViewInstance(Position position, boolean isDefault, String name, Abstract.Precedence precedence, List<Argument> arguments, DefCallExpression classView, List<ClassFieldImpl> classFieldImpls) {
      super(position, name, precedence);
      myDefault = isDefault;
      myArguments = arguments;
      myClassView = classView;
      myClassFieldImpls = classFieldImpls;
    }

    @Override
    public boolean isDefault() {
      return myDefault;
    }

    @Override
    public List<Argument> getArguments() {
      return myArguments;
    }

    @Override
    public DefCallExpression getClassView() {
      return myClassView;
    }

    @Override
    public Abstract.Definition getClassifyingDefinition() {
      return myClassifyingDefinition;
    }

    public void setClassifyingDefinition(Abstract.Definition classifyingDefinition) {
      myClassifyingDefinition = classifyingDefinition;
    }

    @Override
    public List<ClassFieldImpl> getClassFieldImpls() {
      return myClassFieldImpls;
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClassViewInstance(this, params);
    }
  }

  // Statements

  public static abstract class Statement extends SourceNode implements Abstract.Statement {
    public Statement(Position position) {
      super(position);
    }
  }

  public static class DefineStatement extends Statement implements Abstract.DefineStatement {
    private final Definition myDefinition;

    public DefineStatement(Position position, Definition definition) {
      super(position);
      myDefinition = definition;
    }

    @Override
    public Definition getDefinition() {
      return myDefinition;
    }

    @Override
    public <P, R> R accept(AbstractStatementVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitDefine(this, params);
    }
  }

  public static class NamespaceCommandStatement extends Statement implements Abstract.NamespaceCommandStatement {
    private final Kind myKind;
    private Abstract.Definition myDefinition;
    private final ModulePath myModulePath;
    private final List<String> myPath;
    private final boolean myHiding;
    private final List<String> myNames;

    public NamespaceCommandStatement(Position position, Kind kind, List<String> modulePath, List<String> path, boolean isHiding, List<String> names) {
      super(position);
      myKind = kind;
      myDefinition = null;
      myModulePath = modulePath != null ? new ModulePath(modulePath) : null;
      myPath = path;
      myHiding = isHiding;
      myNames = names;
    }

    @Override
    public Kind getKind() {
      return myKind;
    }

    @Override
    public ModulePath getModulePath() {
      return myModulePath;
    }

    @Override
    public List<String> getPath() {
      return myPath;
    }

    public void setResolvedClass(Abstract.Definition resolvedClass) {
      myDefinition = resolvedClass;
    }

    @Override
    public Abstract.Definition getResolvedClass() {
      return myDefinition;
    }

    @Override
    public boolean isHiding() {
      return myHiding;
    }

    @Override
    public List<String> getNames() {
      return myNames;
    }

    @Override
    public <P, R> R accept(AbstractStatementVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitNamespaceCommand(this, params);
    }
  }

  // Patterns

  public static class PatternArgument extends SourceNode implements Abstract.PatternArgument {
    private final boolean myHidden;
    private final boolean myExplicit;
    private Pattern myPattern;

    public PatternArgument(Position position, Pattern pattern, boolean explicit, boolean hidden) {
      super(position);
      this.myHidden = hidden;
      this.myPattern = pattern;
      this.myExplicit = explicit;
    }

    public void replaceWithConstructor() {
      myPattern = new ConstructorPattern(myPattern.getPosition(), myPattern.getName(), Collections.<PatternArgument>emptyList());
    }

    @Override
    public boolean isHidden() {
      return myHidden;
    }

    @Override
    public boolean isExplicit() {
      return myExplicit;
    }

    @Override
    public Abstract.Pattern getPattern() {
      return myPattern;
    }
  }

  public static abstract class Pattern extends SourceNode implements Abstract.Pattern {
    public Pattern(Position position) {
      super(position);
    }

    @Override
    public void setWellTyped(com.jetbrains.jetpad.vclang.core.pattern.Pattern pattern) {

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
    private final String myConstructorName;
    private final List<PatternArgument> myArguments;

    public ConstructorPattern(Position position, String constructorName, List<PatternArgument> arguments) {
      super(position);
      myConstructorName = constructorName;
      myArguments = arguments;
    }

    @Override
    public String getConstructorName() {
      return myConstructorName;
    }

    @Override
    public List<Concrete.PatternArgument> getArguments() {
      return myArguments;
    }

    @Override
    public String getName() {
      return myConstructorName;
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
}
