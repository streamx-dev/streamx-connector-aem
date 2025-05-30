package dev.streamx.aem.connector.blueprints;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageDataServiceShortenContentPathsTest {

  private static final String SAMPLE_INPUT_CONTENT = joinLines(
      "<a href='/content/space-1/pages/page-1'>Page ((1))</a>",
      "<a href='/content/space-1/blogs/blog-1'>Blog ((1))</a>",
      "<a href='/content/space-1/pages/page-2'>Page ((2))</a>",
      "<a href='/content/space-1/blogs/blog-2'>Blog ((2))</a>",
      "<a href='/content/space-2/pages/page-3'>Page ((3))</a>",
      "<a href='/content/space-2/blogs/blog-3'>Blog ((3))</a>"
  );

  @Test
  void shouldShortenContentPaths() {
    // given
    String path = "/content/space-1";

    // when
    String result = PageDataService.shortenContentPaths(path, SAMPLE_INPUT_CONTENT);

    // then
    assertThat(result).isEqualTo(joinLines(
        "<a href='/page-1'>Page {{1}}</a>",
        "<a href='/blogs/blog-1'>Blog {{1}}</a>",
        "<a href='/page-2'>Page {{2}}</a>",
        "<a href='/blogs/blog-2'>Blog {{2}}</a>",
        "<a href='/content/space-2/pages/page-3'>Page {{3}}</a>",
        "<a href='/content/space-2/blogs/blog-3'>Blog {{3}}</a>"
    ));
  }

  @Test
  void shouldNotShortenContentPaths_WhenPathWithoutSpaceElement() {
    // given
    String path = "/content";

    // when
    String result = PageDataService.shortenContentPaths(path, SAMPLE_INPUT_CONTENT);

    // then
    assertThat(result).isEqualTo(SAMPLE_INPUT_CONTENT);
  }

  @Test
  void shouldNotShortenContentPaths_WhenPathIsNotContentPath() {
    // given
    String path = "/sites/space-3";

    // when
    String result = PageDataService.shortenContentPaths(path, SAMPLE_INPUT_CONTENT);

    // then
    assertThat(result).isEqualTo(SAMPLE_INPUT_CONTENT);
  }

  private static String joinLines(String... lines) {
    return String.join("/n", lines);
  }
}
