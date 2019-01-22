package org.arend.term.concrete;

import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.naming.reference.*;
import org.arend.term.ClassFieldKind;
import org.arend.term.Fixity;
import org.arend.term.Precedence;
import org.arend.term.prettyprint.PrettyPrintVisitor;
import org.arend.term.prettyprint.PrettyPrintable;
import org.arend.term.prettyprint.PrettyPrinterConfig;
import org.arend.typechecking.error.local.LocalError;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class Concrete {
  private Concrete() {}

  public interface SourceNode extends PrettyPrintable, DataContainer {
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

    @Nonnull
    public abstract List<? extends Referable> getReferableList();

    public abstract int getNumberOfParameters();

    public List<String> getNames() {
      List<? extends Referable> referableList = getReferableList();
      List<String> names = new ArrayList<>(referableList.size());
      for (Referable referable : referableList) {
        names.add(referable == null ? null : referable.textRepresentation());
      }
      return names;
    }

    @Nullable
    public Expression getType() {
      return null;
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

    @Override
    @Nonnull
    public List<? extends Referable> getReferableList() {
      return Collections.singletonList(myReferable);
    }

    @Override
    public int getNumberOfParameters() {
      return 1;
    }
  }

  public static class TypeParameter extends Parameter {
    public Expression type;

    public TypeParameter(Object data, boolean explicit, Expression type) {
      super(data, explicit);
      this.type = type;
    }

    public TypeParameter(boolean explicit, Expression type) {
      this(type.getData(), explicit, type);
    }

    @Override
    @Nonnull
    public List<? extends Referable> getReferableList() {
      return Collections.singletonList(null);
    }

    @Override
    public int getNumberOfParameters() {
      return 1;
    }

    @Override
    @Nonnull
    public Expression getType() {
      return type;
    }
  }

  public static class TelescopeParameter extends TypeParameter {
    private final List<? extends Referable> myReferableList;

    public TelescopeParameter(Object data, boolean explicit, List<? extends Referable> referableList, Expression type) {
      super(data, explicit, type);
      myReferableList = referableList;
    }

    @Override
    @Nonnull
    public List<? extends Referable> getReferableList() {
      return myReferableList;
    }

    @Override
    public int getNumberOfParameters() {
      return myReferableList.size();
    }
  }

  // Expressions

  public static abstract class Expression extends SourceNodeImpl {
    public static final byte PREC = -12;

    public Expression(Object data) {
      super(data);
    }

    public abstract <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params);

    public Referable getUnderlyingReferable() {
      Expression expr = this;
      while (expr instanceof Concrete.ClassExtExpression) {
        expr = ((ClassExtExpression) expr).getBaseClassExpression();
      }
      if (expr instanceof Concrete.AppExpression) {
        expr = ((AppExpression) expr).getFunction();
      }
      return expr instanceof ReferenceExpression ? ((ReferenceExpression) expr).getReferent() : null;
    }

    public TCClassReferable getUnderlyingClassReferable(boolean resolveClassSynonym) {
      Referable ref = getUnderlyingReferable();
      if (!(ref instanceof TCClassReferable)) {
        return null;
      }

      return resolveClassSynonym ? ((TCClassReferable) ref).getUnderlyingTypecheckable() : (TCClassReferable) ref;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      accept(new PrettyPrintVisitor(builder, 0), new Precedence(Expression.PREC));
      return builder.toString();
    }
  }

  public static class Argument {
    public Expression expression;
    private final boolean myExplicit;

    public Argument(Expression expression, boolean explicit) {
      this.expression = expression;
      myExplicit = explicit;
    }

    @Nonnull
    public Expression getExpression() {
      return expression;
    }

    public boolean isExplicit() {
      return myExplicit;
    }
  }

  public static class TypedExpression extends Expression {
    public static final byte PREC = 0;
    public Expression expression;
    public Expression type;

    public TypedExpression(Object data, Expression expression, Expression type) {
      super(data);
      this.expression = expression;
      this.type = type;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitTyped(this, params);
    }
  }

  public static class AppExpression extends Expression {
    public static final byte PREC = 11;
    private Expression myFunction;
    private List<Argument> myArguments;

    private AppExpression(Object data, Expression function, List<Argument> arguments) {
      super(data);
      myFunction = function;
      myArguments = arguments;
    }

    public static Expression make(Object data, Expression function, List<Argument> arguments) {
      if (arguments.isEmpty()) {
        return function;
      }
      if (function instanceof Concrete.AppExpression) {
        ((AppExpression) function).myArguments.addAll(arguments);
        return function;
      }
      return new AppExpression(data, function, arguments);
    }

    public static Expression make(Object data, Expression function, Expression argument, boolean isExplicit) {
      if (function instanceof Concrete.AppExpression) {
        ((AppExpression) function).myArguments.add(new Argument(argument, isExplicit));
        return function;
      }

      List<Argument> arguments = new ArrayList<>();
      arguments.add(new Argument(argument, isExplicit));
      return new AppExpression(data, function, arguments);
    }

    @Nonnull
    public Expression getFunction() {
      return myFunction;
    }

    public void setFunction(Expression function) {
      if (function instanceof AppExpression) {
        myFunction = ((AppExpression) function).myFunction;
        ((AppExpression) function).getArguments().addAll(myArguments);
        myArguments = ((AppExpression) function).getArguments();
      } else {
        myFunction = function;
      }
    }

    @Nonnull
    public List<Argument> getArguments() {
      return myArguments;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitApp(this, params);
    }
  }

  public static class BinOpSequenceElem {
    public Expression expression;
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

  public static class ReferenceExpression extends Expression implements Reference {
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

  public static class ThisExpression extends Expression {
    public static final byte PREC = 12;
    private Referable myReferent;

    public ThisExpression(Object data, Referable referable) {
      super(data);
      myReferent = referable;
    }

    @Nonnull
    public Referable getReferent() {
      return myReferent;
    }

    public void setReferent(Referable referent) {
      myReferent = referent;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitThis(this, params);
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
    public static final byte PREC = 11;
    private Expression myBaseClassExpression;
    private final List<ClassFieldImpl> myDefinitions;

    private ClassExtExpression(Object data, Expression baseClassExpression, List<ClassFieldImpl> definitions) {
      super(data);
      this.myBaseClassExpression = baseClassExpression;
      myDefinitions = definitions;
    }

    public static ClassExtExpression make(Object data, Expression baseClassExpression, List<ClassFieldImpl> definitions) {
      if (baseClassExpression instanceof ClassExtExpression) {
        ((ClassExtExpression) baseClassExpression).getStatements().addAll(definitions);
        return (ClassExtExpression) baseClassExpression;
      } else {
        return new ClassExtExpression(data, baseClassExpression, definitions);
      }
    }

    @Nonnull
    public Expression getBaseClassExpression() {
      return myBaseClassExpression;
    }

    public void setBaseClassExpression(Expression baseClassExpression) {
      if (baseClassExpression instanceof ClassExtExpression) {
        myBaseClassExpression = ((ClassExtExpression) baseClassExpression).getBaseClassExpression();
        myDefinitions.addAll(0, ((ClassExtExpression) baseClassExpression).myDefinitions);
      } else {
        myBaseClassExpression = baseClassExpression;
      }
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
    public Expression implementation;
    public final List<ClassFieldImpl> subClassFieldImpls;

    public ClassFieldImpl(Object data, Referable implementedField, Expression implementation, List<ClassFieldImpl> subClassFieldImpls) {
      super(data);
      myImplementedField = implementedField;
      this.implementation = implementation;
      this.subClassFieldImpls = subClassFieldImpls;
    }

    public Referable getImplementedField() {
      return myImplementedField;
    }

    public void setImplementedField(Referable newImplementedField) {
      myImplementedField = newImplementedField;
    }
  }

  public static class NewExpression extends Expression {
    public static final byte PREC = 11;
    public Expression expression;

    public NewExpression(Object data, Expression expression) {
      super(data);
      this.expression = expression;
    }

    @Nonnull
    public Expression getExpression() {
      return expression;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitNew(this, params);
    }
  }

  public static class GoalExpression extends Expression {
    public static final byte PREC = 12;
    private final String myName;
    public Expression expression;

    public GoalExpression(Object data, String name, Expression expression) {
      super(data);
      myName = name;
      this.expression = expression;
    }

    public String getName() {
      return myName;
    }

    public Expression getExpression() {
      return expression;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitGoal(this, params);
    }
  }

  public static class HoleExpression extends Expression {
    public static final byte PREC = 12;

    public HoleExpression(Object data) {
      super(data);
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitHole(this, params);
    }

    public LocalError getError() {
      return null;
    }
  }

  public static class ErrorHoleExpression extends HoleExpression {
    private final LocalError myError;

    public ErrorHoleExpression(Object data, LocalError error) {
      super(data);
      myError = error;
    }

    @Override
    public LocalError getError() {
      return myError;
    }
  }

  public static class LamExpression extends Expression {
    public static final byte PREC = -5;
    private final List<Parameter> myArguments;
    public Expression body;

    public LamExpression(Object data, List<Parameter> arguments, Expression body) {
      super(data);
      myArguments = arguments;
      this.body = body;
    }

    @Nonnull
    public List<Parameter> getParameters() {
      return myArguments;
    }

    @Nonnull
    public Expression getBody() {
      return body;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitLam(this, params);
    }
  }

  public static class LetClause implements SourceNode {
    private final List<Parameter> myParameters;
    public Expression resultType;
    public Expression term;
    private final Referable myReferable;

    public LetClause(Referable referable, List<Parameter> parameters, Expression resultType, Expression term) {
      myParameters = parameters;
      this.resultType = resultType;
      this.term = term;
      myReferable = referable;
    }

    @Nonnull
    @Override
    public Referable getData() {
      return myReferable;
    }

    @Nonnull
    public Expression getTerm() {
      return term;
    }

    @Nonnull
    public List<Parameter> getParameters() {
      return myParameters;
    }

    public Expression getResultType() {
      return resultType;
    }

    @Override
    public void prettyPrint(StringBuilder builder, PrettyPrinterConfig ppConfig) {
      new PrettyPrintVisitor(builder, 0, !ppConfig.isSingleLine()).prettyPrintLetClause(this, false);
    }
  }

  public static class LetExpression extends Expression {
    public static final byte PREC = -9;
    private final List<LetClause> myClauses;
    public Expression expression;

    public LetExpression(Object data, List<LetClause> clauses, Expression expression) {
      super(data);
      myClauses = clauses;
      this.expression = expression;
    }

    @Nonnull
    public List<LetClause> getClauses() {
      return myClauses;
    }

    @Nonnull
    public Expression getExpression() {
      return expression;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitLet(this, params);
    }
  }

  public static class PiExpression extends Expression {
    public static final byte PREC = -4;
    private final List<TypeParameter> myParameters;
    public Expression codomain;

    public PiExpression(Object data, List<TypeParameter> parameters, Expression codomain) {
      super(data);
      myParameters = parameters;
      this.codomain = codomain;
    }

    @Nonnull
    public List<TypeParameter> getParameters() {
      return myParameters;
    }

    @Nonnull
    public Expression getCodomain() {
      return codomain;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitPi(this, params);
    }
  }

  public static class SigmaExpression extends Expression {
    public static final byte PREC = -3;
    private final List<TypeParameter> myParameters;

    public SigmaExpression(Object data, List<TypeParameter> parameters) {
      super(data);
      myParameters = parameters;
    }

    @Nonnull
    public List<TypeParameter> getParameters() {
      return myParameters;
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
    public Expression expression;
    private final int myField;

    public ProjExpression(Object data, Expression expression, int field) {
      super(data);
      this.expression = expression;
      myField = field;
    }

    @Nonnull
    public Expression getExpression() {
      return expression;
    }

    public int getField() {
      return myField;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitProj(this, params);
    }
  }

  public static class CaseArgument {
    public @Nonnull Expression expression;
    public final @Nullable Referable referable;
    public @Nullable Expression type;

    public CaseArgument(@Nonnull Expression expression, @Nullable Referable referable, @Nullable Expression type) {
      this.expression = expression;
      this.referable = referable;
      this.type = type;
    }
  }

  public static class CaseExpression extends Expression {
    public static final byte PREC = -8;
    private final List<CaseArgument> myArguments;
    private Expression myResultType;
    private Expression myResultTypeLevel;
    private final List<FunctionClause> myClauses;

    public CaseExpression(Object data, List<CaseArgument> arguments, Expression resultType, Expression resultTypeLevel, List<FunctionClause> clauses) {
      super(data);
      myArguments = arguments;
      myResultType = resultType;
      myResultTypeLevel = resultTypeLevel;
      myClauses = clauses;
    }

    @Nonnull
    public List<? extends CaseArgument> getArguments() {
      return myArguments;
    }

    @Nullable
    public Expression getResultType() {
      return myResultType;
    }

    public void setResultType(Expression resultType) {
      myResultType = resultType;
    }

    @Nullable
    public Expression getResultTypeLevel() {
      return myResultTypeLevel;
    }

    public void setResultTypeLevel(Expression resultTypeLevel) {
      myResultTypeLevel = resultTypeLevel;
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

  public static class FunctionClause extends Clause {
    public Expression expression;

    public FunctionClause(Object data, List<Pattern> patterns, Expression expression) {
      super(data, patterns);
      this.expression = expression;
    }

    @Nullable
    public Expression getExpression() {
      return expression;
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

  public static Collection<? extends TypeParameter> getParameters(ReferableDefinition definition) {
    if (definition instanceof FunctionDefinition) {
      return ((FunctionDefinition) definition).getParameters();
    }
    if (definition instanceof Instance) {
      return ((Instance) definition).getParameters();
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
    if (definition instanceof ClassDefinition) {
      List<Concrete.TypeParameter> parameters = new ArrayList<>();
      for (ClassField field : ((ClassDefinition) definition).getFields()) {
        if (field.getData().isParameterField()) {
          Expression type = field.getResultType();
          if (!field.getParameters().isEmpty()) {
            type = new PiExpression(field.getParameters().get(0).getData(), field.getParameters(), type);
          }
          parameters.add(new Concrete.TypeParameter(field.getData(), field.getData().isExplicitField(), type));
        }
      }
      return parameters;
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

  public enum Resolved { NOT_RESOLVED, TYPE_CLASS_REFERENCES_RESOLVED, RESOLVED }

  public static abstract class Definition extends ReferableDefinition {
    Resolved myResolved = Resolved.TYPE_CLASS_REFERENCES_RESOLVED;
    public TCClassReferable enclosingClass;
    private boolean myHasErrors = false;

    public Definition(TCReferable referable) {
      super(referable);
    }

    public boolean hasErrors() {
      return myHasErrors;
    }

    public void setHasErrors() {
      myHasErrors = true;
    }

    public Resolved getResolved() {
      return myResolved;
    }

    public void setResolved() {
      myResolved = Resolved.RESOLVED;
    }

    public void setTypeClassReferencesResolved() {
      if (myResolved == Resolved.NOT_RESOLVED) {
        myResolved = Resolved.TYPE_CLASS_REFERENCES_RESOLVED;
      }
    }

    @Nonnull
    @Override
    public Definition getRelatedDefinition() {
      return this;
    }

    public List<TCReferable> getUsedDefinitions() {
      return Collections.emptyList();
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
    private final boolean myRecord;
    private final List<ReferenceExpression> mySuperClasses;
    private final List<ClassField> myFields;
    private final List<ClassFieldImpl> myImplementations;
    private TCFieldReferable myCoercingField;
    private boolean myForcedCoercingField;
    private List<TCReferable> myUsedDefinitions = Collections.emptyList();

    public ClassDefinition(TCClassReferable referable, boolean isRecord, List<ReferenceExpression> superClasses, List<ClassField> fields, List<ClassFieldImpl> implementations) {
      super(referable);
      myRecord = isRecord;
      myResolved = Resolved.NOT_RESOLVED;
      mySuperClasses = superClasses;
      myFields = fields;
      myImplementations = implementations;
    }

    @Nonnull
    @Override
    public TCClassReferable getData() {
      return (TCClassReferable) super.getData();
    }

    public boolean isRecord() {
      return myRecord;
    }

    @Nullable
    public TCFieldReferable getCoercingField() {
      return myCoercingField;
    }

    public boolean isForcedCoercingField() {
      return myForcedCoercingField;
    }

    public void setCoercingField(TCFieldReferable coercingField, boolean isForced) {
      if (myCoercingField == null) {
        myCoercingField = coercingField;
        myForcedCoercingField = isForced;
      }
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
    public List<TCReferable> getUsedDefinitions() {
      return myUsedDefinitions;
    }

    public void setUsedDefinitions(List<TCReferable> usedDefinitions) {
      myUsedDefinitions = usedDefinitions;
    }

    @Override
    public <P, R> R accept(ConcreteDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClass(this, params);
    }
  }

  public static class ClassField extends ReferableDefinition {
    private final ClassDefinition myParentClass;
    private final boolean myExplicit;
    private final ClassFieldKind myKind;
    private final List<TypeParameter> myParameters;
    private Expression myResultType;
    private Expression myResultTypeLevel;

    public ClassField(TCFieldReferable referable, ClassDefinition parentClass, boolean isExplicit, ClassFieldKind kind, List<TypeParameter> parameters, Expression resultType, Expression resultTypeLevel) {
      super(referable);
      myParentClass = parentClass;
      myExplicit = isExplicit;
      myKind = kind;
      myParameters = parameters;
      myResultType = resultType;
      myResultTypeLevel = resultTypeLevel;
    }

    @Nonnull
    @Override
    public TCFieldReferable getData() {
      return (TCFieldReferable) super.getData();
    }

    public boolean isExplicit() {
       return myExplicit;
    }

    public ClassFieldKind getKind() {
      return myKind;
    }

    @Nonnull
    public List<TypeParameter> getParameters() {
      return myParameters;
    }

    @Nonnull
    public Expression getResultType() {
      return myResultType;
    }

    public void setResultType(Expression resultType) {
      myResultType = resultType;
    }

    @Nullable
    public Expression getResultTypeLevel() {
      return myResultTypeLevel;
    }

    public void setResultTypeLevel(Expression resultTypeLevel) {
      myResultTypeLevel = resultTypeLevel;
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
    FunctionBody(Object data) {
      super(data);
    }

    @Nullable
    public Expression getTerm() {
      return null;
    }

    @Nonnull
    public List<ClassFieldImpl> getClassFieldImpls() {
      return Collections.emptyList();
    }

    @Nonnull
    public List<? extends ReferenceExpression> getEliminatedReferences() {
      return Collections.emptyList();
    }

    @Nonnull
    public List<FunctionClause> getClauses() {
      return Collections.emptyList();
    }
  }

  public static class TermFunctionBody extends FunctionBody {
    private Expression myTerm;

    public TermFunctionBody(Object data, Expression term) {
      super(data);
      myTerm = term;
    }

    @Override
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

  public static class CoelimFunctionBody extends FunctionBody {
    private final List<ClassFieldImpl> myClassFieldImpls;

    public CoelimFunctionBody(Object data, List<ClassFieldImpl> classFieldImpls) {
      super(data);
      myClassFieldImpls = classFieldImpls;
    }

    @Nonnull
    public List<ClassFieldImpl> getClassFieldImpls() {
      return myClassFieldImpls;
    }
  }

  public static class FunctionDefinition extends Definition {
    private final List<TelescopeParameter> myParameters;
    private Expression myResultType;
    private Expression myResultTypeLevel;
    private final FunctionBody myBody;
    private final Kind myKind;

    public enum Kind {
      COERCE, LEVEL,
      FUNC {
        @Override
        public boolean isUse() {
          return false;
        }
      },
      LEMMA {
        @Override
        public boolean isUse() {
          return false;
        }
      };

      public boolean isUse() {
        return true;
      }
    }

    public FunctionDefinition(Kind kind, TCReferable referable, List<TelescopeParameter> parameters, Expression resultType, Expression resultTypeLevel, FunctionBody body) {
      super(referable);
      myKind = kind;
      myResolved = Resolved.NOT_RESOLVED;
      myParameters = parameters;
      myResultType = resultType;
      myResultTypeLevel = resultTypeLevel;
      myBody = body;
    }

    public Kind getKind() {
      return myKind;
    }

    public TCReferable getUseParent() {
      return null;
    }

    @Nonnull
    public List<TelescopeParameter> getParameters() {
      return myParameters;
    }

    @Nullable
    public Expression getResultType() {
      return myResultType;
    }

    public void setResultType(Expression resultType) {
      myResultType = resultType;
    }

    @Nullable
    public Expression getResultTypeLevel() {
      return myResultTypeLevel;
    }

    public void setResultTypeLevel(Expression resultTypeLevel) {
      myResultTypeLevel = resultTypeLevel;
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

  public static class UseDefinition extends FunctionDefinition {
    private final TCReferable myCoerceParent;

    private UseDefinition(Kind kind, TCReferable referable, List<TelescopeParameter> parameters, Expression resultType, Expression resultTypeLevel, FunctionBody body, TCReferable coerceParent) {
      super(kind, referable, parameters, resultType, resultTypeLevel, body);
      myCoerceParent = coerceParent;
    }

    public static FunctionDefinition make(Kind kind, TCReferable referable, List<TelescopeParameter> parameters, Expression resultType, Expression resultTypeLevel, FunctionBody body, LocatedReferable coerceParent) {
      return coerceParent instanceof TCReferable && kind.isUse() ? new UseDefinition(kind, referable, parameters, resultType, resultTypeLevel, body, (TCReferable) coerceParent) : new FunctionDefinition(kind.isUse() ? Kind.FUNC : kind, referable, parameters, resultType, resultTypeLevel, body);
    }

    public TCReferable getUseParent() {
      return myCoerceParent;
    }
  }

  public static class DataDefinition extends Definition {
    private final List<TypeParameter> myParameters;
    private final List<ReferenceExpression> myEliminatedReferences;
    private final List<ConstructorClause> myConstructorClauses;
    private final boolean myIsTruncated;
    private final UniverseExpression myUniverse;
    private List<TCReferable> myUsedDefinitions = Collections.emptyList();

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
    public List<TCReferable> getUsedDefinitions() {
      return myUsedDefinitions;
    }

    public void setUsedDefinitions(List<TCReferable> usedDefinitions) {
      myUsedDefinitions = usedDefinitions;
    }

    @Override
    public <P, R> R accept(ConcreteDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitData(this, params);
    }
  }

  public static abstract class Clause extends SourceNodeImpl {
    private final List<Pattern> myPatterns;

    public Clause(Object data, List<Pattern> patterns) {
      super(data);
      myPatterns = patterns;
    }

    public List<Pattern> getPatterns() {
      return myPatterns;
    }
  }

  public static class ConstructorClause extends Clause {
    private final List<Constructor> myConstructors;

    public ConstructorClause(Object data, List<Pattern> patterns, List<Constructor> constructors) {
      super(data, patterns);
      myConstructors = constructors;
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
    private Expression myResultType;

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

    public Expression getResultType() {
      return myResultType;
    }

    public void setResultType(Expression resultType) {
      myResultType = resultType;
    }

    @Override
    public <P, R> R accept(ConcreteReferableDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitConstructor(this, params);
    }
  }

  // Class synonyms

  public static ReferenceExpression getReferenceExpressionInType(Expression type) {
    if (type instanceof Concrete.ClassExtExpression) {
      type = ((ClassExtExpression) type).myBaseClassExpression;
    }
    if (type instanceof Concrete.AppExpression) {
      type = ((AppExpression) type).getFunction();
    }
    return type instanceof ReferenceExpression ? (ReferenceExpression) type : null;
  }

  public static class Instance extends Definition {
    private final List<TelescopeParameter> myParameters;
    private Expression myResultType;
    private final List<ClassFieldImpl> myClassFieldImpls;

    public Instance(TCReferable referable, List<TelescopeParameter> parameters, Expression classRef, List<ClassFieldImpl> classFieldImpls) {
      super(referable);
      myResolved = Resolved.NOT_RESOLVED;
      myParameters = parameters;
      myResultType = classRef;
      myClassFieldImpls = classFieldImpls;
    }

    @Nonnull
    public List<TelescopeParameter> getParameters() {
      return myParameters;
    }

    @Nonnull
    public Expression getResultType() {
      return myResultType;
    }

    @Nullable
    public ReferenceExpression getReferenceExpressionInType() {
      return Concrete.getReferenceExpressionInType(myResultType);
    }

    @Nullable
    public Referable getReferenceInType() {
      ReferenceExpression type = getReferenceExpressionInType();
      return type == null ? null : type.myReferent;
    }

    public void setResultType(Expression resultType) {
      myResultType = resultType;
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

  public static class NumberPattern extends Pattern {
    public final static int MAX_VALUE = 100;
    private final int myNumber;

    public NumberPattern(Object data, int number) {
      super(data);
      myNumber = number;
    }

    public int getNumber() {
      return myNumber;
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

  public static class ConstructorPattern extends Pattern {
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
    public List<Pattern> getPatterns() {
      return myArguments;
    }
  }

  public static class TuplePattern extends Pattern {
    private final List<Pattern> myPatterns;

    public TuplePattern(Object data, List<Pattern> patterns) {
      super(data);
      myPatterns = patterns;
    }

    public TuplePattern(Object data, boolean isExplicit, List<Pattern> patterns) {
      super(data);
      setExplicit(isExplicit);
      myPatterns = patterns;
    }

    public List<Pattern> getPatterns() {
      return myPatterns;
    }
  }
}
