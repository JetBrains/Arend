package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DefParameters {
  private final DependentLink myParameters;
  private Map<DependentLink, ClassField> myViews;

  public DefParameters(DependentLink parameters) {
    myParameters = parameters;
    myViews = Collections.emptyMap();
  }

  public DependentLink getParameters() {
    return myParameters;
  }

  public ClassField getClassifyingField(DependentLink parameter) {
    return myViews.get(parameter);
  }

  public void addClassifyingField(DependentLink parameter, ClassField field) {
    if (myViews.isEmpty()) {
      myViews = new HashMap<>();
    }
    myViews.put(parameter, field);
  }
}
