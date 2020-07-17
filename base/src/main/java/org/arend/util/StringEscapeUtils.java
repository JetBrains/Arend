package org.arend.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * IntelliJ's StringUtil is used as the primary
 * <a href="https://github.com/JetBrains/intellij-community/blob/master/platform/util/src/com/intellij/openapi/util/text/StringUtil.java">reference</a>.
 */
public class StringEscapeUtils {
  @Contract(pure = true)
  public static boolean isHexDigit(char c) {
    return '0' <= c && c <= '9' || 'a' <= c && c <= 'f' || 'A' <= c && c <= 'F';
  }

  @Contract(pure = true)
  public static boolean isOctalDigit(char c) {
    return '0' <= c && c <= '7';
  }

  @Contract(pure = true)
  public static @NotNull String unescapeStringCharacters(@NotNull String s) {
    var buffer = new StringBuilder(s.length());
    unescapeStringCharacters(s.length(), s, buffer);
    return buffer.toString();
  }

  private static void unescapeStringCharacters(int length, @NotNull String s, @NotNull StringBuilder buffer) {
    var escaped = false;
    for (int idx = 0; idx < length; idx++) {
      char ch = s.charAt(idx);
      if (!escaped) {
        if (ch == '\\') {
          escaped = true;
        } else {
          buffer.append(ch);
        }
      } else {
        int octalEscapeMaxLength = 2;
        switch (ch) {
          case 'n':
            buffer.append('\n');
            break;

          case 'r':
            buffer.append('\r');
            break;

          case 'b':
            buffer.append('\b');
            break;

          case 't':
            buffer.append('\t');
            break;

          case 'f':
            buffer.append('\f');
            break;

          case '\'':
            buffer.append('\'');
            break;

          case '\"':
            buffer.append('\"');
            break;

          case '\\':
            buffer.append('\\');
            break;

          case 'u':
            if (idx + 4 < length) {
              try {
                int code = Integer.parseInt(s.substring(idx + 1, idx + 5), 16);
                idx += 4;
                buffer.append((char) code);
              } catch (NumberFormatException e) {
                buffer.append("\\u");
              }
            } else {
              buffer.append("\\u");
            }
            break;

          case '0':
          case '1':
          case '2':
          case '3':
            octalEscapeMaxLength = 3;
          case '4':
          case '5':
          case '6':
          case '7':
            int escapeEnd = idx + 1;
            while (escapeEnd < length && escapeEnd < idx + octalEscapeMaxLength && isOctalDigit(s.charAt(escapeEnd)))
              escapeEnd++;
            try {
              buffer.append((char) Integer.parseInt(s.substring(idx, escapeEnd), 8));
            } catch (NumberFormatException e) {
              throw new RuntimeException("Couldn't parse " + s.substring(idx, escapeEnd), e);
              // ^ shouldn't happen
            }
            idx = escapeEnd - 1;
            break;

          default:
            buffer.append(ch);
            break;
        }
        escaped = false;
      }
    }

    if (escaped) buffer.append('\\');
  }

  @Contract(pure = true)
  public static @NotNull String escapeStringCharacters(@NotNull String s) {
    var buffer = new StringBuilder(s.length());
    escapeStringCharacters(s.length(), s, "\"", buffer);
    return buffer.toString();
  }

  @Contract(pure = true)
  public static @NotNull String escapeCharCharacters(@NotNull String s) {
    var buffer = new StringBuilder(s.length());
    escapeStringCharacters(s.length(), s, "'", buffer);
    return buffer.toString();
  }


  public static @NotNull StringBuilder escapeStringCharacters(
    int length,
    @NotNull String str,
    @Nullable String additionalChars,
    @NotNull StringBuilder buffer
  ) {
    return escapeStringCharacters(length, str, additionalChars, true, buffer);
  }

  public static @NotNull StringBuilder escapeStringCharacters(
    int length,
    @NotNull String str,
    @Nullable String additionalChars,
    boolean escapeSlash,
    @NotNull StringBuilder buffer
  ) {
    return escapeStringCharacters(length, str, additionalChars, escapeSlash, true, buffer);
  }

  public static @NotNull StringBuilder escapeStringCharacters(
    int length,
    @NotNull String str,
    @Nullable String additionalChars,
    boolean escapeSlash,
    boolean escapeUnicode,
    @NotNull StringBuilder buffer
  ) {
    char prev = 0;
    for (int idx = 0; idx < length; idx++) {
      var ch = str.charAt(idx);
      switch (ch) {
        case '\b':
          buffer.append("\\b");
          break;

        case '\t':
          buffer.append("\\t");
          break;

        case '\n':
          buffer.append("\\n");
          break;

        case '\f':
          buffer.append("\\f");
          break;

        case '\r':
          buffer.append("\\r");
          break;

        default:
          if (escapeSlash && ch == '\\') {
            buffer.append("\\\\");
          } else if (additionalChars != null && additionalChars.indexOf(ch) > -1 && (escapeSlash || prev != '\\')) {
            buffer.append("\\").append(ch);
          } else if (escapeUnicode && !isPrintableUnicode(ch)) {
            var hexCode = Integer.toHexString(ch).toUpperCase();
            buffer.append("\\u");
            var paddingCount = 4 - hexCode.length();
            while (paddingCount --> 0) {
              buffer.append(0);
            }
            buffer.append(hexCode);
          } else {
            buffer.append(ch);
          }
      }
      prev = ch;
    }
    return buffer;
  }

  @Contract(pure = true)
  public static boolean isPrintableUnicode(char c) {
    var t = Character.getType(c);
    return t != Character.UNASSIGNED && t != Character.LINE_SEPARATOR && t != Character.PARAGRAPH_SEPARATOR &&
      t != Character.CONTROL && t != Character.FORMAT && t != Character.PRIVATE_USE && t != Character.SURROGATE;
  }
}
