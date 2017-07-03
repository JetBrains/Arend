package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.elimtree.BranchElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.Pattern;
import com.jetbrains.jetpad.vclang.core.elimtree.Patterns;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Constructor extends Definition implements Function, BranchElimTree.Pattern {
  private final DataDefinition myDataType;
  private DependentLink myParameters;
  private Patterns myPatterns;
  private ElimTree myConditions;

  public Constructor(Abstract.Constructor abstractDef, DataDefinition dataType) {
    super(abstractDef, TypeCheckingStatus.HEADER_HAS_ERRORS);
    myDataType = dataType;
    myParameters = null;
  }

  public void setElimTree(ElimTree condition) {
    myConditions = condition;
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
  public ElimTree getElimTree() {
    return myConditions;
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
    return myPatterns == null ? myDataType.getParameters() : myPatterns.getFirstBinding();
  }

  public List<Expression> matchDataTypeArguments(List<Expression> arguments) {
    assert myParameters != null && myDataType.status().headerIsOK();
    if (myPatterns == null) {
      return arguments;
    } else {
      List<Expression> result = new ArrayList<>();
      return myPatterns.match(arguments, result) == Pattern.MatchResult.OK ? result : null;
    }
  }

  public DataCallExpression getDataTypeExpression(Sort sortArgument) {
    return getDataTypeExpression(sortArgument, null);
  }

  public DataCallExpression getDataTypeExpression(Sort sortArgument, List<? extends Expression> dataTypeArguments) {
    assert myParameters != null && myDataType.status().headerIsOK();

    List<Expression> arguments;
    if (myPatterns == null) {
      if (dataTypeArguments == null) {
        arguments = new ArrayList<>();
        for (DependentLink link = getDataTypeParameters(); link.hasNext(); link = link.getNext()) {
          arguments.add(new ReferenceExpression(link));
        }
      } else {
        arguments = new ArrayList<>(dataTypeArguments);
      }
    } else {
      if (dataTypeArguments == null) {
        arguments = myPatterns.getPatternList().stream().map(Pattern::toExpression).collect(Collectors.toList());
      } else {
        ExprSubstitution substitution = new ExprSubstitution();
        DependentLink link = myPatterns.getFirstBinding();
        for (Expression argument : dataTypeArguments) {
          substitution.add(link, argument);
          link = link.getNext();
        }
        arguments = myPatterns.getPatternList().stream().map(pattern -> pattern.toExpression().subst(substitution)).collect(Collectors.toList());
      }
    }

    return myDataType.getDefCall(sortArgument, arguments);
  }

  @Override
  public DataCallExpression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument) {
    if (myParameters == null) {
      return null;
    }

    LevelSubstitution polySubst = sortArgument.toLevelSubstitution();
    DataCallExpression resultType = getDataTypeExpression(sortArgument);
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