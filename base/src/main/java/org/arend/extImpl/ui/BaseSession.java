package org.arend.extImpl.ui;

import org.arend.ext.ui.ArendSession;
import org.arend.extImpl.Disableable;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public abstract class BaseSession extends Disableable implements ArendSession {
  public String description;
  public Consumer<Boolean> callback;

  @Override
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  @Override
  public void setCallback(@Nullable Consumer<Boolean> callback) {
    this.callback = callback;
  }
}
