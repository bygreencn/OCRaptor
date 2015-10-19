package mj.ocraptor.file_handler;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Monitor;

public class PausableExecutor extends ScheduledThreadPoolExecutor {

  private final Logger LOG = LoggerFactory.getLogger(getClass());
  private boolean isPaused;

  private final Monitor monitor = new Monitor();
  private final Monitor.Guard paused = new Monitor.Guard(monitor) {
    @Override
    public boolean isSatisfied() {
      return isPaused;
    }
  };

  private final Monitor.Guard notPaused = new Monitor.Guard(monitor) {
    @Override
    public boolean isSatisfied() {
      return !isPaused;
    }
  };

  public PausableExecutor(int corePoolSize, ThreadFactory threadFactory) {
    super(corePoolSize, threadFactory);
  }

  protected void beforeExecute(Thread t, Runnable r) {
    super.beforeExecute(t, r);
    monitor.enterWhenUninterruptibly(notPaused);
    try {
      monitor.waitForUninterruptibly(notPaused);
    } finally {
      monitor.leave();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see java.util.concurrent.ThreadPoolExecutor#afterExecute(Runnable,Throwable)
   */
  protected void afterExecute(Runnable r, Throwable t) {
    super.afterExecute(r, t);
    if (t == null && r instanceof Future<?>) {
      try {
        Future<?> future = (Future<?>) r;
        if (future.isDone())
          future.get();
      } catch (CancellationException ce) {
        t = ce;
      } catch (ExecutionException ee) {
        t = ee.getCause();
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt(); // ignore/reset
      }
    }
    // TODO: 
    if (t != null) {
      LOG.error("Thread interrupted", t);
    }
  }

  public void pause() {
    monitor.enterIf(notPaused);
    try {
      isPaused = true;
    } finally {
      monitor.leave();
    }
  }

  public void resume() {
    monitor.enterIf(paused);
    try {
      isPaused = false;
    } finally {
      monitor.leave();
    }
  }

  /**
   * @return the isPaused
   */
  public boolean isPaused() {
    return isPaused;
  }
}
