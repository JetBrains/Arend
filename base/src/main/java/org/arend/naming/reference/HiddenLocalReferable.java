package org.arend.naming.reference;

public class HiddenLocalReferable extends LocalReferable {
  public HiddenLocalReferable(String name) {
    super(name);
  }

  @Override
  public boolean isHidden() {
    return true;
  }
}
