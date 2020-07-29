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

package som.interpreter;

import som.vm.Universe;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SMethod;
import som.vmobjects.SObject;


/**
 * @formatter:off
 * Frame layout:
 *
 * +-----------------+
 * | Arguments       | 0
 * +-----------------+
 * | Local Variables | <-- localOffset
 * +-----------------+
 * | Stack           | <-- stackPointer
 * | ...             |
 * +-----------------+
 * @formatter:on
 */
public class Frame {

  private int stackPointer;

  // the offset at which local variables start
  private int localOffset;

  private final SMethod           method;
  private final Frame             context;
  private Frame                   previousFrame;
  private final SAbstractObject[] stack;

  public Frame(final SObject nilObject, final Frame previousFrame,
      final Frame context, final SMethod method, final long stackElements) {
    this.previousFrame = previousFrame;
    this.context = context;
    this.method = method;
    this.stack = new SAbstractObject[(int) stackElements];

    for (int i = 0; i < stackElements; i++) {
      stack[i] = nilObject;
    }
  }

  public Frame getPreviousFrame() {
    return previousFrame;
  }

  public void clearPreviousFrame() {
    previousFrame = null;
  }

  public boolean hasPreviousFrame(final SAbstractObject nilObject) {
    return previousFrame != null;
  }

  public boolean isBootstrapFrame(final SAbstractObject nilObject) {
    return !hasPreviousFrame(nilObject);
  }

  public Frame getContext() {
    return context;
  }

  public boolean hasContext() {
    return context != null;
  }

  public Frame getContext(int level) {
    // Get the context frame at the given level
    Frame frame = this;

    // Iterate through the context chain until the given level is reached
    while (level > 0) {
      // Get the context of the current frame
      frame = frame.getContext();

      // Go to the next level
      level = level - 1;
    }

    // Return the found context
    return frame;
  }

  public Frame getOuterContext(final SAbstractObject nilObject) {
    // Compute the outer context of this frame
    Frame frame = this;

    // Iterate through the context chain until null is reached
    while (frame.hasContext()) {
      frame = frame.getContext();
    }

    // Return the outer context
    return frame;
  }

  public SMethod getMethod() {
    return method;
  }

  public SAbstractObject pop() {
    // Pop an object from the expression stack and return it
    int stackPointer = getStackPointer();
    setStackPointer(stackPointer - 1);
    return stack[stackPointer];
  }

  public void push(final SAbstractObject value) {
    // Push an object onto the expression stack
    int stackPointer = getStackPointer() + 1;
    stack[stackPointer] = value;
    setStackPointer(stackPointer);
  }

  public int getStackPointer() {
    // Get the current stack pointer for this frame
    return stackPointer;
  }

  public void setStackPointer(final int value) {
    // Set the current stack pointer for this frame
    stackPointer = value;
  }

  public void resetStackPointer() {
    // arguments are stored in front of local variables
    localOffset = getMethod().getNumberOfArguments();

    // Set the stack pointer to its initial value thereby clearing the stack
    setStackPointer(localOffset
        + (int) getMethod().getNumberOfLocals().getEmbeddedInteger() - 1);
  }

  public SAbstractObject getStackElement(final int index) {
    // Get the stack element with the given index
    // (an index of zero yields the top element)
    return stack[getStackPointer() - index];
  }

  public void setStackElement(final int index, final SAbstractObject value) {
    // Set the stack element with the given index to the given value
    // (an index of zero yields the top element)
    stack[getStackPointer() - index] = value;
  }

  private SAbstractObject getLocal(final int index) {
    return stack[localOffset + index];
  }

  private void setLocal(final int index, final SAbstractObject value) {
    stack[localOffset + index] = value;
  }

  public SAbstractObject getLocal(final int index, final int contextLevel) {
    // Get the local with the given index in the given context
    return getContext(contextLevel).getLocal(index);
  }

  public void setLocal(final int index, final int contextLevel, final SAbstractObject value) {
    // Set the local with the given index in the given context to the given
    // value
    getContext(contextLevel).setLocal(index, value);
  }

  public SAbstractObject getArgument(final int index, final int contextLevel) {
    // Get the context
    Frame context = getContext(contextLevel);

    // Get the argument with the given index
    return context.stack[index];
  }

  public void setArgument(final int index, final int contextLevel,
      final SAbstractObject value) {
    // Get the context
    Frame context = getContext(contextLevel);

    // Set the argument with the given index to the given value
    context.stack[index] = value;
  }

  public void copyArgumentsFrom(final Frame frame) {
    // copy arguments from frame:
    // - arguments are at the top of the stack of frame.
    // - copy them into the argument area of the current frame
    int numArgs = getMethod().getNumberOfArguments();
    for (int i = 0; i < numArgs; ++i) {
      stack[i] = frame.getStackElement(numArgs - 1 - i);
    }
  }

  public void printStackTrace(final SAbstractObject nilObject, final int bytecodeIndex) {
    // Print a stack trace starting in this frame
    int numberOfArguments = method.getNumberOfArguments();
    Universe.print(getMethod().getHolder().getName().getEmbeddedString() + ">>");
    Universe.println(getMethod().getSignature().getEmbeddedString() + " @" + bytecodeIndex);
    if (hasPreviousFrame(nilObject)) {
      getPreviousFrame().printStackTrace(nilObject, bytecodeIndex);
    }
  }

  public void popArgumentsAndPushResult(final SAbstractObject result, SMethod method) {
    int numberOfArguments = method.getNumberOfArguments();

    for (int i = 0; i < numberOfArguments; i++) {
      this.pop();
    }

    this.push(result);
  }
}
