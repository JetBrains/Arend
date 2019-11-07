package org.arend.core.definition;

import org.arend.core.pattern.Pattern;
import org.arend.naming.reference.TCReferable;

public class DConstructor extends FunctionDefinition {
  private int myNumberOfParameters;
  private Pattern myPattern;

  public DConstructor(TCReferable referable) {
    super(referable);
  }

  public int getNumberOfParameters() {
    return myNumberOfParameters;
  }

  public void setNumberOfParameters(int numberOfParameters) {
    myNumberOfParameters = numberOfParameters;
  }

  public Pattern getPattern() {
    return myPattern;
  }

  public void setPattern(Pattern pattern) {
    myPattern = pattern;
  }
}
