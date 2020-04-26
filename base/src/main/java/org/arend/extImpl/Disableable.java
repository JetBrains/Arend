package org.arend.extImpl;

public class Disableable {
  private boolean myEnabled = true;

  public void disable() {
    myEnabled = false;
  }

  public void checkEnabled(String message) {
    if (!myEnabled) {
      throw new IllegalStateException(message);
    }
  }

  public void checkEnabled() {
    checkEnabled(getClass() + " was disabled");
  }

  public void checkAndDisable() {
    checkEnabled();
    disable();
  }

  public void checkAndDisable(String message) {
    checkEnabled(message);
    disable();
  }
}
