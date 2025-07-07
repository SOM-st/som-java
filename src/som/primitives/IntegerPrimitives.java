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

import java.math.BigInteger;

import som.interpreter.Frame;
import som.interpreter.Interpreter;
import som.vm.Universe;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SBigInteger;
import som.vmobjects.SInteger;
import som.vmobjects.SNumber;
import som.vmobjects.SPrimitive;
import som.vmobjects.SString;


public class IntegerPrimitives extends Primitives {

  public IntegerPrimitives(final Universe universe) {
    super(universe);
  }

  @Override
  public void installPrimitives() {
    installInstancePrimitive(new SPrimitive("asString", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SNumber self = (SNumber) frame.pop();
        frame.push(self.primAsString(universe));
      }
    });

    installInstancePrimitive(new SPrimitive("asDouble", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SNumber self = (SNumber) frame.pop();
        frame.push(self.primAsDouble(universe));
      }
    });

    installInstancePrimitive(new SPrimitive("sqrt", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SInteger self = (SInteger) frame.pop();
        frame.push(self.primSqrt(universe));
      }
    });

    installInstancePrimitive(new SPrimitive("atRandom", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SInteger self = (SInteger) frame.pop();
        frame.push(universe.newInteger(
            (long) (self.getEmbeddedInteger() * Math.random())));
      }
    });

    installInstancePrimitive(new SPrimitive("+", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SNumber right = (SNumber) frame.pop();
        SNumber left = (SNumber) frame.pop();
        frame.push(left.primAdd(right, universe));
      }
    });

    installInstancePrimitive(new SPrimitive("-", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SNumber right = (SNumber) frame.pop();
        SNumber left = (SNumber) frame.pop();
        frame.push(left.primSubtract(right, universe));
      }
    });

    installInstancePrimitive(new SPrimitive("*", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SNumber right = (SNumber) frame.pop();
        SNumber left = (SNumber) frame.pop();
        frame.push(left.primMultiply(right, universe));
      }
    });

    installInstancePrimitive(new SPrimitive("//", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SNumber right = (SNumber) frame.pop();
        SNumber left = (SNumber) frame.pop();
        frame.push(left.primDoubleDivide(right, universe));
      }
    });

    installInstancePrimitive(new SPrimitive("/", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SNumber right = (SNumber) frame.pop();
        SNumber left = (SNumber) frame.pop();
        frame.push(left.primIntegerDivide(right, universe));
      }
    });

    installInstancePrimitive(new SPrimitive("%", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SNumber right = (SNumber) frame.pop();
        SNumber left = (SNumber) frame.pop();
        frame.push(left.primModulo(right, universe));
      }
    });

    installInstancePrimitive(new SPrimitive("rem:", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SNumber right = (SNumber) frame.pop();
        SInteger left = (SInteger) frame.pop();
        frame.push(left.primRemainder(right, universe));
      }
    });

    installInstancePrimitive(new SPrimitive("&", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SNumber right = (SNumber) frame.pop();
        SNumber left = (SNumber) frame.pop();
        frame.push(left.primBitAnd(right, universe));
      }
    });

    installInstancePrimitive(new SPrimitive("=", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SAbstractObject right = frame.pop();
        SNumber left = (SNumber) frame.pop();
        frame.push(left.primEqual(right, universe));
      }
    });

    installInstancePrimitive(new SPrimitive("<", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SNumber right = (SNumber) frame.pop();
        SNumber left = (SNumber) frame.pop();
        frame.push(left.primLessThan(right, universe));
      }
    });

    installInstancePrimitive(new SPrimitive("<<", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SNumber right = (SNumber) frame.pop();
        SNumber left = (SNumber) frame.pop();
        frame.push(left.primLeftShift(right, universe));
      }
    });

    installInstancePrimitive(new SPrimitive("bitXor:", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SNumber right = (SNumber) frame.pop();
        SNumber left = (SNumber) frame.pop();
        frame.push(left.primBitXor(right, universe));
      }
    });

    installInstancePrimitive(new SPrimitive("as32BitSignedValue", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SNumber rcvr = (SNumber) frame.pop();

        int result;
        if (rcvr instanceof SInteger) {
          result = (int) ((SInteger) rcvr).getEmbeddedInteger();
        } else {
          result = ((SBigInteger) rcvr).getEmbeddedBiginteger().intValue();
        }
        frame.push(universe.newInteger(result));
      }
    });

    installInstancePrimitive(new SPrimitive("as32BitUnsignedValue", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SNumber receiver = (SNumber) frame.pop();
        int value;
        if (receiver instanceof SInteger rcvr) {
          value = (int) rcvr.getEmbeddedInteger();
        } else {
          value = ((SBigInteger) receiver).getEmbeddedBiginteger().intValue();
        }
        frame.push(universe.newInteger(Integer.toUnsignedLong(value)));
      }
    });

    installInstancePrimitive(new SPrimitive(">>>", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SInteger right = (SInteger) frame.pop();
        SInteger rcvr = (SInteger) frame.pop();
        frame.push(
            universe.newInteger(rcvr.getEmbeddedInteger() >>> right.getEmbeddedInteger()));
      }
    });

    installClassPrimitive(new SPrimitive("fromString:", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SString param = (SString) frame.pop();
        frame.pop();

        try {
          long result = Long.parseLong(param.getEmbeddedString());
          frame.push(universe.newInteger(result));
        } catch (NumberFormatException e) {
          BigInteger result = new BigInteger(param.getEmbeddedString());
          frame.push(new SBigInteger(result));
        }
      }
    });
  }
}
