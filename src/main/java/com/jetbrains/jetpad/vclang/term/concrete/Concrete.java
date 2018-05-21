package com.jetbrains.jetpad.vclang.term.concrete;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.term.Fixity;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Concrete {
  private Concrete() {}

  public interface SourceNode extends PrettyPrintable {
    @Nullable Object getData();
  }

  public static abstract class SourceNodeImpl implements SourceNode {
    private final Object myData;

    SourceNodeImpl(Object data) {
      myData = data;
    }

    @Override
    @Nullable
    public Object getData() {
      return myData;
    }

    @Override
    public void prettyPrint(StringBuilder builder, PrettyPrinterConfig infoProvider) {
      PrettyPrintVisitor.prettyPrint(builder, this); // TODO[pretty]: implement this properly
    }
  }

  // Parameters

  public static abstract class Parameter extends SourceNodeImpl {
    private boolean myExplicit;

    public Parameter(Object data, boolean explicit) {
      super(data);
      myExplicit = explicit;
    }

    public boolean getExplicit() {
      return myExplicit;
    }

    public void setExplicit(boolean explicit) {
      myExplicit = explicit;
    }
  }

  public static class NameParameter extends Parameter {
    private final Referable myReferable;

    public NameParameter(Object data, boolean explicit, Referable referable) {
      super(data, explicit);
      myReferable = referable;
    }

    @Nullable
    public Referable getReferable() {
      return myReferable;
    }
  }

  public static class TypeParameter extends Parameter {
    private final Expression myType;

    public TypeParameter(Object data, boolean explicit, Expression type) {
      super(data, explicit);
      myType = type;
    }

    public TypeParameter(boolean explicit, Expression type) {
      this(type.getData(), explicit, type);
    }

    @Nonnull
    public Expression getType() {
      return myType;
    }
  }

  public static class TelescopeParameter extends TypeParameter {
    private final List<? extends Referable> myReferableList;

    public TelescopeParameter(Object data, boolean explicit, List<? extends Referable> referableList, Expression type) {
      super(data, explicit, type);
      myReferableList = referableList;
    }

    @Nonnull
    public List<? extends Referable> getReferableList() {
      return myReferableList;
    }
  }

  // Expressions

  public static GlobalReferable getUnderlyingClassDef(Expression expr) { // TODO[classes]
    if (expr instanceof ReferenceExpression) {
      Referable definition = ((ReferenceExpression) expr).getReferent();
      if (definition instanceof GlobalReferable) {
        return (GlobalReferable) definition;
      }
    }

    if (expr instanceof ClassExtExpression) {
      return getUnderlyingClassDef(((ClassExtExpression) expr).getBaseClassExpression());
    } else {
      return null;
    }
  }

  public static abstract class Expression extends SourceNodeImpl {
    public static final byte PREC = -12;

    public Expression(Object data) {
      super(data);
    }

    public abstract <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params);

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      accept(new PrettyPrintVisitor(builder, 0), new Precedence(Expression.PREC));
      return builder.toString();
    }
  }

  public static class Argument {
    private final Expression myExpression;
    private final boolean myExplicit;

    public Argument(Expression expression, boolean explicit) {
      myExpression = expression;
      myExplicit = explicit;
    }

    @Nonnull
    public Expression getExpression() {
      return myExpression;
    }

    public boolean isExplicit() {
      return myExplicit;
    }
  }

  public static class AppExpression extends Expression {
    public static final byte PREC = 11;
    private final Expression myFunction;
    private final Argument myArgument;

    public AppExpression(Object data, Expression function, Argument argument) {
      super(data);
      myFunction = function;
      myArgument = argument;
    }

    @Nonnull
    public Expression getFunction() {
      return myFunction;
    }

    @Nonnull
    public Argument getArgument() {
      return myArgument;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitApp(this, params);
    }
  }

  public static class BinOpSequenceElem {
    public final Expression expression;
    public final Fixity fixity;
    public final boolean isExplicit;

    public BinOpSequenceElem(@Nonnull Expression expression, @Nonnull Fixity fixity, boolean isExplicit) {
      this.expression = expression;
      this.fixity = fixity;
      this.isExplicit = isExplicit;
    }

    public boolean isReference() {
      return isExplicit && expression instanceof Concrete.ReferenceExpression;
    }

    public boolean isInfixReference() {
      return isReference() && ((ReferenceExpression) expression).getReferent() instanceof GlobalReferable && ((GlobalReferable) ((ReferenceExpression) expression).getReferent()).getPrecedence().isInfix;
    }
  }

  public static class BinOpSequenceExpression extends Expression {
    public static final byte PREC = 0;
    private final List<BinOpSequenceElem> mySequence;

    public BinOpSequenceExpression(Object data, List<BinOpSequenceElem> sequence) {
      super(data);
      mySequence = sequence;
    }

    @Nonnull
    public List<BinOpSequenceElem> getSequence() {
      return mySequence;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitBinOpSequence(this, params);
    }
  }

  public static class ReferenceExpression extends Expression {
    public static final byte PREC = 12;
    private Referable myReferent;
    private final Concrete.LevelExpression myPLevel;
    private final Concrete.LevelExpression myHLevel;

    public ReferenceExpression(Object data, Referable referable, Concrete.LevelExpression pLevel, Concrete.LevelExpression hLevel) {
      super(data);
      myReferent = referable;
      myPLevel = pLevel;
      myHLevel = hLevel;
    }

    public ReferenceExpression(Object data, Referable referable) {
      super(data);
      myReferent = referable;
      myPLevel = null;
      myHLevel = null;
    }

    public static ReferenceExpression make(Object data, Referable referable, Concrete.LevelExpression pLevel, Concrete.LevelExpression hLevel) {
      LevelVariable.LvlType type1 = pLevel == null ? null : getLevelType(pLevel);
      LevelVariable.LvlType type2 = hLevel == null ? null : getLevelType(hLevel);
      return type1 == LevelVariable.LvlType.HLVL && type2 != LevelVariable.LvlType.HLVL || type1 == null && type2 == LevelVariable.LvlType.PLVL
        ? new ReferenceExpression(data, referable, hLevel, pLevel)
        : new ReferenceExpression(data, referable, pLevel, hLevel);
    }

    private static LevelVariable.LvlType getLevelType(Concrete.LevelExpression expr) {
      return expr.accept(new ConcreteLevelExpressionVisitor<Void, LevelVariable.LvlType>() {
        @Override
        public LevelVariable.LvlType visitInf(Concrete.InfLevelExpression expr, Void param) {
          return LevelVariable.LvlType.HLVL;
        }

        @Override
        public LevelVariable.LvlType visitLP(Concrete.PLevelExpression expr, Void param) {
          return LevelVariable.LvlType.PLVL;
        }

        @Override
        public LevelVariable.LvlType visitLH(Concrete.HLevelExpression expr, Void param) {
          return LevelVariable.LvlType.HLVL;
        }

        @Override
        public LevelVariable.LvlType visitNumber(Concrete.NumberLevelExpression expr, Void param) {
          return null;
        }

        @Override
        public LevelVariable.LvlType visitSuc(Concrete.SucLevelExpression expr, Void param) {
          return expr.getExpression().accept(this, null);
        }

        @Override
        public LevelVariable.LvlType visitMax(Concrete.MaxLevelExpression expr, Void param) {
          LevelVariable.LvlType type1 = expr.getLeft().accept(this, null);
          LevelVariable.LvlType type2 = expr.getRight().accept(this, null);
          return type1 == null || type1 == type2 ? type2 : type2 == null ? type1 : null;
        }

        @Override
        public LevelVariable.LvlType visitVar(Concrete.InferVarLevelExpression expr, Void param) {
          return expr.getVariable().getType();
        }
      }, null);
    }

    @Nonnull
    public Referable getReferent() {
      return myReferent;
    }

    public void setReferent(Referable referent) {
      myReferent = referent;
    }

    public Concrete.LevelExpression getPLevel() {
      return myPLevel;
    }

    public Concrete.LevelExpression getHLevel() {
      return myHLevel;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitReference(this, params);
    }
  }

  public static class InferenceReferenceExpression extends Expression {
    private final InferenceVariable myVariable;

    public InferenceReferenceExpression(Object data, InferenceVariable variable) {
      super(data);
      myVariable = variable;
    }

    @Nonnull
    public InferenceVariable getVariable() {
      return myVariable;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitInferenceReference(this, params);
    }
  }

  public static class ClassExtExpression extends Expression {
    public static final byte PREC = 12;
    private final Expression myBaseClassExpression;
    private final List<ClassFieldImpl> myDefinitions;

    public ClassExtExpression(Object data, Expression baseClassExpression, List<ClassFieldImpl> definitions) {
      super(data);
      myBaseClassExpression = baseClassExpression;
      myDefinitions = definitions;
    }

    @Nonnull
    public Expression getBaseClassExpression() {
      return myBaseClassExpression;
    }

    @Nonnull
    public List<ClassFieldImpl> getStatements() {
      return myDefinitions;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClassExt(this, params);
    }
  }

  public static class ClassFieldImpl extends SourceNodeImpl {
    private Referable myImplementedField;
    private final Expression myExpression;

    public ClassFieldImpl(Object data, Referable implementedField, Expression expression) {
      super(data);
      myImplementedField = implementedField;
      myExpression = expression;
    }

    @Nonnull
    public Referable getImplementedField() {
      return myImplementedField;
    }

    public void setImplementedField(Referable newImplementedField) {
      myImplementedField = newImplementedField;
    }

    @Nonnull
    public Expression getImplementation() {
      return myExpression;
    }
  }

  public static class NewExpression extends Expression {
    public static final byte PREC = 11;
    private final Expression myExpression;

    public NewExpression(Object data, Expression expression) {
      super(data);
      myExpression = expression;
    }

    @Nonnull
    public Expression getExpression() {
      return myExpression;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitNew(this, params);
    }
  }

  public static class GoalExpression extends Expression {
    public static final byte PREC = 12;
    private final String myName;
    private final Expression myExpression;

    public GoalExpression(Object data, String name, Expression expression) {
      super(data);
      myName = name;
      myExpression = expression;
    }

    public String getName() {
      return myName;
    }

    public Expression getExpression() {
      return myExpression;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitGoal(this, params);
    }
  }

  public static class InferHoleExpression extends Expression {
    public static final byte PREC = 12;
    public InferHoleExpression(Object data) {
      super(data);
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitInferHole(this, params);
    }
  }

  public static class LamExpression extends Expression {
    public static final byte PREC = -5;
    private final List<Parameter> myArguments;
    private final Expression myBody;

    public LamExpression(Object data, List<Parameter> arguments, Expression body) {
      super(data);
      myArguments = arguments;
      myBody = body;
    }

    @Nonnull
    public List<Parameter> getParameters() {
      return myArguments;
    }

    @Nonnull
    public Expression getBody() {
      return myBody;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitLam(this, params);
    }
  }

  public static class LetClause implements SourceNode {
    private final List<Parameter> myParameters;
    private final Expression myResultType;
    private final Expression myTerm;
    private final Referable myReferable;

    public LetClause(Referable referable, List<Parameter> parameters, Expression resultType, Expression term) {
      myParameters = parameters;
      myResultType = resultType;
      myTerm = term;
      myReferable = referable;
    }

    @Nonnull
    @Override
    public Referable getData() {
      return myReferable;
    }

    @Nonnull
    public Expression getTerm() {
      return myTerm;
    }

    @Nonnull
    public List<Parameter> getParameters() {
      return myParameters;
    }

    public Expression getResultType() {
      return myResultType;
    }

    @Override
    public void prettyPrint(StringBuilder builder, PrettyPrinterConfig ppConfig) {
      new PrettyPrintVisitor(builder, 0, !ppConfig.isSingleLine()).prettyPrintLetClause(this, false);
    }
  }

  public static class LetExpression extends Expression {
    public static final byte PREC = -9;
    private final List<LetClause> myClauses;
    private final Expression myExpression;

    public LetExpression(Object data, List<LetClause> clauses, Expression expression) {
      super(data);
      myClauses = clauses;
      myExpression = expression;
    }

    @Nonnull
    public List<LetClause> getClauses() {
      return myClauses;
    }

    @Nonnull
    public Expression getExpression() {
      return myExpression;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitLet(this, params);
    }
  }

  public static class PiExpression extends Expression {
    public static final byte PREC = -4;
    private final List<TypeParameter> myParameters;
    private final Expression myCodomain;

    public PiExpression(Object data, List<TypeParameter> parameters, Expression codomain) {
      super(data);
      myParameters = parameters;
      myCodomain = codomain;
    }

    @Nonnull
    public List<TypeParameter> getParameters() {
      return myParameters;
    }

    @Nonnull
    public Expression getCodomain() {
      return myCodomain;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitPi(this, params);
    }
  }

  public static class SigmaExpression extends Expression {
    public static final byte PREC = -3;
    private final List<TypeParameter> myArguments;

    public SigmaExpression(Object data, List<TypeParameter> arguments) {
      super(data);
      myArguments = arguments;
    }

    @Nonnull
    public List<TypeParameter> getParameters() {
      return myArguments;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitSigma(this, params);
    }
  }

  public static class TupleExpression extends Expression {
    public static final byte PREC = 12;
    private final List<Expression> myFields;

    public TupleExpression(Object data, List<Expression> fields) {
      super(data);
      myFields = fields;
    }

    @Nonnull
    public List<Expression> getFields() {
      return myFields;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitTuple(this, params);
    }
  }

  public static class UniverseExpression extends Expression {
    public static final byte PREC = 12;
    private final LevelExpression myPLevel;
    private final LevelExpression myHLevel;

    public UniverseExpression(Object data, LevelExpression pLevel, LevelExpression hLevel) {
      super(data);
      myPLevel = pLevel;
      myHLevel = hLevel;
    }

    @Nullable
    public LevelExpression getPLevel() {
      return myPLevel;
    }

    @Nullable
    public LevelExpression getHLevel() {
      return myHLevel;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitUniverse(this, params);
    }
  }

  public static class ProjExpression extends Expression {
    public static final byte PREC = 12;
    private final Expression myExpression;
    private final int myField;

    public ProjExpression(Object data, Expression expression, int field) {
      super(data);
      myExpression = expression;
      myField = field;
    }

    @Nonnull
    public Expression getExpression() {
      return myExpression;
    }

    public int getField() {
      return myField;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitProj(this, params);
    }
  }

  public static class CaseExpression extends Expression {
    public static final byte PREC = -8;
    private final List<Expression> myExpressions;
    private final List<FunctionClause> myClauses;

    public CaseExpression(Object data, List<Expression> expressions, List<FunctionClause> clauses) {
      super(data);
      myExpressions = expressions;
      myClauses = clauses;
    }

    @Nonnull
    public List<Expression> getExpressions() {
      return myExpressions;
    }

    @Nonnull
    public List<FunctionClause> getClauses() {
      return myClauses;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitCase(this, params);
    }
  }

  public interface PatternContainer {
    List<Pattern> getPatterns();
  }

  public static class FunctionClause extends Clause {
    private final List<Pattern> myPatterns;
    private final Expression myExpression;

    public FunctionClause(Object data, List<Pattern> patterns, Expression expression) {
      super(data);
      myPatterns = patterns;
      myExpression = expression;
    }

    @Nonnull
    @Override
    public List<Pattern> getPatterns() {
      return myPatterns;
    }

    @Nullable
    public Expression getExpression() {
      return myExpression;
    }
  }

  public static class NumericLiteral extends Expression {
    private final BigInteger myNumber;

    public NumericLiteral(Object data, BigInteger number) {
      super(data);
      myNumber = number;
    }

    public BigInteger getNumber() {
      return myNumber;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitNumericLiteral(this, params);
    }
  }

  // Level expressions

  public static abstract class LevelExpression extends SourceNodeImpl {
    LevelExpression(Object data) {
      super(data);
    }

    public abstract <P, R> R accept(ConcreteLevelExpressionVisitor<? super P, ? extends R> visitor, P params);
  }

  public static class InferVarLevelExpression extends LevelExpression {
    private final InferenceLevelVariable myVariable;

    public InferVarLevelExpression(Object data, InferenceLevelVariable variable) {
      super(data);
      myVariable = variable;
    }

    @Nonnull
    public InferenceLevelVariable getVariable() {
      return myVariable;
    }

    @Override
    public <P, R> R accept(ConcreteLevelExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitVar(this, params);
    }
  }

  public static class PLevelExpression extends LevelExpression {
    public PLevelExpression(Object data) {
      super(data);
    }

    @Override
    public <P, R> R accept(ConcreteLevelExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitLP(this, params);
    }
  }

  public static class HLevelExpression extends LevelExpression {
    public HLevelExpression(Object data) {
      super(data);
    }

    @Override
    public <P, R> R accept(ConcreteLevelExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitLH(this, params);
    }
  }

  public static class InfLevelExpression extends LevelExpression {
    public InfLevelExpression(Object data) {
      super(data);
    }

    @Override
    public <P, R> R accept(ConcreteLevelExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitInf(this, params);
    }
  }

  public static class NumberLevelExpression extends LevelExpression {
    private final int myNumber;

    public NumberLevelExpression(Object data, int number) {
      super(data);
      myNumber = number;
    }

    public int getNumber() {
      return myNumber;
    }

    @Override
    public <P, R> R accept(ConcreteLevelExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitNumber(this, params);
    }
  }

  public static class SucLevelExpression extends LevelExpression {
    private final LevelExpression myExpression;

    public SucLevelExpression(Object data, LevelExpression expression) {
      super(data);
      myExpression = expression;
    }

    @Nonnull
    public LevelExpression getExpression() {
      return myExpression;
    }

    @Override
    public <P, R> R accept(ConcreteLevelExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitSuc(this, params);
    }
  }

  public static class MaxLevelExpression extends LevelExpression {
    private final LevelExpression myLeft;
    private final LevelExpression myRight;

    public MaxLevelExpression(Object data, LevelExpression left, LevelExpression right) {
      super(data);
      myLeft = left;
      myRight = right;
    }

    @Nonnull
    public LevelExpression getLeft() {
      return myLeft;
    }

    @Nonnull
    public LevelExpression getRight() {
      return myRight;
    }

    @Override
    public <P, R> R accept(ConcreteLevelExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitMax(this, params);
    }
  }

  // Definitions

  public static  Collection<? extends Parameter> getParameters(ReferableDefinition definition) {
    if (definition instanceof FunctionDefinition) {
      return ((FunctionDefinition) definition).getParameters();
    }
    if (definition instanceof DataDefinition) {
      return ((DataDefinition) definition).getParameters();
    }
    if (definition instanceof Constructor) {
      List<TypeParameter> dataTypeParameters = ((Concrete.Constructor) definition).getRelatedDefinition().getParameters();
      List<TypeParameter> parameters = ((Constructor) definition).getParameters();
      List<TypeParameter> totalParameters = new ArrayList<>(dataTypeParameters.size() + parameters.size());
      totalParameters.addAll(dataTypeParameters);
      totalParameters.addAll(parameters);
      return totalParameters;
    }
    return null;
  }

  public static abstract class ReferableDefinition implements SourceNode {
    private final TCReferable myReferable;

    public ReferableDefinition(TCReferable referable) {
      myReferable = referable;
    }

    @Nonnull
    @Override
    public TCReferable getData() {
      return myReferable;
    }

    // TODO: Delete this method.
    @Nonnull
    public abstract Definition getRelatedDefinition();

    @Override
    public void prettyPrint(StringBuilder builder, PrettyPrinterConfig infoProvider) {
      builder.append(myReferable); // TODO[pretty]: implement this properly
    }

    public abstract <P, R> R accept(ConcreteReferableDefinitionVisitor<? super P, ? extends R> visitor, P params);

    @Override
    public String toString() {
      return myReferable.textRepresentation();
    }
  }

  public static abstract class Definition extends ReferableDefinition {
    public Definition(TCReferable referable) {
      super(referable);
    }

    @Nonnull
    @Override
    public Definition getRelatedDefinition() {
      return this;
    }

    @Override
    public void prettyPrint(StringBuilder builder, PrettyPrinterConfig ppConfig) {
      accept(new PrettyPrintVisitor(builder, 0, !ppConfig.isSingleLine()), null);
    }

    public abstract <P, R> R accept(ConcreteDefinitionVisitor<? super P, ? extends R> visitor, P params);

    @Override
    public <P, R> R accept(ConcreteReferableDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return accept((ConcreteDefinitionVisitor<? super P, ? extends R>) visitor, params);
    }
  }

  public static class ClassDefinition extends Definition {
    private final List<ReferenceExpression> mySuperClasses;
    private final List<ClassField> myFields;
    private final List<ClassFieldImpl> myImplementations;
    private boolean myHasParameter;

    public ClassDefinition(TCClassReferable referable, List<ReferenceExpression> superClasses, List<ClassField> fields, List<ClassFieldImpl> implementations, boolean hasParameter) {
      super(referable);
      mySuperClasses = superClasses;
      myFields = fields;
      myImplementations = implementations;
      myHasParameter = hasParameter;
    }

    public boolean hasParameter() {
      return myHasParameter;
    }

    public void setHasParameter() {
      myHasParameter = true;
    }

    @Nonnull
    @Override
    public TCClassReferable getData() {
      return (TCClassReferable) super.getData();
    }

    @Nonnull
    public List<ReferenceExpression> getSuperClasses() {
      return mySuperClasses;
    }

    @Nonnull
    public List<ClassField> getFields() {
      return myFields;
    }

    @Nonnull
    public List<ClassFieldImpl> getImplementations() {
      return myImplementations;
    }

    @Override
    public <P, R> R accept(ConcreteDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClass(this, params);
    }
  }

  public static class ClassField extends ReferableDefinition {
    private final ClassDefinition myParentClass;
    private Expression myResultType;

    public ClassField(TCReferable referable, ClassDefinition parentClass, Expression resultType) {
      super(referable);
      myParentClass = parentClass;
      myResultType = resultType;
    }

    @Nonnull
    public Expression getResultType() {
      return myResultType;
    }

    public void setResultType(Concrete.Expression resultType) {
      myResultType = resultType;
    }

    @Nonnull
    @Override
    public ClassDefinition getRelatedDefinition() {
      return myParentClass;
    }

    @Override
    public <P, R> R accept(ConcreteReferableDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClassField(this, params);
    }
  }

  public static abstract class FunctionBody extends SourceNodeImpl {
    public FunctionBody(Object data) {
      super(data);
    }
  }

  public static class TermFunctionBody extends FunctionBody {
    private Expression myTerm;

    public TermFunctionBody(Object data, Expression term) {
      super(data);
      myTerm = term;
    }

    @Nonnull
    public Expression getTerm() {
      return myTerm;
    }

    public void setTerm(Expression term) {
      myTerm = term;
    }
  }

  public static class ElimFunctionBody extends FunctionBody {
    private final List<ReferenceExpression> myExpressions;
    private final List<FunctionClause> myClauses;

    public ElimFunctionBody(Object data, List<ReferenceExpression> expressions, List<FunctionClause> clauses) {
      super(data);
      myExpressions = expressions;
      myClauses = clauses;
    }

    @Nonnull
    public List<? extends ReferenceExpression> getEliminatedReferences() {
      return myExpressions;
    }

    @Nonnull
    public List<FunctionClause> getClauses() {
      return myClauses;
    }
  }

  public static class FunctionDefinition extends Definition {
    private final List<Parameter> myParameters;
    private Expression myResultType;
    private final FunctionBody myBody;

    public FunctionDefinition(TCReferable referable, List<Parameter> parameters, Expression resultType, FunctionBody body) {
      super(referable);
      myParameters = parameters;
      myResultType = resultType;
      myBody = body;
    }

    @Nonnull
    public List<Parameter> getParameters() {
      return myParameters;
    }

    @Nullable
    public Expression getResultType() {
      return myResultType;
    }

    public void setResultType(Expression resultType) {
      myResultType = resultType;
    }

    @Nonnull
    public FunctionBody getBody() {
      return myBody;
    }

    @Override
    public <P, R> R accept(ConcreteDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitFunction(this, params);
    }
  }

  public static class DataDefinition extends Definition {
    private final List<TypeParameter> myParameters;
    private final List<ReferenceExpression> myEliminatedReferences;
    private final List<ConstructorClause> myConstructorClauses;
    private final boolean myIsTruncated;
    private final UniverseExpression myUniverse;

    public DataDefinition(TCReferable referable, List<TypeParameter> parameters, List<ReferenceExpression> eliminatedReferences, boolean isTruncated, UniverseExpression universe, List<ConstructorClause> constructorClauses) {
      super(referable);
      myParameters = parameters;
      myEliminatedReferences = eliminatedReferences;
      myConstructorClauses = constructorClauses;
      myIsTruncated = isTruncated;
      myUniverse = universe;
    }

    @Nonnull
    public List<TypeParameter> getParameters() {
      return myParameters;
    }

    @Nullable
    public List<ReferenceExpression> getEliminatedReferences() {
      return myEliminatedReferences;
    }

    @Nonnull
    public List<ConstructorClause> getConstructorClauses() {
      return myConstructorClauses;
    }

    public boolean isTruncated() {
      return myIsTruncated;
    }

    @Nullable
    public UniverseExpression getUniverse() {
      return myUniverse;
    }

    @Override
    public <P, R> R accept(ConcreteDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitData(this, params);
    }
  }

  public static abstract class Clause extends SourceNodeImpl implements PatternContainer {
    public Clause(Object data) {
      super(data);
    }
  }

  public static class ConstructorClause extends Clause {
    private final List<Pattern> myPatterns;
    private final List<Constructor> myConstructors;

    public ConstructorClause(Object data, List<Pattern> patterns, List<Constructor> constructors) {
      super(data);
      myPatterns = patterns;
      myConstructors = constructors;
    }

    @Override
    public List<Pattern> getPatterns() {
      return myPatterns;
    }

    @Nonnull
    public List<Constructor> getConstructors() {
      return myConstructors;
    }
  }

  public static class Constructor extends ReferableDefinition {
    private final DataDefinition myDataType;
    private final List<TypeParameter> myParameters;
    private final List<ReferenceExpression> myEliminatedReferences;
    private final List<FunctionClause> myClauses;

    public Constructor(TCReferable referable, DataDefinition dataType, List<TypeParameter> parameters, List<ReferenceExpression> eliminatedReferences, List<FunctionClause> clauses) {
      super(referable);
      myDataType = dataType;
      myParameters = parameters;
      myEliminatedReferences = eliminatedReferences;
      myClauses = clauses;
    }

    @Nonnull
    public List<TypeParameter> getParameters() {
      return myParameters;
    }

    @Nonnull
    public List<ReferenceExpression> getEliminatedReferences() {
      return myEliminatedReferences;
    }

    @Nonnull
    public List<FunctionClause> getClauses() {
      return myClauses;
    }

    @Nonnull
    @Override
    public DataDefinition getRelatedDefinition() {
      return myDataType;
    }

    @Override
    public <P, R> R accept(ConcreteReferableDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitConstructor(this, params);
    }
  }

  // Class synonyms

  public static class ClassSynonym extends Definition {
    private final List<ReferenceExpression> mySuperClasses;
    private final ReferenceExpression myUnderlyingClass;
    private final List<ClassFieldSynonym> myFields;

    public ClassSynonym(TCClassReferable referable, List<ReferenceExpression> superClasses, ReferenceExpression underlyingClass, List<ClassFieldSynonym> fields) {
      super(referable);
      mySuperClasses = superClasses;
      myUnderlyingClass = underlyingClass;
      myFields = fields;
    }

    @Nonnull
    @Override
    public TCClassReferable getData() {
      return (TCClassReferable) super.getData();
    }

    @Nonnull
    public List<ReferenceExpression> getSuperClasses() {
      return mySuperClasses;
    }

    @Nonnull
    public ReferenceExpression getUnderlyingClass() {
      return myUnderlyingClass;
    }

    @Nonnull
    public List<ClassFieldSynonym> getFields() {
      return myFields;
    }

    @Override
    public <P, R> R accept(ConcreteDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClassSynonym(this, params);
    }
  }

  public static class ClassFieldSynonym extends ReferableDefinition {
    private final ReferenceExpression myUnderlyingField;
    private final ClassSynonym myClassSynonym;

    public ClassFieldSynonym(TCReferable referable, ReferenceExpression underlyingField, ClassSynonym classSynonym) {
      super(referable);
      myUnderlyingField = underlyingField;
      myClassSynonym = classSynonym;
    }

    @Nonnull
    public ReferenceExpression getUnderlyingField() {
      return myUnderlyingField;
    }

    @Nonnull
    @Override
    public ClassSynonym getRelatedDefinition() {
      return myClassSynonym;
    }

    @Override
    public <P, R> R accept(ConcreteReferableDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClassFieldSynonym(this, params);
    }
  }

  public static class Instance extends Definition {
    private final List<Parameter> myArguments;
    private final ReferenceExpression myClass;
    private final List<ClassFieldImpl> myClassFieldImpls;

    public Instance(TCReferable referable, List<Parameter> arguments, ReferenceExpression classRef, List<ClassFieldImpl> classFieldImpls) {
      super(referable);
      myArguments = arguments;
      myClass = classRef;
      myClassFieldImpls = classFieldImpls;
    }

    @Nonnull
    public List<Parameter> getParameters() {
      return myArguments;
    }

    @Nonnull
    public ReferenceExpression getClassReference() {
      return myClass;
    }

    @Nonnull
    public List<ClassFieldImpl> getClassFieldImpls() {
      return myClassFieldImpls;
    }

    @Override
    public <P, R> R accept(ConcreteDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitInstance(this, params);
    }
  }

  // Patterns

  public static abstract class Pattern extends SourceNodeImpl {
    public static final byte PREC = 11;
    private boolean myExplicit;

    public Pattern(Object data) {
      super(data);
      myExplicit = true;
    }

    public boolean isExplicit() {
      return myExplicit;
    }

    public void setExplicit(boolean isExplicit) {
      myExplicit = isExplicit;
    }
  }

  public static class NamePattern extends Pattern {
    private final @Nullable Referable myReferable;

    public NamePattern(Object data, @Nullable Referable referable) {
      super(data);
      myReferable = referable;
    }

    public NamePattern(Object data, boolean isExplicit, @Nullable Referable referable) {
      super(data);
      setExplicit(isExplicit);
      myReferable = referable;
    }

    @Nullable
    public Referable getReferable() {
      return myReferable;
    }
  }

  public static class ConstructorPattern extends Pattern implements PatternContainer {
    private Referable myConstructor;
    private final List<Pattern> myArguments;

    public ConstructorPattern(Object data, boolean isExplicit, Referable constructor, List<Pattern> arguments) {
      super(data);
      setExplicit(isExplicit);
      myConstructor = constructor;
      myArguments = arguments;
    }

    public ConstructorPattern(Object data, Referable constructor, List<Pattern> arguments) {
      super(data);
      myConstructor = constructor;
      myArguments = arguments;
    }

    @Nonnull
    public Referable getConstructor() {
      return myConstructor;
    }

    public void setConstructor(Referable constructor) {
      myConstructor = constructor;
    }

    @Nonnull
    @Override
    public List<Pattern> getPatterns() {
      return myArguments;
    }
  }

  public static class EmptyPattern extends Pattern {
    public EmptyPattern(Object data) {
      super(data);
    }

    public EmptyPattern(Object data, boolean isExplicit) {
      super(data);
      setExplicit(isExplicit);
    }
  }
}
