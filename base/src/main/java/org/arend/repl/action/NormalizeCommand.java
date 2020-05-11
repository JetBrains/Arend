package org.arend.repl.action;

import org.arend.ext.core.ops.NormalizationMode;
import org.arend.repl.Repl;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class NormalizeCommand implements ReplCommand {
  public static final @NotNull NormalizeCommand INSTANCE = new NormalizeCommand();

  private NormalizeCommand() {
  }

  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description() {
    return "Modify the normalization level of printed expressions";
  }

  @Override
  public void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> scanner) {
    if ("null".equalsIgnoreCase(line) || line.isBlank()) {
      api.println("[INFO] Unset normalization mode.");
      api.setNormalizationMode(null);
    } else {
      var mode = line.trim();
      boolean found = false;
      for (var normalizationMode : NormalizationMode.values())
        if (normalizationMode.name().equalsIgnoreCase(mode)) {
          found = true;
          api.setNormalizationMode(normalizationMode);
          break;
        }
      if (!found) api.eprintln("[ERROR] Unrecognized normalization level: " + mode);
    }
  }
}
