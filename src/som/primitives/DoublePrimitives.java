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

package som.primitives;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.Interpreter;
import som.interpreter.StackUtils;
import som.vm.Universe;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SDouble;
import som.vmobjects.SInteger;
import som.vmobjects.SNumber;
import som.vmobjects.SPrimitive;
import som.vmobjects.SString;


public class DoublePrimitives extends Primitives {

  public DoublePrimitives(final Universe universe) {
    super(universe);
  }

  @Override
  public void installPrimitives() {
    installInstancePrimitive(new SPrimitive("asString", universe) {
      @Override
      @CompilerDirectives.TruffleBoundary
      public void invoke(final VirtualFrame frame,
          final Interpreter interpreter) throws FrameSlotTypeException {
        SDouble selfT = (SDouble) StackUtils.pop(frame);

        SString value = selfT.primAsString(universe);
        StackUtils.push(frame, value);
      }
    });

    installInstancePrimitive(new SPrimitive("asInteger", universe) {
      @Override
      public void invoke(final VirtualFrame frame,
          final Interpreter interpreter) throws FrameSlotTypeException {

        SDouble selfT = (SDouble) StackUtils.pop(frame);

        SInteger value = selfT.primAsInteger(universe);

        StackUtils.push(frame, value);
      }
    });

    installInstancePrimitive(new SPrimitive("sqrt", universe) {
      @Override
      public void invoke(final VirtualFrame frame,
          final Interpreter interpreter) throws FrameSlotTypeException {

        SDouble selfT = (SDouble) StackUtils.pop(frame);

        SAbstractObject value = selfT.primSqrt(universe);
        StackUtils.push(frame, value);
      }
    });

    installInstancePrimitive(new SPrimitive("+", universe) {
      @Override
      public void invoke(final VirtualFrame frame,
          final Interpreter interpreter) throws FrameSlotTypeException {

        SNumber op1 = (SNumber) StackUtils.pop(frame);
        SDouble op2 = (SDouble) StackUtils.pop(frame);

        SAbstractObject value = op2.primAdd(op1, universe);
        StackUtils.push(frame, value);
      }
    });

    installInstancePrimitive(new SPrimitive("-", universe) {
      @Override
      public void invoke(final VirtualFrame frame,
          final Interpreter interpreter) throws FrameSlotTypeException {

        SNumber op1 = (SNumber) StackUtils.pop(frame);
        SDouble op2 = (SDouble) StackUtils.pop(frame);

        SAbstractObject value = op2.primSubtract(op1, universe);

        StackUtils.push(frame, value);
      }
    });

    installInstancePrimitive(new SPrimitive("*", universe) {
      @Override
      public void invoke(final VirtualFrame frame,
          final Interpreter interpreter) throws FrameSlotTypeException {

        SNumber op1 = (SNumber) StackUtils.pop(frame);
        SDouble op2 = (SDouble) StackUtils.pop(frame);

        SAbstractObject value = op2.primMultiply(op1, universe);
        StackUtils.push(frame, value);
      }
    });

    installInstancePrimitive(new SPrimitive("//", universe) {
      @Override
      public void invoke(final VirtualFrame frame,
          final Interpreter interpreter) throws FrameSlotTypeException {

        SNumber op1 = (SNumber) StackUtils.pop(frame);
        SDouble op2 = (SDouble) StackUtils.pop(frame);

        SAbstractObject value = op2.primDoubleDivide(op1, universe);
        StackUtils.push(frame, value);
      }
    });

    installInstancePrimitive(new SPrimitive("%", universe) {
      @Override
      public void invoke(final VirtualFrame frame,
          final Interpreter interpreter) throws FrameSlotTypeException {

        SNumber op1 = (SNumber) StackUtils.pop(frame);
        SDouble op2 = (SDouble) StackUtils.pop(frame);

        SAbstractObject value = op2.primModulo(op1, universe);
        StackUtils.push(frame, value);
      }
    });

    installInstancePrimitive(new SPrimitive("=", universe) {
      @Override
      public void invoke(final VirtualFrame frame,
          final Interpreter interpreter) throws FrameSlotTypeException {

        SNumber op1 = (SNumber) StackUtils.pop(frame);
        SDouble op2 = (SDouble) StackUtils.pop(frame);

        SAbstractObject value = op2.primEqual(op1, universe);
        StackUtils.push(frame, value);
      }
    });

    installInstancePrimitive(new SPrimitive("<", universe) {
      @Override
      public void invoke(final VirtualFrame frame,
          final Interpreter interpreter) throws FrameSlotTypeException {

        SNumber op1 = (SNumber) StackUtils.pop(frame);
        SDouble op2 = (SDouble) StackUtils.pop(frame);

        SAbstractObject value = op2.primLessThan(op1, universe);

        StackUtils.push(frame, value);
      }
    });

    installInstancePrimitive(new SPrimitive("round", universe) {
      @Override
      public void invoke(final VirtualFrame frame,
          final Interpreter interpreter) throws FrameSlotTypeException {

        SDouble rcvr = (SDouble) StackUtils.pop(frame);

        long result = Math.round(rcvr.getEmbeddedDouble());
        SInteger value = universe.newInteger(result);

        StackUtils.push(frame, value);
      }
    });

    installInstancePrimitive(new SPrimitive("sin", universe) {
      @Override
      public void invoke(final VirtualFrame frame,
          final Interpreter interpreter) throws FrameSlotTypeException {

        SDouble rcvr = (SDouble) StackUtils.pop(frame);

        double result = Math.sin(rcvr.getEmbeddedDouble());
        SDouble value = universe.newDouble(result);

        StackUtils.push(frame, value);
      }
    });

    installInstancePrimitive(new SPrimitive("cos", universe) {
      @Override
      public void invoke(final VirtualFrame frame,
          final Interpreter interpreter) throws FrameSlotTypeException {

        SDouble rcvr = (SDouble) StackUtils.pop(frame);

        double result = Math.cos(rcvr.getEmbeddedDouble());
        SDouble value = universe.newDouble(result);

        StackUtils.push(frame, value);
      }
    });

    installClassPrimitive(new SPrimitive("PositiveInfinity", universe) {
      @Override
      public void invoke(final VirtualFrame frame,
          final Interpreter interpreter) throws FrameSlotTypeException {

        StackUtils.pop(frame);

        SDouble value = universe.newDouble(Double.POSITIVE_INFINITY);

        StackUtils.push(frame, value);
      }
    });
  }
}
