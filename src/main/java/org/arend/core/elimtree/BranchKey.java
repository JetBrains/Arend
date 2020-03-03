package org.arend.core.elimtree;

import org.arend.core.context.param.DependentLink;

public interface BranchKey {
  int getNumberOfParameters();
  DependentLink getParameters();
  Body getBody();
}
