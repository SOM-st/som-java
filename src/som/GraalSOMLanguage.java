package som;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;

import som.compiler.ProgramDefinitionError;
import som.vm.Universe;
import som.vmobjects.SAbstractObject;


@TruffleLanguage.Registration(id = GraalSOMLanguage.ID,
    name = "GraalSOM",
    defaultMimeType = GraalSOMLanguage.MIME_TYPE,
    characterMimeTypes = GraalSOMLanguage.MIME_TYPE,
    contextPolicy = TruffleLanguage.ContextPolicy.SHARED,
    fileTypeDetectors = GSFileDetector.class)
@ProvidedTags({StandardTags.CallTag.class,
    StandardTags.StatementTag.class,
    StandardTags.RootTag.class,
    StandardTags.RootBodyTag.class,
    StandardTags.ExpressionTag.class,
    DebuggerTags.AlwaysHalt.class})
public final class GraalSOMLanguage extends TruffleLanguage<Universe> {

  public static final String ID           = "GS";
  public static final String MIME_TYPE    = "application/x-graal-som";
  public static String[]     args;
  public static final String START_SOURCE = "START";

  @Option(
      help = "For Testing purpose only - Selector of the test ran (see BasicInterpreterTests>>testSomeTest)",
      category = OptionCategory.USER) public static final OptionKey<String> TestSelector =
          new OptionKey<>("");

  @Option(
      help = "For Testing purpose only - Class of the test ran (see BasicInterpreterTests>>testSomeTest)",
      category = OptionCategory.USER) public static final OptionKey<String> TestClass =
          new OptionKey<>("");

  @Option(
      help = "For Testing purpose only - Required classpath to execute a given TestClass>>TestSelector (see BasicInterpreterTests>>testSomeTest)",
      category = OptionCategory.USER) public static final OptionKey<String> TestClasspath =
          new OptionKey<>("");

  public GraalSOMLanguage() {}

  @Override
  protected Universe createContext(final Env env) {
    return new Universe(env, this);
  }

  @Override
  protected OptionDescriptors getOptionDescriptors() {
    // this class is generated by the annotation processor
    return new GraalSOMLanguageOptionDescriptors();
  }

  @Override
  protected boolean isObjectOfLanguage(final Object object) {
    return object instanceof SAbstractObject;
  }

  public static Universe getCurrentContext() {
    return getCurrentContext(GraalSOMLanguage.class);
  }

  @Override
  protected CallTarget parse(final ParsingRequest request) throws Exception {
    Source code = request.getSource();
    Universe universe = getCurrentContext();
    args = universe.getEnv().getApplicationArguments();

    if (code.getCharacters().equals(START_SOURCE) && code.getName().equals(START_SOURCE)) {
      return Truffle.getRuntime().createCallTarget(new StartInterpretation(this, universe));
    }
    throw new Exception("An unknowm marker has been provided");
  }

  private static class StartInterpretation extends RootNode {

    private final Universe universe;

    protected StartInterpretation(final GraalSOMLanguage lang, final Universe universe) {
      super(lang, null);
      this.universe = universe;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      try {
        if (universe.isForTesting()) {
          return universe.interpret(universe.testClass(), universe.testSelector());
        } else {
          return universe.interpret(args);
        }
      } catch (ProgramDefinitionError | FrameSlotTypeException e) {
        GraalSOMLanguage.getCurrentContext().errorExit(e.getMessage());
        return 1;
      }
    }
  }
}
