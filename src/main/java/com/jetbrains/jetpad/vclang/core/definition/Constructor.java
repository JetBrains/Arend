package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.elimtree.BranchElimTree;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.core.pattern.Pattern;
import com.jetbrains.jetpad.vclang.core.pattern.PatternArgument;
import com.jetbrains.jetpad.vclang.core.pattern.Patterns;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.StdLevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.ArrayList;
import java.util.List;

public class Constructor extends Definition implements Function, BranchElimTree.Pattern {
  private final DataDefinition myDataType;
  private DependentLink myParameters;
  private Patterns myPatterns;
  private ElimTreeNode myCondition;

  public Constructor(Abstract.Constructor abstractDef, DataDefinition dataType) {
    super(abstractDef, TypeCheckingStatus.HEADER_HAS_ERRORS);
    myDataType = dataType;
    myParameters = null;
  }

  public ElimTreeNode getCondition() {
    return myCondition;
  }

  public void setCondition(ElimTreeNode condition) {
    myCondition = condition;
  }

  public Patterns getPatterns() {
    assert myParameters != null;
    return myPatterns;
  }

  public void setPatterns(Patterns patterns) {
    myPatterns = patterns;
  }

  @Override
  public Abstract.Constructor getAbstractDefinition() {
    return (Abstract.Constructor) super.getAbstractDefinition();
  }

  @Override
  public ElimTreeNode getElimTree() {
    return myCondition == null ? EmptyElimTreeNode.getInstance() : myCondition;
  }

  @Override
  public DependentLink getParameters() {
    assert myParameters != null;
    return myParameters;
  }

  public void setParameters(DependentLink parameters) {
    assert parameters != null;
    myParameters = parameters;
  }

  public DataDefinition getDataType() {
    return myDataType;
  }

  public DependentLink getDataTypeParameters() {
    assert myParameters != null && myDataType.status().headerIsOK();
    return myPatterns == null ? myDataType.getParameters() : myPatterns.getParameters();
  }

  public List<Expression> matchDataTypeArguments(List<Expression> arguments) {
    assert myParameters != null && myDataType.status().headerIsOK();
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

  public Expression getDataTypeExpression(Sort sortArgument) {
    return getDataTypeExpression(null, sortArgument);
  }

  public Expression getDataTypeExpression(ExprSubstitution substitution, Sort sortArgument) {
    assert myParameters != null && myDataType.status().headerIsOK();

    List<Expression> arguments;
    if (myPatterns == null) {
      // TODO: Why substitution is not applied?
      arguments = new ArrayList<>();
      for (DependentLink link = myDataType.getParameters(); link.hasNext(); link = link.getNext()) {
        arguments.add(new ReferenceExpression(link));
      }
    } else {
      ExprSubstitution subst = new ExprSubstitution();

      DependentLink dataTypeParams = myDataType.getParameters();
      LevelSubstitution levelSubst = sortArgument == null ? LevelSubstitution.EMPTY : sortArgument.toLevelSubstitution();
      arguments = new ArrayList<>(myPatterns.getPatterns().size());
      for (PatternArgument patternArg : myPatterns.getPatterns()) {
        ExprSubstitution innerSubst = new ExprSubstitution();
        LevelSubstitution innerLevelSubst;

        if (patternArg.getPattern() instanceof ConstructorPattern) {
          DataCallExpression dataCall = dataTypeParams.getType().getExpr().subst(subst).normalize(NormalizeVisitor.Mode.WHNF).toDataCall();
          List<? extends Expression> argDataTypeParams = dataCall.getDefCallArguments();
          innerSubst = ((ConstructorPattern) patternArg.getPattern()).getMatchedArguments(new ArrayList<>(argDataTypeParams));
          innerLevelSubst = new StdLevelSubstitution(dataCall.getSortArgument());
        } else {
          innerLevelSubst = LevelSubstitution.EMPTY;
        }

        if (substitution != null) {
          innerSubst.addAll(substitution);
        }
        Expression expr = patternArg.getPattern().toExpression(innerSubst).subst(innerLevelSubst).subst(levelSubst);

        subst.add(dataTypeParams, expr);
        arguments.add(expr);
        dataTypeParams = dataTypeParams.getNext();
      }
    }

    if (sortArgument == null) {
      sortArgument = Sort.STD;
    }

    return myDataType.getDefCall(sortArgument, arguments);
  }

  @Override
  public Expression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument) {
    if (myParameters == null) {
      return null;
    }

    LevelSubstitution polySubst = sortArgument.toLevelSubstitution();
    Expression resultType = getDataTypeExpression(sortArgument);
    DependentLink parameters = getDataTypeParameters();
    ExprSubstitution substitution = new ExprSubstitution();
    List<DependentLink> paramList = null;
    if (parameters.hasNext()) {
      parameters = DependentLink.Helper.subst(parameters, substitution, polySubst);
      for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
        link.setExplicit(false);
      }
      paramList = DependentLink.Helper.toList(parameters);
      params.addAll(paramList);
    }
    DependentLink conParams = DependentLink.Helper.subst(myParameters, substitution, polySubst);
    if (paramList != null && !paramList.isEmpty()) {
      paramList.get(paramList.size() - 1).setNext(conParams);
    }
    params.addAll(DependentLink.Helper.toList(conParams));
    resultType = resultType.subst(substitution, polySubst);
    return resultType;
  }

  @Override
  public ConCallExpression getDefCall(Sort sortArgument, Expression thisExpr, List<Expression> args) {
    int dataTypeArgsNumber = DependentLink.Helper.size(getDataTypeParameters());
    List<Expression> dataTypeArgs = new ArrayList<>(dataTypeArgsNumber);
    if (thisExpr != null) {
      dataTypeArgs.add(thisExpr);
      dataTypeArgsNumber--;
    }
    dataTypeArgs.addAll(args.subList(0, dataTypeArgsNumber));
    return new ConCallExpression(this, sortArgument, dataTypeArgs, args.subList(dataTypeArgsNumber, args.size()));
  }

  @Override
  public ConCallExpression getDefCall(Sort sortArgument, List<Expression> args) {
    throw new IllegalStateException();
  }
}