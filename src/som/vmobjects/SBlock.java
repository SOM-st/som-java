/**
 * Copyright (c) 2009 Michael Haupt, michael.haupt@hpi.uni-potsdam.de
 * Software Architecture Group, Hasso Plattner Institute, Potsdam, Germany
 * http://www.hpi.uni-potsdam.de/swa/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package som.vmobjects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.profiles.ValueProfile;
import som.interpreter.Frame;
import som.interpreter.Interpreter;
import som.vm.Universe;


public class SBlock extends SAbstractObject {

  private final ValueProfile valueProfile;

  @CompilerDirectives.TruffleBoundary
  public final ValueProfile getValueProfile() {
    return valueProfile;
  }

  @CompilerDirectives.TruffleBoundary
  public SBlock(final SMethod method, final Frame context, final SClass blockClass) {
    this.method = method;
    this.context = context;
    this.blockClass = blockClass;
    this.valueProfile = ValueProfile.createClassProfile();
  }

  public SMethod getMethod() {
    return method;
  }

  public Frame getContext() {
    return context;
  }

  @Override
  public final SClass getSOMClassBis(final Universe universe,
      final ValueProfile classProfiled) {
    return classProfiled.profile(blockClass);
  }

  @Override
  public final SClass getSOMClass(final Universe universe) {
    return blockClass;
  }

  public static SPrimitive getEvaluationPrimitive(int numberOfArguments,
      final Universe universe) {
    return new Evaluation(numberOfArguments, universe);
  }

  public static class Evaluation extends SPrimitive {

    public Evaluation(int numberOfArguments, final Universe universe) {
      super(computeSignatureString(numberOfArguments), universe);
      this.numberOfArguments = numberOfArguments;
    }

    @Override
    public void invoke(final Frame frame, final Interpreter interpreter) {
      // Get the block (the receiver) from the stack
      SBlock self = (SBlock) frame.getStackElement(numberOfArguments - 1);

      // Get the context of the block...
      Frame context = self.getContext();

      // Push a new frame and set its context to be the one specified in
      // the block
      Frame newFrame = interpreter.newFrame(frame, self.getMethod(), context);
      newFrame.copyArgumentsFrom(frame);
      IndirectCallNode indirectCallNode = interpreter.getIndirectCallNode();

      SAbstractObject result =
          (SAbstractObject) indirectCallNode.call(self.getMethod().getCallTarget(),
              interpreter, newFrame);

      frame.popArgumentsAndPushResult(result, self.getMethod());
      newFrame.clearPreviousFrame();
    }

    private static java.lang.String computeSignatureString(int numberOfArguments) {
      // Compute the signature string
      java.lang.String signatureString = "value";
      if (numberOfArguments > 1) {
        signatureString += ":";
      }

      // Add extra value: selector elements if necessary
      for (int i = 2; i < numberOfArguments; i++) {
        signatureString += "with:";
      }

      // Return the signature string
      return signatureString;
    }

    private final int numberOfArguments;
  }

  private final SMethod method;
  private final Frame   context;
  private final SClass  blockClass;
}
