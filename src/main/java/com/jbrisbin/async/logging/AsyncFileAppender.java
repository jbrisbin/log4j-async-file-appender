package com.jbrisbin.async.logging;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class AsyncFileAppender extends AppenderSkeleton {

  private static final int THREADS = (int) Math.round(Runtime.getRuntime().availableProcessors() * .75);

  private final Object writeMutex = new Object();

  protected int threads = THREADS;
  protected ExecutorService threadPool = Executors.newFixedThreadPool(THREADS);
  protected Layout layout;
  protected boolean append = true;
  protected Path parentDir;
  protected Path path;
  protected AsynchronousFileChannel log;
  protected AtomicLong position = new AtomicLong(0);

  public AsyncFileAppender() {
  }

  public AsyncFileAppender(Layout layout) {
    this.layout = layout;
  }

  public AsyncFileAppender(Layout layout, String path, boolean append) {
    this.layout = layout;
    this.append = append;
    setPath(path);
  }

  public Layout getLayout() {
    return layout;
  }

  public void setLayout(Layout layout) {
    this.layout = layout;
  }

  public boolean isAppend() {
    return append;
  }

  public void setAppend(boolean append) {
    this.append = append;
  }

  public String getPath() {
    return path.toString();
  }

  public void setPath(String path) {
    this.path = Paths.get(path);
    this.parentDir = this.path.getParent();
    if (null != parentDir && !Files.exists(parentDir)) {
      try {
        Files.createDirectories(parentDir);
      } catch (IOException e) {
        errorHandler.error(e.getMessage(), e, ErrorCode.FILE_OPEN_FAILURE);
      }
    }
  }

  public int getThreads() {
    return threads;
  }

  public void setThreads(int threads) {
    if (threads != this.threads) {
      threadPool = Executors.newFixedThreadPool(threads);
    }
    this.threads = threads;
  }

  @Override protected void append(LoggingEvent event) {

    AsynchronousFileChannel log = log();
    if (null == log) {
      errorHandler.error("No file set for path: " + path);
      return;
    }

    if (null == layout) {
      errorHandler.error("No layout set.");
      return;
    }

    String msg = layout.format(event);
    ByteBuffer buffer = ByteBuffer.allocateDirect(msg.length());
    buffer.put(msg.getBytes());
    buffer.flip();

    long position = this.position.getAndAdd(msg.length());
    log.write(buffer, position);

  }

  @Override public void close() {
    if (null != log) {
      try {
        log.close();
      } catch (IOException e) {
        errorHandler.error(e.getMessage(), e, ErrorCode.CLOSE_FAILURE);
      }
    }
  }

  @Override public boolean requiresLayout() {
    return true;
  }

  protected AsynchronousFileChannel log() {

    if (null == this.log) {
      Set<OpenOption> openOptions = new HashSet<>();
      openOptions.add(StandardOpenOption.CREATE);
      if (!append) {
        openOptions.add(StandardOpenOption.TRUNCATE_EXISTING);
      }
      openOptions.add(StandardOpenOption.WRITE);

      try {
        this.log = AsynchronousFileChannel.open(this.path, openOptions, threadPool);
        if (append) {
          this.position.set(this.log.size());
        }
      } catch (IOException e) {
        errorHandler.error(e.getMessage(), e, ErrorCode.FILE_OPEN_FAILURE);
      }
    }

    return log;
  }

}
