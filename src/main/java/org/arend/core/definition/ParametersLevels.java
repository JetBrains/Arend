package org.arend.core.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParametersLevels<P extends ParametersLevel> {
  private List<P> myList = Collections.emptyList();

  public List<? extends P> getList() {
    return myList;
  }

  public void add(P parametersLevel) {
    if (myList.isEmpty()) {
      myList = new ArrayList<>();
    }

    for (P another : myList) {
      if (another.hasEquivalentDomain(parametersLevel)) {
        another.mergeCodomain(parametersLevel);
        return;
      }
    }

    myList.add(parametersLevel);
  }
}
