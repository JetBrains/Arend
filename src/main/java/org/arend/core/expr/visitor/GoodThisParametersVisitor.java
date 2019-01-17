package org.arend.core.expr.visitor;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.ConCallExpression;
import org.arend.core.expr.DefCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;

import java.util.*;

public class GoodThisParametersVisitor extends VoidExpressionVisitor<Void> {
  private final List<Boolean> myGoodParameters;
  private final Map<DependentLink, Integer> myIndexMap;

  public GoodThisParametersVisitor(DependentLink parameters) {
    if (!parameters.hasNext()) {
      myGoodParameters = new ArrayList<>(0);
      myIndexMap = Collections.emptyMap();
      return;
    }

    myGoodParameters = new ArrayList<>();
    myIndexMap = new HashMap<>();
    int i = 0;
    for (DependentLink link = parameters; link.hasNext(); link = link.getNext(), i++) {
      myGoodParameters.add(true);
      myIndexMap.put(link, i);
    }
    visitParameters(parameters, null);
  }

  public List<Boolean> getGoodParameters() {
    for (Boolean good : myGoodParameters) {
      if (good) {
        return myGoodParameters;
      }
    }
    return Collections.emptyList();
  }

  @Override
  public Void visitReference(ReferenceExpression expr, Void params) {
    if (expr.getBinding() instanceof DependentLink) {
      Integer index = myIndexMap.get(expr.getBinding());
      if (index != null) {
        myGoodParameters.set(index, false);
      }
    }
    return null;
  }

  private void visitArguments(List<? extends Expression> args, List<Boolean> goodParameters) {
    for (int i = 0; i < args.size(); i++) {
      if (!(args.get(i) instanceof ReferenceExpression && i < goodParameters.size() && goodParameters.get(i))) {
        args.get(i).accept(this, null);
      }
    }
  }

  @Override
  public Void visitDefCall(DefCallExpression expr, Void params) {
    visitArguments(expr.getDefCallArguments(), expr.getDefinition().getGoodThisParameters());
    return null;
  }

  @Override
  public Void visitConCall(ConCallExpression expr, Void params) {
    if (expr.getDefinition().getPatterns() == null) {
      visitArguments(expr.getDataTypeArguments(), expr.getDefinition().getDataType().getGoodThisParameters());
      visitArguments(expr.getDefCallArguments(), expr.getDefinition().getGoodThisParameters());
    } else {
      List<Expression> args = new ArrayList<>(expr.getDataTypeArguments());
      args.addAll(expr.getDefCallArguments());
      visitArguments(args, expr.getDefinition().getDataType().getGoodThisParameters());
    }
    return null;
  }
}
