package som;

import java.nio.charset.Charset;

import com.oracle.truffle.api.TruffleFile;


public final class GSFileDetector implements TruffleFile.FileTypeDetector {

  @Override
  public String findMimeType(final TruffleFile file) {
    String name = file.getName();
    if (name != null && name.endsWith(".som")) {
      return GraalSOMLanguage.MIME_TYPE;
    }
    return null;
  }

  @Override
  public Charset findEncoding(final TruffleFile file) {
    return null;
  }
}
