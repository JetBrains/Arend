package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.core.pattern.Pattern;
import com.jetbrains.jetpad.vclang.core.pattern.PatternArgument;
import com.jetbrains.jetpad.vclang.core.pattern.Patterns;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.ArrayList;
import java.util.List;

public class Constructor extends Definition implements Function {
  private DataDefinition myDataType;
  private DependentLink myParameters;
  private Patterns myPatterns;
  private boolean myTypeHasError;

  public Constructor(Abstract.Constructor abstractDef, DataDefinition dataType) {
    super(abstractDef);
    myDataType = dataType;
    myParameters = EmptyDependentLink.getInstance();
    if (dataType != null) {
      setPolyParams(dataType.getPolyParams());
    }
    myTypeHasError = true;
  }

  public Constructor(Abstract.Constructor abstractDef, DependentLink parameters, DataDefinition dataType, Patterns patterns) {
    super(abstractDef);
    myDataType = dataType;
    myParameters = parameters;
    myPatterns = patterns;
    if (dataType != null) {
      setPolyParams(dataType.getPolyParams());
    }
    myTypeHasError = myParameters == null;
  }

  public Constructor(Abstract.Constructor abstractDef, DependentLink parameters, DataDefinition dataType) {
    this(abstractDef, parameters, dataType, null);
  }

  public Patterns getPatterns() {
    assert !typeHasErrors();
    return myPatterns;
  }

  public void setPatterns(Patterns patterns) {
    myPatterns = patterns;
  }

  @Override
  public ElimTreeNode getElimTree() {
    Condition condition = myDataType.getCondition(this);
    return condition == null ? EmptyElimTreeNode.getInstance() : condition.getElimTree();
  }

  @Override
  public DependentLink getParameters() {
    assert !typeHasErrors();
    return myParameters;
  }

  @Override
  public List<LevelBinding> getPolyParams() {
    return myDataType.getPolyParams();
  }

  @Override
  public int getNumberOfRequiredArguments() {
    return DependentLink.Helper.size(myParameters);
  }

  public void setParameters(DependentLink parameters) {
    assert parameters != null;
    myParameters = parameters;
  }

  public DataDefinition getDataType() {
    return myDataType;
  }

  public void setDataType(DataDefinition dataType) {
    myDataType = dataType;
    setPolyParams(dataType.getPolyParams());
  }

  public DependentLink getDataTypeParameters() {
    assert !typeHasErrors() && !myDataType.typeHasErrors();
    return myPatterns == null ? myDataType.getParameters() : myPatterns.getParameters();
  }

  public List<Expression> matchDataTypeArguments(List<Expression> arguments) {
    assert !typeHasErrors() && !myDataType.typeHasErrors();
    if (myPatterns == null) {
      return arguments;
    } else {
      Pattern.MatchResult result = myPatterns.match(arguments);
      if (result instanceof Pattern.MatchOKResult) {
        return ((Pattern.MatchOKResult) result).expressions;
      } else {
        return null;
      }
    }
  }

  public Expression getDataTypeExpression(LevelArguments polyParams) {
    return getDataTypeExpression(null, polyParams);
  }

  public Expression getDataTypeExpression(ExprSubstitution substitution, LevelArguments polyParams) {
    assert !typeHasErrors() && !myDataType.typeHasErrors();

    List<Expression> arguments;
    if (myPatterns == null) {
      arguments = new ArrayList<>();
      for (DependentLink link = myDataType.getParameters(); link.hasNext(); link = link.getNext()) {
        arguments.add(ExpressionFactory.Reference(link));
      }
    } else {
      ExprSubstitution subst = new ExprSubstitution();

      DependentLink dataTypeParams = myDataType.getParameters();
      LevelSubstitution levelSubst = polyParams == null ? new LevelSubstitution() : new LevelSubstitution(myDataType.getPolyParams(), polyParams.getLevels());
      arguments = new ArrayList<>(myPatterns.getPatterns().size());
      for (PatternArgument patternArg : myPatterns.getPatterns()) {
        ExprSubstitution innerSubst = new ExprSubstitution();
        LevelSubstitution innerLevelSubst = new LevelSubstitution();

        if (patternArg.getPattern() instanceof ConstructorPattern) {
          DataCallExpression dataCall = dataTypeParams.getType().toExpression().subst(subst).normalize(NormalizeVisitor.Mode.WHNF).toDataCall();
          List<? extends Expression> argDataTypeParams = dataCall.getDefCallArguments();
          innerSubst = ((ConstructorPattern) patternArg.getPattern()).getMatchedArguments(new ArrayList<>(argDataTypeParams));
          innerLevelSubst.add(new LevelSubstitution(((ConstructorPattern) patternArg.getPattern()).getConstructor().getPolyParams(), dataCall.getPolyArguments().getLevels()));
        }

        if (substitution != null) {
          innerSubst.add(substitution);
        }
        Expression expr = patternArg.getPattern().toExpression(innerSubst).subst(innerLevelSubst).subst(levelSubst);

        subst.add(dataTypeParams, expr);
        arguments.add(expr);
        dataTypeParams = dataTypeParams.getNext();
      }
    }

    return myDataType.getDefCall(polyParams, arguments);
  }

  @Override
  public Expression getTypeWithParams(List<DependentLink> params, LevelArguments polyArguments) {
    if (typeHasErrors()) {
      return null;
    }

    LevelSubstitution polySubst = polyArguments.toLevelSubstitution(this);
    Expression resultType = getDataTypeExpression(polyArguments);
    DependentLink parameters = getDataTypeParameters();
    ExprSubstitution substitution = new ExprSubstitution();
    if (parameters.hasNext()) {
      parameters = DependentLink.Helper.subst(parameters, substitution, polySubst);
      for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
        link.setExplicit(false);
      }
      params.addAll(DependentLink.Helper.toList(parameters));
    }
    DependentLink conParams = DependentLink.Helper.subst(myParameters, substitution, polySubst);
    if (!params.isEmpty()) {
      params.get(params.size() - 1).setNext(conParams);
    }
    params.addAll(DependentLink.Helper.toList(conParams));
    resultType = resultType.subst(substitution, polySubst);
    return resultType;
  }

  @Override
  public ConCallExpression getDefCall(LevelArguments polyArguments) {
    return new ConCallExpression(this, polyArguments, new ArrayList<Expression>(), new ArrayList<Expression>());
  }

  @Override
  public ConCallExpression getDefCall(LevelArguments polyArguments, List<Expression> args) {
    throw new IllegalStateException();
  }

  @Override
  public boolean typeHasErrors() {
    return myTypeHasError;
  }

  public void typeHasErrors(boolean has) {
    myTypeHasError = has;
  }

  @Override
  public TypeCheckingStatus hasErrors() {
    return myTypeHasError ? TypeCheckingStatus.HAS_ERRORS : TypeCheckingStatus.NO_ERRORS;
  }

  @Override
  public void hasErrors(TypeCheckingStatus status) {
  }
}