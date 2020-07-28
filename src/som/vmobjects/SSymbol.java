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
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import com.oracle.truffle.api.profiles.ValueProfile;
import som.vm.Universe;


@ExportLibrary(InteropLibrary.class)
public class SSymbol extends SString {

  @CompilerDirectives.CompilationFinal private final ValueProfile valueProfile =
      ValueProfile.createClassProfile();

  @CompilerDirectives.TruffleBoundary
  public final ValueProfile getValueProfile() {
    return valueProfile;
  }

  public SSymbol(final String value) {
    super(value);
    numberOfSignatureArguments = determineNumberOfSignatureArguments();
  }

  private int determineNumberOfSignatureArguments() {
    // Check for binary signature
    if (isBinarySignature()) {
      return 2;
    } else {
      // Count the colons in the signature string
      int numberOfColons = 0;

      // Iterate through every character in the signature string
      for (char c : getEmbeddedString().toCharArray()) {
        if (c == ':') {
          numberOfColons++;
        }
      }

      // The number of arguments is equal to the number of colons plus one
      return numberOfColons + 1;
    }
  }

  @Override
  public String toString() {
    return "#" + getEmbeddedString();
  }

  public int getNumberOfSignatureArguments() {
    return numberOfSignatureArguments;
  }

  public boolean isBinarySignature() {
    // Check the individual characters of the string
    for (char c : getEmbeddedString().toCharArray()) {
      if (c != '~' && c != '&' && c != '|' && c != '*' && c != '/' && c != '@'
          && c != '+' && c != '-' && c != '=' && c != '>' && c != '<'
          && c != ',' && c != '%' && c != '\\') {
        return false;
      }
    }
    return true;
  }

  @Override
  public SClass getSOMClass(final Universe universe) {
    return universe.symbolClass;
  }

  @Override
  public final SClass getSOMClassBis(Universe universe, ValueProfile profiledClass) {
    return profiledClass.profile(universe.symbolClass);
  }

  private final int numberOfSignatureArguments;

  /**
   * INTEROP.
   */

  @ExportMessage
  boolean isString() {
    return true;
  }

  @ExportMessage
  final String asString() throws UnsupportedMessageException {
    return this.getEmbeddedString();
  }

}
