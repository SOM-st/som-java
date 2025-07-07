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

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.sun.management.ThreadMXBean;
import som.compiler.ProgramDefinitionError;
import som.interpreter.Frame;
import som.interpreter.Interpreter;
import som.vm.Universe;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SArray;
import som.vmobjects.SClass;
import som.vmobjects.SInteger;
import som.vmobjects.SPrimitive;
import som.vmobjects.SString;
import som.vmobjects.SSymbol;


public class SystemPrimitives extends Primitives {

  public SystemPrimitives(final Universe universe) {
    super(universe);
  }

  @Override
  public void installPrimitives() {
    installInstancePrimitive(new SPrimitive("load:", universe) {

      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SSymbol argument = (SSymbol) frame.pop();
        frame.pop(); // not required
        SClass result = null;
        try {
          result = universe.loadClass(argument);
        } catch (ProgramDefinitionError e) {
          Universe.errorPrintln("Program Definition Error: " + e.getMessage());
          universe.exit(1);
        }
        frame.push(result != null ? result : universe.nilObject);
      }
    });

    installInstancePrimitive(new SPrimitive("exit:", universe) {

      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SInteger error = (SInteger) frame.pop();
        universe.exit(error.getEmbeddedInteger());
      }
    });

    installInstancePrimitive(new SPrimitive("global:", universe) {

      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SSymbol argument = (SSymbol) frame.pop();
        frame.pop(); // not required
        SAbstractObject result = universe.getGlobal(argument);
        frame.push(result != null ? result : universe.nilObject);
      }
    });

    installInstancePrimitive(new SPrimitive("global:put:", universe) {

      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SAbstractObject value = frame.pop();
        SSymbol argument = (SSymbol) frame.pop();
        universe.setGlobal(argument, value);
      }
    });

    installInstancePrimitive(new SPrimitive("printString:", universe) {

      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SString argument = (SString) frame.pop();
        Universe.print(argument.getEmbeddedString());
      }
    });

    installInstancePrimitive(new SPrimitive("printNewline", universe) {

      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        Universe.println("");
      }
    });

    installInstancePrimitive(new SPrimitive("errorPrint:", universe) {

      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SString argument = (SString) frame.pop();
        Universe.errorPrint(argument.getEmbeddedString());
      }
    });

    installInstancePrimitive(new SPrimitive("errorPrintln:", universe) {

      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SString argument = (SString) frame.pop();
        Universe.errorPrintln(argument.getEmbeddedString());
      }
    });

    startMicroTime = System.nanoTime() / 1000L;
    startTime = startMicroTime / 1000L;
    installInstancePrimitive(new SPrimitive("time", universe) {

      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        frame.pop(); // ignore
        int time = (int) (System.currentTimeMillis() - startTime);
        frame.push(universe.newInteger(time));
      }
    });

    installInstancePrimitive(new SPrimitive("ticks", universe) {

      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        frame.pop(); // ignore
        int time = (int) (System.nanoTime() / 1000L - startMicroTime);
        frame.push(universe.newInteger(time));
      }
    });

    installInstancePrimitive(new SPrimitive("fullGC", universe) {

      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        frame.pop();
        System.gc();
        frame.push(universe.trueObject);
      }
    });

    installInstancePrimitive(new SPrimitive("gcStats", universe) {

      @Override
      public void invoke(Frame frame, Interpreter interpreter) {
        frame.pop();

        final List<GarbageCollectorMXBean> gcBeans =
            ManagementFactory.getGarbageCollectorMXBeans();
        final ThreadMXBean threadBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        threadBean.setThreadAllocatedMemoryEnabled(true);

        final long allocatedBytes = threadBean.getCurrentThreadAllocatedBytes();
        long counts = 0;
        long time = 0;

        for (GarbageCollectorMXBean b : gcBeans) {
          long c = b.getCollectionCount();
          if (c != -1) {
            counts += c;
          }

          long t = b.getCollectionTime();
          if (t != -1) {
            time += t;
          }
        }

        final SArray arr = new SArray(universe.nilObject, 3L);
        arr.setIndexableField(0L, SInteger.getInteger(counts));
        arr.setIndexableField(1L, SInteger.getInteger(time));
        arr.setIndexableField(2L, SInteger.getInteger(allocatedBytes));

        frame.push(arr);
      }
    });

    installInstancePrimitive(new SPrimitive("loadFile:", universe) {

      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        SString fileName = (SString) frame.pop();
        frame.pop();

        Path p = Paths.get(fileName.getEmbeddedString());
        try {
          String content = new String(Files.readAllBytes(p));
          frame.push(universe.newString(content));
        } catch (IOException e) {
          frame.push(universe.nilObject);
        }
      }
    });

    installInstancePrimitive(new SPrimitive("printStackTrace", universe) {

      @Override
      public void invoke(final Frame frame, final Interpreter interpreter) {
        frame.pop();
        frame.printStackTrace();
        frame.push(universe.trueObject);
      }
    });
  }

  private long startTime;
  private long startMicroTime;
}
