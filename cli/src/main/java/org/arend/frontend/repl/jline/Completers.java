package org.arend.frontend.repl.jline;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Supplier;

/**
 * Ported from https://github.com/jline/jline3/blob/81e38b948f8fce71b9f572b9b7a4414c8d6d63d9/builtins/src/main/java/org/jline/builtins/Completers.java
 * Avoid introducing a very big dependency (jline groovy).
 */
@SuppressWarnings("unused")
public class Completers {
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
  public static class FileNameCompleter implements Completer {

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