package org.arend.term.concrete;

import org.arend.core.context.binding.inference.BaseInferenceVariable;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.ext.concrete.*;
import org.arend.ext.concrete.expr.*;
import org.arend.ext.error.GeneralError;
import org.arend.ext.error.LocalError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.reference.Precedence;
import org.arend.naming.reference.*;
import org.arend.term.ClassFieldKind;
import org.arend.term.Fixity;
import org.arend.term.FunctionKind;
import org.arend.term.prettyprint.PrettyPrintVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class Concrete {
  private Concrete() {}

  public interface SourceNode extends ConcreteSourceNode {
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
    public void prettyPrint(StringBuilder builder, PrettyPrinterConfig ppConfig) {
      PrettyPrintVisitor.prettyPrint(builder, this); // TODO[pretty]: implement this properly
    }
  }

  // Parameters

  public static abstract class Parameter extends SourceNodeImpl implements org.arend.naming.reference.Parameter, ConcreteParameter {
    private boolean myExplicit;

    public Parameter(Object data, boolean explicit) {
      super(data);
      myExplicit = explicit;
    }

    @Override
    public boolean isExplicit() {
      return myExplicit;
    }

    public void setExplicit(boolean explicit) {
      myExplicit = explicit;
    }

    @Nonnull
    @Override
    public ConcreteParameter implicit() {
      myExplicit = false;
      return this;
    }

    @Override
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

  public static abstract class Expression extends SourceNodeImpl implements ConcreteExpression {
    public static final byte PREC = -12;

    public Expression(Object data) {
      super(data);
    }

    public abstract <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params);

    public ReferenceExpression getUnderlyingReferenceExpression() {
      Expression expr = this;
      if (expr instanceof ClassExtExpression) {
        expr = ((ClassExtExpression) expr).getBaseClassExpression();
      }
      if (expr instanceof AppExpression) {
        expr = ((AppExpression) expr).getFunction();
      }
      return expr instanceof ReferenceExpression ? (ReferenceExpression) expr : null;
    }

    public Referable getUnderlyingReferable() {
      ReferenceExpression expr = getUnderlyingReferenceExpression();
      return expr == null ? null : expr.getReferent();
    }

    public TCClassReferable getUnderlyingTypeClass() {
      Referable ref = getUnderlyingReferable();
      return ref instanceof TCClassReferable && !((TCClassReferable) ref).isRecord() ? (TCClassReferable) ref : null;
    }

    @Nonnull
    @Override
    public ConcreteExpression app(@Nonnull ConcreteExpression argument) {
      if (!(argument instanceof Expression)) {
        throw new IllegalArgumentException();
      }
      return AppExpression.make(null, this, (Expression) argument, true);
    }

    @Nonnull
    @Override
    public ConcreteExpression appImp(@Nonnull ConcreteExpression argument) {
      if (!(argument instanceof Expression)) {
        throw new IllegalArgumentException();
      }
      return AppExpression.make(null, this, (Expression) argument, false);
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      accept(new PrettyPrintVisitor(builder, 0), new Precedence(Expression.PREC));
      return builder.toString();
    }
  }

  public static Expression makeRightSection(Object data, Referable function, Referable firstArg, Expression secondArg) {
    List<Argument> arguments = new ArrayList<>(2);
    arguments.add(new Argument(new ReferenceExpression(data, firstArg), true));
    arguments.add(new Argument(secondArg, true));
    return new LamExpression(data, Collections.singletonList(new NameParameter(data, true, firstArg)), AppExpression.make(data, new ReferenceExpression(data, function), arguments));
  }

  public static class Argument implements ConcreteArgument {
    public Expression expression;
    private boolean myExplicit;

    public Argument(Expression expression, boolean explicit) {
      this.expression = expression;
      myExplicit = explicit;
    }

    @Override
    @Nonnull
    public Expression getExpression() {
      return expression;
    }

    @Override
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

  public static class AppExpression extends Expression implements ConcreteAppExpression {
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

    @Override
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

    @Override
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

    public BinOpSequenceElem(@Nonnull Expression expression, Fixity fixity, boolean isExplicit) {
      this.expression = expression;
      this.fixity = fixity != Fixity.UNKNOWN ? fixity
        : !isExplicit ? Fixity.NONFIX
        : expression instanceof FixityReferenceExpression
          ? ((FixityReferenceExpression) expression).fixity
          : expression instanceof ReferenceExpression ? Fixity.UNKNOWN : Fixity.NONFIX;
      if (isExplicit && fixity == Fixity.UNKNOWN && expression instanceof FixityReferenceExpression) {
        ((FixityReferenceExpression) expression).fixity = Fixity.NONFIX;
      }
      this.isExplicit = isExplicit;
    }

    // Constructor for the first element in a BinOpSequence
    public BinOpSequenceElem(@Nonnull Expression expression) {
      this.expression = expression;
      this.fixity = expression instanceof FixityReferenceExpression ? ((FixityReferenceExpression) expression).fixity : Fixity.NONFIX;
      if (expression instanceof FixityReferenceExpression) {
        ((FixityReferenceExpression) expression).fixity = Fixity.NONFIX;
      }
      this.isExplicit = true;
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

  public static class ReferenceExpression extends Expression implements Reference, ConcreteReferenceExpression {
    public static final byte PREC = 12;
    private Referable myReferent;
    private final Concrete.LevelExpression myPLevel;
    private final Concrete.LevelExpression myHLevel;

    public ReferenceExpression(Object data, Referable referable, LevelExpression pLevel, LevelExpression hLevel) {
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
    @Override
    public Referable getReferent() {
      return myReferent;
    }

    public void setReferent(Referable referent) {
      myReferent = referent;
    }

    @Override
    public LevelExpression getPLevel() {
      return myPLevel;
    }

    @Override
    public LevelExpression getHLevel() {
      return myHLevel;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitReference(this, params);
    }
  }

  public static class FixityReferenceExpression extends ReferenceExpression {
    public Fixity fixity;

    public FixityReferenceExpression(Object data, Referable referable, Fixity fixity) {
      super(data, referable);
      this.fixity = fixity;
    }

    public static ReferenceExpression make(Object data, Referable referable, Fixity fixity, LevelExpression pLevel, LevelExpression hLevel) {
      return fixity == null ? new ReferenceExpression(data, referable, pLevel, hLevel) : new FixityReferenceExpression(data, referable, fixity);
    }
  }

  public static class ThisExpression extends Expression {
    public static final byte PREC = 12;
    private Referable myReferent;

    public ThisExpression(Object data, Referable referable) {
      super(data);
      myReferent = referable;
    }

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
    private final BaseInferenceVariable myVariable;

    public InferenceReferenceExpression(Object data, BaseInferenceVariable variable) {
      super(data);
      myVariable = variable;
    }

    @Nonnull
    public BaseInferenceVariable getVariable() {
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

  public interface ClassElement extends SourceNode, ConcreteClassElement {
  }

  public interface CoClauseElement extends ClassElement {
    Referable getImplementedField();
    void setImplementedField(Referable newImplementedField);
  }

  public static class CoClauseFunctionReference extends SourceNodeImpl implements CoClauseElement {
    private Referable myImplementedField;
    private final TCReferable myFunctionReference;

    public CoClauseFunctionReference(Object data, Referable implementedField, TCReferable functionReference) {
      super(data);
      myImplementedField = implementedField;
      myFunctionReference = functionReference;
    }

    public CoClauseFunctionReference(Referable implementedField, TCReferable functionReference) {
      this(functionReference.getData(), implementedField, functionReference);
    }

    @Override
    public Referable getImplementedField() {
      return myImplementedField;
    }

    @Override
    public void setImplementedField(Referable newImplementedField) {
      myImplementedField = newImplementedField;
    }

    public TCReferable getFunctionReference() {
      return myFunctionReference;
    }
  }

  public static class ClassFieldImpl extends SourceNodeImpl implements CoClauseElement {
    private Referable myImplementedField;
    public Expression implementation;
    public final List<ClassFieldImpl> subClassFieldImpls;

    public ClassFieldImpl(Object data, Referable implementedField, Expression implementation, List<ClassFieldImpl> subClassFieldImpls) {
      super(data);
      myImplementedField = implementedField;
      this.implementation = implementation;
      this.subClassFieldImpls = subClassFieldImpls;
    }

    @Override
    public Referable getImplementedField() {
      return myImplementedField;
    }

    @Override
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

    public boolean isErrorHole() {
      return false;
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
    public boolean isErrorHole() {
      return true;
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

  public static class LetClausePattern implements SourceNode, ConcreteSinglePattern {
    private final Object myData;
    private final Referable myReferable;
    public Expression type;
    private final List<LetClausePattern> myPatterns;
    private final boolean myIgnored;

    public LetClausePattern(Referable referable, Expression type) {
      myData = referable;
      myReferable = referable;
      this.type = type;
      myPatterns = Collections.emptyList();
      myIgnored = referable == null;
    }

    public LetClausePattern(Object data, List<LetClausePattern> patterns) {
      myData = data;
      myReferable = null;
      type = null;
      myPatterns = patterns;
      myIgnored = false;
    }

    public LetClausePattern(Object data) {
      myData = data;
      myReferable = null;
      type = null;
      myPatterns = Collections.emptyList();
      myIgnored = true;
    }

    @Nullable
    @Override
    public Object getData() {
      return myData;
    }

    public boolean isIgnored() {
      return myIgnored;
    }

    @Nullable
    public Referable getReferable() {
      return myReferable;
    }

    @Nonnull
    public List<? extends LetClausePattern> getPatterns() {
      return myPatterns;
    }

    @Override
    public void prettyPrint(StringBuilder builder, PrettyPrinterConfig ppConfig) {
      new PrettyPrintVisitor(builder, 0, !ppConfig.isSingleLine()).prettyPrintLetClausePattern(this);
    }
  }

  public static class LetClause implements SourceNode, ConcreteLetClause {
    private final List<Parameter> myParameters;
    public Expression resultType;
    public Expression term;
    private final LetClausePattern myPattern;

    public LetClause(Referable referable, List<Parameter> parameters, Expression resultType, Expression term) {
      myParameters = parameters;
      this.resultType = resultType;
      this.term = term;
      myPattern = new LetClausePattern(referable, (Expression) null);
    }

    public LetClause(LetClausePattern pattern, Expression resultType, Expression term) {
      myParameters = Collections.emptyList();
      this.resultType = resultType;
      this.term = term;
      myPattern = pattern;
    }

    @Nullable
    @Override
    public Object getData() {
      return myPattern.getData();
    }

    public LetClausePattern getPattern() {
      return myPattern;
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
    private final boolean myStrict;
    private final List<LetClause> myClauses;
    public Expression expression;

    public LetExpression(Object data, boolean isStrict, List<LetClause> clauses, Expression expression) {
      super(data);
      myStrict = isStrict;
      myClauses = clauses;
      this.expression = expression;
    }

    public boolean isStrict() {
      return myStrict;
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

  public static class TupleExpression extends Expression implements ConcreteTupleExpression {
    public static final byte PREC = 12;
    private final List<Expression> myFields;

    public TupleExpression(Object data, List<Expression> fields) {
      super(data);
      myFields = fields;
    }

    @Override
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

  public static class CaseArgument implements ConcreteCaseArgument {
    public @Nonnull Expression expression;
    public final @Nullable Referable referable;
    public @Nullable Expression type;
    public boolean isElim;

    public CaseArgument(@Nonnull Expression expression, @Nullable Referable referable, @Nullable Expression type) {
      this.expression = expression;
      this.referable = referable;
      this.type = type;
      isElim = false;
    }

    public CaseArgument(@Nonnull ReferenceExpression expression, @Nullable Expression type) {
      this.expression = expression;
      this.referable = null;
      this.type = type;
      isElim = true;
    }
  }

  public static class CaseExpression extends Expression {
    public static final byte PREC = -8;
    private final boolean mySCase;
    private final List<CaseArgument> myArguments;
    private Expression myResultType;
    private Expression myResultTypeLevel;
    private final List<FunctionClause> myClauses;

    public CaseExpression(Object data, boolean isSCase, List<CaseArgument> arguments, Expression resultType, Expression resultTypeLevel, List<FunctionClause> clauses) {
      super(data);
      mySCase = isSCase;
      myArguments = arguments;
      myResultType = resultType;
      myResultTypeLevel = resultTypeLevel;
      myClauses = clauses;
    }

    public boolean isSCase() {
      return mySCase;
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

  public static class EvalExpression extends Expression {
    public static final byte PREC = -7;
    private final boolean myPEVal;
    private Expression myExpression;

    public EvalExpression(Object data, boolean isPEval, Expression expression) {
      super(data);
      myPEVal = isPEval;
      myExpression = expression;
    }

    public boolean isPEval() {
      return myPEVal;
    }

    public Expression getExpression() {
      return myExpression;
    }

    public void setExpression(Expression expression) {
      myExpression = expression;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitEval(this, params);
    }
  }

  public static class FunctionClause extends Clause {
    public Expression expression;

    public FunctionClause(Object data, List<Pattern> patterns, Expression expression) {
      super(data, patterns);
      this.expression = expression;
    }

    @Override
    @Nullable
    public Expression getExpression() {
      return expression;
    }
  }

  public static class NumericLiteral extends Expression implements ConcreteNumberExpression {
    private final BigInteger myNumber;

    public NumericLiteral(Object data, BigInteger number) {
      super(data);
      myNumber = number;
    }

    @Override
    @Nonnull
    public BigInteger getNumber() {
      return myNumber;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitNumericLiteral(this, params);
    }
  }

  // Level expressions

  public static abstract class LevelExpression extends SourceNodeImpl implements ConcreteLevel {
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

  public static Collection<? extends Parameter> getParameters(ReferableDefinition definition, boolean onlyThisDef) {
    if (definition instanceof BaseFunctionDefinition) {
      return ((BaseFunctionDefinition) definition).getParameters();
    }
    if (definition instanceof DataDefinition) {
      return ((DataDefinition) definition).getParameters();
    }
    if (definition instanceof Constructor) {
      if (onlyThisDef) {
        return ((Constructor) definition).getParameters();
      } else {
        List<TypeParameter> dataTypeParameters = ((Concrete.Constructor) definition).getRelatedDefinition().getParameters();
        List<TypeParameter> parameters = ((Constructor) definition).getParameters();
        List<TypeParameter> totalParameters = new ArrayList<>(dataTypeParameters.size() + parameters.size());
        totalParameters.addAll(dataTypeParameters);
        totalParameters.addAll(parameters);
        return totalParameters;
      }
    }
    if (definition instanceof ClassDefinition) {
      List<Concrete.TypeParameter> parameters = new ArrayList<>();
      for (ClassElement element : ((ClassDefinition) definition).getElements()) {
        if (element instanceof ClassField && ((ClassField) element).getData().isParameterField()) {
          ClassField field = (ClassField) element;
          Expression type = field.getResultType();
          List<TypeParameter> fieldParams = field.getParameters();
          if (fieldParams.size() > 1 || !fieldParams.isEmpty() && !definition.isDesugarized()) {
            type = new PiExpression(field.getParameters().get(0).getData(), definition.isDesugarized() ? fieldParams.subList(1, fieldParams.size()) : fieldParams, type);
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

    public boolean isDesugarized() {
      return getRelatedDefinition().isDesugarized();
    }

    @Override
    public void prettyPrint(StringBuilder builder, PrettyPrinterConfig ppConfig) {
      builder.append(myReferable); // TODO[pretty]: implement this properly
    }

    public abstract <P, R> R accept(ConcreteReferableDefinitionVisitor<? super P, ? extends R> visitor, P params);

    @Override
    public String toString() {
      return myReferable.textRepresentation();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ReferableDefinition that = (ReferableDefinition) o;
      return myReferable.equals(that.myReferable);
    }

    @Override
    public int hashCode() {
      return myReferable.hashCode();
    }
  }

  public enum Resolved { NOT_RESOLVED, TYPE_CLASS_REFERENCES_RESOLVED, RESOLVED }

  public enum Status {
    NO_ERRORS { @Override public org.arend.core.definition.Definition.TypeCheckingStatus getTypecheckingStatus() { return org.arend.core.definition.Definition.TypeCheckingStatus.NO_ERRORS; } },
    HAS_WARNINGS { @Override public org.arend.core.definition.Definition.TypeCheckingStatus getTypecheckingStatus() { return org.arend.core.definition.Definition.TypeCheckingStatus.HAS_WARNINGS; } },
    HAS_ERRORS { @Override public org.arend.core.definition.Definition.TypeCheckingStatus getTypecheckingStatus() { return org.arend.core.definition.Definition.TypeCheckingStatus.HAS_ERRORS; } };

    public abstract org.arend.core.definition.Definition.TypeCheckingStatus getTypecheckingStatus();

    Status max(Status another) {
      return ordinal() >= another.ordinal() ? this : another;
    }
  }

  public static abstract class Definition extends ReferableDefinition {
    Resolved myResolved = Resolved.TYPE_CLASS_REFERENCES_RESOLVED;
    public TCClassReferable enclosingClass;
    private Status myStatus = Status.NO_ERRORS;
    private boolean myDesugarized = false;
    private boolean myRecursive = false;

    public Definition(TCReferable referable) {
      super(referable);
    }

    public Status getStatus() {
      return myStatus;
    }

    public void setStatus(Status status) {
      myStatus = myStatus.max(status);
    }

    public void setStatus(GeneralError.Level level) {
      if (level == GeneralError.Level.ERROR) {
        myStatus = myStatus.max(Concrete.Status.HAS_ERRORS);
      } else if (level.ordinal() >= GeneralError.Level.WARNING_UNUSED.ordinal()) {
        myStatus = myStatus.max(Concrete.Status.HAS_WARNINGS);
      }
    }

    public boolean isRecursive() {
      return myRecursive;
    }

    public void setRecursive(boolean isRecursive) {
      myRecursive = isRecursive;
    }

    @Override
    public boolean isDesugarized() {
      return myDesugarized;
    }

    public void setDesugarized() {
      myDesugarized = true;
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
    private final boolean myWithoutClassifying;
    private final List<ReferenceExpression> mySuperClasses;
    private final List<ClassElement> myElements;
    private TCFieldReferable myCoercingField;
    private boolean myForcedCoercingField;
    private List<TCReferable> myUsedDefinitions = Collections.emptyList();

    public ClassDefinition(TCClassReferable referable, boolean isRecord, boolean withoutClassifying, List<ReferenceExpression> superClasses, List<ClassElement> elements) {
      super(referable);
      myRecord = isRecord;
      myWithoutClassifying = withoutClassifying;
      myResolved = Resolved.NOT_RESOLVED;
      mySuperClasses = superClasses;
      myElements = elements;
    }

    @Nonnull
    @Override
    public TCClassReferable getData() {
      return (TCClassReferable) super.getData();
    }

    public boolean isRecord() {
      return myRecord;
    }

    public boolean withoutClassifying() {
      return myWithoutClassifying;
    }

    @Nullable
    public TCFieldReferable getCoercingField() {
      return myCoercingField;
    }

    public boolean isForcedCoercingField() {
      return myForcedCoercingField;
    }

    public void setCoercingField(TCFieldReferable coercingField, boolean isForced) {
      myCoercingField = coercingField;
      myForcedCoercingField = isForced;
    }

    @Nonnull
    public List<ReferenceExpression> getSuperClasses() {
      return mySuperClasses;
    }

    @Nonnull
    public List<ClassElement> getElements() {
      return myElements;
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

  public static class CoClauseFunctionDefinition extends BaseFunctionDefinition {
    private final TCReferable myEnclosingDefinition;
    private Referable myImplementedField;

    public CoClauseFunctionDefinition(TCReferable referable, TCReferable enclosingDefinition, Referable implementedField, List<Parameter> parameters, Expression resultType, Expression resultTypeLevel, FunctionBody body) {
      super(referable, parameters, resultType, resultTypeLevel, body);
      myEnclosingDefinition = enclosingDefinition;
      myImplementedField = implementedField;
    }

    @Nonnull
    @Override
    public FunctionKind getKind() {
      return FunctionKind.COCLAUSE_FUNC;
    }

    @Nonnull
    public TCReferable getEnclosingDefinition() {
      return myEnclosingDefinition;
    }

    public Referable getImplementedField() {
      return myImplementedField;
    }

    public void setImplementedField(Referable newImplementedField) {
      myImplementedField = newImplementedField;
    }
  }

  public interface BaseClassField extends ClassElement {
    @Nonnull List<TypeParameter> getParameters();
    @Nonnull Expression getResultType();
    void setResultType(Expression resultType);
    @Nullable Expression getResultTypeLevel();
    void setResultTypeLevel(Expression resultTypeLevel);
  }

  public static class ClassField extends ReferableDefinition implements BaseClassField {
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
    @Override
    public List<TypeParameter> getParameters() {
      return myParameters;
    }

    @Nonnull
    @Override
    public Expression getResultType() {
      return myResultType;
    }

    @Override
    public void setResultType(Expression resultType) {
      myResultType = resultType;
    }

    @Nullable
    @Override
    public Expression getResultTypeLevel() {
      return myResultTypeLevel;
    }

    @Override
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

  public static class OverriddenField extends SourceNodeImpl implements BaseClassField {
    private Referable myOverriddenField;
    private final List<TypeParameter> myParameters;
    private Expression myResultType;
    private Expression myResultTypeLevel;

    public OverriddenField(Object data, Referable overriddenField, List<TypeParameter> parameters, Expression resultType, Expression resultTypeLevel) {
      super(data);
      myOverriddenField = overriddenField;
      myParameters = parameters;
      myResultType = resultType;
      myResultTypeLevel = resultTypeLevel;
    }

    @Nonnull
    public Referable getOverriddenField() {
      return myOverriddenField;
    }

    public void setOverriddenField(Referable overriddenField) {
      myOverriddenField = overriddenField;
    }

    @Nonnull
    @Override
    public List<TypeParameter> getParameters() {
      return myParameters;
    }

    @Nonnull
    @Override
    public Expression getResultType() {
      return myResultType;
    }

    @Override
    public void setResultType(Expression resultType) {
      myResultType = resultType;
    }

    @Nullable
    @Override
    public Expression getResultTypeLevel() {
      return myResultTypeLevel;
    }

    @Override
    public void setResultTypeLevel(Expression resultTypeLevel) {
      myResultTypeLevel = resultTypeLevel;
    }

    @Override
    public Doc prettyPrint(PrettyPrinterConfig ppConfig) {
      return DocFactory.refDoc(myOverriddenField);
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
    public List<CoClauseElement> getCoClauseElements() {
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
    private final List<CoClauseElement> myCoClauseElements;

    public CoelimFunctionBody(Object data, List<CoClauseElement> coClauseElements) {
      super(data);
      myCoClauseElements = coClauseElements;
    }

    @Nonnull
    public List<CoClauseElement> getCoClauseElements() {
      return myCoClauseElements;
    }
  }

  public static abstract class BaseFunctionDefinition extends Definition {
    private final List<Parameter> myParameters;
    private Expression myResultType;
    private Expression myResultTypeLevel;
    private final FunctionBody myBody;

    public BaseFunctionDefinition(TCReferable referable, List<Parameter> parameters, Expression resultType, Expression resultTypeLevel, FunctionBody body) {
      super(referable);
      myResolved = Resolved.NOT_RESOLVED;
      myParameters = parameters;
      myResultType = resultType;
      myResultTypeLevel = resultTypeLevel;
      myBody = body;
    }

    @Nonnull
    public abstract FunctionKind getKind();

    public TCReferable getUseParent() {
      return null;
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

  public static class FunctionDefinition extends BaseFunctionDefinition {
    private final FunctionKind myKind;
    private List<TCReferable> myUsedDefinitions = Collections.emptyList();

    public FunctionDefinition(FunctionKind kind, TCReferable referable, List<Parameter> parameters, Expression resultType, Expression resultTypeLevel, FunctionBody body) {
      super(referable, parameters, resultType, resultTypeLevel, body);
      myKind = kind;
      myResolved = Resolved.NOT_RESOLVED;
    }

    @Override
    @Nonnull
    public FunctionKind getKind() {
      return myKind;
    }

    @Override
    public List<TCReferable> getUsedDefinitions() {
      return myUsedDefinitions;
    }

    public void setUsedDefinitions(List<TCReferable> usedDefinitions) {
      myUsedDefinitions = usedDefinitions;
    }
  }

  public static class UseDefinition extends FunctionDefinition {
    private final TCReferable myCoerceParent;

    private UseDefinition(FunctionKind kind, TCReferable referable, List<Parameter> parameters, Expression resultType, Expression resultTypeLevel, FunctionBody body, TCReferable coerceParent) {
      super(kind, referable, parameters, resultType, resultTypeLevel, body);
      myCoerceParent = coerceParent;
    }

    public static FunctionDefinition make(FunctionKind kind, TCReferable referable, List<Parameter> parameters, Expression resultType, Expression resultTypeLevel, FunctionBody body, LocatedReferable coerceParent) {
      return coerceParent instanceof TCReferable && kind.isUse() ? new UseDefinition(kind, referable, parameters, resultType, resultTypeLevel, body, (TCReferable) coerceParent) : new FunctionDefinition(kind.isUse() ? FunctionKind.FUNC : kind, referable, parameters, resultType, resultTypeLevel, body);
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

  public static abstract class Clause extends SourceNodeImpl implements PatternHolder, ConcreteClause {
    private final List<Pattern> myPatterns;

    public Clause(Object data, List<Pattern> patterns) {
      super(data);
      myPatterns = patterns;
    }

    @Override
    public List<Pattern> getPatterns() {
      return myPatterns;
    }

    @Override
    public Clause getSourceNode() {
      return this;
    }

    @Nullable
    public Expression getExpression() {
      return null;
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

  // Patterns

  public interface PatternHolder {
    List<Pattern> getPatterns();
    SourceNode getSourceNode();
  }

  public static abstract class Pattern extends SourceNodeImpl implements ConcretePattern {
    public static final byte PREC = 11;
    private boolean myExplicit;
    private final List<TypedReferable> myAsReferables;

    public Pattern(Object data, List<TypedReferable> asReferables) {
      super(data);
      myExplicit = true;
      myAsReferables = asReferables;
    }

    public boolean isExplicit() {
      return myExplicit;
    }

    public void setExplicit(boolean isExplicit) {
      myExplicit = isExplicit;
    }

    @Nonnull
    @Override
    public ConcretePattern implicit() {
      myExplicit = false;
      return this;
    }

    @Nonnull
    @Override
    public ConcretePattern as(@Nonnull ArendRef ref, @Nullable ConcreteExpression type) {
      if (!(ref instanceof Referable && type instanceof Expression)) {
        throw new IllegalArgumentException();
      }
      myAsReferables.add(new TypedReferable(null, (Referable) ref, (Expression) type));
      return this;
    }

    @Nonnull
    public List<TypedReferable> getAsReferables() {
      return myAsReferables;
    }
  }

  public static class NumberPattern extends Pattern {
    public final static int MAX_VALUE = 100;
    private final int myNumber;

    public NumberPattern(Object data, int number, List<TypedReferable> asReferables) {
      super(data, asReferables);
      myNumber = number;
    }

    public int getNumber() {
      return myNumber;
    }
  }

  public static class NamePattern extends Pattern {
    private final @Nullable Referable myReferable;
    public @Nullable Expression type;

    public NamePattern(Object data, boolean isExplicit, @Nullable Referable referable, @Nullable Expression type) {
      super(data, Collections.emptyList());
      setExplicit(isExplicit);
      myReferable = referable;
      this.type = type;
    }

    @Nullable
    public Referable getReferable() {
      return myReferable;
    }

    @Nonnull
    @Override
    public ConcretePattern as(@Nonnull ArendRef ref, @Nullable ConcreteExpression type) {
      throw new IllegalArgumentException("\\as is not allowed for variable patterns");
    }
  }

  public static class TypedReferable extends SourceNodeImpl {
    public final Referable referable;
    public Expression type;

    public TypedReferable(Object data, Referable referable, Expression type) {
      super(data);
      this.referable = referable;
      this.type = type;
    }

    @Override
    public void prettyPrint(StringBuilder builder, PrettyPrinterConfig ppConfig) {
      new PrettyPrintVisitor(builder, 0, !ppConfig.isSingleLine()).prettyPrintTypedReferable(this);
    }
  }

  public static class ConstructorPattern extends Pattern implements PatternHolder {
    private Referable myConstructor;
    private final List<Pattern> myArguments;

    public ConstructorPattern(Object data, boolean isExplicit, Referable constructor, List<Pattern> arguments, List<TypedReferable> asReferables) {
      super(data, asReferables);
      setExplicit(isExplicit);
      myConstructor = constructor;
      myArguments = arguments;
    }

    public ConstructorPattern(Object data, Referable constructor, List<Pattern> arguments, List<TypedReferable> asReferables) {
      super(data, asReferables);
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

    @Override
    @Nonnull
    public List<Pattern> getPatterns() {
      return myArguments;
    }

    @Override
    public ConstructorPattern getSourceNode() {
      return this;
    }
  }

  public static class TuplePattern extends Pattern {
    private final List<Pattern> myPatterns;

    public TuplePattern(Object data, List<Pattern> patterns, List<TypedReferable> asReferables) {
      super(data, asReferables);
      myPatterns = patterns;
    }

    public TuplePattern(Object data, boolean isExplicit, List<Pattern> patterns, List<TypedReferable> asReferables) {
      super(data, asReferables);
      setExplicit(isExplicit);
      myPatterns = patterns;
    }

    public List<Pattern> getPatterns() {
      return myPatterns;
    }
  }
}
