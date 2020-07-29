/**
 * Copyright (c) 2013 Stefan Marr,   stefan.marr@vub.ac.be
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

import com.oracle.truffle.api.nodes.DirectCallNode;

import som.interpreter.Frame;
import som.interpreter.Interpreter;
import som.vm.Universe;


public abstract class SPrimitive extends SAbstractObject implements SInvokable {

  private final SSymbol signature;
  private SClass        holder;

  @Override
  public boolean isPrimitive() {
    return true;
  }

  public SPrimitive(String signatureString, final Universe universe) {
    signature = universe.symbolFor(signatureString);
  }

  @Override
  public SSymbol getSignature() {
    return signature;
  }

  @Override
  public SClass getHolder() {
    return holder;
  }

  @Override
  public void setHolder(SClass value) {
    holder = value;
  }

  public boolean isEmpty() {
    // By default a primitive is not empty
    return false;
  }

  public abstract void invoke(Frame frame, Interpreter interpreter);

  public void indirectInvoke(Frame frame, Interpreter interpreter) {
    invoke(frame, interpreter);
  }

  public void directInvoke(Frame frame, Interpreter interpreter, DirectCallNode callNode) {
    invoke(frame, interpreter);
  }

  @Override
  public SClass getSOMClass(final Universe universe) {
    return universe.primitiveClass;
  }

  public static SPrimitive getEmptyPrimitive(java.lang.String signatureString,
      final Universe universe) {
    // Return an empty primitive with the given signature
    return (new SPrimitive(signatureString, universe) {

      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        // Write a warning to the screen
        Universe.println("Warning: undefined primitive "
            + this.getSignature().getEmbeddedString() + " called");
      }

      @Override
      public boolean isEmpty() {
        // The empty primitives are empty
        return true;
      }
    });
  }
}
