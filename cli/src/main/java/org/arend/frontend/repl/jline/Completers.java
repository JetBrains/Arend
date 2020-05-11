package org.arend.frontend.repl.jline;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;

/**
 * Ported from https://github.com/jline/jline3/blob/81e38b948f8fce71b9f572b9b7a4414c8d6d63d9/builtins/src/main/java/org/jline/builtins/Completers.java
 * Avoid introducing a very big dependency (jline groovy).
 */
@SuppressWarnings("unused")
public class Completers {

  public interface CompletionEnvironment {
    Map<String, List<CompletionData>> getCompletions();

    Set<String> getCommands();

    String resolveCommand(String command);

    String commandName(String command);

    Object evaluate(LineReader reader, ParsedLine line, String func) throws Exception;
  }

  public static class CompletionData {
    public final List<String> options;
    public final String description;
    public final String argument;
    public final String condition;

    public CompletionData(List<String> options, String description, String argument, String condition) {
      this.options = options;
      this.description = description;
      this.argument = argument;
      this.condition = condition;
    }
  }

  public static class Completer implements org.jline.reader.Completer {

    private final CompletionEnvironment environment;

    public Completer(CompletionEnvironment environment) {
      this.environment = environment;
    }

    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
      if (line.wordIndex() == 0) {
        completeCommand(candidates);
      } else {
        tryCompleteArguments(reader, line, candidates);
      }
    }

    protected void tryCompleteArguments(LineReader reader, ParsedLine line, List<Candidate> candidates) {
      String command = line.words().get(0);
      String resolved = environment.resolveCommand(command);
      Map<String, List<CompletionData>> comp = environment.getCompletions();
      if (comp != null) {
        List<CompletionData> cmd = comp.get(resolved);
        if (cmd != null) {
          completeCommandArguments(reader, line, candidates, cmd);
        }
      }
    }

    protected void completeCommandArguments(LineReader reader, ParsedLine line, List<Candidate> candidates, List<CompletionData> completions) {
      for (CompletionData completion : completions) {
        boolean isOption = line.word().startsWith("-");
        String prevOption = line.wordIndex() >= 2 && line.words().get(line.wordIndex() - 1).startsWith("-")
          ? line.words().get(line.wordIndex() - 1) : null;
        String key = UUID.randomUUID().toString();
        boolean conditionValue = true;
        if (completion.condition != null) {
          Object res = Boolean.FALSE;
          try {
            res = environment.evaluate(reader, line, completion.condition);
          } catch (Throwable ignored) {
            // ignored.getCause();
            // Ignore
          }
          conditionValue = isTrue(res);
        }
        if (conditionValue && isOption && completion.options != null) {
          for (String opt : completion.options) {
            candidates.add(new Candidate(opt, opt, "options", completion.description, null, key, true));
          }
        } else if (!isOption && prevOption != null && completion.argument != null
          && (completion.options != null && completion.options.contains(prevOption))) {
          Object res = null;
          try {
            res = environment.evaluate(reader, line, completion.argument);
          } catch (Throwable t) {
            // Ignore
          }
          if (res instanceof Candidate) {
            candidates.add((Candidate) res);
          } else if (res instanceof String) {
            candidates.add(new Candidate((String) res, (String) res, null, null, null, null, true));
          } else if (res instanceof Collection<?>) {
            for (Object s : (Collection<?>) res) {
              if (s instanceof Candidate) {
                candidates.add((Candidate) s);
              } else if (s instanceof String) {
                candidates.add(new Candidate((String) s, (String) s, null, null, null, null, true));
              }
            }
          } else if (res != null && res.getClass().isArray()) {
            for (int i = 0, l = Array.getLength(res); i < l; i++) {
              Object s = Array.get(res, i);
              if (s instanceof Candidate) {
                candidates.add((Candidate) s);
              } else if (s instanceof String) {
                candidates.add(new Candidate((String) s, (String) s, null, null, null, null, true));
              }
            }
          }
        } else if (!isOption && completion.argument != null) {
          Object res = null;
          try {
            res = environment.evaluate(reader, line, completion.argument);
          } catch (Throwable t) {
            // Ignore
          }
          if (res instanceof Candidate) {
            candidates.add((Candidate) res);
          } else if (res instanceof String) {
            candidates.add(new Candidate((String) res, (String) res, null, completion.description, null, null, true));
          } else if (res instanceof Collection) {
            for (Object s : (Collection<?>) res) {
              if (s instanceof Candidate) {
                candidates.add((Candidate) s);
              } else if (s instanceof String) {
                candidates.add(new Candidate((String) s, (String) s, null, completion.description, null, null, true));
              }
            }
          }
        }
      }
    }

