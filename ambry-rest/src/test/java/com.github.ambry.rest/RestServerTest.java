package com.github.ambry.rest;

import com.github.ambry.clustermap.ClusterMap;
import com.github.ambry.clustermap.MockClusterMap;
import com.github.ambry.commons.LoggingNotificationSystem;
import com.github.ambry.config.VerifiableProperties;
import com.github.ambry.notification.NotificationSystem;
import com.github.ambry.router.InMemoryRouterFactory;
import java.io.IOException;
import java.util.Properties;
import org.junit.Test;

import static org.junit.Assert.fail;


/**
 * Test functionality of {@link RestServer}.
 */
public class RestServerTest {

  /**
   * Tests {@link RestServer#start()} and {@link RestServer#shutdown()}.
   * @throws InstantiationException
   * @throws InterruptedException
   * @throws IOException
   */
  @Test
  public void startShutdownTest()
      throws InstantiationException, InterruptedException, IOException {
    Properties properties = new Properties();
    VerifiableProperties verifiableProperties = getVProps(properties);
    ClusterMap clusterMap = new MockClusterMap();
    NotificationSystem notificationSystem = new LoggingNotificationSystem();

    RestServer server = new RestServer(verifiableProperties, clusterMap, notificationSystem);
    server.start();
    server.shutdown();
    server.awaitShutdown();
  }

  /**
   * Tests for {@link RestServer#shutdown()} when {@link RestServer#start()} had not been called previously. This test
   * is for cases where {@link RestServer#start()} has failed and {@link RestServer#shutdown()} needs to be run.
   * @throws InstantiationException
   * @throws InterruptedException
   * @throws IOException
   */
  @Test
  public void shutdownWithoutStartTest()
      throws InstantiationException, InterruptedException, IOException {
    Properties properties = new Properties();
    VerifiableProperties verifiableProperties = getVProps(properties);
    ClusterMap clusterMap = new MockClusterMap();
    NotificationSystem notificationSystem = new LoggingNotificationSystem();

    RestServer server = new RestServer(verifiableProperties, clusterMap, notificationSystem);
    server.shutdown();
    server.awaitShutdown();
  }

  /**
   * Tests for correct exceptions thrown on {@link RestServer} instantiation/{@link RestServer#start()} with bad input.
   * @throws InstantiationException
   * @throws IOException
   */
  @Test
  public void serverCreationWithBadInputTest()
      throws InstantiationException, IOException {
    badArgumentsTest();
    badFactoriesTest();
  }

  /**
   * Tests for correct exceptions thrown on {@link RestServer#start()}/{@link RestServer#shutdown()} with bad
   * components.
   * @throws InstantiationException
   * @throws IOException
   */
  @Test
  public void startShutdownTestWithBadComponent()
      throws InstantiationException, IOException {
    Properties properties = new Properties();
    properties.setProperty("rest.server.nio.server.factory", MockNioServerFactory.class.getCanonicalName());
    // makes MockNioServer throw exceptions.
    properties.setProperty(MockNioServerFactory.IS_FAULTY_KEY, "true");
    VerifiableProperties verifiableProperties = getVProps(properties);
    ClusterMap clusterMap = new MockClusterMap();
    NotificationSystem notificationSystem = new LoggingNotificationSystem();
    RestServer server = new RestServer(verifiableProperties, clusterMap, notificationSystem);
    try {
      server.start();
      fail("start() should not be successful. MockNioServer::start() would have thrown InstantiationException");
    } catch (InstantiationException e) {
      // nothing to do. expected.
    } finally {
      try {
        server.shutdown();
        fail("RestServer shutdown should have failed.");
      } catch (RuntimeException e) {
        // nothing to do. expected.
      }
    }
  }

  // helpers
  // general

