package dev.streamx.aem.connector.blueprints;

class Mapping {

  private final String pagePathRegExp;
  private final String channelName;
  private final boolean isExperienceFragment;

  Mapping(String pagePathRegExp, String channelName, boolean isExperienceFragment) {
    this.pagePathRegExp = pagePathRegExp;
    this.channelName = channelName;
    this.isExperienceFragment = isExperienceFragment;
  }

  public String pagePathRegExp() {
    return pagePathRegExp;
  }

  public String channelName() {
    return channelName;
  }

  public boolean isExperienceFragment() {
    return isExperienceFragment;
  }

  @Override
  public String toString() {
    return "Mapping{" +
        "pagePathRegExp='" + pagePathRegExp + '\'' +
        ", channelName='" + channelName + '\'' +
        ", isExperienceFragment=" + isExperienceFragment +
        '}';
  }
}
