package org.arend.frontend.repl.jline;

import org.arend.frontend.repl.CommonCliRepl;
import org.arend.repl.CommandHandler;
import org.arend.repl.action.PrettyPrintFlagCommand;
import org.arend.library.SourceLibrary;
import org.arend.repl.action.DirectoryArgumentCommand;
import org.arend.repl.action.FileArgumentCommand;
import org.arend.repl.action.NormalizeCommand;
import org.arend.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class JLineCliRepl extends CommonCliRepl {
  private final Terminal myTerminal;

  public JLineCliRepl(@NotNull Terminal terminal) {
    myTerminal = terminal;
  }

  @Override
  public void eprintln(Object anything) {
    println(new AttributedStringBuilder()
        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
        .append(String.valueOf(anything))
        .style(AttributedStyle.DEFAULT)
        .toAnsi());
  }

  @Override
  public void print(Object anything) {
    var writer = myTerminal.writer();
    writer.print(anything);
    writer.flush();
  }

  @Override
  public void println(Object anything) {
    myTerminal.writer().println(anything);
  }

  @Override
  public void println() {
    myTerminal.writer().println();
  }

  public void runRepl() {
    Path dir = FileUtils.USER_HOME.resolve(FileUtils.USER_CONFIG_DIR);
    Path history = dir.resolve("history");
    try {
      // Assuming user.home exists
      if (Files.notExists(dir) || Files.isRegularFile(dir)) {
        Files.deleteIfExists(dir);
        Files.createDirectory(dir);
      }
      if (Files.notExists(history) || Files.isDirectory(history)) {
        Files.deleteIfExists(history);
        Files.createFile(history);
      }
    } catch (IOException e) {
      eprintln("[ERROR] Failed to load REPL history: " + e.getLocalizedMessage());
      history = null;
    }
    var reader = LineReaderBuilder.builder()
      .appName(APP_NAME)
      .variable(LineReader.HISTORY_FILE, history)
      .history(new DefaultHistory())
      .parser(new ArendReplParser())
      .completer(new AggregateCompleter(
        new SpecialCommandCompleter(DirectoryArgumentCommand.class, new Completers.DirectoriesCompleter(() -> pwd)),
        new SpecialCommandCompleter(FileArgumentCommand.class, new Completers.FilesCompleter(() -> pwd)),
        new SpecialCommandCompleter(NormalizeCommand.class, new StringsCompleter(NormalizeCommand.AVAILABLE_OPTIONS)),
        new SpecialCommandCompleter(PrettyPrintFlagCommand.class, new StringsCompleter(PrettyPrintFlagCommand.AVAILABLE_OPTIONS)),
        new SpecialCommandCompleter(CommandHandler.HelpCommand.class, new StringsCompleter(CommandHandler.INSTANCE.commandMap.keySet())),
        new ScopeCompleter(this::getInScopeElements),
        new ImportCompleter(this::modulePaths),
        KeywordCompleter.INSTANCE,
        CommandsCompleter.INSTANCE
      ))
      .terminal(myTerminal)
      .build();
    while (true) try {
      if (repl(reader.readLine(prompt()), reader::readLine)) break;
    } catch (UserInterruptException ignored) {
    } catch (EndOfFileException e) {
      break;
    }
    saveUserConfig();
  }

  @NotNull
  private Stream<String> modulePaths() {
    return myLibraryManager.getRegisteredLibraries()
      .stream()
      .flatMap(library -> Stream.concat(
        library.getLoadedModules()
          .stream()
          .map(Objects::toString),
        library instanceof SourceLibrary
          ? ((SourceLibrary) library).getAdditionalModules()
          .stream()
          .map(Map.Entry::getKey)
          .map(Objects::toString)
          : Stream.empty()
      ));
  }

  public static void main(String... args) {
    launch(false, Collections.emptyList());
  }

  public static void launch(
    boolean recompile,
    @NotNull Collection<? extends Path> libDirs
  ) {
    Terminal terminal;
    try {
      terminal = TerminalBuilder
        .builder()
        .encoding("UTF-8")
        .jansi(true)
        .jna(false)
        .build();
    } catch (IOException e) {
      System.err.println("[FATAL] Failed to create terminal: " + e.getLocalizedMessage());
      System.exit(1);
      return;
    }
    var repl = new JLineCliRepl(terminal);
    repl.println(ASCII_BANNER);
    repl.println();
    repl.addLibraryDirectories(libDirs);
    if (recompile) repl.getReplLibrary().addFlag(SourceLibrary.Flag.RECOMPILE);
    repl.initialize();
    repl.runRepl();
  }

}
