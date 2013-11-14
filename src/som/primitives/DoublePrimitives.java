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

import som.vm.Universe;
import som.interpreter.Interpreter;
import som.vmobjects.SDouble;
import som.vmobjects.SFrame;
import som.vmobjects.SInteger;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SPrimitive;

public class DoublePrimitives extends Primitives {

  public DoublePrimitives(final Universe universe) {
    super(universe);
  }

  private SDouble coerceToDouble(SAbstractObject o) {
    if (o instanceof SDouble) { return (SDouble) o; }
    if (o instanceof SInteger) {
      return universe.newDouble((double) ((SInteger) o).getEmbeddedInteger());
    }
    throw new ClassCastException("Cannot coerce to Double!");
  }

  public void installPrimitives() {
    installInstancePrimitive(new SPrimitive("asString", universe) {

      public void invoke(final SFrame frame, final Interpreter interpreter) {
        SDouble self = (SDouble) frame.pop();
        frame.push(universe.newString(
            java.lang.Double.toString(self.getEmbeddedDouble())));
      }
    });

    installInstancePrimitive(new SPrimitive("sqrt", universe) {

      public void invoke(final SFrame frame, final Interpreter interpreter) {
        SDouble self = (SDouble) frame.pop();
        frame.push(universe.newDouble(Math.sqrt(self.getEmbeddedDouble())));
      }
    });

    installInstancePrimitive(new SPrimitive("+", universe) {

      public void invoke(final SFrame frame, final Interpreter interpreter) {
        SDouble op1 = coerceToDouble(frame.pop());
        SDouble op2 = (SDouble) frame.pop();
        frame.push(universe.newDouble(op1.getEmbeddedDouble()
            + op2.getEmbeddedDouble()));
      }
    });

    installInstancePrimitive(new SPrimitive("-", universe) {

      public void invoke(final SFrame frame, final Interpreter interpreter) {
        SDouble op1 = coerceToDouble(frame.pop());
        SDouble op2 = (SDouble) frame.pop();
        frame.push(universe.newDouble(op2.getEmbeddedDouble()
            - op1.getEmbeddedDouble()));
      }
    });

    installInstancePrimitive(new SPrimitive("*", universe) {

      public void invoke(final SFrame frame, final Interpreter interpreter) {
        SDouble op1 = coerceToDouble(frame.pop());
        SDouble op2 = (SDouble) frame.pop();
        frame.push(universe.newDouble(op2.getEmbeddedDouble()
            * op1.getEmbeddedDouble()));
      }
    });

    installInstancePrimitive(new SPrimitive("//", universe) {

      public void invoke(final SFrame frame, final Interpreter interpreter) {
        SDouble op1 = coerceToDouble(frame.pop());
        SDouble op2 = (SDouble) frame.pop();
        frame.push(universe.newDouble(op2.getEmbeddedDouble()
            / op1.getEmbeddedDouble()));
      }
    });

    installInstancePrimitive(new SPrimitive("%", universe) {

      public void invoke(final SFrame frame, final Interpreter interpreter) {
        SDouble op1 = coerceToDouble(frame.pop());
        SDouble op2 = (SDouble) frame.pop();
        frame.push(universe.newDouble(op2.getEmbeddedDouble()
            % op1.getEmbeddedDouble()));
      }
    });

    installInstancePrimitive(new SPrimitive("=", universe) {

      public void invoke(final SFrame frame, final Interpreter interpreter) {
        SDouble op1 = coerceToDouble(frame.pop());
        SDouble op2 = (SDouble) frame.pop();
        if (op1.getEmbeddedDouble() == op2.getEmbeddedDouble()) {
          frame.push(universe.trueObject);
        } else {
          frame.push(universe.falseObject);
        }
      }
    });

    installInstancePrimitive(new SPrimitive("<", universe) {

      public void invoke(SFrame frame, final Interpreter interpreter) {
        SDouble op1 = coerceToDouble(frame.pop());
        SDouble op2 = (SDouble) frame.pop();
        if (op2.getEmbeddedDouble() < op1.getEmbeddedDouble()) {
          frame.push(universe.trueObject);
        } else {
          frame.push(universe.falseObject);
        }
      }
    });

    installInstancePrimitive(new SPrimitive("round", universe) {

      public void invoke(SFrame frame, Interpreter interpreter) {
        SDouble rcvr = (SDouble) frame.pop();
        long result = Math.round(rcvr.getEmbeddedDouble());
        if (result > java.lang.Integer.MAX_VALUE
            || result < java.lang.Integer.MIN_VALUE) {
          frame.push(universe.newBigInteger(result));
        } else {
          frame.push(universe.newInteger((int) result));
        }
      }
    });
  }
}
