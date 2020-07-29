/**
 * Copyright (c) 2016 Michael Haupt, github@haupz.de
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

import som.interpreter.Frame;
import som.interpreter.Interpreter;
import som.vm.Universe;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SArray;
import som.vmobjects.SClass;
import som.vmobjects.SInteger;
import som.vmobjects.SInvokable;
import som.vmobjects.SObject;
import som.vmobjects.SPrimitive;
import som.vmobjects.SSymbol;


public class ObjectPrimitives extends Primitives {

  public ObjectPrimitives(final Universe universe) {
    super(universe);
  }

  @Override
  public void installPrimitives() {

    installInstancePrimitive(new SPrimitive("==", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SAbstractObject op1 = frame.pop();
        SAbstractObject op2 = frame.pop();
        if (op1 == op2) {
          frame.push(universe.trueObject);
        } else {
          frame.push(universe.falseObject);
        }
      }
    });

    installInstancePrimitive(new SPrimitive("hashcode", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SAbstractObject self = frame.pop();
        frame.push(universe.newInteger(self.hashCode()));
      }
    });

    installInstancePrimitive(new SPrimitive("objectSize", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SAbstractObject self = frame.pop();

        // each object holds its class as an implicit member that contributes to its size
        int size = 1;
        if (self instanceof SArray) {
          size += ((SArray) self).getNumberOfIndexableFields();
        }
        if (self instanceof SObject) {
          size += ((SObject) self).getNumberOfFields();
        }
        frame.push(universe.newInteger(size));
      }
    });

    installInstancePrimitive(new SPrimitive("perform:", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SAbstractObject arg = frame.pop();
        SAbstractObject self = frame.getStackElement(0);
        SSymbol selector = (SSymbol) arg;

        SInvokable invokable = self.getSOMClass(universe).lookupInvokable(selector);
        invokable.indirectInvoke(frame, interpreter);
      }
    });

    installInstancePrimitive(new SPrimitive("perform:inSuperclass:", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SAbstractObject arg2 = frame.pop();
        SAbstractObject arg = frame.pop();
        // Object self = frame.getStackElement(0);

        SSymbol selector = (SSymbol) arg;
        SClass clazz = (SClass) arg2;

        SInvokable invokable = clazz.lookupInvokable(selector);
        invokable.indirectInvoke(frame, interpreter);
      }
    });

    installInstancePrimitive(new SPrimitive("perform:withArguments:", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SAbstractObject arg2 = frame.pop();
        SAbstractObject arg = frame.pop();
        SAbstractObject self = frame.getStackElement(0);

        SSymbol selector = (SSymbol) arg;
        SArray args = (SArray) arg2;

        for (int i = 0; i < args.getNumberOfIndexableFields(); i++) {
          frame.push(args.getIndexableField(i));
        }

        SInvokable invokable = self.getSOMClass(universe).lookupInvokable(selector);
        invokable.indirectInvoke(frame, interpreter);
      }
    });

    installInstancePrimitive(new SPrimitive("instVarAt:", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SAbstractObject arg = frame.pop();
        SObject self = (SObject) frame.pop();
        SInteger idx = (SInteger) arg;

        frame.push(self.getField(idx.getEmbeddedInteger() - 1));
      }
    });

    installInstancePrimitive(new SPrimitive("instVarAt:put:", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SAbstractObject val = frame.pop();
        SAbstractObject arg = frame.pop();
        SObject self = (SObject) frame.getStackElement(0);

        SInteger idx = (SInteger) arg;

        self.setField(idx.getEmbeddedInteger() - 1, val);
      }
    });

    installInstancePrimitive(new SPrimitive("class", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SAbstractObject self = frame.pop();
        frame.push(self.getSOMClass(universe));
      }
    });

    installInstancePrimitive(new SPrimitive("halt", universe) {
      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        Universe.errorPrintln("BREAKPOINT");
      }
    });
  }
}
