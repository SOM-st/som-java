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
import som.vmobjects.SBlock;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable;
import som.vmobjects.SMethod;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;

public class Interpreter {

  private final Universe universe;

  public Interpreter(final Universe universe) {
    this.universe = universe;
  }

  private void doDup() {
    // Handle the dup bytecode
    getFrame().push(getFrame().getStackElement(0));
  }

  private void doPushLocal(int bytecodeIndex) {
    // Handle the push local bytecode
    getFrame().push(
        getFrame().getLocal(getMethod().getBytecode(bytecodeIndex + 1),
            getMethod().getBytecode(bytecodeIndex + 2)));
  }

  private void doPushArgument(int bytecodeIndex) {
    // Handle the push argument bytecode
    getFrame().push(
        getFrame().getArgument(getMethod().getBytecode(bytecodeIndex + 1),
            getMethod().getBytecode(bytecodeIndex + 2)));
  }

  private void doPushField(int bytecodeIndex) {
    // Handle the push field bytecode
    int fieldIndex = getMethod().getBytecode(bytecodeIndex + 1);

    // Push the field with the computed index onto the stack
    getFrame().push(((SObject) getSelf()).getField(fieldIndex));
  }

  private void doPushBlock(int bytecodeIndex) {
    // Handle the push block bytecode
    SMethod blockMethod = (SMethod) getMethod().getConstant(bytecodeIndex);

    // Push a new block with the current getFrame() as context onto the
    // stack
    getFrame().push(
        universe.newBlock(blockMethod, getFrame(),
            blockMethod.getNumberOfArguments()));
  }

  private void doPushConstant(int bytecodeIndex) {
    // Handle the push constant bytecode
    getFrame().push(getMethod().getConstant(bytecodeIndex));
  }

  private void doPushGlobal(int bytecodeIndex) {
    // Handle the push global bytecode
    SSymbol globalName = (SSymbol) getMethod().getConstant(bytecodeIndex);

    // Get the global from the universe
    SAbstractObject global = universe.getGlobal(globalName);

    if (global != null) {
      // Push the global onto the stack
      getFrame().push(global);
    } else {
      // Send 'unknownGlobal:' to self
      getSelf().sendUnknownGlobal(globalName, universe, this);
    }
  }

  private void doPop() {
    // Handle the pop bytecode
    getFrame().pop();
  }

  private void doPopLocal(int bytecodeIndex) {
    // Handle the pop local bytecode
    getFrame().setLocal(getMethod().getBytecode(bytecodeIndex + 1),
        getMethod().getBytecode(bytecodeIndex + 2), getFrame().pop());
  }

  private void doPopArgument(int bytecodeIndex) {
    // Handle the pop argument bytecode
    getFrame().setArgument(getMethod().getBytecode(bytecodeIndex + 1),
        getMethod().getBytecode(bytecodeIndex + 2), getFrame().pop());
  }

  private void doPopField(int bytecodeIndex) {
    // Handle the pop field bytecode
    int fieldIndex = getMethod().getBytecode(bytecodeIndex + 1);

    // Set the field with the computed index to the value popped from the stack
    ((SObject) getSelf()).setField(fieldIndex, getFrame().pop());
  }

  private void doSuperSend(int bytecodeIndex) {
    // Handle the super send bytecode
    SSymbol signature = (SSymbol) getMethod().getConstant(bytecodeIndex);

    // Send the message
    // Lookup the invokable with the given signature
    SInvokable invokable = getMethod().getHolder().getSuperClass().lookupInvokable(signature);

    if (invokable != null) {
      // Invoke the invokable in the current frame
      invokable.invoke(getFrame(), this);
    } else {
      // Compute the number of arguments
      int numberOfArguments = signature.getNumberOfSignatureArguments();

      // Compute the receiver
      SAbstractObject receiver = getFrame().getStackElement(numberOfArguments - 1);

      receiver.sendDoesNotUnderstand(signature, universe, this);
    }
  }

  private void doReturnLocal() {
    // Handle the return local bytecode
    SAbstractObject result = getFrame().pop();

    // Pop the top frame and push the result
    popFrameAndPushResult(result);
  }

  private void doReturnNonLocal() {
    // Handle the return non local bytecode
    SAbstractObject result = getFrame().pop();

    // Compute the context for the non-local return
    Frame context = getFrame().getOuterContext(universe.nilObject);

    // Make sure the block context is still on the stack
    if (!context.hasPreviousFrame(universe.nilObject)) {
      // Try to recover by sending 'escapedBlock:' to the sending object
      // this can get a bit nasty when using nested blocks. In this case
      // the "sender" will be the surrounding block and not the object
      // that actually sent the 'value' message.
      SBlock block = (SBlock) getFrame().getArgument(0, 0);
      SAbstractObject sender = getFrame().getPreviousFrame().getOuterContext(universe.nilObject).getArgument(0, 0);

      // pop the frame of the currently executing block...
      popFrame();

      // ... and execute the escapedBlock message instead
      sender.sendEscapedBlock(block, universe, this);
      return;
    }

    // Unwind the frames
    while (getFrame() != context) {
      popFrame();
    }

    // Pop the top frame and push the result
    popFrameAndPushResult(result);
  }

