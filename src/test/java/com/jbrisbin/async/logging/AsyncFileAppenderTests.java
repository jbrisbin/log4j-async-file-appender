package com.jbrisbin.async.logging;

import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class AsyncFileAppenderTests {

  @Test
  public void testAsyncFileAppender() {
    Logger log = Logger.getLogger(getClass());
    log.trace("TRACE message");
    log.debug("DEBUG message");
    log.info("INFO message");
    log.warn("WARN message");
    log.error("ERROR message");
  }

}
