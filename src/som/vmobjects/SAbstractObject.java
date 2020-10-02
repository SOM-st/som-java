/**
 * Copyright (c) 2009 Michael Haupt, michael.haupt@hpi.uni-potsdam.de
 * Software Architecture Group, Hasso Plattner Institute, Potsdam, Germany
 * http://www.hpi.uni-potsdam.de/swa/
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package som.vmobjects;

import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;

import som.interpreter.Interpreter;
import som.interpreter.StackUtils;
import som.vm.Universe;


public abstract class SAbstractObject implements TruffleObject {

  public abstract SClass getSOMClass(Universe universe);

  public MaterializedFrame getMaterializedContext() {
    return null;
  }

  // TODO - hack on truffle frame
  public void send(final String selectorString, final SAbstractObject[] arguments,
      final Universe universe, final Interpreter interpreter,
      VirtualFrame truffleFrame) throws FrameSlotTypeException {
    // Turn the selector string into a selector
    SSymbol selector = universe.symbolFor(selectorString);

    // Push the receiver onto the stack
    StackUtils.push(truffleFrame, this);

    // Push the arguments onto the stack
    for (SAbstractObject arg : arguments) {
      StackUtils.push(truffleFrame, arg);
    }

    // Lookup the invokable
    SInvokable invokable = getSOMClass(universe).lookupInvokable(selector);

    // Invoke the invokable
    invokable.indirectInvoke(truffleFrame, interpreter);
  }

  public void sendDoesNotUnderstand(final SSymbol selector,
      final Universe universe, final Interpreter interpreter,
      VirtualFrame truffleFrame) throws FrameSlotTypeException {
    // Compute the number of arguments
    int numberOfArguments = selector.getNumberOfSignatureArguments();

    // Allocate an array with enough room to hold all arguments
    // except for the receiver, which is passed implicitly, as receiver of #dnu.
    SArray argumentsArray = universe.newArray(numberOfArguments - 1);

    // Remove all arguments and put them in the freshly allocated array
    for (int i = numberOfArguments - 2; i >= 0; i--) {
      argumentsArray.setIndexableField(i, StackUtils.pop(truffleFrame));
    }

    StackUtils.pop(truffleFrame);

    SAbstractObject[] args = {selector, argumentsArray};
    send("doesNotUnderstand:arguments:", args, universe, interpreter, truffleFrame);
  }

  public void sendUnknownGlobal(final SSymbol globalName,
      final Universe universe, final Interpreter interpreter,
      VirtualFrame truffleFrame) throws FrameSlotTypeException {
    SAbstractObject[] arguments = {globalName};
    send("unknownGlobal:", arguments, universe, interpreter, truffleFrame);
  }

  public void sendEscapedBlock(final SBlock block, final Universe universe,
      final Interpreter interpreter, VirtualFrame truffleFrame)
      throws FrameSlotTypeException {
    SAbstractObject[] arguments = {block};
    send("escapedBlock:", arguments, universe, interpreter, truffleFrame);
  }

  @Override
  public String toString() {
    return "a " + getSOMClass(Universe.current()).getName().getEmbeddedString();
  }

  /**
   * Used by Truffle interop.
   */
  public static boolean isInstance(final TruffleObject obj) {
    return obj instanceof SAbstractObject;
  }
}
