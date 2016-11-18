package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelArguments;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.type.PiUniverseType;
import com.jetbrains.jetpad.vclang.term.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.ConCall;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.DataCall;

public class DataDefinition extends Definition {
  private List<Constructor> myConstructors;
  private DependentLink myParameters;
  private Map<Constructor, Condition> myConditions;
  private SortMax mySorts;
  private boolean myMatchesOnInterval;
  private boolean myTypeHasErrors;
  private TypeCheckingStatus myHasErrors;

  public DataDefinition(Abstract.DataDefinition abstractDef) {
    this(abstractDef, new SortMax(), EmptyDependentLink.getInstance());
  }

  public DataDefinition(Abstract.DataDefinition abstractDef, SortMax sorts, DependentLink parameters) {
    super(abstractDef);
    myConstructors = new ArrayList<>();
    myParameters = parameters;
    mySorts = sorts;
    myMatchesOnInterval = false;
    myTypeHasErrors = myParameters != null;
    myHasErrors = myTypeHasErrors ? TypeCheckingStatus.HAS_ERRORS : TypeCheckingStatus.TYPE_CHECKING;
  }

  public SortMax getSorts() {
    return mySorts;
  }

  public void setSorts(SortMax sorts) {
    mySorts = sorts;
  }

  @Override
  public DependentLink getParameters() {
    assert !typeHasErrors();
    return myParameters;
  }

  public void setParameters(DependentLink parameters) {
    assert parameters != null;
    myParameters = parameters;
  }

  public List<Constructor> getConstructors() {
    return myConstructors;
  }

  public List<ConCallExpression> getMatchedConstructors(DataCallExpression dataCall) {
    List<ConCallExpression> result = new ArrayList<>();
    for (Constructor constructor : myConstructors) {
      if (constructor.typeHasErrors())
        continue;
      List<? extends Expression> matchedParameters;
      if (constructor.getPatterns() != null) {
        Pattern.MatchResult matchResult = constructor.getPatterns().match(dataCall.getDefCallArguments());
        if (matchResult instanceof Pattern.MatchMaybeResult) {
          return null;
        } else if (matchResult instanceof Pattern.MatchFailedResult) {
          continue;
        } else if (matchResult instanceof Pattern.MatchOKResult) {
          matchedParameters = ((Pattern.MatchOKResult) matchResult).expressions;
        } else {
          throw new IllegalStateException();
        }
      } else {
        matchedParameters = dataCall.getDefCallArguments();
      }

      result.add(ConCall(constructor, dataCall.getPolyArguments(), new ArrayList<>(matchedParameters), new ArrayList<Expression>()));
    }
    return result;
  }

  public void addCondition(Condition condition) {
    if (myConditions == null) {
      myConditions = new HashMap<>();
    }
    myConditions.put(condition.getConstructor(), condition);
  }

  public Collection<Condition> getConditions() {
    return myConditions == null ? Collections.<Condition>emptyList() : myConditions.values();
  }

  public Condition getCondition(Constructor constructor) {
    return myConditions == null ? null : myConditions.get(constructor);
  }

  public Constructor getConstructor(String name) {
    for (Constructor constructor : myConstructors) {
      if (constructor.getName().equals(name)) {
        return constructor;
      }
    }
    return null;
  }

  public void addConstructor(Constructor constructor) {
    myConstructors.add(constructor);
  }

  public boolean matchesOnInterval() { return myMatchesOnInterval; }

  public void setMatchesOnInterval() { myMatchesOnInterval = true; }

  @Override
  public TypeMax getTypeWithParams(List<DependentLink> params, LevelArguments polyArguments) {
    if (typeHasErrors()) {
      return null;
    }

    ExprSubstitution subst = new ExprSubstitution();
    LevelSubstitution polySubst = polyArguments.toLevelSubstitution(this);
    params.addAll(DependentLink.Helper.toList(DependentLink.Helper.subst(myParameters, subst, polySubst)));
    return new PiUniverseType(EmptyDependentLink.getInstance(), mySorts).subst(subst, polySubst);
  }

  @Override
  public DataCallExpression getDefCall(LevelArguments polyArguments) {
    return DataCall(this, polyArguments, new ArrayList<Expression>());
  }

  @Override
  public DataCallExpression getDefCall(LevelArguments polyArguments, List<Expression> args) {
    return new DataCallExpression(this, polyArguments, args);
  }

  @Override
  public boolean typeHasErrors() {
    return myTypeHasErrors;
  }

  public void typeHasErrors(boolean has) {
    myTypeHasErrors = has;
    if (has) {
      myHasErrors = TypeCheckingStatus.HAS_ERRORS;
    }
  }

  @Override
  public TypeCheckingStatus hasErrors() {
    return myHasErrors;
  }

  @Override
  public void hasErrors(TypeCheckingStatus status) {
    myHasErrors = status;
  }
}
