package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.frontend.resolving.HasOpens;
import com.jetbrains.jetpad.vclang.frontend.resolving.OpenCommand;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;
import com.jetbrains.jetpad.vclang.term.provider.SourceInfoProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Concrete {
  private Concrete() {}

  public static class SourceNode<T> implements Abstract.SourceNode, PrettyPrintable {
    private final T myData;

    public SourceNode(T data) {
      myData = data;
    }

    @Nullable
    public T getData() {
      return myData;
    }

    @Override
    public String prettyPrint(PrettyPrinterInfoProvider infoProvider) {
      return PrettyPrintVisitor.prettyPrint(this, infoProvider); // TODO[abstract]: implement this properly
    }
  }

  // Parameters

  public static class Parameter<T> extends SourceNode<T> implements Abstract.Parameter {
    private boolean myExplicit;

    public Parameter(T data, boolean explicit) {
      super(data);
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

  public static class NameParameter<T> extends Parameter<T> implements Abstract.NameParameter {
    private final String myName;

    public NameParameter(T data, boolean explicit, String name) {
      super(data, explicit);
      myName = name;
    }

    @Nullable
    @Override
    public String getName() {
      return myName;
    }
  }

  public static class TypeParameter<T> extends Parameter<T> implements Abstract.TypeParameter {
    private final Expression<T> myType;

    public TypeParameter(T data, boolean explicit, Expression<T> type) {
      super(data, explicit);
      myType = type;
    }

    public TypeParameter(boolean explicit, Expression<T> type) {
      this(type.getData(), explicit, type);
    }

    @Nonnull
    @Override
    public Expression<T> getType() {
      return myType;
    }
  }

  public static class TelescopeParameter<T> extends TypeParameter<T> implements Abstract.TelescopeParameter {
    private final List<? extends Referable> myReferableList;

    public TelescopeParameter(T data, boolean explicit, List<? extends Referable> referableList, Expression<T> type) {
      super(data, explicit, type);
      myReferableList = referableList;
    }

    @Nonnull
    @Override
    public List<? extends Referable> getReferableList() {
      return myReferableList;
    }
  }

  // Expressions

  public static GlobalReferable getUnderlyingClassDef(Expression expr) { // TODO[abstract]
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

  public static ClassView getUnderlyingClassView(Expression expr) {
    if (expr instanceof ReferenceExpression) {
      Referable definition = ((ReferenceExpression) expr).getReferent();
      if (definition instanceof ClassView) {
        return (ClassView) definition;
      }
    }

    if (expr instanceof ClassExtExpression) {
      return getUnderlyingClassView(((ClassExtExpression) expr).getBaseClassExpression());
    } else {
      return null;
    }
  }

  public static abstract class Expression<T> extends SourceNode<T> {
    public static final byte PREC = -12;

    public Expression(T data) {
      super(data);
    }

    public abstract <P, R> R accept(ConcreteExpressionVisitor<T, ? super P, ? extends R> visitor, P params);

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      accept(new PrettyPrintVisitor<>(builder, SourceInfoProvider.TRIVIAL, 0), Expression.PREC);
      return builder.toString();
    }
  }

  public static class Argument<T> {
    private final Expression<T> myExpression;
    private final boolean myExplicit;

    public Argument(Expression<T> expression, boolean explicit) {
      myExpression = expression;
      myExplicit = explicit;
    }

    @Nonnull
    public Expression<T> getExpression() {
      return myExpression;
    }

    public boolean isExplicit() {
      return myExplicit;
    }
  }

  public static class AppExpression<T> extends Expression<T> {
    public static final byte PREC = 11;
    private final Expression<T> myFunction;
    private final Argument<T> myArgument;

    public AppExpression(T data, Expression<T> function, Argument<T> argument) {
      super(data);
      myFunction = function;
      myArgument = argument;
    }

    @Nonnull
    public Expression<T> getFunction() {
      return myFunction;
    }

    @Nonnull
    public Argument<T> getArgument() {
      return myArgument;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitApp(this, params);
    }
  }

  public static class BinOpSequenceElem<T> {
    public final ReferenceExpression<T> binOp;
    public final Expression<T> argument;

    public BinOpSequenceElem(ReferenceExpression<T> binOp, Expression<T> argument) {
      this.binOp = binOp;
      this.argument = argument;
    }
  }

  public static class BinOpSequenceExpression<T> extends Expression<T> {
    public static final byte PREC = 0;
    private Expression<T> myLeft;
    private final List<BinOpSequenceElem<T>> mySequence;

    public BinOpSequenceExpression(T data, Expression<T> left, List<BinOpSequenceElem<T>> sequence) {
      super(data);
      myLeft = left;
      mySequence = sequence;
    }

    @Nonnull
    public Expression<T> getLeft() {
      return myLeft;
    }

    @Nonnull
    public List<BinOpSequenceElem<T>> getSequence() {
      return mySequence;
    }

    public BinOpExpression<T> makeBinOp(Expression<T> left, Referable binOp, ReferenceExpression<T> var, Expression<T> right) {
      return new BinOpExpression<>(var.getData(), left, binOp, right);
    }

    public void replace(Expression<T> expression) {
      myLeft = expression;
      mySequence.clear();
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitBinOpSequence(this, params);
    }
  }

  public static class BinOpExpression<T> extends ReferenceExpression<T> { // TODO[abstract]: replace binops with applications
    private final Expression<T> myLeft;
    private final Expression<T> myRight;

    public BinOpExpression(T data, Expression<T> left, Referable binOp, Expression<T> right) {
      super(data, binOp);
      myLeft = left;
      myRight = right;
    }

    @Nonnull
    public Expression<T> getLeft() {
      return myLeft;
    }

    @Nullable
    public Expression<T> getRight() {
      return myRight;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitBinOp(this, params);
    }
  }

  public static class ReferenceExpression<T> extends Expression<T> {
    public static final byte PREC = 12;
    private @Nullable Expression<T> myExpression;
    private Referable myReferent;

    public ReferenceExpression(T data, @Nullable Expression<T> expression, Referable referable) {
      super(data);
      myExpression = expression;
      myReferent = referable;
    }

    public ReferenceExpression(T data, Referable referable) {
      super(data);
      myExpression = null;
      myReferent = referable;
    }

    @Nullable
    public Expression<T> getExpression() {
      return myExpression;
    }

    public void setExpression(Expression<T> expression) {
      myExpression = expression;
    }

    @Nonnull
    public Referable getReferent() {
      return myReferent;
    }

    public void setReferent(Referable referent) {
      myReferent = referent;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitReference(this, params);
    }
  }

  public static class InferenceReferenceExpression<T> extends Expression<T> {
    private final InferenceVariable myVariable;

    public InferenceReferenceExpression(T data, InferenceVariable variable) {
      super(data);
      myVariable = variable;
    }

    @Nonnull
    public InferenceVariable getVariable() {
      return myVariable;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitInferenceReference(this, params);
    }
  }

  public static class ModuleCallExpression<T> extends Expression<T> {
    public static final byte PREC = 12;
    private final ModulePath myPath;
    private GlobalReferable myModule;

    public ModuleCallExpression(T data, List<String> path) {
      super(data);
      this.myPath = new ModulePath(path);
    }

    @Nonnull
    public ModulePath getPath() {
      return myPath;
    }

    public GlobalReferable getModule() {
      return myModule;
    }

    public void setModule(GlobalReferable module) {
      myModule = module;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitModuleCall(this, params);
    }
  }

  public static class ClassExtExpression<T> extends Expression<T> {
    public static final byte PREC = 12;
    private final Expression<T> myBaseClassExpression;
    private final List<ClassFieldImpl<T>> myDefinitions;

    public ClassExtExpression(T data, Expression<T> baseClassExpression, List<ClassFieldImpl<T>> definitions) {
      super(data);
      myBaseClassExpression = baseClassExpression;
      myDefinitions = definitions;
    }

    @Nonnull
    public Expression<T> getBaseClassExpression() {
      return myBaseClassExpression;
    }

    @Nonnull
    public List<ClassFieldImpl<T>> getStatements() {
      return myDefinitions;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitClassExt(this, params);
    }
  }

  public static class ClassFieldImpl<T> extends SourceNode<T> {
    private Referable myImplementedField;
    private final Expression<T> myExpression;

    public ClassFieldImpl(T data, Referable implementedField, Expression<T> expression) {
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
    public Expression<T> getImplementation() {
      return myExpression;
    }
  }

  public static class NewExpression<T> extends Expression<T> {
    public static final byte PREC = 11;
    private final Expression<T> myExpression;

    public NewExpression(T data, Expression<T> expression) {
      super(data);
      myExpression = expression;
    }

    @Nonnull
    public Expression<T> getExpression() {
      return myExpression;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitNew(this, params);
    }
  }

  public static class GoalExpression<T> extends Expression<T> {
    public static final byte PREC = 12;
    private final String myName;
    private final Expression<T> myExpression;

    public GoalExpression(T data, String name, Expression<T> expression) {
      super(data);
      myName = name;
      myExpression = expression;
    }

    public String getName() {
      return myName;
    }

    public Expression<T> getExpression() {
      return myExpression;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitGoal(this, params);
    }
  }

  public static class InferHoleExpression<T> extends Expression<T> {
    public static final byte PREC = 12;
    public InferHoleExpression(T data) {
      super(data);
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitInferHole(this, params);
    }
  }

  public static class LamExpression<T> extends Expression<T> {
    public static final byte PREC = -5;
    private final List<Parameter<T>> myArguments;
    private final Expression<T> myBody;

    public LamExpression(T data, List<Parameter<T>> arguments, Expression<T> body) {
      super(data);
      myArguments = arguments;
      myBody = body;
    }

    @Nonnull
    public List<Parameter<T>> getParameters() {
      return myArguments;
    }

    @Nonnull
    public Expression<T> getBody() {
      return myBody;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitLam(this, params);
    }
  }

  public static class LetClause<T> extends SourceNode<T> {
    private final List<Parameter<T>> myArguments;
    private final Expression<T> myResultType;
    private final Expression<T> myTerm;
    private final Referable myReferable;

    public LetClause(T data, Referable referable, List<Parameter<T>> arguments, Expression<T> resultType, Expression<T> term) {
      super(data);
      myArguments = arguments;
      myResultType = resultType;
      myTerm = term;
      myReferable = referable;
    }

    public Referable getReferable() {
      return myReferable;
    }

    @Nonnull
    public Expression<T> getTerm() {
      return myTerm;
    }

    @Nonnull
    public List<Parameter<T>> getParameters() {
      return myArguments;
    }

    public Expression<T> getResultType() {
      return myResultType;
    }
  }

  public static class LetExpression<T> extends Expression<T> {
    public static final byte PREC = -9;
    private final List<LetClause<T>> myClauses;
    private final Expression<T> myExpression;

    public LetExpression(T data, List<LetClause<T>> clauses, Expression<T> expression) {
      super(data);
      myClauses = clauses;
      myExpression = expression;
    }

    @Nonnull
    public List<LetClause<T>> getClauses() {
      return myClauses;
    }

    @Nonnull
    public Expression<T> getExpression() {
      return myExpression;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitLet(this, params);
    }
  }

  public static class PiExpression<T> extends Expression<T> {
    public static final byte PREC = -4;
    private final List<TypeParameter<T>> myArguments;
    private final Expression<T> myCodomain;

    public PiExpression(T data, List<TypeParameter<T>> arguments, Expression<T> codomain) {
      super(data);
      myArguments = arguments;
      myCodomain = codomain;
    }

    @Nonnull
    public List<TypeParameter<T>> getParameters() {
      return myArguments;
    }

    @Nonnull
    public Expression<T> getCodomain() {
      return myCodomain;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitPi(this, params);
    }
  }

  public static class SigmaExpression<T> extends Expression<T> {
    public static final byte PREC = -3;
    private final List<TypeParameter<T>> myArguments;

    public SigmaExpression(T data, List<TypeParameter<T>> arguments) {
      super(data);
      myArguments = arguments;
    }

    @Nonnull
    public List<TypeParameter<T>> getParameters() {
      return myArguments;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitSigma(this, params);
    }
  }

  public static class TupleExpression<T> extends Expression<T> {
    public static final byte PREC = 12;
    private final List<Expression<T>> myFields;

    public TupleExpression(T data, List<Expression<T>> fields) {
      super(data);
      myFields = fields;
    }

    @Nonnull
    public List<Expression<T>> getFields() {
      return myFields;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitTuple(this, params);
    }
  }

  public static class UniverseExpression<T> extends Expression<T> {
    public static final byte PREC = 12;
    private final LevelExpression<T> myPLevel;
    private final LevelExpression<T> myHLevel;

    public UniverseExpression(T data, LevelExpression<T> pLevel, LevelExpression<T> hLevel) {
      super(data);
      myPLevel = pLevel;
      myHLevel = hLevel;
    }

    @Nullable
    public LevelExpression<T> getPLevel() {
      return myPLevel;
    }

    @Nullable
    public LevelExpression<T> getHLevel() {
      return myHLevel;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitUniverse(this, params);
    }
  }

  public static class ProjExpression<T> extends Expression<T> {
    public static final byte PREC = 12;
    private final Expression<T> myExpression;
    private final int myField;

    public ProjExpression(T data, Expression<T> expression, int field) {
      super(data);
      myExpression = expression;
      myField = field;
    }

    @Nonnull
    public Expression<T> getExpression() {
      return myExpression;
    }

    public int getField() {
      return myField;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitProj(this, params);
    }
  }

  public static class CaseExpression<T> extends Expression<T> {
    public static final byte PREC = -8;
    private final List<Expression<T>> myExpressions;
    private final List<FunctionClause<T>> myClauses;

    public CaseExpression(T data, List<Expression<T>> expressions, List<FunctionClause<T>> clauses) {
      super(data);
      myExpressions = expressions;
      myClauses = clauses;
    }

    @Nonnull
    public List<Expression<T>> getExpressions() {
      return myExpressions;
    }

    @Nonnull
    public List<FunctionClause<T>> getClauses() {
      return myClauses;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitCase(this, params);
    }
  }

  public interface PatternContainer<T> {
    List<Pattern<T>> getPatterns();
  }

  public static class FunctionClause<T> extends Clause<T> {
    private final List<Pattern<T>> myPatterns;
    private final Expression<T> myExpression;

    public FunctionClause(T data, List<Pattern<T>> patterns, Expression<T> expression) {
      super(data);
      myPatterns = patterns;
      myExpression = expression;
    }

    @Nonnull
    @Override
    public List<Pattern<T>> getPatterns() {
      return myPatterns;
    }

    @Nullable
    public Expression<T> getExpression() {
      return myExpression;
    }
  }

  public static class NumericLiteral<T> extends Expression<T> {
    private final int myNumber;

    public NumericLiteral(T data, int number) {
      super(data);
      myNumber = number;
    }

    public int getNumber() {
      return myNumber;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitNumericLiteral(this, params);
    }
  }

  // Level expressions

  public static abstract class LevelExpression<T> extends SourceNode<T> {
    protected LevelExpression(T data) {
      super(data);
    }

    public abstract <P, R> R accept(ConcreteLevelExpressionVisitor<T, ? super P, ? extends R> visitor, P params);
  }

  public static class InferVarLevelExpression<T> extends LevelExpression<T> {
    private final InferenceLevelVariable myVariable;

    public InferVarLevelExpression(T data, InferenceLevelVariable variable) {
      super(data);
      myVariable = variable;
    }

    @Nonnull
    public InferenceLevelVariable getVariable() {
      return myVariable;
    }

    @Override
    public <P, R> R accept(ConcreteLevelExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitVar(this, params);
    }
  }

  public static class PLevelExpression<T> extends LevelExpression<T> {
    public PLevelExpression(T data) {
      super(data);
    }

    @Override
    public <P, R> R accept(ConcreteLevelExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitLP(this, params);
    }
  }

  public static class HLevelExpression<T> extends LevelExpression<T> {
    public HLevelExpression(T data) {
      super(data);
    }

    @Override
    public <P, R> R accept(ConcreteLevelExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitLH(this, params);
    }
  }

  public static class InfLevelExpression<T> extends LevelExpression<T> {
    public InfLevelExpression(T data) {
      super(data);
    }

    @Override
    public <P, R> R accept(ConcreteLevelExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitInf(this, params);
    }
  }

  public static class NumberLevelExpression<T> extends LevelExpression<T> {
    private final int myNumber;

    public NumberLevelExpression(T data, int number) {
      super(data);
      myNumber = number;
    }

    public int getNumber() {
      return myNumber;
    }

    @Override
    public <P, R> R accept(ConcreteLevelExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitNumber(this, params);
    }
  }

  public static class SucLevelExpression<T> extends LevelExpression<T> {
    private final LevelExpression<T> myExpression;

    public SucLevelExpression(T data, LevelExpression<T> expression) {
      super(data);
      myExpression = expression;
    }

    @Nonnull
    public LevelExpression<T> getExpression() {
      return myExpression;
    }

    @Override
    public <P, R> R accept(ConcreteLevelExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitSuc(this, params);
    }
  }

  public static class MaxLevelExpression<T> extends LevelExpression<T> {
    private final LevelExpression<T> myLeft;
    private final LevelExpression<T> myRight;

    public MaxLevelExpression(T data, LevelExpression<T> left, LevelExpression<T> right) {
      super(data);
      myLeft = left;
      myRight = right;
    }

    @Nonnull
    public LevelExpression<T> getLeft() {
      return myLeft;
    }

    @Nonnull
    public LevelExpression<T> getRight() {
      return myRight;
    }

    @Override
    public <P, R> R accept(ConcreteLevelExpressionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitMax(this, params);
    }
  }

  // Definitions

  public static <T> Collection<? extends Parameter<T>> getParameters(Definition<T> definition) {
    if (definition instanceof FunctionDefinition) {
      return ((FunctionDefinition<T>) definition).getParameters();
    }
    if (definition instanceof DataDefinition) {
      return ((DataDefinition<T>) definition).getParameters();
    }
    if (definition instanceof Constructor) {
      return ((Constructor<T>) definition).getParameters();
    }
    return null;
  }

  public static abstract class Definition<T> extends SourceNode<T> implements Abstract.Definition, GlobalReferable {
    private final String myName;
    private final Abstract.Precedence myPrecedence; // TODO[abstract]: Get rid of precedence
    private Definition<T> myParent;
    private boolean myStatic;

    public Definition(T data, String name, Abstract.Precedence precedence) {
      super(data);
      myStatic = true;
      myName = name;
      myPrecedence = precedence;
    }

    @Nullable
    @Override
    public String getName() {
      return myName;
    }

    @Nonnull
    @Override
    public Abstract.Precedence getPrecedence() {
      return myPrecedence;
    }

    @Override
    public Definition<T> getParentDefinition() {
      return myParent;
    }

    public void setParent(Definition<T> parent) {
      myParent = parent;
    }

    @Override
    public boolean isStatic() {
      return myStatic;
    }

    public void setNotStatic() {
      myStatic = false;
    }

    @Override
    public String toString() {
      return getName();
    }

    public abstract <P, R> R accept(ConcreteDefinitionVisitor<T, ? super P, ? extends R> visitor, P params);
  }

  public static class SuperClass<T> extends SourceNode<T> implements Abstract.SuperClass {
    private final Expression<T> mySuperClass;

    public SuperClass(T data, Expression<T> superClass) {
      super(data);
      mySuperClass = superClass;
    }

    @Nonnull
    @Override
    public Expression<T> getSuperClass() {
      return mySuperClass;
    }
  }

  public interface DefinitionCollection<T> extends Abstract.DefinitionCollection {
    @Nonnull Collection<? extends Definition<T>> getGlobalDefinitions();
  }

  public static class ClassDefinition<T> extends Definition<T> implements Abstract.ClassDefinition, StatementCollection<T> {
    private final List<TypeParameter<T>> myPolyParameters;
    private final List<SuperClass<T>> mySuperClasses;
    private final List<ClassField<T>> myFields;
    private final List<Implementation<T>> myImplementations;
    private final List<Statement<T>> myGlobalStatements;
    private final List<Definition<T>> myInstanceDefinitions;

    public ClassDefinition(T data, String name, List<TypeParameter<T>> polyParams, List<SuperClass<T>> superClasses, List<ClassField<T>> fields, List<Implementation<T>> implementations, List<Statement<T>> globalStatements, List<Definition<T>> instanceDefinitions) {
      super(data, name, Abstract.Precedence.DEFAULT);
      myPolyParameters = polyParams;
      mySuperClasses = superClasses;
      myFields = fields;
      myImplementations = implementations;
      myGlobalStatements = globalStatements;
      myInstanceDefinitions = instanceDefinitions;
    }

    public ClassDefinition(T data, String name, List<Statement<T>> globalStatements) {
      this(data, name, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), globalStatements, Collections.emptyList());
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClass(this, params);
    }

    @Nonnull
    @Override
    public List<TypeParameter<T>> getPolyParameters() {
      return myPolyParameters;
    }

    @Nonnull
    @Override
    public List<SuperClass<T>> getSuperClasses() {
      return mySuperClasses;
    }

    @Nonnull
    @Override
    public List<ClassField<T>> getFields() {
      return myFields;
    }

    @Nonnull
    @Override
    public List<Implementation<T>> getImplementations() {
      return myImplementations;
    }

    @Nonnull
    @Override
    public List<Definition<T>> getInstanceDefinitions() {
      return myInstanceDefinitions;
    }

    @Override
    public List<Statement<T>> getGlobalStatements() {
      return myGlobalStatements;
    }

    @Override
    public <P, R> R accept(ConcreteDefinitionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitClass(this, params);
    }
  }

  public static class ClassField<T> extends Definition<T> implements Abstract.ClassField {
    private final Expression<T> myResultType;

    public ClassField(T data, String name, Abstract.Precedence precedence, Expression<T> resultType) {
      super(data, name, precedence);
      setNotStatic();
      myResultType = resultType;
    }

    @Nonnull
    @Override
    public ClassDefinition<T> getParentDefinition() {
      //noinspection ConstantConditions
      return (ClassDefinition<T>) super.getParentDefinition();
    }

    @Override
    public <P, R> R accept(ConcreteDefinitionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitClassField(this, params);
    }

    @Nonnull
    public Expression<T> getResultType() {
      return myResultType;
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClassField(this, params);
    }
  }

  public static class Implementation<T> extends Definition<T> implements Abstract.Implementation {
    private Referable myImplementedField;
    private final Expression<T> myExpression;

    public Implementation(T data, Referable implementedField, Expression<T> expression) {
      super(data, implementedField.getName(), Abstract.Precedence.DEFAULT);
      myImplementedField = implementedField;
      myExpression = expression;
      setNotStatic();
    }

    @Nonnull
    @Override
    public Referable getImplementedField() {
      return myImplementedField;
    }

    public void setImplementedField(Referable field) {
      myImplementedField = field;
    }

    @Nonnull
    @Override
    public Expression<T> getImplementation() {
      return myExpression;
    }

    @Override
    public ClassDefinition<T> getParentDefinition() {
      return (ClassDefinition<T>) super.getParentDefinition();
    }

    @Override
    public <P, R> R accept(ConcreteDefinitionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitImplement(this, params);
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitImplement(this, params);
    }
  }

  public static abstract class FunctionBody<T> extends SourceNode<T> implements Abstract.FunctionBody {
    public FunctionBody(T data) {
      super(data);
    }
  }

  public static class TermFunctionBody<T> extends FunctionBody<T> implements Abstract.TermFunctionBody {
    private final Expression<T> myTerm;

    public TermFunctionBody(T data, Expression<T> term) {
      super(data);
      myTerm = term;
    }

    @Nonnull
    @Override
    public Expression<T> getTerm() {
      return myTerm;
    }
  }

  public static class ElimFunctionBody<T> extends FunctionBody<T> implements Abstract.ElimFunctionBody {
    private final List<ReferenceExpression<T>> myExpressions;
    private final List<FunctionClause<T>> myClauses;

    public ElimFunctionBody(T data, List<ReferenceExpression<T>> expressions, List<FunctionClause<T>> clauses) {
      super(data);
      myExpressions = expressions;
      myClauses = clauses;
    }

    @Nonnull
    @Override
    public List<? extends ReferenceExpression<T>> getEliminatedReferences() {
      return myExpressions;
    }

    @Nonnull
    @Override
    public List<? extends FunctionClause<T>> getClauses() {
      return myClauses;
    }
  }

  public static class FunctionDefinition<T> extends Definition<T> implements Abstract.FunctionDefinition, StatementCollection<T> {
    private final List<Parameter<T>> myParameters;
    private final Expression<T> myResultType;
    private final FunctionBody<T> myBody;
    private final List<Statement<T>> myStatements;

    public FunctionDefinition(T data, String name, Abstract.Precedence precedence, List<Parameter<T>> parameters, Expression<T> resultType, FunctionBody<T> body, List<Statement<T>> statements) {
      super(data, name, precedence);
      myParameters = parameters;
      myResultType = resultType;
      myBody = body;
      myStatements = statements;
    }

    @Nonnull
    @Override
    public List<Parameter<T>> getParameters() {
      return myParameters;
    }

    @Nullable
    @Override
    public Expression<T> getResultType() {
      return myResultType;
    }

    @Nonnull
    @Override
    public FunctionBody<T> getBody() {
      return myBody;
    }

    @Override
    public List<? extends Statement<T>> getGlobalStatements() {
      return myStatements;
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitFunction(this, params);
    }

    @Override
    public <P, R> R accept(ConcreteDefinitionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitFunction(this, params);
    }
  }

  public static class DataDefinition<T> extends Definition<T> implements Abstract.DataDefinition {
    private final List<TypeParameter<T>> myParameters;
    private final List<ReferenceExpression<T>> myEliminatedReferences;
    private final List<ConstructorClause<T>> myConstructorClauses;
    private final boolean myIsTruncated;
    private final UniverseExpression<T> myUniverse;

    public DataDefinition(T data, String name, Abstract.Precedence precedence, List<TypeParameter<T>> parameters, List<ReferenceExpression<T>> eliminatedReferences, boolean isTruncated, UniverseExpression<T> universe, List<ConstructorClause<T>> constructorClauses) {
      super(data, name, precedence);
      myParameters = parameters;
      myEliminatedReferences = eliminatedReferences;
      myConstructorClauses = constructorClauses;
      myIsTruncated = isTruncated;
      myUniverse = universe;
    }

    @Nonnull
    @Override
    public List<TypeParameter<T>> getParameters() {
      return myParameters;
    }

    @Nullable
    @Override
    public List<ReferenceExpression<T>> getEliminatedReferences() {
      return myEliminatedReferences;
    }

    @Nonnull
    @Override
    public List<ConstructorClause<T>> getConstructorClauses() {
      return myConstructorClauses;
    }

    @Override
    public boolean isTruncated() {
      return myIsTruncated;
    }

    @Nullable
    @Override
    public UniverseExpression<T> getUniverse() {
      return myUniverse;
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitData(this, params);
    }

    @Override
    public <P, R> R accept(ConcreteDefinitionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitData(this, params);
    }
  }

  public static abstract class Clause<T> extends SourceNode<T> implements PatternContainer<T> {
    public Clause(T data) {
      super(data);
    }
  }

  public static class ConstructorClause<T> extends Clause<T> {
    private final List<Pattern<T>> myPatterns;
    private final List<Constructor<T>> myConstructors;

    public ConstructorClause(T data, List<Pattern<T>> patterns, List<Constructor<T>> constructors) {
      super(data);
      myPatterns = patterns;
      myConstructors = constructors;
    }

    @Override
    public List<Pattern<T>> getPatterns() {
      return myPatterns;
    }

    @Nonnull
    public List<Constructor<T>> getConstructors() {
      return myConstructors;
    }
  }

  public static class Constructor<T> extends Definition<T> implements Abstract.Constructor {
    private final DataDefinition<T> myDataType;
    private final List<TypeParameter<T>> myArguments;
    private final List<ReferenceExpression<T>> myEliminatedReferences;
    private final List<FunctionClause<T>> myClauses;

    public Constructor(T data, String name, Abstract.Precedence precedence, DataDefinition<T> dataType, List<TypeParameter<T>> arguments, List<ReferenceExpression<T>> eliminatedReferences, List<FunctionClause<T>> clauses) {
      super(data, name, precedence);
      myDataType = dataType;
      myArguments = arguments;
      myEliminatedReferences = eliminatedReferences;
      myClauses = clauses;
    }

    @Nonnull
    @Override
    public List<TypeParameter<T>> getParameters() {
      return myArguments;
    }

    @Nonnull
    @Override
    public List<ReferenceExpression<T>> getEliminatedReferences() {
      return myEliminatedReferences;
    }

    @Nonnull
    @Override
    public List<FunctionClause<T>> getClauses() {
      return myClauses;
    }

    @Nonnull
    @Override
    public DataDefinition<T> getDataType() {
      return myDataType;
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitConstructor(this, params);
    }

    @Override
    public <P, R> R accept(ConcreteDefinitionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitConstructor(this, params);
    }
  }

  // ClassViews

  public static class ClassView<T> extends Definition<T> implements Abstract.ClassView {
    private final ReferenceExpression<T> myUnderlyingClass;
    private Referable myClassifyingField;
    private final List<ClassViewField<T>> myFields;

    public ClassView(T data, String name, ReferenceExpression<T> underlyingClass, Referable classifyingField, List<ClassViewField<T>> fields) {
      super(data, name, Abstract.Precedence.DEFAULT);
      myUnderlyingClass = underlyingClass;
      myFields = fields;
      myClassifyingField = classifyingField;
    }

    @Nonnull
    @Override
    public ReferenceExpression<T> getUnderlyingClass() {
      return myUnderlyingClass;
    }

    @Nonnull
    @Override
    public Referable getClassifyingField() {
      return myClassifyingField;
    }

    public void setClassifyingField(Referable classifyingField) {
      myClassifyingField = classifyingField;
    }

    @Nonnull
    @Override
    public List<ClassViewField<T>> getFields() {
      return myFields;
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClassView(this, params);
    }

    @Override
    public <P, R> R accept(ConcreteDefinitionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitClassView(this, params);
    }
  }

  public static class ClassViewField<T> extends Definition<T> implements Abstract.ClassViewField {
    private Referable myUnderlyingField;
    private final ClassView<T> myOwnView;

    public ClassViewField(T data, String name, Abstract.Precedence precedence, Referable underlyingField, ClassView<T> ownView) {
      super(data, name, precedence);
      myUnderlyingField = underlyingField;
      myOwnView = ownView;
    }

    @Nonnull
    @Override
    public Referable getUnderlyingField() {
      return myUnderlyingField;
    }

    @Nonnull
    @Override
    public ClassView<T> getOwnView() {
      return myOwnView;
    }

    public void setUnderlyingField(Referable underlyingField) {
      myUnderlyingField = underlyingField;
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClassViewField(this, params);
    }

    @Override
    public <P, R> R accept(ConcreteDefinitionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitClassViewField(this, params);
    }
  }

  public static class ClassViewInstance<T> extends Definition<T> implements Abstract.ClassViewInstance {
    private final boolean myDefault;
    private final List<Parameter<T>> myArguments;
    private final ReferenceExpression<T> myClassView;
    private final List<ClassFieldImpl<T>> myClassFieldImpls;
    private GlobalReferable myClassifyingDefinition;

    public ClassViewInstance(T data, boolean isDefault, String name, Abstract.Precedence precedence, List<Parameter<T>> arguments, ReferenceExpression<T> classView, List<ClassFieldImpl<T>> classFieldImpls) {
      super(data, name, precedence);
      myDefault = isDefault;
      myArguments = arguments;
      myClassView = classView;
      myClassFieldImpls = classFieldImpls;
    }

    @Override
    public boolean isDefault() {
      return myDefault;
    }

    @Nonnull
    @Override
    public List<Parameter<T>> getParameters() {
      return myArguments;
    }

    @Nonnull
    @Override
    public ReferenceExpression<T> getClassView() {
      return myClassView;
    }

    @Nonnull
    @Override
    public GlobalReferable getClassifyingDefinition() {
      return myClassifyingDefinition;
    }

    public void setClassifyingDefinition(GlobalReferable classifyingDefinition) {
      myClassifyingDefinition = classifyingDefinition;
    }

    @Nonnull
    @Override
    public List<ClassFieldImpl<T>> getClassFieldImpls() {
      return myClassFieldImpls;
    }

    @Override
    public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClassViewInstance(this, params);
    }

    @Override
    public <P, R> R accept(ConcreteDefinitionVisitor<T, ? super P, ? extends R> visitor, P params) {
      return visitor.visitClassViewInstance(this, params);
    }
  }

  // Statements

  public static abstract class Statement<T> extends SourceNode<T> {
    public Statement(T data) {
      super(data);
    }
  }

  public static class DefineStatement<T> extends Statement<T> {
    private final Definition<T> myDefinition;

    public DefineStatement(T data, Definition<T> definition) {
      super(data);
      myDefinition = definition;
    }

    @Nonnull
    public Definition<T> getDefinition() {
      return myDefinition;
    }
  }

  public static class NamespaceCommandStatement<T> extends Statement<T> implements OpenCommand {
    private GlobalReferable myDefinition;
    private final ModulePath myModulePath;
    private final List<String> myPath;
    private final boolean myHiding;
    private final List<String> myNames;
    private final Kind myKind;

    public enum Kind { OPEN, EXPORT }

    public NamespaceCommandStatement(T data, Kind kind, List<String> modulePath, List<String> path, boolean isHiding, List<String> names) {
      super(data);
      myDefinition = null;
      myModulePath = modulePath != null ? new ModulePath(modulePath) : null;
      myPath = path;
      myHiding = isHiding;
      myNames = names;
      myKind = kind;
    }

    @Nonnull
    public Kind getKind() {
      return myKind;
    }

    @Override
    public @Nullable ModulePath getModulePath() {
      return myModulePath;
    }

    @Override
    public @Nonnull List<String> getPath() {
      return myPath;
    }

    public void setResolvedClass(GlobalReferable resolvedClass) {
      myDefinition = resolvedClass;
    }

    @Override
    public GlobalReferable getResolvedClass() {
      return myDefinition;
    }

    @Override
    public boolean isHiding() {
      return myHiding;
    }

    @Override
    public @Nullable List<String> getNames() {
      return myNames;
    }
  }

  interface StatementCollection<T> extends DefinitionCollection<T>, HasOpens {
    List<? extends Statement<T>> getGlobalStatements();

    @Nonnull
    @Override
    default Collection<? extends Definition<T>> getGlobalDefinitions() {
      return getGlobalStatements().stream().flatMap(s -> {
        if (s instanceof DefineStatement) {
          return Stream.of(((DefineStatement<T>) s).getDefinition());
        } else {
          return Stream.empty();
        }
      }).collect(Collectors.toList());
    }

    @Nonnull
    @Override
    default Iterable<OpenCommand> getOpens() {
      return getGlobalStatements().stream().flatMap(s -> {
        if (s instanceof NamespaceCommandStatement) {
          return Stream.of((NamespaceCommandStatement) s);
        } else {
          return Stream.empty();
        }
      }).collect(Collectors.toList());
    }
  }

  // Patterns

  public static abstract class Pattern<T> extends SourceNode<T> implements Abstract.Pattern {
    private boolean myExplicit;

    public Pattern(T data) {
      super(data);
      myExplicit = true;
    }

    @Override
    public boolean isExplicit() {
      return myExplicit;
    }

    public void setExplicit(boolean isExplicit) {
      myExplicit = isExplicit;
    }
  }

  public static class NamePattern<T> extends Pattern<T> implements Abstract.NamePattern {
    private final @Nullable String myName;

    public NamePattern(T data, @Nullable String name) {
      super(data);
      myName = name;
    }

    public NamePattern(T data, boolean isExplicit, @Nullable String name) {
      super(data);
      setExplicit(isExplicit);
      myName = name;
    }

    @Nullable
    @Override
    public String getName() {
      return myName;
    }
  }

  public static class ConstructorPattern<T> extends Pattern<T> implements PatternContainer<T> {
    private Referable myConstructor;
    private final List<Pattern<T>> myArguments;

    public ConstructorPattern(T data, boolean isExplicit, Referable constructor, List<Pattern<T>> arguments) {
      super(data);
      setExplicit(isExplicit);
      myConstructor = constructor;
      myArguments = arguments;
    }

    public ConstructorPattern(T data, Referable constructor, List<Pattern<T>> arguments) {
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
    public List<Pattern<T>> getPatterns() {
      return myArguments;
    }
  }

  public static class EmptyPattern<T> extends Pattern<T> implements Abstract.EmptyPattern {
    public EmptyPattern(T data) {
      super(data);
    }

    public EmptyPattern(T data, boolean isExplicit) {
      super(data);
      setExplicit(isExplicit);
    }
  }
}
