package som;

import java.io.IOException;
import java.io.PrintStream;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;


public class Launcher {

  /** This source is a marker to start execution based on the arguments provided. */
  public static final Source START = createMarkerSource(GraalSOMLanguage.START_SOURCE);

  public static void main(final String[] arguments) throws IOException {
    System.exit(executeSource(START, arguments));
  }

  private static int executeSource(final Source source, final String[] arguments)
      throws IOException {
    Context context;
    PrintStream err = System.err;

    Context.Builder builder = Context.newBuilder();
    builder.arguments(GraalSOMLanguage.ID, arguments);
    context = builder.build();

    // Try to evaluate the source code
    try {
      Value result = context.eval(source);
      return 0;
    } catch (PolyglotException ex) {
      if (ex.isInternalError()) {
        // for internal errors we print the full stack trace
        ex.printStackTrace();
      } else {
        err.println(ex.getMessage());
      }
      return 1;
    } finally {
      context.close();
    }
  }

  private static Source createMarkerSource(final String marker) {
    try {
      return Source.newBuilder(GraalSOMLanguage.ID, marker, marker).internal(true).build();
    } catch (IOException e) {
      // should never happen
      throw new RuntimeException(e);
    }
  }
}
