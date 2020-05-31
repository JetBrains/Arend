package org.arend.frontend.repl.action;

import org.arend.frontend.repl.CommonCliRepl;
import org.arend.repl.action.DirectoryArgumentCommand;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

public final class CdCommand implements CliReplCommand, DirectoryArgumentCommand {
  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description() {
    return "Modify current working directory";
  }

  @Override
  public void invoke(@NotNull String line, @NotNull CommonCliRepl api, @NotNull Supplier<@NotNull String> scanner) {
    if (line.isBlank()) {
      api.pwd = CommonCliRepl.USER_HOME;
      return;
    }
    Path newPath = api.pwd.resolve(line).normalize();
    if (Files.notExists(newPath)) {
      api.eprintln("[ERROR] No such file or directory: `" + newPath + "`.");
      return;
    }
    if (Files.isDirectory(newPath))
      api.pwd = newPath;
    else {
      api.eprintln("[ERROR] Directory expected, found file: `" + newPath + "`.");
    }
  }
}
