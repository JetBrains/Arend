package org.arend.term.concrete;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.ParamLevelVariable;
import org.arend.ext.concrete.*;
import org.arend.ext.concrete.definition.*;
import org.arend.ext.concrete.expr.*;
import org.arend.ext.concrete.pattern.ConcreteConstructorPattern;
import org.arend.ext.concrete.pattern.ConcreteNumberPattern;
import org.arend.ext.concrete.pattern.ConcretePattern;
import org.arend.ext.concrete.pattern.ConcreteReferencePattern;
import org.arend.ext.error.GeneralError;
import org.arend.ext.error.LocalError;
import org.arend.ext.module.LongName;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.GoalSolver;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.naming.reference.*;
import org.arend.ext.concrete.definition.ClassFieldKind;
import org.arend.term.Fixity;
import org.arend.term.abs.Abstract;
import org.arend.term.group.Statement;
import org.arend.term.prettyprint.PrettyPrintVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.*;

public final class Concrete {
  private Concrete() {}

  public interface SourceNode extends ConcreteSourceNode {
    void prettyPrint(PrettyPrintVisitor visitor, Precedence prec);
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
      prettyPrint(new PrettyPrintVisitor(builder, 0), new Precedence(Expression.PREC));
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

    @NotNull
    @Override
    public ConcreteParameter implicit() {
      myExplicit = false;
      return this;
    }

    public boolean isStrict() {
      return false;
    }

    public SigmaFieldKind getSigmaFieldKind() {
      return SigmaFieldKind.ANY;
    }

    @Override
    @NotNull
    public abstract List<? extends Referable> getReferableList();

    @Override
    public @NotNull List<? extends Referable> getRefList() {
      return getReferableList();
    }

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

    @Override
    public void prettyPrint(PrettyPrintVisitor visitor, Precedence prec) {
      visitor.prettyPrintParameter(this);
    }
  }

  public static class NameParameter extends Parameter {
    private Referable myReferable;

    public NameParameter(Object data, boolean explicit, Referable referable) {
      super(data, explicit);
      myReferable = referable;
    }

    @Nullable
    public Referable getReferable() {
      return myReferable;
    }

    public void setReferable(Referable ref) {
      myReferable = ref;
    }

    @Override
    @NotNull
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
    @NotNull
    public List<? extends Referable> getReferableList() {
      return Collections.singletonList(null);
    }

    @Override
    public int getNumberOfParameters() {
      return 1;
    }

    @Override
    @NotNull
    public Expression getType() {
      return type;
    }
  }

  public static class TelescopeParameter extends TypeParameter {
    private List<? extends Referable> myReferableList;

    public TelescopeParameter(Object data, boolean explicit, List<? extends Referable> referableList, Expression type) {
      super(data, explicit, type);
      myReferableList = referableList;
    }

    @Override
    @NotNull
    public List<? extends Referable> getReferableList() {
      return myReferableList;
    }

    public void setReferableList(List<? extends Referable> refs) {
      myReferableList = refs;
    }

    @Override
    public int getNumberOfParameters() {
      return myReferableList.size();
    }
  }

  public static class DefinitionTypeParameter extends TelescopeParameter {
    private final boolean myStrict;

    public DefinitionTypeParameter(Object data, boolean explicit, boolean isStrict, Expression type) {
      super(data, explicit, Collections.singletonList(null), type);
      myStrict = isStrict;
    }

    public DefinitionTypeParameter(boolean explicit, boolean isStrict, Expression type) {
      super(type.getData(), explicit, Collections.singletonList(null), type);
      myStrict = isStrict;
    }

    @Override
    public boolean isStrict() {
      return myStrict;
    }
  }

  public static class DefinitionTelescopeParameter extends TelescopeParameter {
    private final boolean myStrict;

    public DefinitionTelescopeParameter(Object data, boolean explicit, boolean myStrict, List<? extends Referable> referableList, Expression type) {
      super(data, explicit, referableList, type);
      this.myStrict = myStrict;
    }

    @Override
    public boolean isStrict() {
      return myStrict;
    }
  }

  public static class SigmaTypeParameter extends TypeParameter implements SourceNode {
    private final SigmaFieldKind mySigmaFieldKind;

    public SigmaTypeParameter(Object data, Expression type, @NotNull SigmaFieldKind mySigmaFieldKind) {
      super(data, true, type);
      this.mySigmaFieldKind = mySigmaFieldKind;
    }

    public SigmaTypeParameter(Expression type, @NotNull SigmaFieldKind mySigmaFieldKind) {
      super(true, type);
      this.mySigmaFieldKind = mySigmaFieldKind;
    }

    @Override
    public SigmaFieldKind getSigmaFieldKind() {
      return mySigmaFieldKind;
    }
  }

  public static class SigmaTelescopeParameter extends TelescopeParameter implements SourceNode {
    private final SigmaFieldKind mySigmaFieldKind;

    public SigmaTelescopeParameter(Object data, @NotNull List<? extends Referable> referableList, Expression type, @NotNull SigmaFieldKind mySigmaFieldKind) {
      super(data, true, referableList, type);
      this.mySigmaFieldKind = mySigmaFieldKind;
    }

    @Override
    public SigmaFieldKind getSigmaFieldKind() {
      return mySigmaFieldKind;
    }
  }

  public static int getNumberOfParameters(Collection<? extends Parameter> parameters) {
    int sum = 0;
    for (Parameter parameter : parameters) {
      sum += parameter.getNumberOfParameters();
    }
    return sum;
  }

  // Expressions

  public static abstract class Expression extends SourceNodeImpl implements ConcreteExpression {
    public static final byte PREC = -12;

    public Expression(Object data) {
      super(data);
    }

    public abstract <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params);

    @Override
    public @NotNull List<ConcreteArgument> getArgumentsSequence() {
      return Collections.singletonList(new Argument(this, true));
    }

    public ReferenceExpression getUnderlyingReferenceExpression() {
      Expression expr = this;
      while (true) {
        if (expr instanceof ClassExtExpression) {
          expr = ((ClassExtExpression) expr).getBaseClassExpression();
        } else if (expr instanceof AppExpression) {
          expr = ((AppExpression) expr).getFunction();
        } else if (expr instanceof ReferenceExpression) {
          Referable ref = ((ReferenceExpression) expr).getReferent();
          if (ref instanceof MetaReferable) {
            MetaDefinition metaDef = ((MetaReferable) ref).getDefinition();
            if (metaDef instanceof DefinableMetaDefinition) {
              expr = ((DefinableMetaDefinition) metaDef).body;
              continue;
            }
          }
          break;
        } else {
          break;
        }
      }
      return expr instanceof ReferenceExpression ? (ReferenceExpression) expr : null;
    }

    public Referable getUnderlyingReferable() {
      ReferenceExpression expr = getUnderlyingReferenceExpression();
      return expr == null ? null : expr.getReferent();
    }

    @Override
    public @NotNull ConcreteExpression substitute(@NotNull Map<ArendRef, ConcreteExpression> substitution) {
      Map<Referable, Expression> map = new HashMap<>();
      for (Map.Entry<ArendRef, ConcreteExpression> entry : substitution.entrySet()) {
        if (!(entry.getKey() instanceof Referable && entry.getValue() instanceof Expression)) {
          throw new IllegalArgumentException();
        }
        map.put((Referable) entry.getKey(), (Expression) entry.getValue());
      }
      return accept(new SubstConcreteExpressionVisitor(map, null), null);
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      accept(new PrettyPrintVisitor(builder, 0), new Precedence(Expression.PREC));
      return builder.toString();
    }

