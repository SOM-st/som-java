package som.interpreter;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;

import som.compiler.ProgramDefinitionError;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SMethod;


public final class Method extends Invokable {

  @CompilationFinal(dimensions = 1) final byte[] bytecodes;
  private final SMethod                          method;

  public Method(final TruffleLanguage<?> language, final int numberOfBytecodes,
      final SMethod method) {
    super(language);
    this.bytecodes = new byte[numberOfBytecodes];
    this.method = method;
  }

  @Override
  public Object execute(final VirtualFrame frame) throws ReturnException {
    Interpreter interpreter = (Interpreter) frame.getArguments()[0];
    final Frame newFrame = (Frame) frame.getArguments()[1];
    assert this.method == newFrame.getMethod();

    while (true) {
      try {
        SAbstractObject result = interpreter.start(newFrame, this.method);
        return result;
      } catch (ReturnException e) {
        if (e.hasReachedTarget(newFrame)) {
          SAbstractObject result = e.getResult();
          return result;
        }
        throw e;
      } catch (ProgramDefinitionError programDefinitionError) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        programDefinitionError.printStackTrace();
        System.exit(1);
      } catch (RestartLoopException rle) {
        newFrame.resetStackPointer();
      }
    }
  }

  public int getNumberOfBytecodes() {
    // Get the number of bytecodes in this method
    return bytecodes.length;
  }

  public byte getBytecode(final int index) {
    // Get the bytecode at the given index
    return bytecodes[index];
  }

  public void setBytecode(final int index, final byte value) {
    // Set the bytecode at the given index to the given value
    bytecodes[index] = value;
  }

  public byte[] getBytecodes() {
    return this.bytecodes;
  }

  @Override
  public String toString() {
    return method.toString();
  }
}
