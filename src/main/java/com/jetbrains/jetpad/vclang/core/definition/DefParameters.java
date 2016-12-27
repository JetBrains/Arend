package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DefParameters {
  private final DependentLink myParameters;
  private Map<DependentLink, Abstract.ClassView> myViews;

  public DefParameters(DependentLink parameters) {
    myParameters = parameters;
    myViews = Collections.emptyMap();
  }

  public DependentLink getParameters() {
    return myParameters;
  }

  public Abstract.ClassView getView(DependentLink parameter) {
    return myViews.get(parameter);
  }

  public void addView(DependentLink parameter, Abstract.ClassView view) {
    if (myViews.isEmpty()) {
      myViews = new HashMap<>();
    }
    myViews.put(parameter, view);
  }
}
