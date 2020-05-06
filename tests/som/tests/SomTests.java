/**
 * Copyright (c) 2013 Stefan Marr, stefan.marr@vub.ac.be
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
package som.tests;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import som.GraalSOMLanguage;
import som.Launcher;
import som.compiler.ProgramDefinitionError;


@RunWith(Parameterized.class)
public class SomTests {

  @Parameters(name = "{0} [{index}]")
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"Array"},
        {"Block"},
        {"ClassLoading"},
        {"ClassStructure"},

        {"Closure"},
        {"Coercion"},
        {"CompilerReturn"},
        {"DoesNotUnderstand"},
        {"Double"},

        {"Empty"},
        {"Global"},
        {"Hash"},
        {"Integer"},

        {"Preliminary"},
        {"Reflection"},
        {"SelfBlock"},
        {"SpecialSelectorsTest"},
        {"Super"},

        {"Set"},
        {"String"},
        {"Symbol"},
        {"System"},
        {"Vector"}
    });
  }

  private final String testSelector;

  public SomTests(final String testSelector) {
    this.testSelector = testSelector;
  }

  @Test
  public void testSomeTest() throws ProgramDefinitionError {
    int returnCode = 0;
    String[] args = new String[] {
        "-cp",
        "Smalltalk",
        "TestSuite/TestHarness.som", testSelector};
    Source source = Launcher.START;

    Context.Builder builder = Context.newBuilder(GraalSOMLanguage.ID).in(System.in)
                                     .out(System.out).allowAllAccess(true);
    builder.arguments(GraalSOMLanguage.ID, args);
    Context context = builder.build();

    try {
      context.eval(source);
    } catch (PolyglotException ex) {
      returnCode = 1;
    }
    assertEquals(0, returnCode);
  }

}