  /**
   * Gets {@link VerifiableProperties} with some mandatory values set.
   * @param properties the {@link Properties} file to use to build the {@link VerifiableProperties}.
   * @return an instance of {@link VerifiableProperties}.
   */
  private VerifiableProperties getVProps(Properties properties) {
    properties.setProperty("rest.server.router.factory", InMemoryRouterFactory.class.getCanonicalName());
    properties.setProperty("rest.server.rest.response.handler.factory",
        MockRestRequestResponseHandlerFactory.class.getCanonicalName());
    properties.setProperty("rest.server.blob.storage.service.factory",
        MockBlobStorageServiceFactory.class.getCanonicalName());
    properties.setProperty("rest.server.rest.request.handler.factory",
        MockRestRequestResponseHandlerFactory.class.getCanonicalName());
    properties.setProperty("rest.server.nio.server.factory", MockNioServerFactory.class.getCanonicalName());
    return new VerifiableProperties(properties);
  }

  // serverCreationWithBadInputTest() helpers

  /**
   * Tests {@link RestServer} instantiation attempts with bad input.
   * @throws InstantiationException
   * @throws IOException
   */
  private void badArgumentsTest()
      throws InstantiationException, IOException {
    // dud properties. server should pick up defaults
    Properties properties = new Properties();
    VerifiableProperties verifiableProperties = new VerifiableProperties(properties);
    ClusterMap clusterMap = new MockClusterMap();
    NotificationSystem notificationSystem = new LoggingNotificationSystem();

    try {
      // no props.
      new RestServer(null, clusterMap, notificationSystem);
      fail("Properties missing, yet no exception was thrown");
    } catch (IllegalArgumentException e) {
      // nothing to do. expected.
    }

    try {
      // no ClusterMap.
      new RestServer(verifiableProperties, null, notificationSystem);
      fail("ClusterMap missing, yet no exception was thrown");
    } catch (IllegalArgumentException e) {
      // nothing to do. expected.
    }

    try {
      // no NotificationSystem.
      new RestServer(verifiableProperties, clusterMap, null);
      fail("NotificationSystem missing, yet no exception was thrown");
    } catch (IllegalArgumentException e) {
      // nothing to do. expected.
    }
  }

  private void badFactoriesTest()
      throws IOException {
    doBadFactoryClassTest("rest.server.nio.server.factory");
    doBadFactoryClassTest("rest.server.blob.storage.service.factory");
    doBadFactoryClassTest("rest.server.router.factory");
  }

  private void doBadFactoryClassTest(String configKey)
      throws IOException {
    Properties properties = new Properties();
    properties.setProperty("rest.server.router.factory", InMemoryRouterFactory.class.getCanonicalName());
    properties.setProperty("rest.server.rest.response.handler.factory",
        MockRestRequestResponseHandlerFactory.class.getCanonicalName());
    properties.setProperty("rest.server.blob.storage.service.factory",
        MockBlobStorageServiceFactory.class.getCanonicalName());
    properties.setProperty("rest.server.rest.request.handler.factory",
        MockRestRequestResponseHandlerFactory.class.getCanonicalName());
    properties.setProperty("rest.server.nio.server.factory", MockNioServerFactory.class.getCanonicalName());

    // Non existent class.
    properties.setProperty(configKey, "non.existent.factory");
    VerifiableProperties verifiableProperties = new VerifiableProperties(properties);
    try {
      new RestServer(verifiableProperties, new MockClusterMap(), new LoggingNotificationSystem());
      fail("Properties file contained non existent " + configKey + ", yet no exception was thrown");
    } catch (InstantiationException e) {
      // nothing to do. expected.
    }

    // invalid factory class.
    properties.setProperty(configKey, RestServer.class.getCanonicalName());
    verifiableProperties = new VerifiableProperties(properties);
    try {
      new RestServer(verifiableProperties, new MockClusterMap(), new LoggingNotificationSystem());
      fail("Properties file contained invalid " + configKey + " class, yet no exception was thrown");
    } catch (InstantiationException e) {
      // nothing to do. expected.
    }

    // faulty factory class
    properties.setProperty(configKey, FaultyFactory.class.getCanonicalName());
    verifiableProperties = new VerifiableProperties(properties);
    try {
      RestServer restServer =
          new RestServer(verifiableProperties, new MockClusterMap(), new LoggingNotificationSystem());
      restServer.start();
      fail("Properties file contained faulty " + configKey + " class, yet no exception was thrown");
    } catch (InstantiationException e) {
      // nothing to do. expected.
    } catch (NullPointerException e) {
      // nothing to do. expected.
    }
  }
}
