package dev.streamx.aem.connector.blueprints;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AssetHashManager {

  private static final Logger LOG = LoggerFactory.getLogger(AssetHashManager.class);

  private static final String BASE_NODE_PATH_FOR_HASHES = "/var/streamx/connector/sling/hashes/assets";
  private static final String PN_LAST_PUBLISH_HASH = "lastPublishHash";

  private AssetHashManager() {
    // no instances
  }

  /**
   * Loads current hash for the Asset from JCR, and overwrites the hash if it has changed for given {@code assetContent}
   * @return true if the hash has changed, false otherwise
   */
  static boolean hasAssetContentChanged(String assetResourcePath, byte[] assetContent, ResourceResolver resourceResolver)
      throws RepositoryException, NoSuchAlgorithmException {

    Session session = resourceResolver.adaptTo(Session.class);
    if (session == null) {
      LOG.error("Error accessing Session to verify if Asset '{}' has changed", assetResourcePath);
      return true;
    }

    Node assetHashNode = getOrCreateAssetHashNode(assetResourcePath, session);

    String newHash = computeHash(assetContent);
    if (assetHashNode.hasProperty(PN_LAST_PUBLISH_HASH)) {
      String oldHash = assetHashNode.getProperty(PN_LAST_PUBLISH_HASH).getString();
      if (newHash.equals(oldHash)) {
        return false;
      }
    }

    assetHashNode.setProperty(PN_LAST_PUBLISH_HASH, newHash);
    session.save();
    return true;
  }

  private static Node getOrCreateAssetHashNode(String assetResourcePath, Session session) throws RepositoryException {
    String hashNodePath = computeAssetHashNodePath(assetResourcePath);
    String[] nodePathItems = StringUtils.split(hashNodePath, "/");
    String currentPath = "";
    Node currentNode = session.getRootNode();

    for (int i = 0; i < nodePathItems.length; i++) {
      boolean isLastPathItem = (i == nodePathItems.length - 1);
      String pathItem = nodePathItems[i];

      currentPath = String.format("%s/%s", currentPath, pathItem);
      if (session.nodeExists(currentPath)) {
        currentNode = session.getNode(currentPath);
      } else {
        String nodeType = isLastPathItem ? "nt:unstructured" : "sling:Folder";
        currentNode = currentNode.addNode(pathItem, nodeType);
      }
    }

    return currentNode;
  }

  private static String computeAssetHashNodePath(String assetResourcePath) {
    return BASE_NODE_PATH_FOR_HASHES + assetResourcePath;
  }

  private static String computeHash(byte[] bytes) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hashBytes = digest.digest(bytes);
    return bytesToHex(hashBytes);
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  static void deleteAssetHash(String assetResourcePath, ResourceResolver resourceResolver) {
    Session session = resourceResolver.adaptTo(Session.class);
    if (session == null) {
      LOG.error("Error accessing Session to delete hash for Asset '{}'", assetResourcePath);
      return;
    }

    String assetHashNodePath = computeAssetHashNodePath(assetResourcePath);
    Resource assetResource = resourceResolver.getResource(assetHashNodePath);
    if (assetResource != null) {
      try {
        resourceResolver.delete(assetResource);
        session.save();
      } catch (Exception e) {
        LOG.error("Error deleting hash for Asset '{}'", assetResourcePath, e);
      }
    }
  }

}
