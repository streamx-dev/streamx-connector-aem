package dev.streamx.aem.connector.blueprints;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

class ReplacingInputStreamTest {

  @Test
  void testReplacing() throws IOException {
    shouldReplaceString(
        "Hello, Abc",
        "Abc",
        "Def",
        "Hello, Def"
    );

    shouldReplaceString(
        "Hello, Abc",
        "Def",
        "Abc",
        "Hello, Abc"
    );

    shouldReplaceString(
        "Xyz Abc Abc Abc Xyz",
        "Abc",
        "Def",
        "Xyz Def Def Def Xyz"
    );

    shouldReplaceString(
        "Abc Ab Abc",
        "Abc",
        "Def",
        "Def Ab Def"
    );

    shouldReplaceString(
        "Abc Def",
        "Abc",
        "",
        " Def"
    );

    shouldReplaceString(
        "A",
        "A",
        "B",
        "B"
    );
  }

  private void shouldReplaceString(String inputString, String pattern, String replacement, String expectedOutputString) throws IOException {
    byte[] inputBytes = inputString.getBytes(UTF_8);
    ByteArrayInputStream inputStream = new ByteArrayInputStream(inputBytes);

    var replacingInputStream = new ReplacingInputStream(
        inputStream,
        pattern,
        replacement
    );

    byte[] outputBytes = IOUtils.readFully(replacingInputStream, expectedOutputString.length());
    String outputString = new String(outputBytes, UTF_8);
    assertThat(outputString).isEqualTo(expectedOutputString);
  }

}