package org.arend.core.expr.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.elimtree.*;
import org.arend.core.expr.*;
import org.arend.core.pattern.BindingPattern;
import org.arend.core.pattern.Pattern;

import java.util.*;

public class GoodThisParametersVisitor extends VoidExpressionVisitor<Void> {
  private final List<Boolean> myGoodParameters;
  private final Map<DependentLink, Integer> myIndexMap;
  private final Set<ClassField> myGoodFields;

  public GoodThisParametersVisitor(DependentLink parameters) {
    myGoodFields = Collections.emptySet();
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

  public GoodThisParametersVisitor(ElimBody elimBody, int numberOfParameters) {
    myGoodFields = Collections.emptySet();
    myGoodParameters = new ArrayList<>();
    for (int i = 0; i < numberOfParameters; i++) {
      myGoodParameters.add(true);
    }
    myIndexMap = new HashMap<>();

    for (ElimClause<Pattern> clause : elimBody.getClauses()) {
      for (int i = 0; i < numberOfParameters; i++) {
        Pattern pattern = clause.getPatterns().get(i);
        if (pattern instanceof BindingPattern) {
          myIndexMap.put(pattern.getFirstBinding(), i);
        } else {
          myGoodParameters.set(i, false);
        }
      }
      if (clause.getExpression() != null) {
        clause.getExpression().accept(this, null);
      }
      myIndexMap.clear();
    }
  }

  public GoodThisParametersVisitor(Expression expression, DependentLink parameters) {
    myGoodFields = Collections.emptySet();
    myGoodParameters = new ArrayList<>();
    myIndexMap = new HashMap<>();

    int index = 0;
    for (DependentLink link = parameters; link.hasNext(); link = link.getNext()) {
      myGoodParameters.add(true);
      myIndexMap.put(link, index);
      index++;
    }

    expression.accept(this, null);
  }

  public GoodThisParametersVisitor(Set<ClassField> fields) {
    myGoodFields = fields;
    myGoodParameters = new ArrayList<>(0);
    myIndexMap = Collections.emptyMap();
  }

  public List<Boolean> getGoodParameters() {
    for (Boolean good : myGoodParameters) {
      if (good) {
        return myGoodParameters;
      }
    }
    return Collections.emptyList();
  }

  public Set<ClassField> getGoodFields() {
    return myGoodFields.isEmpty() ? Collections.emptySet() : myGoodFields;
  }

  @Override
  public Void visitReference(ReferenceExpression expr, Void params) {
    Binding binding = expr.getBinding();
    if (binding instanceof DependentLink) {
      Integer index = myIndexMap.get(binding);
      if (index != null) {
        myGoodParameters.set(index, false);
      }
    }
    return null;
  }

  private void visitArguments(List<? extends Expression> args, List<Boolean> goodParameters) {
    for (int i = 0; i < args.size(); i++) {
      if (args.get(i) instanceof FieldCallExpression && i < goodParameters.size() && goodParameters.get(i)) {
        visitDefCall((FieldCallExpression) args.get(i), null);
      } else if (!(args.get(i) instanceof ReferenceExpression && i < goodParameters.size() && goodParameters.get(i))) {
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
      visitArguments(args, expr.getDefinition().getGoodThisParameters());
    }
    return null;
  }

  @Override
  public Void visitClassCall(ClassCallExpression expr, Void params) {
    for (Map.Entry<ClassField, Expression> entry : expr.getImplementedHere().entrySet()) {
      if (entry.getValue() instanceof FieldCallExpression && expr.getDefinition().isGoodField(entry.getKey())) {
        visitDefCall((FieldCallExpression) entry.getValue(), null);
      } else if (!(entry.getValue() instanceof ReferenceExpression && expr.getDefinition().isGoodField(entry.getKey()))) {
        entry.getValue().accept(this, null);
      }
    }
    return null;
  }

  @Override
  public Void visitFieldCall(FieldCallExpression expr, Void params) {
    if (!myGoodFields.isEmpty()) {
      myGoodFields.remove(expr.getDefinition());
    }
    return super.visitFieldCall(expr, params);
  }
}
