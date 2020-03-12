package org.arend.extImpl;

public class Disableable {
  private boolean myEnabled = true;

  public void disable() {
    myEnabled = false;
  }

  protected void checkEnabled() {
    if (!myEnabled) {
      throw new IllegalStateException(getClass() + " was disabled");
    }
  }
}
