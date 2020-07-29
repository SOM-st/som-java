package som.interpreter;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.nodes.RootNode;


public abstract class Invokable extends RootNode {

  protected Invokable(TruffleLanguage<?> language) {
    super(language);
  }

}
