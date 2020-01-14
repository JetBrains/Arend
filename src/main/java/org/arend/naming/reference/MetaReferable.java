package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.MetaDefinition;

import javax.annotation.Nonnull;

public class MetaReferable implements GlobalReferable {
  private Precedence myPrecedence;
  private final String myName;
  private MetaDefinition myDefinition;

  public MetaReferable(Precedence precedence, String name, MetaDefinition definition) {
    myPrecedence = precedence;
    myName = name;
    myDefinition = definition;
  }

  public MetaDefinition getDefinition() {
    return myDefinition;
  }

  public void setDefinition(MetaDefinition definition) {
    myDefinition = definition;
  }

  public void setPrecedence(Precedence precedence) {
    myPrecedence = precedence;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    return myName;
  }

  @Nonnull
  @Override
  public Precedence getPrecedence() {
    return myPrecedence;
  }

  @Nonnull
  @Override
  public Kind getKind() {
    return Kind.OTHER;
  }
}
