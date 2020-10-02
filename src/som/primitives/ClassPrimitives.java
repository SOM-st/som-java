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

import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.Interpreter;
import som.interpreter.StackUtils;
import som.vm.Universe;
import som.vmobjects.*;


public class ClassPrimitives extends Primitives {

  public ClassPrimitives(final Universe universe) {
    super(universe);
  }

  @Override
  public void installPrimitives() {
    installInstancePrimitive(new SPrimitive("new", universe) {

      @Override
      public void invoke(VirtualFrame truffleFrame,
          final Interpreter interpreter) throws FrameSlotTypeException {
        SClass selfT = (SClass) StackUtils.pop(truffleFrame);

        SAbstractObject instance = universe.newInstance(selfT);

        StackUtils.push(truffleFrame, instance);
      }
    });

    installInstancePrimitive(new SPrimitive("name", universe) {

      @Override
      public void invoke(VirtualFrame truffleFrame,
          final Interpreter interpreter) throws FrameSlotTypeException {

        SClass selfT = (SClass) StackUtils.pop(truffleFrame);

        SSymbol name = selfT.getName();

        StackUtils.push(truffleFrame, name);
      }
    });

    installInstancePrimitive(new SPrimitive("superclass", universe) {

      @Override
      public void invoke(VirtualFrame truffleFrame,
          final Interpreter interpreter) throws FrameSlotTypeException {

        SClass selfT = (SClass) StackUtils.pop(truffleFrame);

        SObject superClass = selfT.getSuperClass();

        StackUtils.push(truffleFrame, superClass);
      }
    });

    installInstancePrimitive(new SPrimitive("fields", universe) {

      @Override
      public void invoke(VirtualFrame truffleFrame,
          final Interpreter interpreter) throws FrameSlotTypeException {

        SClass selfT = (SClass) StackUtils.pop(truffleFrame);

        SArray instanceFields = selfT.getInstanceFields();

        StackUtils.push(truffleFrame, instanceFields);
      }
    });

    installInstancePrimitive(new SPrimitive("methods", universe) {

      @Override
      public void invoke(VirtualFrame truffleFrame,
          final Interpreter interpreter) throws FrameSlotTypeException {

        SClass selfT = (SClass) StackUtils.pop(truffleFrame);

        SArray instanceInvokable = selfT.getInstanceInvokables();
        StackUtils.push(truffleFrame, instanceInvokable);
      }
    });
  }
}
