package org.arend.frontend.ui;

import org.arend.ext.ui.ArendQuery;
import org.arend.ext.ui.ArendSession;
import org.arend.extImpl.ui.BaseSession;
import org.arend.extImpl.ui.SimpleQuery;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CliSession extends BaseSession {
  private final static String TRUE_VALUE = "yes";
  private final static String FALSE_VALUE = "no";

  private final static class Request {
    public final String message;
    public final Object data;
    public final SimpleQuery<Object> query;
    public final Object defaultValue;

    public Request(String message, Object data, SimpleQuery<?> query, Object defaultValue) {
      this.message = message;
      this.data = data;
      //noinspection unchecked
      this.query = (SimpleQuery<Object>) query;
      this.defaultValue = defaultValue;
    }
  }

  private final List<Request> myRequests = new ArrayList<>();

  @Override
  public void message(@NotNull String message) {
    myRequests.add(new Request(message, null, null, null));
  }

  @Override
  public @NotNull <T> ArendQuery<T> listQuery(@Nullable String message, @NotNull List<T> options, @Nullable T defaultOption) {
    SimpleQuery<T> query = new SimpleQuery<>();
    myRequests.add(new Request(message, options, query, defaultOption));
    return query;
  }

  @Override
  public @NotNull ArendQuery<Boolean> binaryQuery(@Nullable String message, @Nullable Boolean defaultValue) {
    SimpleQuery<Boolean> query = new SimpleQuery<>();
    myRequests.add(new Request(message, false, query, defaultValue));
    return query;
  }

  @Override
  public @NotNull ArendQuery<String> stringQuery(@Nullable String message, @Nullable String defaultValue) {
    SimpleQuery<String> query = new SimpleQuery<>();
    myRequests.add(new Request(message, "", query, defaultValue));
    return query;
  }

  @Override
  public @NotNull ArendQuery<Integer> intQuery(@Nullable String message, @Nullable Integer defaultValue) {
    SimpleQuery<Integer> query = new SimpleQuery<>();
    myRequests.add(new Request(message, 0, query, defaultValue));
    return query;
  }

  @Override
  public void embedded(@NotNull ArendSession session) {
    myRequests.add(new Request(null, session, null, null));
  }

  private String readLine(BufferedReader reader) {
    System.out.print(": ");
    System.out.flush();
    try {
      String line = reader.readLine();
      if (line == null) {
        callback.accept(false);
      }
      return line;
    } catch (IOException e) {
      callback.accept(false);
      return null;
    }
  }

  private void printDefault(Object defaultValue) {
    System.out.print("[default: " + defaultValue + "]");
  }

  @Override
  public void startSession() {
    checkAndDisable();

    if (description != null) {
      System.out.println(description);
    }

    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    for (Request request : myRequests) {
      if (request.data instanceof ArendSession) {
        ((ArendSession) request.data).startSession();
        continue;
      }

      if (request.message != null) {
        System.out.println(request.message);
      }

      if (request.data == null) {
        continue;
      }

      Object defaultValue = request.defaultValue;
      if (request.data instanceof List) {
        List<?> list = (List<?>) request.data;
        for (int i = 0; i < list.size(); i++) {
          System.out.println(i + ") " + list.get(i));
        }
        if (defaultValue != null) {
          int defaultIndex = -1;
          for (int i = 0; i < list.size(); i++) {
            if (defaultValue.equals(list.get(i))) {
              defaultIndex = i;
              break;
            }
          }
          printDefault(defaultIndex >= 0 ? defaultIndex : defaultValue);
        }
      } else if (request.data instanceof Boolean) {
        System.out.print("("  + TRUE_VALUE + "/" + FALSE_VALUE + ")");
        if (defaultValue != null) {
          System.out.print(" ");
          printDefault(defaultValue == Boolean.TRUE ? TRUE_VALUE : FALSE_VALUE);
        }
      } else if (defaultValue != null) {
        printDefault(defaultValue);
      }

      while (true) {
        String line = readLine(reader);
        if (line == null) {
          return;
        }

        Object result = null;
        if (request.data instanceof List) {
          List<?> list = (List<?>) request.data;
          if (line.isEmpty()) {
            result = defaultValue;
          } else {
            int index;
            try {
              index = Integer.parseInt(line);
            } catch (NumberFormatException e) {
              index = -1;
            }
            if (index >= 1 && index <= list.size()) {
              result = list.get(index - 1);
            }
          }
          if (result == null) {
            System.out.println("Enter a number between 1 and " + list.size());
          }
        } else if (request.data instanceof Boolean) {
          if (line.equals(TRUE_VALUE)) {
            result = true;
          } else if (line.equals(FALSE_VALUE)) {
            result = false;
          } else if (line.isEmpty() && defaultValue != null) {
            result = defaultValue;
          } else {
            System.out.println("Enter \"" + TRUE_VALUE + "\" or \"" + FALSE_VALUE + "\"");
          }
        } else if (request.data instanceof String) {
          result = line.isEmpty() && defaultValue != null ? defaultValue : line;
        } else if (request.data instanceof Integer) {
          if (line.isEmpty()) {
            result = defaultValue;
          } else {
            try {
              result = Integer.parseInt(line);
            } catch (NumberFormatException ignored) {}
          }
          if (result == null) {
            System.out.println("Enter a number");
          }
        } else {
          throw new IllegalStateException();
        }

        if (result != null) {
          request.query.setResult(result);
          break;
        }
      }
    }

    if (callback != null) {
      callback.accept(true);
    }
  }
}
