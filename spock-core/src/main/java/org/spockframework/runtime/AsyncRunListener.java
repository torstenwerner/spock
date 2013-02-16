/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.spockframework.runtime;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.spockframework.runtime.model.*;

public class AsyncRunListener implements IRunListener {
  private static final Runnable STOP = new Runnable() {
    public void run() {
      throw new IllegalStateException("should never run");
    }
  };

  private final BlockingQueue<Runnable> events = new LinkedBlockingQueue<Runnable>();

  private String threadName;
  private final IRunListener delegate;
  
  private volatile boolean stopped = false;
  
  private final Thread workerThread = new Thread(threadName) {
    @Override
    public void run() {
      while(true) {
        Runnable event = events.poll();
        if (event == STOP) return;
        try {
          event.run();
        } catch (Throwable t) {
          stopped = true;
          t.printStackTrace();
          return;
        }
      }
    }
  };

  public AsyncRunListener(String threadName, IRunListener delegate) {
    this.threadName = threadName;
    this.delegate = delegate;
  }

  public void start() {
    workerThread.start();
  }

  public void stop() throws InterruptedException {
    addEvent(STOP);
    workerThread.join();
  }

  public void beforeSpec(final SpecInfo spec) {
    addEvent(new Runnable() {
      public void run() {
        delegate.beforeSpec(spec);
      }
    });  
  }

  public void beforeFeature(final FeatureInfo feature) {
    if (stopped) return;
    
    addEvent(new Runnable() {
      public void run() {
        delegate.beforeFeature(feature);
      }
    });
  }

  public void beforeIteration(final IterationInfo iteration) {
    addEvent(new Runnable() {
      public void run() {
        delegate.beforeIteration(iteration);
      }
    });
  }

  public void afterIteration(final IterationInfo iteration) {
    addEvent(new Runnable() {
      public void run() {
        delegate.afterIteration(iteration);
      }
    });
  }

  public void afterFeature(final FeatureInfo feature) {
    addEvent(new Runnable() {
      public void run() {
        delegate.afterFeature(feature);
      }
    });
  }

  public void afterSpec(final SpecInfo spec) {
    addEvent(new Runnable() {
      public void run() {
        delegate.afterSpec(spec);
      }
    });
  }

  public void error(final ErrorInfo error) {
    addEvent(new Runnable() {
      public void run() {
        delegate.error(error);
      }
    });
  }

  public void specSkipped(final SpecInfo spec) {
    addEvent(new Runnable() {
      public void run() {
        delegate.specSkipped(spec);
      }
    });
  }

  public void featureSkipped(final FeatureInfo feature) {
    addEvent(new Runnable() {
      public void run() {
        delegate.featureSkipped(feature);
      }
    });
  }

  protected void addEvent(Runnable event) {
    if (stopped) return;
    events.add(event);
  }
}