    @Override
    public void prettyPrint(PrettyPrintVisitor visitor, Precedence prec) {
      accept(visitor, prec);
    }
  }

  public static class Argument implements ConcreteArgument {
    public Expression expression;
    private final boolean myExplicit;

    public Argument(Expression expression, boolean explicit) {
      this.expression = expression;
      myExplicit = explicit;
    }

    @Override
    @NotNull
    public Expression getExpression() {
      return expression;
    }

    @Override
    public boolean isExplicit() {
      return myExplicit;
    }
  }

  public static class TypedExpression extends Expression implements ConcreteTypedExpression {
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

    @Override
    public @NotNull Concrete.Expression getExpression() {
      return expression;
    }

    @Override
    public @NotNull Concrete.Expression getType() {
      return type;
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
      if (function instanceof AppExpression) {
        ((AppExpression) function).myArguments.addAll(arguments);
        return function;
      }
      return new AppExpression(data, function, arguments);
    }

    public static Expression make(Object data, Expression function, Expression argument, boolean isExplicit) {
      if (function instanceof AppExpression) {
        ((AppExpression) function).myArguments.add(new Argument(argument, isExplicit));
        return function;
      }

      List<Argument> arguments = new ArrayList<>();
      arguments.add(new Argument(argument, isExplicit));
      return new AppExpression(data, function, arguments);
    }

    @Override
    @NotNull
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
    @NotNull
    public List<Argument> getArguments() {
      return myArguments;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitApp(this, params);
    }

    @Override
    public @NotNull List<ConcreteArgument> getArgumentsSequence() {
      List<ConcreteArgument> result = new ArrayList<>(1 + myArguments.size());
      result.add(new Argument(myFunction, true));
      result.addAll(myArguments);
      return result;
    }

    public int getNumberOfExplicitArguments() {
      int sum = 0;
      for (Argument arg : myArguments) {
        if (arg.myExplicit) sum++;
      }
      return sum;
    }
  }

  public static class BinOpSequenceElem {
    public Expression expression;
    public final Fixity fixity;
    public final boolean isExplicit;

    public BinOpSequenceElem(@NotNull Expression expression, Fixity fixity, boolean isExplicit) {
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
    public BinOpSequenceElem(@NotNull Expression expression) {
      this.expression = expression;
      this.fixity = expression instanceof FixityReferenceExpression ? ((FixityReferenceExpression) expression).fixity : Fixity.NONFIX;
      if (expression instanceof FixityReferenceExpression) {
        ((FixityReferenceExpression) expression).fixity = Fixity.NONFIX;
      }
      this.isExplicit = true;
    }

    public boolean isInfixReference() {
      return isExplicit && (fixity == Fixity.INFIX || fixity == Fixity.UNKNOWN && getReferencePrecedence().isInfix);
    }

    public boolean isPostfixReference() {
      return isExplicit && fixity == Fixity.POSTFIX;
    }

    public Precedence getReferencePrecedence() {
      Expression expr = expression;
      // after the reference is resolved, it may become an application
      if (expression instanceof AppExpression && fixity != Fixity.NONFIX) {
        expr = ((AppExpression) expression).getFunction();
      }
      return expr instanceof ReferenceExpression && ((ReferenceExpression) expr).getReferent() instanceof GlobalReferable ? ((GlobalReferable) ((ReferenceExpression) expr).getReferent()).getPrecedence() : Precedence.DEFAULT;
    }
  }

  public static class FunctionClauses extends SourceNodeImpl implements ConcreteClauses {
    private final List<FunctionClause> myClauses;

    public FunctionClauses(Object data, List<FunctionClause> clauses) {
      super(data);
      myClauses = clauses;
    }

    @NotNull
    @Override
    public List<FunctionClause> getClauseList() {
      return myClauses;
    }

    @Override
    public void prettyPrint(PrettyPrintVisitor visitor, Precedence prec) {
      visitor.prettyPrintFunctionClauses(this);
    }
  }

  public static class BinOpSequenceExpression extends Expression {
    public static final byte PREC = 0;
    private final List<BinOpSequenceElem> mySequence;
    private final FunctionClauses myClauses;

    public BinOpSequenceExpression(Object data, List<BinOpSequenceElem> sequence, FunctionClauses clauses) {
      super(data);
      mySequence = sequence;
      myClauses = clauses;
    }

    @NotNull
    public List<BinOpSequenceElem> getSequence() {
      return mySequence;
    }

    public FunctionClauses getClauses() {
      return myClauses;
    }

    public List<FunctionClause> getClauseList() {
      return myClauses == null ? Collections.emptyList() : myClauses.getClauseList();
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitBinOpSequence(this, params);
    }

    @Override
    public @NotNull List<ConcreteArgument> getArgumentsSequence() {
      List<ConcreteArgument> result = new ArrayList<>(mySequence.size());
      for (BinOpSequenceElem elem : mySequence) {
        result.add(new Argument(elem.expression, elem.isExplicit));
      }
      return result;
    }
  }

  public static class ReferenceExpression extends Expression implements Reference, ConcreteReferenceExpression {
    public static final byte PREC = 12;
    private Referable myReferent;
    private List<LevelExpression> myPLevels;
    private List<LevelExpression> myHLevels;

    public ReferenceExpression(Object data, @NotNull Referable referable, List<LevelExpression> pLevels, List<LevelExpression> hLevels) {
      super(data);
      myReferent = referable;
      myPLevels = pLevels;
      myHLevels = hLevels;
    }

    public ReferenceExpression(Object data, @NotNull Referable referable) {
      this(data, referable, null, null);
    }

    @NotNull
    @Override
    public Referable getReferent() {
      return myReferent;
    }

    public void setReferent(@NotNull Referable referent) {
      myReferent = referent;
    }

    @Override
    public List<LevelExpression> getPLevels() {
      return myPLevels;
    }

    public void setPLevels(List<LevelExpression> levels) {
      myPLevels = levels;
    }

    @Override
    public List<LevelExpression> getHLevels() {
      return myHLevels;
    }

    public void setHLevels(List<LevelExpression> levels) {
      myHLevels = levels;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitReference(this, params);
    }
  }

  public static class LongReferenceExpression extends ReferenceExpression {
    private final LongName myLongName;
    private final @Nullable Concrete.ReferenceExpression myQualifier;

    public LongReferenceExpression(Object data, @Nullable Concrete.ReferenceExpression qualifier, LongName longName, Referable referable, List<LevelExpression> pLevels, List<LevelExpression> hLevels) {
      super(data, referable, pLevels, hLevels);
      myLongName = longName;
      myQualifier = qualifier;
    }

    public LongReferenceExpression(Object data, @Nullable Concrete.ReferenceExpression qualifier, LongName longName, Referable referable) {
      super(data, referable);
      myLongName = longName;
      myQualifier = qualifier;
    }

    public LongName getLongName() {
      return myLongName;
    }

    public @Nullable Concrete.ReferenceExpression getQualifier() {
      return myQualifier;
    }
  }

  public static class FixityReferenceExpression extends ReferenceExpression {
    public Fixity fixity;

    public FixityReferenceExpression(Object data, Referable referable, Fixity fixity) {
      super(data, referable);
      this.fixity = fixity;
    }

    public static ReferenceExpression make(Object data, Referable referable, Fixity fixity, List<LevelExpression> pLevels, List<LevelExpression> hLevels) {
      return fixity == null ? new ReferenceExpression(data, referable, pLevels, hLevels) : new FixityReferenceExpression(data, referable, fixity);
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

  /**
   * <code>__</code> expressions, see
   * <a href="https://github.com/JetBrains/Arend/issues/147">#147</a>
   */
  public static class ApplyHoleExpression extends Expression {
    public static final byte PREC = 12;
    public ApplyHoleExpression(Object data) {
      super(data);
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitApplyHole(this, params);
    }
  }

  public static class Coclauses extends SourceNodeImpl implements ConcreteCoclauses {
    private final List<ClassFieldImpl> myCoclauses;

    public Coclauses(Object data, List<ClassFieldImpl> coclauses) {
      super(data);
      myCoclauses = coclauses;
    }

    @NotNull
    @Override
    public List<ClassFieldImpl> getCoclauseList() {
      return myCoclauses;
    }

    @Override
    public void prettyPrint(PrettyPrintVisitor visitor, Precedence prec) {
      visitor.prettyprintCoclauses(this);
    }
  }

  public static class ClassExtExpression extends Expression implements ConcreteClassExtExpression {
    public static final byte PREC = 11;
    private Expression myBaseClassExpression;
    private final Coclauses myCoclauses;

    private ClassExtExpression(Object data, Expression baseClassExpression, Coclauses coclauses) {
      super(data);
      this.myBaseClassExpression = baseClassExpression;
      myCoclauses = coclauses;
    }

    public static ClassExtExpression make(Object data, Expression baseClassExpression, Coclauses coclauses) {
      if (baseClassExpression instanceof ClassExtExpression) {
        ((ClassExtExpression) baseClassExpression).getStatements().addAll(coclauses.getCoclauseList());
        return (ClassExtExpression) baseClassExpression;
      } else {
        return new ClassExtExpression(data, baseClassExpression, coclauses);
      }
    }

    @NotNull
    @Override
    public Expression getBaseClassExpression() {
      return myBaseClassExpression;
    }

    public void setBaseClassExpression(Expression baseClassExpression) {
      if (baseClassExpression instanceof ClassExtExpression) {
        myBaseClassExpression = ((ClassExtExpression) baseClassExpression).getBaseClassExpression();
        myCoclauses.getCoclauseList().addAll(0, ((ClassExtExpression) baseClassExpression).myCoclauses.getCoclauseList());
      } else {
        myBaseClassExpression = baseClassExpression;
      }
    }

    @NotNull
    @Override
    public Concrete.Coclauses getCoclauses() {
      return myCoclauses;
    }

    @NotNull
    public List<ClassFieldImpl> getStatements() {
      return myCoclauses.getCoclauseList();
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

  public static class CoClauseFunctionReference extends ClassFieldImpl {
    public CoClauseFunctionReference(Object data, Referable implementedField, TCDefReferable functionReference, boolean isDefault) {
      super(data, implementedField, new ReferenceExpression(data, functionReference), null, isDefault);
    }

    public CoClauseFunctionReference(Referable implementedField, TCDefReferable functionReference, boolean isDefault) {
      this(functionReference.getData(), implementedField, functionReference, isDefault);
    }

    public ReferenceExpression getReferenceExpression() {
      Expression impl = getImplementation();
      assert impl instanceof ReferenceExpression || impl instanceof AppExpression;
      return (ReferenceExpression) (impl instanceof ReferenceExpression ? impl : ((AppExpression) impl).getFunction());
    }

    public TCDefReferable getFunctionReference() {
      return (TCDefReferable) getReferenceExpression().getReferent();
    }
  }

  public static class ClassFieldImpl extends SourceNodeImpl implements CoClauseElement, ConcreteCoclause {
    private Referable myImplementedField;
    public Expression implementation;
    public TCDefReferable classRef; // the class of fields in subClassFieldImpls
    private final Coclauses mySubCoclauses;
    private final boolean myDefault;

    public ClassFieldImpl(Object data, Referable implementedField, Expression implementation, Coclauses subCoclauses, boolean isDefault) {
      super(data);
      myImplementedField = implementedField;
      this.implementation = implementation;
      mySubCoclauses = subCoclauses;
      myDefault = isDefault;
    }

    public ClassFieldImpl(Object data, Referable implementedField, Expression implementation, Coclauses subCoclauses) {
      this(data, implementedField, implementation, subCoclauses, false);
    }

    @Override
    public Referable getImplementedField() {
      return myImplementedField;
    }

    @Override
    public void setImplementedField(Referable newImplementedField) {
      myImplementedField = newImplementedField;
    }

    @Override
    public @NotNull Referable getImplementedRef() {
      return myImplementedField;
    }

    @Override
    public @Nullable Expression getImplementation() {
      return implementation;
    }

    public List<ClassFieldImpl> getSubCoclauseList() {
      return mySubCoclauses == null ? Collections.emptyList() : mySubCoclauses.getCoclauseList();
    }

    @Override
    public @Nullable Coclauses getSubCoclauses() {
      return mySubCoclauses;
    }

    public boolean isDefault() {
      return myDefault;
    }

    @Override
    public void prettyPrint(PrettyPrintVisitor visitor, Precedence prec) {
      visitor.prettyPrintClassFieldImpl(this);
    }
  }

  public static class NewExpression extends Expression {
    public static final byte PREC = 11;
    public Expression expression;

    public NewExpression(Object data, Expression expression) {
      super(data);
      this.expression = expression;
    }

    @NotNull
    public Expression getExpression() {
      return expression;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitNew(this, params);
    }
  }

  public static class GoalExpression extends Expression implements ConcreteGoalExpression {
    public static final byte PREC = 12;
    private final String myName;
    public Expression expression;
    public final Expression originalExpression;
    public final GoalSolver goalSolver;
    public final boolean useGoalSolver;
    public final List<GeneralError> errors;

    public GoalExpression(Object data, String name, Expression expression) {
      super(data);
      myName = name;
      this.expression = expression;
      this.originalExpression = expression;
      this.goalSolver = null;
      this.useGoalSolver = false;
      this.errors = Collections.emptyList();
    }

    public GoalExpression(Object data, String name, Expression expression, GoalSolver goalSolver, boolean useGoalSolver, List<GeneralError> errors) {
      super(data);
      myName = name;
      this.expression = expression;
      this.originalExpression = expression;
      this.goalSolver = goalSolver;
      this.useGoalSolver = useGoalSolver;
      this.errors = errors;
    }

    @Override
    public String getName() {
      return myName;
    }

    @Override
    public @Nullable ConcreteExpression getOriginalExpression() {
      return originalExpression;
    }

    @Override
    public Expression getExpression() {
      return expression;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitGoal(this, params);
    }
  }

  public static class IncompleteExpression extends GoalExpression implements ConcreteIncompleteExpression {
    public IncompleteExpression(Object data) {
      super(data, null, null);
    }
  }

  public static class HoleExpression extends Expression implements ConcreteHoleExpression {
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

  public static class LamExpression extends Expression implements ConcreteLamExpression {
    public static final byte PREC = -5;
    private final List<Parameter> myArguments;
    public Expression body;

    public LamExpression(Object data, List<Parameter> arguments, Expression body) {
      super(data);
      myArguments = arguments;
      this.body = body;
    }

    @Override
    @NotNull
    public List<Parameter> getParameters() {
      return myArguments;
    }

    @Override
    @NotNull
    public Expression getBody() {
      return body;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitLam(this, params);
    }
  }

  public static class PatternLamExpression extends LamExpression {
    // null elements of the list correspond to parameters of the lambda
    private final List<Pattern> myPatterns;

    private PatternLamExpression(Object data, List<Parameter> parameters, List<Pattern> patterns, Expression body) {
      super(data, parameters, body);
      myPatterns = patterns;
    }

    public static LamExpression make(Object data, List<Parameter> parameters, List<Pattern> patterns, Expression body) {
      return patterns.isEmpty() ? new LamExpression(data, parameters, body) : new PatternLamExpression(data, parameters, patterns, body);
    }

    public List<Pattern> getPatterns() {
      return myPatterns;
    }
  }

  public static class LetClause implements SourceNode, ConcreteLetClause {
    private final List<Parameter> myParameters;
    public Expression resultType;
    public Expression term;
    private final Pattern myPattern;

    public LetClause(List<Parameter> parameters, Expression resultType, Expression term, Pattern pattern) {
      myParameters = parameters;
      this.resultType = resultType;
      this.term = term;
      myPattern = pattern;
    }

    public LetClause(Referable referable, List<Parameter> parameters, Expression resultType, Expression term) {
      this(parameters, resultType, term, new NamePattern(referable, true, referable, null));
    }

    public LetClause(Pattern pattern, Expression resultType, Expression term) {
      this(Collections.emptyList(), resultType, term, pattern);
    }

    @Nullable
    @Override
    public Object getData() {
      return myPattern.getData();
    }

    public Pattern getPattern() {
      return myPattern;
    }

    @NotNull
    public Expression getTerm() {
      return term;
    }

    @NotNull
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

    @Override
    public void prettyPrint(PrettyPrintVisitor visitor, Precedence prec) {
      visitor.prettyPrintLetClause(this, false);
    }
  }

  public static class LetExpression extends Expression implements ConcreteLetExpression {
    public static final byte PREC = -9;
    private final boolean myHave;
    private final boolean myStrict;
    private final List<LetClause> myClauses;
    public Expression expression;
    public boolean isGeneratedFromLambda;

    public LetExpression(Object data, boolean isHave, boolean isStrict, List<LetClause> clauses, Expression expression) {
      super(data);
      myHave = isHave;
      myStrict = isStrict;
      myClauses = clauses;
      this.expression = expression;
    }

    @Override
    public boolean isHave() {
      return myHave;
    }

    @Override
    public boolean isStrict() {
      return myStrict;
    }

    @Override
    @NotNull
    public List<LetClause> getClauses() {
      return myClauses;
    }

    @Override
    @NotNull
    public Expression getExpression() {
      return expression;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitLet(this, params);
    }
  }

  public static class PiExpression extends Expression implements ConcretePiExpression {
    public static final byte PREC = -4;
    private final List<TypeParameter> myParameters;
    public Expression codomain;

    public PiExpression(Object data, List<TypeParameter> parameters, Expression codomain) {
      super(data);
      myParameters = parameters;
      this.codomain = codomain;
    }

    @Override
    @NotNull
    public List<TypeParameter> getParameters() {
      return myParameters;
    }

    @Override
    @NotNull
    public Expression getCodomain() {
      return codomain;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitPi(this, params);
    }
  }

  public static class SigmaExpression extends Expression implements ConcreteSigmaExpression {
    public static final byte PREC = -3;
    private final List<TypeParameter> myParameters;

    public SigmaExpression(Object data, List<TypeParameter> parameters) {
      super(data);
      myParameters = parameters;
    }

    @Override
    @NotNull
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
    @NotNull
    public List<Expression> getFields() {
      return myFields;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitTuple(this, params);
    }
  }

  public static class UniverseExpression extends Expression implements ConcreteUniverseExpression {
    public static final byte PREC = 12;
    private final LevelExpression myPLevel;
    private final LevelExpression myHLevel;

    public UniverseExpression(Object data, LevelExpression pLevel, LevelExpression hLevel) {
      super(data);
      myPLevel = pLevel;
      myHLevel = hLevel;
    }

    @Override
    @Nullable
    public LevelExpression getPLevel() {
      return myPLevel;
    }

    @Override
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

    @NotNull
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
    public @NotNull Expression expression;
    public @Nullable Referable referable;
    public @Nullable Expression type;
    public final boolean isElim;

    public CaseArgument(@NotNull Expression expression, @Nullable Referable referable, @Nullable Expression type) {
      this.expression = expression;
      this.referable = referable;
      this.type = type;
      isElim = false;
    }

    public CaseArgument(@NotNull ReferenceExpression expression, @Nullable Expression type) {
      this.expression = expression;
      this.referable = null;
      this.type = type;
      isElim = expression.getReferent().isLocalRef() || expression.getReferent() instanceof UnresolvedReference;
    }

    public CaseArgument(@NotNull ApplyHoleExpression expression, @Nullable Expression type) {
      this.expression = expression;
      this.referable = null;
      this.type = type;
      isElim = true;
    }

    @Override
    public @NotNull ConcreteExpression getExpression() {
      return expression;
    }

    @Override
    public @Nullable ArendRef getAsRef() {
      return referable;
    }

    @Override
    public @Nullable ConcreteExpression getType() {
      return type;
    }

    @Override
    public boolean isElim() {
      return isElim;
    }
  }

  public static class CaseExpression extends Expression implements ConcreteCaseExpression {
    public static final byte PREC = -8;
    private boolean mySCase;
    private final List<CaseArgument> myArguments;
    private Expression myResultType;
    private Expression myResultTypeLevel;
    private final List<FunctionClause> myClauses;
    public int level = -2; // the level of the result type; -2 means not truncated

    public CaseExpression(Object data, boolean isSCase, List<CaseArgument> arguments, Expression resultType, Expression resultTypeLevel, List<FunctionClause> clauses) {
      super(data);
      mySCase = isSCase;
      myArguments = arguments;
      myResultType = resultType;
      myResultTypeLevel = resultTypeLevel;
      myClauses = clauses;
    }

    @Override
    public boolean isSCase() {
      return mySCase;
    }

    public void setSCase(boolean isSCase) {
      mySCase = isSCase;
    }

    @Override
    @NotNull
    public List<? extends CaseArgument> getArguments() {
      return myArguments;
    }

    @Override
    @Nullable
    public Expression getResultType() {
      return myResultType;
    }

    public void setResultType(Expression resultType) {
      myResultType = resultType;
    }

    @Override
    @Nullable
    public Expression getResultTypeLevel() {
      return myResultTypeLevel;
    }

    public void setResultTypeLevel(Expression resultTypeLevel) {
      myResultTypeLevel = resultTypeLevel;
    }

    @Override
    @NotNull
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

  public static class FunctionClause extends Clause implements ConcreteClause {
    public Expression expression;

    public FunctionClause(Object data, List<Pattern> patterns, Expression expression) {
      super(data, patterns);
      this.expression = expression;
    }

    @Override
    public @NotNull List<Pattern> getPatterns() {
      return super.getPatterns();
    }

    @Override
    @Nullable
    public Expression getExpression() {
      return expression;
    }

    @Override
    public void prettyPrint(PrettyPrintVisitor visitor, Precedence prec) {
      visitor.prettyPrintFunctionClause(this);
    }
  }

  public static class NumericLiteral extends Expression implements ConcreteNumberExpression {
    private final BigInteger myNumber;

    public NumericLiteral(Object data, BigInteger number) {
      super(data);
      myNumber = number;
    }

    @Override
    @NotNull
    public BigInteger getNumber() {
      return myNumber;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitNumericLiteral(this, params);
    }
  }

  public static class StringLiteral extends Expression implements ConcreteStringExpression {
    private final String myUnescapedString;

    public StringLiteral(Object data, String unescapedString) {
      super(data);
      myUnescapedString = unescapedString;
    }

    @Override
    public @NotNull String getUnescapedString() {
      return myUnescapedString;
    }

    @Override
    public <P, R> R accept(ConcreteExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitStringLiteral(this, params);
    }
  }

  // Level expressions

  public static abstract class LevelExpression extends SourceNodeImpl implements ConcreteLevel {
    LevelExpression(Object data) {
      super(data);
    }

    public abstract <P, R> R accept(ConcreteLevelExpressionVisitor<? super P, ? extends R> visitor, P params);

    @Override
    public void prettyPrint(PrettyPrintVisitor visitor, Precedence prec) {
      accept(visitor, prec);
    }
  }

  public static class VarLevelExpression extends LevelExpression {
    private final LevelVariable myVariable;

    public VarLevelExpression(Object data, LevelVariable variable) {
      super(data);
      myVariable = variable;
    }

    @NotNull
    public LevelVariable getVariable() {
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

  public static class IdLevelExpression extends LevelExpression {
    private final Referable myReferent;

    public IdLevelExpression(Object data, Referable referable) {
      super(data);
      myReferent = referable;
    }

    public Referable getReferent() {
      return myReferent;
    }

    @Override
    public <P, R> R accept(ConcreteLevelExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitId(this, params);
    }
  }

  public static class SucLevelExpression extends LevelExpression {
    private final LevelExpression myExpression;

    public SucLevelExpression(Object data, LevelExpression expression) {
      super(data);
      myExpression = expression;
    }

    @NotNull
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

    @NotNull
    public LevelExpression getLeft() {
      return myLeft;
    }

    @NotNull
    public LevelExpression getRight() {
      return myRight;
    }

    @Override
    public <P, R> R accept(ConcreteLevelExpressionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitMax(this, params);
    }
  }

  // Definitions

  public static Collection<? extends Parameter> getParameters(GeneralDefinition definition, boolean onlyThisDef) {
    if (definition instanceof BaseFunctionDefinition) {
      return ((BaseFunctionDefinition) definition).getParameters();
    }
    if (definition instanceof DataDefinition) {
      return ((DataDefinition) definition).getParameters();
    }
    if (definition instanceof DefinableMetaDefinition) {
      return ((DefinableMetaDefinition) definition).getParameters();
    }
    if (definition instanceof Constructor) {
      if (onlyThisDef) {
        return ((Constructor) definition).getParameters();
      } else {
        List<TypeParameter> dataTypeParameters = ((Constructor) definition).getRelatedDefinition().getParameters();
        List<TypeParameter> parameters = ((Constructor) definition).getParameters();
        List<TypeParameter> totalParameters = new ArrayList<>(dataTypeParameters.size() + parameters.size());
        totalParameters.addAll(dataTypeParameters);
        totalParameters.addAll(parameters);
        return totalParameters;
      }
    }
    if (definition instanceof ClassDefinition) {
      List<TypeParameter> parameters = new ArrayList<>();
      for (ClassElement element : ((ClassDefinition) definition).getElements()) {
        if (element instanceof ClassField && ((ClassField) element).getData().isParameterField()) {
          ClassField field = (ClassField) element;
          Expression type = field.getResultType();
          List<TypeParameter> fieldParams = field.getParameters();
          boolean isDesugarized = definition.getStage().ordinal() >= Stage.DESUGARIZED.ordinal();
          if (fieldParams.size() > 1 || !fieldParams.isEmpty() && !isDesugarized) {
            type = new PiExpression(field.getParameters().get(0).getData(), isDesugarized ? fieldParams.subList(1, fieldParams.size()) : fieldParams, type);
          }
          parameters.add(new TypeParameter(field.getData(), field.getData().isExplicitField(), type));
        }
      }
      return parameters;
    }
    return null;
  }

  public interface ReferableDefinition extends GeneralDefinition {
    @NotNull
    @Override
    TCDefReferable getData();

    @NotNull
    @Override
    Definition getRelatedDefinition();

    @Override
    default @NotNull Stage getStage() {
      return getRelatedDefinition().getStage();
    }

    <P, R> R accept(ConcreteReferableDefinitionVisitor<? super P, ? extends R> visitor, P params);

    default boolean equalsImpl(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      var that = (ReferableDefinition) o;
      return getData().equals(that.getData());
    }

    default int hashCodeImpl() {
      return getData().hashCode();
    }
  }

  public enum Stage { NOT_RESOLVED, TYPE_CLASS_REFERENCES_RESOLVED, RESOLVED, DESUGARIZED, TYPECHECKED }

  public enum Status {
    NO_ERRORS { @Override public org.arend.core.definition.Definition.TypeCheckingStatus getTypecheckingStatus() { return org.arend.core.definition.Definition.TypeCheckingStatus.NO_ERRORS; } },
    HAS_WARNINGS { @Override public org.arend.core.definition.Definition.TypeCheckingStatus getTypecheckingStatus() { return org.arend.core.definition.Definition.TypeCheckingStatus.HAS_WARNINGS; } },
    HAS_ERRORS { @Override public org.arend.core.definition.Definition.TypeCheckingStatus getTypecheckingStatus() { return org.arend.core.definition.Definition.TypeCheckingStatus.HAS_ERRORS; } };

    public abstract org.arend.core.definition.Definition.TypeCheckingStatus getTypecheckingStatus();

    Status max(Status another) {
      return ordinal() >= another.ordinal() ? this : another;
    }
  }

  public interface GeneralDefinition extends SourceNode {
    @Override
    @NotNull TCReferable getData();

    @NotNull Stage getStage();

    @NotNull ResolvableDefinition getRelatedDefinition();
  }

  public static abstract class ResolvableDefinition implements GeneralDefinition {
    Stage stage = Stage.TYPE_CLASS_REFERENCES_RESOLVED;
    private Status myStatus = Status.NO_ERRORS;

    public Status getStatus() {
      return myStatus;
    }

    public void setStatus(Status status) {
      myStatus = myStatus.max(status);
    }

    public void setStatus(GeneralError.Level level) {
      if (level == GeneralError.Level.ERROR) {
        myStatus = myStatus.max(Status.HAS_ERRORS);
      } else if (level.ordinal() >= GeneralError.Level.WARNING_UNUSED.ordinal()) {
        myStatus = myStatus.max(Status.HAS_WARNINGS);
      }
    }

    public List<TCDefReferable> getUsedDefinitions() {
      return Collections.emptyList();
    }

    public TCDefReferable getEnclosingClass() {
      return null;
    }

    @Override
    public @NotNull Stage getStage() {
      return stage;
    }

    public void setResolved() {
      stage = Stage.RESOLVED;
    }

    public void setDesugarized() {
      stage = Stage.DESUGARIZED;
    }

    public void setTypechecked() {
      stage = Stage.TYPECHECKED;
    }

    public void setTypeClassReferencesResolved() {
      if (stage == Stage.NOT_RESOLVED) {
        stage = Stage.TYPE_CLASS_REFERENCES_RESOLVED;
      }
    }

    public abstract <P, R> R accept(ConcreteResolvableDefinitionVisitor<? super P, ? extends R> visitor, P params);

    @Override
    public void prettyPrint(StringBuilder builder, PrettyPrinterConfig ppConfig) {
      accept(new PrettyPrintVisitor(builder, 0, !ppConfig.isSingleLine()), null);
    }
  }

  private static List<Abstract.Comparison> makeComparisonList(List<? extends LevelReferable> referables, boolean isIncreasing) {
    if (referables.isEmpty()) return Collections.emptyList();
    List<Abstract.Comparison> result = new ArrayList<>(referables.size() - 1);
    for (int i = 0; i < referables.size() - 1; i++) {
      result.add(isIncreasing ? Abstract.Comparison.LESS_OR_EQUALS : Abstract.Comparison.GREATER_OR_EQUALS);
    }
    return result;
  }

  public static class LevelParameters extends SourceNodeImpl implements Abstract.LevelParameters, ConcreteLevelParameters {
    public final List<? extends LevelReferable> referables;
    public final boolean isIncreasing;

    public LevelParameters(Object data, List<? extends LevelReferable> referables, boolean isIncreasing) {
      super(data);
      this.referables = referables;
      this.isIncreasing = isIncreasing;
    }

    @Override
    public @NotNull List<? extends Referable> getReferables() {
      return referables;
    }

    @Override
    public @NotNull Collection<Abstract.Comparison> getComparisonList() {
      return makeComparisonList(referables, isIncreasing);
    }

    @Override
    public boolean isIncreasing() {
      return isIncreasing;
    }

    public static List<LevelReferable> getLevelParametersRefs(Abstract.LevelParameters params) {
      if (params == null) return null;
      List<LevelReferable> result = new ArrayList<>();
      for (Referable ref : params.getReferables()) {
        result.add(new DataLevelReferable(ref, ref.getRefName()));
      }
      return result;
    }

    public static Concrete.LevelParameters makeLevelParameters(List<? extends LevelVariable> variables) {
      List<LevelReferable> refs = new ArrayList<>(variables.size());
      for (LevelVariable variable : variables) {
        refs.add(new DataLevelReferable(null, variable.getName()));
      }
      return new Concrete.LevelParameters(null, refs, variables.size() <= 1 || variables.get(0).getStd() == variables.get(0) || variables.get(0) instanceof ParamLevelVariable && variables.get(1) instanceof ParamLevelVariable && ((ParamLevelVariable) variables.get(0)).getSize() <= ((ParamLevelVariable) variables.get(1)).getSize());
    }

    @Override
    public void prettyPrint(PrettyPrintVisitor visitor, Precedence prec) {
      visitor.prettyPrintLevelParameters(this);
    }
  }

  public static class LevelsDefinition extends SourceNodeImpl implements Statement, Abstract.LevelParameters {
    private final List<TCLevelReferable> myReferables;
    private final boolean myIncreasing;
    private final boolean myPLevels;

    public LevelsDefinition(Object data, List<TCLevelReferable> referables, boolean isIncreasing, boolean isPLevels) {
      super(data);
      myReferables = referables;
      myIncreasing = isIncreasing;
      myPLevels = isPLevels;
    }

    @Override
    public @NotNull List<? extends TCLevelReferable> getReferables() {
      return myReferables;
    }

    @Override
    public @NotNull Collection<Abstract.Comparison> getComparisonList() {
      return makeComparisonList(myReferables, myIncreasing);
    }

    @Override
    public boolean isIncreasing() {
      return myIncreasing;
    }

    public boolean isPLevels() {
      return myPLevels;
    }

    @Override
    public Abstract.LevelParameters getPLevelsDefinition() {
      return myPLevels ? this : null;
    }

    @Override
    public Abstract.LevelParameters getHLevelsDefinition() {
      return myPLevels ? null : this;
    }

    @Override
    public void prettyPrint(PrettyPrintVisitor visitor, Precedence prec) {
      visitor.prettyPrintLevelsDefinition(this);
    }
  }

  public static abstract class Definition extends ResolvableDefinition implements ReferableDefinition, ConcreteDefinition {
    private final TCDefReferable myReferable;
    private LevelParameters myPLevelParameters;
    private LevelParameters myHLevelParameters;
    public TCDefReferable enclosingClass;
    private Set<TCDefReferable> myRecursiveDefinitions = Collections.emptySet();
    public TCDefReferable pOriginalDef; // definition from which p-levels were copied, or null if they are not inherited
    public TCDefReferable hOriginalDef;

    @Override
    public @NotNull TCDefReferable getData() {
      return myReferable;
    }

    @Override
    public TCDefReferable getEnclosingClass() {
      return enclosingClass;
    }

    public boolean isRecursive() {
      return !myRecursiveDefinitions.isEmpty();
    }

    public Set<TCDefReferable> getRecursiveDefinitions() {
      return myRecursiveDefinitions;
    }

    public void setRecursiveDefinitions(Set<TCDefReferable> recursiveDefinitions) {
      myRecursiveDefinitions = recursiveDefinitions;
    }

    @Override
    public String toString() {
      return myReferable.textRepresentation();
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
      return equalsImpl(o);
    }

    @Override
    public int hashCode() {
      return hashCodeImpl();
    }

    public Definition(TCDefReferable referable, LevelParameters pParams, LevelParameters hParams) {
      myReferable = referable;
      myPLevelParameters = pParams;
      myHLevelParameters = hParams;
    }

    public Definition(TCDefReferable referable) {
      myReferable = referable;
      myPLevelParameters = null;
      myHLevelParameters = null;
    }

    @NotNull
    @Override
    public Definition getRelatedDefinition() {
      return this;
    }

    @Override
    public LevelParameters getPLevelParameters() {
      return myPLevelParameters;
    }

    @Override
    public void setPLevelParameters(ConcreteLevelParameters parameters) {
      if (!(parameters instanceof LevelParameters || parameters == null)) {
        throw new IllegalArgumentException();
      }
      myPLevelParameters = (LevelParameters) parameters;
    }

    @Override
    public LevelParameters getHLevelParameters() {
      return myHLevelParameters;
    }

    @Override
    public void setHLevelParameters(ConcreteLevelParameters parameters) {
      if (!(parameters instanceof LevelParameters || parameters == null)) {
        throw new IllegalArgumentException();
      }
      myHLevelParameters = (LevelParameters) parameters;
    }

    @Override
    public void setDynamic() {
      LocatedReferable parent = myReferable.getLocatedReferableParent();
      if (!(parent instanceof ConcreteResolvedClassReferable)) {
        throw new IllegalStateException();
      }
      ((ConcreteResolvedClassReferable) parent).addDynamicReferable(myReferable);
    }

    public abstract <P, R> R accept(ConcreteDefinitionVisitor<? super P, ? extends R> visitor, P params);

    @Override
    public <P, R> R accept(ConcreteReferableDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return accept((ConcreteDefinitionVisitor<? super P, ? extends R>) visitor, params);
    }

    @Override
    public <P, R> R accept(ConcreteResolvableDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return accept((ConcreteDefinitionVisitor<? super P, ? extends R>) visitor, params);
    }

    @Override
    public void prettyPrint(PrettyPrintVisitor visitor, Precedence prec) {
      accept(visitor, null);
    }
  }

  public static class ClassDefinition extends Definition {
    private final boolean myRecord;
    private final boolean myWithoutClassifying;
    private final List<ReferenceExpression> mySuperClasses;
    private final List<ClassElement> myElements;
    private TCFieldReferable myClassifyingField;
    private boolean myForcedClassifyingField;
    private List<TCDefReferable> myUsedDefinitions = Collections.emptyList();

    public ClassDefinition(TCDefReferable referable, LevelParameters pParams, LevelParameters hParams, boolean isRecord, boolean withoutClassifying, List<ReferenceExpression> superClasses, List<ClassElement> elements) {
      super(referable, pParams, hParams);
      myRecord = isRecord;
      myWithoutClassifying = withoutClassifying;
      stage = Stage.NOT_RESOLVED;
      mySuperClasses = superClasses;
      myElements = elements;
    }

    public boolean isRecord() {
      return myRecord;
    }

    public boolean withoutClassifying() {
      return myWithoutClassifying;
    }

    @Nullable
    public TCFieldReferable getClassifyingField() {
      return myClassifyingField;
    }

    public boolean isForcedClassifyingField() {
      return myForcedClassifyingField;
    }

    public void setClassifyingField(TCFieldReferable classifyingField, boolean isForced) {
      myClassifyingField = classifyingField;
      myForcedClassifyingField = isForced;
    }

    @NotNull
    public List<ReferenceExpression> getSuperClasses() {
      return mySuperClasses;
    }

    @NotNull
    public List<ClassElement> getElements() {
      return myElements;
    }

    @Override
    public List<TCDefReferable> getUsedDefinitions() {
      return myUsedDefinitions;
    }

    public void setUsedDefinitions(List<TCDefReferable> usedDefinitions) {
      myUsedDefinitions = usedDefinitions;
    }

    @Override
    public <P, R> R accept(ConcreteDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClass(this, params);
    }
  }

  public static class CoClauseFunctionDefinition extends UseDefinition {
    private Referable myImplementedField;
    private int myNumberOfExternalParameters;

    public CoClauseFunctionDefinition(FunctionKind kind, TCDefReferable referable, TCDefReferable enclosingDefinition, Referable implementedField, List<Parameter> parameters, Expression resultType, Expression resultTypeLevel, FunctionBody body) {
      super(kind, referable, null, null, parameters, resultType, resultTypeLevel, body, enclosingDefinition);
      myImplementedField = implementedField;
    }

    public Referable getImplementedField() {
      return myImplementedField;
    }

    public void setImplementedField(Referable newImplementedField) {
      myImplementedField = newImplementedField;
    }

    public int getNumberOfExternalParameters() {
      return myNumberOfExternalParameters;
    }

    public void setNumberOfExternalParameters(int n) {
      myNumberOfExternalParameters = n;
    }
  }

  public interface BaseClassField extends ClassElement {
    @NotNull List<TypeParameter> getParameters();
    @NotNull Expression getResultType();
    void setResultType(Expression resultType);
    @Nullable Expression getResultTypeLevel();
    void setResultTypeLevel(Expression resultTypeLevel);
  }

  public static abstract class ReferableDefinitionBase implements ReferableDefinition {
    @Override
    public String toString() {
      return getData().textRepresentation();
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
      return equalsImpl(o);
    }

    @Override
    public int hashCode() {
      return hashCodeImpl();
    }
  }

  public static class ClassField extends ReferableDefinitionBase implements BaseClassField {
    private final TCFieldReferable myReferable;
    private ClassDefinition myParentClass;
    private final boolean myExplicit;
    private final ClassFieldKind myKind;
    private final List<TypeParameter> myParameters;
    private Expression myResultType;
    private Expression myResultTypeLevel;
    private final boolean myCoerce;

    public ClassField(TCFieldReferable referable, ClassDefinition parentClass, boolean isExplicit, ClassFieldKind kind, List<TypeParameter> parameters, Expression resultType, Expression resultTypeLevel, boolean isCoerce) {
      myReferable = referable;
      myParentClass = parentClass;
      myExplicit = isExplicit;
      myKind = kind;
      myParameters = parameters;
      myResultType = resultType;
      myResultTypeLevel = resultTypeLevel;
      myCoerce = isCoerce;
    }

    @NotNull
    @Override
    public TCFieldReferable getData() {
      return myReferable;
    }

    public boolean isExplicit() {
       return myExplicit;
    }

    public ClassFieldKind getKind() {
      return myKind;
    }

    @NotNull
    @Override
    public List<TypeParameter> getParameters() {
      return myParameters;
    }

    @NotNull
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

    @NotNull
    @Override
    public ClassDefinition getRelatedDefinition() {
      return myParentClass;
    }

    public void setParentClass(Concrete.ClassDefinition parentClass) {
      myParentClass = parentClass;
    }

    public boolean isCoerce() {
      return myCoerce;
    }

    @Override
    public <P, R> R accept(ConcreteReferableDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitClassField(this, params);
    }

    @Override
    public void prettyPrint(PrettyPrintVisitor visitor, Precedence prec) {
      visitor.prettyPrintClassField(this);
    }

    @Override
    public void prettyPrint(StringBuilder builder, PrettyPrinterConfig ppConfig) {
      new PrettyPrintVisitor(builder, 0).prettyPrintClassField(this);
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

    @NotNull
    public Referable getOverriddenField() {
      return myOverriddenField;
    }

    public void setOverriddenField(Referable overriddenField) {
      myOverriddenField = overriddenField;
    }

    @NotNull
    @Override
    public List<TypeParameter> getParameters() {
      return myParameters;
    }

    @NotNull
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

    @Override
    public void prettyPrint(PrettyPrintVisitor visitor, Precedence prec) {
      visitor.prettyPrintOverridden(this);
    }
  }

  public static abstract class FunctionBody extends SourceNodeImpl implements ConcreteFunctionBody {
    FunctionBody(Object data) {
      super(data);
    }

    @Nullable
    public Expression getTerm() {
      return null;
    }

    @NotNull
    public List<CoClauseElement> getCoClauseElements() {
      return Collections.emptyList();
    }

    @NotNull
    public List<? extends ReferenceExpression> getEliminatedReferences() {
      return Collections.emptyList();
    }

    @NotNull
    public List<FunctionClause> getClauses() {
      return Collections.emptyList();
    }

    @Override
    public void prettyPrint(PrettyPrintVisitor visitor, Precedence prec) {
      visitor.prettyPrintBody(this, false);
    }
  }

  public static class TermFunctionBody extends FunctionBody {
    private Expression myTerm;

    public TermFunctionBody(Object data, Expression term) {
      super(data);
      myTerm = term;
    }

    @Override
    @NotNull
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

    @NotNull
    public List<ReferenceExpression> getEliminatedReferences() {
      return myExpressions;
    }

    @NotNull
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

    @NotNull
    public List<CoClauseElement> getCoClauseElements() {
      return myCoClauseElements;
    }
  }

  public static abstract class BaseFunctionDefinition extends Definition {
    private final List<Parameter> myParameters;
    private Expression myResultType;
    private Expression myResultTypeLevel;
    private final FunctionBody myBody;

    public BaseFunctionDefinition(TCDefReferable referable, LevelParameters pParams, LevelParameters hParams, List<Parameter> parameters, Expression resultType, Expression resultTypeLevel, FunctionBody body) {
      super(referable, pParams, hParams);
      stage = Stage.NOT_RESOLVED;
      myParameters = parameters;
      myResultType = resultType;
      myResultTypeLevel = resultTypeLevel;
      myBody = body;
    }

    @NotNull
    public abstract FunctionKind getKind();

    public TCDefReferable getUseParent() {
      return null;
    }

    @NotNull
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

    @NotNull
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
    private List<TCDefReferable> myUsedDefinitions = Collections.emptyList();

    public FunctionDefinition(FunctionKind kind, TCDefReferable referable, List<Parameter> parameters, Expression resultType, Expression resultTypeLevel, FunctionBody body) {
      super(referable, null, null, parameters, resultType, resultTypeLevel, body);
      myKind = kind;
      stage = Stage.NOT_RESOLVED;
    }

    public FunctionDefinition(FunctionKind kind, TCDefReferable referable, LevelParameters pParams, LevelParameters hParams, List<Parameter> parameters, Expression resultType, Expression resultTypeLevel, FunctionBody body) {
      super(referable, pParams, hParams, parameters, resultType, resultTypeLevel, body);
      myKind = kind;
      stage = Stage.NOT_RESOLVED;
    }

    @Override
    @NotNull
    public FunctionKind getKind() {
      return myKind;
    }

    @Override
    public List<TCDefReferable> getUsedDefinitions() {
      return myUsedDefinitions;
    }

    public void setUsedDefinitions(List<TCDefReferable> usedDefinitions) {
      myUsedDefinitions = usedDefinitions;
    }
  }

  public static class UseDefinition extends FunctionDefinition {
    private final TCDefReferable myUseParent;

    private UseDefinition(FunctionKind kind, TCDefReferable referable, LevelParameters pParams, LevelParameters hParams, List<Parameter> parameters, Expression resultType, Expression resultTypeLevel, FunctionBody body, TCDefReferable useParent) {
      super(kind, referable, pParams, hParams, parameters, resultType, resultTypeLevel, body);
      myUseParent = useParent;
    }

    public static FunctionDefinition make(FunctionKind kind, TCDefReferable referable, LevelParameters pParams, LevelParameters hParams, List<Parameter> parameters, Expression resultType, Expression resultTypeLevel, FunctionBody body, LocatedReferable coerceParent) {
      return coerceParent instanceof TCDefReferable && kind.isUse() ? new UseDefinition(kind, referable, pParams, hParams, parameters, resultType, resultTypeLevel, body, (TCDefReferable) coerceParent) : new FunctionDefinition(kind.isUse() ? FunctionKind.FUNC : kind, referable, pParams, hParams, parameters, resultType, resultTypeLevel, body);
    }

    public TCDefReferable getUseParent() {
      return myUseParent;
    }
  }

  public static class DataDefinition extends Definition {
    private final List<TypeParameter> myParameters;
    private final List<ReferenceExpression> myEliminatedReferences;
    private final List<ConstructorClause> myConstructorClauses;
    private final boolean myIsTruncated;
    private final UniverseExpression myUniverse;
    private List<TCDefReferable> myUsedDefinitions = Collections.emptyList();

    public DataDefinition(TCDefReferable referable, LevelParameters pParams, LevelParameters hParams, List<TypeParameter> parameters, List<ReferenceExpression> eliminatedReferences, boolean isTruncated, UniverseExpression universe, List<ConstructorClause> constructorClauses) {
      super(referable, pParams, hParams);
      myParameters = parameters;
      myEliminatedReferences = eliminatedReferences;
      myConstructorClauses = constructorClauses;
      myIsTruncated = isTruncated;
      myUniverse = universe;
    }

    @NotNull
    public List<TypeParameter> getParameters() {
      return myParameters;
    }

    @Nullable
    public List<ReferenceExpression> getEliminatedReferences() {
      return myEliminatedReferences;
    }

    @NotNull
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
    public List<TCDefReferable> getUsedDefinitions() {
      return myUsedDefinitions;
    }

    public void setUsedDefinitions(List<TCDefReferable> usedDefinitions) {
      myUsedDefinitions = usedDefinitions;
    }

    @Override
    public <P, R> R accept(ConcreteDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitData(this, params);
    }
  }

  public static abstract class Clause extends SourceNodeImpl implements PatternHolder {
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

    @Override
    public void prettyPrint(PrettyPrintVisitor visitor, Precedence prec) {
      visitor.prettyPrintClause(this);
    }
  }

  public static class ConstructorClause extends Clause implements ConcreteConstructorClause {
    private final List<Constructor> myConstructors;

    public ConstructorClause(Object data, List<Pattern> patterns, List<Constructor> constructors) {
      super(data, patterns);
      myConstructors = constructors;
    }

    @NotNull
    public List<Constructor> getConstructors() {
      return myConstructors;
    }

    @Override
    public void prettyPrint(PrettyPrintVisitor visitor, Precedence prec) {
      visitor.prettyPrintConstructorClause(this);
    }
  }

  public static class Constructor extends ReferableDefinitionBase implements ConcreteConstructor {
    private final TCDefReferable myReferable;
    private DataDefinition myDataType;
    private final List<TypeParameter> myParameters;
    private final List<ReferenceExpression> myEliminatedReferences;
    private final List<FunctionClause> myClauses;
    private final boolean myCoerce;
    private Expression myResultType;

    public Constructor(TCDefReferable referable, DataDefinition dataType, List<TypeParameter> parameters, List<ReferenceExpression> eliminatedReferences, List<FunctionClause> clauses, boolean isCoerce) {
      myReferable = referable;
      myDataType = dataType;
      myParameters = parameters;
      myEliminatedReferences = eliminatedReferences;
      myClauses = clauses;
      myCoerce = isCoerce;
    }

    @Override
    public @NotNull TCDefReferable getData() {
      return myReferable;
    }

    @NotNull
    public List<TypeParameter> getParameters() {
      return myParameters;
    }

    @NotNull
    public List<ReferenceExpression> getEliminatedReferences() {
      return myEliminatedReferences;
    }

    @NotNull
    public List<FunctionClause> getClauses() {
      return myClauses;
    }

    @NotNull
    @Override
    public DataDefinition getRelatedDefinition() {
      return myDataType;
    }

    public void setDataType(DataDefinition dataType) {
      myDataType = dataType;
    }

    public Expression getResultType() {
      return myResultType;
    }

    public void setResultType(Expression resultType) {
      myResultType = resultType;
    }

    public boolean isCoerce() {
      return myCoerce;
    }

    @Override
    public <P, R> R accept(ConcreteReferableDefinitionVisitor<? super P, ? extends R> visitor, P params) {
      return visitor.visitConstructor(this, params);
    }

    @Override
    public void prettyPrint(PrettyPrintVisitor visitor, Precedence prec) {
      visitor.prettyPrintConstructor(this);
    }

    @Override
    public void prettyPrint(StringBuilder builder, PrettyPrinterConfig ppConfig) {
      new PrettyPrintVisitor(builder, 0).prettyPrintConstructor(this);
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
    private TypedReferable myAsReferable;

    public Pattern(Object data, TypedReferable asReferable) {
      super(data);
      myExplicit = true;
      myAsReferable = asReferable;
    }

    @Override
    public boolean isExplicit() {
      return myExplicit;
    }

    public void setExplicit(boolean isExplicit) {
      myExplicit = isExplicit;
    }

    public abstract Pattern copy();

    public Pattern toConstructor() {
      return this;
    }

    @NotNull
    @Override
    public ConcretePattern implicit() {
      myExplicit = false;
      return this;
    }

    @NotNull
    @Override
    public ConcretePattern as(@NotNull ArendRef ref, @Nullable ConcreteExpression type) {
      if (!(ref instanceof Referable && type instanceof Expression)) {
        throw new IllegalArgumentException();
      }
      myAsReferable = new TypedReferable(getData(), (Referable) ref, (Expression) type);
      return this;
    }

    @Nullable
    public TypedReferable getAsReferable() {
      return myAsReferable;
    }

    public void setAsReferable(TypedReferable asReferable) {
      myAsReferable = asReferable;
    }

    public @NotNull List<? extends Pattern> getPatterns() {
      return Collections.emptyList();
    }

    @Override
    public void prettyPrint(PrettyPrintVisitor visitor, Precedence prec) {
      visitor.prettyPrintPattern(this, prec.priority, false);
    }
  }

  public static class NumberPattern extends Pattern implements ConcreteNumberPattern {
    public final static int MAX_VALUE = 100;
    private final int myNumber;

    public NumberPattern(Object data, int number, TypedReferable asReferable) {
      super(data, asReferable);
      myNumber = number;
    }

    @Override
    public int getNumber() {
      return myNumber;
    }

    @Override
    public Pattern copy() {
      NumberPattern result = new NumberPattern(getData(), myNumber, getAsReferable());
      result.setExplicit(isExplicit());
      return result;
    }
  }

  public static class NamePattern extends Pattern implements ConcreteReferencePattern {
    private @Nullable Referable myReferable;
    public @Nullable Expression type;

    public NamePattern(Object data, boolean isExplicit, @Nullable Referable referable, @Nullable Expression type) {
      super(data, null);
      setExplicit(isExplicit);
      myReferable = referable;
      this.type = type;
    }

    @Override
    public @Nullable Referable getRef() {
      return myReferable;
    }

    @Nullable
    public Referable getReferable() {
      return myReferable;
    }

    public void setReferable(Referable ref) {
      myReferable = ref;
    }

    @Override
    public Pattern toConstructor() {
      return myReferable == null || type != null ? this : new ConstructorPattern(getData(), isExplicit(), new NamedUnresolvedReference(getData(), myReferable.getRefName()), Collections.emptyList(), getAsReferable());
    }

    @Override
    public Pattern copy() {
      return new NamePattern(getData(), isExplicit(), myReferable, type);
    }

    @NotNull
    @Override
    public ConcretePattern as(@NotNull ArendRef ref, @Nullable ConcreteExpression type) {
      throw new IllegalArgumentException("\\as is not allowed for variable patterns");
    }
  }

  public static class TypedReferable extends SourceNodeImpl {
    public Referable referable;
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

    @Override
    public void prettyPrint(PrettyPrintVisitor visitor, Precedence prec) {
      visitor.prettyPrintTypedReferable(this);
    }
  }

  public static class ConstructorPattern extends Pattern implements PatternHolder, ConcreteConstructorPattern {
    private Referable myConstructor;
    private final List<Pattern> myArguments;

    public ConstructorPattern(Object data, boolean isExplicit, Referable constructor, List<Pattern> arguments, TypedReferable asReferable) {
      super(data, asReferable);
      setExplicit(isExplicit);
      myConstructor = constructor;
      myArguments = arguments;
    }

    public ConstructorPattern(Object data, Referable constructor, List<Pattern> arguments, TypedReferable asReferable) {
      super(data, asReferable);
      myConstructor = constructor;
      myArguments = arguments;
    }

    @Override
    @NotNull
    public Referable getConstructor() {
      return myConstructor;
    }

    public void setConstructor(Referable constructor) {
      myConstructor = constructor;
    }

    @Override
    @NotNull
    public List<Pattern> getPatterns() {
      return myArguments;
    }

    @Override
    public ConstructorPattern getSourceNode() {
      return this;
    }

    @Override
    public Pattern copy() {
      return new ConstructorPattern(getData(), isExplicit(), myConstructor, myArguments, getAsReferable());
    }
  }

  public static class TuplePattern extends Pattern {
    private final List<Pattern> myPatterns;

    public TuplePattern(Object data, List<Pattern> patterns, TypedReferable asReferable) {
      super(data, asReferable);
      myPatterns = patterns;
    }

    public TuplePattern(Object data, boolean isExplicit, List<Pattern> patterns, TypedReferable asReferable) {
      super(data, asReferable);
      setExplicit(isExplicit);
      myPatterns = patterns;
    }

    @Override
    public @NotNull List<Pattern> getPatterns() {
      return myPatterns;
    }

    @Override
    public Pattern copy() {
      return new TuplePattern(getData(), isExplicit(), myPatterns, getAsReferable());
    }
  }
}
