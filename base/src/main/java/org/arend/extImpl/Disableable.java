package org.arend.extImpl;

public class Disableable {
  private boolean myEnabled = true;

  public void disable() {
    myEnabled = false;
  }

  protected void checkEnabled(String message) {
    if (!myEnabled) {
      throw new IllegalStateException(message);
    }
  }

  protected void checkEnabled() {
    checkEnabled(getClass() + " was disabled");
  }
}
