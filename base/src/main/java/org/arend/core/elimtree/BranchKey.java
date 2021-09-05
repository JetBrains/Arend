package org.arend.core.elimtree;

import org.arend.core.context.param.DependentLink;
import org.arend.core.pattern.ConstructorExpressionPattern;

public interface BranchKey {
  DependentLink getParameters(ConstructorExpressionPattern pattern);
  Body getBody();
}
