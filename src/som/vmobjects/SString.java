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

package som.vmobjects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.profiles.ValueProfile;
import som.vm.Universe;


public class SString extends SAbstractObject {

  // Private variable holding the string associated to this symbol
  private final String string;

  @CompilerDirectives.CompilationFinal private final ValueProfile valueProfile =
      ValueProfile.createClassProfile();

  public SString(final String value) {
    string = value;
  }

  public String getEmbeddedString() {
    return string;
  }

  @Override
  public String toString() {
    return "\"" + string + "\"";
  }

  @Override
  public SClass getSOMClass(final Universe universe) {
    return universe.stringClass;
  }

  @CompilerDirectives.TruffleBoundary
  public ValueProfile getValueProfile() {
    return valueProfile;
  }

}
