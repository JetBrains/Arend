package org.arend.frontend.repl.action;

import org.arend.ext.module.ModulePath;
import org.arend.frontend.repl.CommonCliRepl;
import org.arend.naming.scope.Scope;
import org.arend.util.FileUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class LoadModuleCommand implements CliReplCommand {
  public static final @NotNull LoadModuleCommand INSTANCE = new LoadModuleCommand();

  private LoadModuleCommand() {
  }

  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description() {
    return "Load an Arend module from working directory";
  }

  @Override
  public void invoke(@NotNull String line, @NotNull CommonCliRepl api, @NotNull Supplier<@NotNull String> scanner) {
    try {
      if (line.endsWith(FileUtils.EXTENSION)) {
        line = line.substring(0, line.length() - FileUtils.EXTENSION.length());
        var paths = StreamSupport
          .stream(Paths.get(line).normalize().spliterator(), false)
          .map(Objects::toString)
          .collect(Collectors.toList());
        if (Objects.equals(paths.get(0), ".")) paths.remove(0);
        line = String.join(".", paths);
      }
      loadModule(api, ModulePath.fromString(line));
    } catch (InvalidPathException e) {
      api.eprintln("The path `" + line + "` is not good because:");
      api.eprintln(e.getLocalizedMessage());
    }
  }

  private static void loadModule(@NotNull CommonCliRepl api, ModulePath modulePath) {
    Scope existingScope = api.getAvailableModuleScopeProvider().forModule(modulePath);
    if (existingScope != null) api.removeScope(existingScope);
    Scope scope = api.loadModule(modulePath);
    if (scope != null) api.addScope(scope);
    else api.println("[INFO] No module loaded.");
    if (!api.checkErrors()) ReloadModuleCommand.lastModulePath = modulePath;
    else api.unloadModule(modulePath);
  }

  public static class ReloadModuleCommand implements CliReplCommand {
    public static final @NotNull ReloadModuleCommand INSTANCE = new ReloadModuleCommand();

    private ReloadModuleCommand() {
    }

    private volatile static @Nullable ModulePath lastModulePath = null;

    @Override
    public void invoke(@NotNull String line, @NotNull CommonCliRepl api, @NotNull Supplier<@NotNull String> scanner) {
      if (lastModulePath != null)
        LoadModuleCommand.loadModule(api, lastModulePath);
      else api.eprintln("[ERROR] No previous module to load.");
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description() {
      return lastModulePath == null
          ? "Reload the module loaded last time"
          : "Reload module `" + lastModulePath + "`";
    }
  }
}