    protected void completeCommand(List<Candidate> candidates) {
      Set<String> commands = environment.getCommands();
      for (String command : commands) {
        String name = environment.commandName(command);
        boolean resolved = command.equals(environment.resolveCommand(name));
        if (!name.startsWith("_")) {
          String desc = null;
          Map<String, List<CompletionData>> comp = environment.getCompletions();
          if (comp != null) {
            List<CompletionData> completions = comp.get(command);
            if (completions != null) {
              for (CompletionData completion : completions) {
                if (completion.description != null
                  && completion.options == null
                  && completion.argument == null
                  && completion.condition == null) {
                  desc = completion.description;
                }
              }
            }
          }
          String key = UUID.randomUUID().toString();
          if (desc != null) {
            candidates.add(new Candidate(command, command, null, desc, null, key, true));
            if (resolved) {
              candidates.add(new Candidate(name, name, null, desc, null, key, true));
            }
          } else {
            candidates.add(new Candidate(command, command, null, null, null, key, true));
            if (resolved) {
              candidates.add(new Candidate(name, name, null, null, null, key, true));
            }
          }
        }
      }
    }

    private boolean isTrue(Object result) {
      if (result == null)
        return false;
      if (result instanceof Boolean)
        return (Boolean) result;
      if (result instanceof Number && 0 == ((Number) result).intValue()) {
        return false;
      }
      return !("".equals(result) || "0".equals(result));

    }

  }

  public static class DirectoriesCompleter extends FileNameCompleter {

    private final Supplier<Path> currentDir;
    private final boolean forceSlash;

    public DirectoriesCompleter(File currentDir) {
      this(currentDir.toPath(), false);
    }

    public DirectoriesCompleter(File currentDir, boolean forceSlash) {
      this(currentDir.toPath(), forceSlash);
    }

    public DirectoriesCompleter(Path currentDir) {
      this(currentDir, false);
    }

    public DirectoriesCompleter(Path currentDir, boolean forceSlash) {
      this.currentDir = () -> currentDir;
      this.forceSlash = forceSlash;
    }

    public DirectoriesCompleter(Supplier<Path> currentDir) {
      this(currentDir, false);
    }

    public DirectoriesCompleter(Supplier<Path> currentDir, boolean forceSlash) {
      this.currentDir = currentDir;
      this.forceSlash = forceSlash;
    }

    @Override
    protected Path getUserDir() {
      return currentDir.get();
    }

    @Override
    protected String getSeparator(boolean useForwardSlash) {
      return forceSlash || useForwardSlash ? "/" : getUserDir().getFileSystem().getSeparator();
    }

    @Override
    protected boolean accept(Path path) {
      return Files.isDirectory(path) && super.accept(path);
    }
  }

  public static class FilesCompleter extends FileNameCompleter {

    private final Supplier<Path> currentDir;
    private final boolean forceSlash;

    public FilesCompleter(File currentDir) {
      this(currentDir.toPath(), false);
    }

    public FilesCompleter(File currentDir, boolean forceSlash) {
      this(currentDir.toPath(), forceSlash);
    }

    public FilesCompleter(Path currentDir) {
      this(currentDir, false);
    }

    public FilesCompleter(Path currentDir, boolean forceSlash) {
      this.currentDir = () -> currentDir;
      this.forceSlash = forceSlash;
    }

    public FilesCompleter(Supplier<Path> currentDir) {
      this(currentDir, false);
    }

    public FilesCompleter(Supplier<Path> currentDir, boolean forceSlash) {
      this.currentDir = currentDir;
      this.forceSlash = forceSlash;
    }

    @Override
    protected Path getUserDir() {
      return currentDir.get();
    }

    @Override
    protected String getSeparator(boolean useForwardSlash) {
      return forceSlash || useForwardSlash ? "/" : getUserDir().getFileSystem().getSeparator();
    }
  }

  /**
   * A file name completer takes the buffer and issues a list of
   * potential completions.
   * <p>
   * This completer tries to behave as similar as possible to
   * <i>bash</i>'s file name completion (using GNU readline)
   * with the following exceptions:
   * <ul>
   * <li>Candidates that are directories will end with "/"</li>
   * <li>Wildcard regular expressions are not evaluated or replaced</li>
   * <li>The "~" character can be used to represent the user's home,
   * but it cannot complete to other users' homes, since java does
   * not provide any way of determining that easily</li>
   * </ul>
   *
   * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
   * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
   * @since 2.3
   */
  public static class FileNameCompleter implements org.jline.reader.Completer {

    public void complete(LineReader reader, ParsedLine commandLine, final List<Candidate> candidates) {
      assert commandLine != null;
      assert candidates != null;

      String buffer = commandLine.word().substring(0, commandLine.wordCursor());

      Path current;
      String curBuf;
      String sep = getSeparator(reader.isSet(LineReader.Option.USE_FORWARD_SLASH));
      int lastSep = buffer.lastIndexOf(sep);
      try {
        if (lastSep >= 0) {
          curBuf = buffer.substring(0, lastSep + 1);
          if (curBuf.startsWith("~")) {
            if (curBuf.startsWith("~" + sep)) {
              current = getUserHome().resolve(curBuf.substring(2));
            } else {
              current = getUserHome().getParent().resolve(curBuf.substring(1));
            }
          } else {
            current = getUserDir().resolve(curBuf);
          }
        } else {
          curBuf = "";
          current = getUserDir();
        }
        try (DirectoryStream<Path> directory = Files.newDirectoryStream(current, this::accept)) {
          directory.forEach(p -> {
            String value = curBuf + p.getFileName().toString();
            if (Files.isDirectory(p)) {
              candidates.add(
                new Candidate(value + (reader.isSet(LineReader.Option.AUTO_PARAM_SLASH) ? sep : ""),
                  getDisplay(reader.getTerminal(), p), null, null,
                  reader.isSet(LineReader.Option.AUTO_REMOVE_SLASH) ? sep : null, null, false));
            } else {
              candidates.add(new Candidate(value, getDisplay(reader.getTerminal(), p), null, null, null, null,
                true));
            }
          });
        } catch (IOException e) {
          // Ignore
        }
      } catch (Exception e) {
        // Ignore
      }
    }

    protected boolean accept(Path path) {
      try {
        return !Files.isHidden(path);
      } catch (IOException e) {
        return false;
      }
    }

    protected Path getUserDir() {
      return Paths.get(System.getProperty("user.dir"));
    }

    protected Path getUserHome() {
      return Paths.get(System.getProperty("user.home"));
    }

    protected String getSeparator(boolean useForwardSlash) {
      return useForwardSlash ? "/" : getUserDir().getFileSystem().getSeparator();
    }

    protected static String getDisplay(Terminal terminal, Path p) {
      // TODO: use $LS_COLORS for output
      String name = p.getFileName().toString();
      if (Files.isDirectory(p)) {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.styled(AttributedStyle.BOLD.foreground(AttributedStyle.RED), name);
        sb.append("/");
        name = sb.toAnsi(terminal);
      } else if (Files.isSymbolicLink(p)) {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.styled(AttributedStyle.BOLD.foreground(AttributedStyle.RED), name);
        sb.append("@");
        name = sb.toAnsi(terminal);
      }
      return name;
    }

  }
}