  private void doSend(int bytecodeIndex) {
    // Handle the send bytecode
    SSymbol signature = (SSymbol) getMethod().getConstant(bytecodeIndex);

    // Get the number of arguments from the signature
    int numberOfArguments = signature.getNumberOfSignatureArguments();

    // Get the receiver from the stack
    SAbstractObject receiver = getFrame().getStackElement(numberOfArguments - 1);

    // Send the message
    send(signature, receiver.getSOMClass(universe), bytecodeIndex);
  }

  public void start() {
    // Iterate through the bytecodes
    while (true) {

      // Get the current bytecode index
      int bytecodeIndex = getFrame().getBytecodeIndex();

      // Get the current bytecode
      byte bytecode = getMethod().getBytecode(bytecodeIndex);

      // Get the length of the current bytecode
      int bytecodeLength = Bytecodes.getBytecodeLength(bytecode);

      // Compute the next bytecode index
      int nextBytecodeIndex = bytecodeIndex + bytecodeLength;

      // Update the bytecode index of the frame
      getFrame().setBytecodeIndex(nextBytecodeIndex);

      // Handle the current bytecode
      switch (bytecode) {

        case Bytecodes.halt: {
          // Handle the halt bytecode
          return;
        }

        case Bytecodes.dup: {
          doDup();
          break;
        }

        case Bytecodes.push_local: {
          doPushLocal(bytecodeIndex);
          break;
        }

        case Bytecodes.push_argument: {
          doPushArgument(bytecodeIndex);
          break;
        }

        case Bytecodes.push_field: {
          doPushField(bytecodeIndex);
          break;
        }

        case Bytecodes.push_block: {
          doPushBlock(bytecodeIndex);
          break;
        }

        case Bytecodes.push_constant: {
          doPushConstant(bytecodeIndex);
          break;
        }

        case Bytecodes.push_global: {
          doPushGlobal(bytecodeIndex);
          break;
        }

        case Bytecodes.pop: {
          doPop();
          break;
        }

        case Bytecodes.pop_local: {
          doPopLocal(bytecodeIndex);
          break;
        }

        case Bytecodes.pop_argument: {
          doPopArgument(bytecodeIndex);
          break;
        }

        case Bytecodes.pop_field: {
          doPopField(bytecodeIndex);
          break;
        }

        case Bytecodes.send: {
          doSend(bytecodeIndex);
          break;
        }

        case Bytecodes.super_send: {
          doSuperSend(bytecodeIndex);
          break;
        }

        case Bytecodes.return_local: {
          doReturnLocal();
          break;
        }

        case Bytecodes.return_non_local: {
          doReturnNonLocal();
          break;
        }

        default:
          Universe.errorPrintln("Nasty bug in interpreter");
          break;
      }
    }
  }

  public Frame pushNewFrame(final SMethod method, final Frame contextFrame) {
    // Allocate a new frame and make it the current one
    frame = universe.newFrame(frame, method, contextFrame);

    // Return the freshly allocated and pushed frame
    return frame;
  }

  public Frame pushNewFrame(final SMethod method) {
    return pushNewFrame(method, null);
  }

  public Frame getFrame() {
    // Get the frame from the interpreter
    return frame;
  }

  public SMethod getMethod() {
    // Get the method from the interpreter
    return getFrame().getMethod();
  }

  public SAbstractObject getSelf() {
    // Get the self object from the interpreter
    return getFrame().getOuterContext(universe.nilObject).getArgument(0, 0);
  }

  private void send(SSymbol selector, SClass receiverClass, int bytecodeIndex) {
    // First try the inline cache
    SInvokable invokable;

    SMethod m = getMethod();
    SClass cachedClass = m.getInlineCacheClass(bytecodeIndex);
    if (cachedClass == receiverClass) {
      invokable = m.getInlineCacheInvokable(bytecodeIndex);
    } else {
      if (cachedClass == null) {
        // Lookup the invokable with the given signature
        invokable = receiverClass.lookupInvokable(selector);
        m.setInlineCache(bytecodeIndex, receiverClass, invokable);
      } else {
        cachedClass = m.getInlineCacheClass(bytecodeIndex + 1); // the bytecode index after the send is used by the selector constant, and can be used safely as another cache item
        if (cachedClass == receiverClass) {
          invokable = m.getInlineCacheInvokable(bytecodeIndex + 1);
        } else {
          invokable = receiverClass.lookupInvokable(selector);
          if (cachedClass == null) {
            m.setInlineCache(bytecodeIndex + 1, receiverClass, invokable);
          }
        }
      }
    }

    if (invokable != null) {
      // Invoke the invokable in the current frame
      invokable.invoke(getFrame(), this);
    } else {
      int numberOfArguments = selector.getNumberOfSignatureArguments();

      // Compute the receiver
      SAbstractObject receiver = frame.getStackElement(numberOfArguments - 1);

      receiver.sendDoesNotUnderstand(selector, universe, this);
    }
  }

  private Frame popFrame() {
    // Save a reference to the top frame
    Frame result = frame;

    // Pop the top frame from the frame stack
    frame = frame.getPreviousFrame();

    // Destroy the previous pointer on the old top frame
    result.clearPreviousFrame();

    // Return the popped frame
    return result;
  }

  private void popFrameAndPushResult(SAbstractObject result) {
    // Pop the top frame from the interpreter frame stack and compute the
    // number of arguments
    int numberOfArguments = popFrame().getMethod().getNumberOfArguments();

    // Pop the arguments
    for (int i = 0; i < numberOfArguments; i++) {
      getFrame().pop();
    }

    // Push the result
    getFrame().push(result);
  }

  private Frame frame;
}
