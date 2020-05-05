package org.apache.helix.task;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.JMException;

import org.apache.helix.ConfigAccessor;
import org.apache.helix.HelixDataAccessor;
import org.apache.helix.HelixManager;
import org.apache.helix.model.ClusterConfig;
import org.apache.helix.model.InstanceConfig;
import org.apache.helix.model.LiveInstance;
import org.apache.helix.monitoring.mbeans.ThreadPoolExecutorMonitor;
import org.apache.helix.participant.statemachine.StateModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Factory class for {@link TaskStateModel}.
 */
public class TaskStateModelFactory extends StateModelFactory<TaskStateModel> {
  private static Logger LOG = LoggerFactory.getLogger(TaskStateModelFactory.class);

  private final HelixManager _manager;
  private final Map<String, TaskFactory> _taskFactoryRegistry;
  private ScheduledExecutorService _taskExecutor;
  private final ScheduledExecutorService _timerTaskExecutor;
  private ThreadPoolExecutorMonitor _monitor;

  public TaskStateModelFactory(HelixManager manager, Map<String, TaskFactory> taskFactoryRegistry) {
    this(manager, taskFactoryRegistry, null);
  }

  // This constructor is only for internal usage. Do not use!
  public TaskStateModelFactory(HelixManager manager, Map<String, TaskFactory> taskFactoryRegistry,
      ScheduledExecutorService taskExecutor) {
    _manager = manager;
    _taskFactoryRegistry = taskFactoryRegistry;
    _taskExecutor = taskExecutor;
    _timerTaskExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, "TaskStateModelFactory-timeTask_thread");
      }
    });
    if (_taskExecutor instanceof ThreadPoolExecutor) {
      try {
        _monitor = new ThreadPoolExecutorMonitor(TaskConstants.STATE_MODEL_NAME,
            (ThreadPoolExecutor) _taskExecutor);
      } catch (JMException e) {
        LOG.warn("Error in creating ThreadPoolExecutorMonitor for TaskStateModelFactory.");
      }
    }
  }

  @Override
  public TaskStateModel createNewStateModel(String resourceName, String partitionKey) {
    if (_taskExecutor == null) {
      int taskThreadPoolSize = getTaskThreadPoolSize();
      updateLiveInstanceWithThreadPoolSize(taskThreadPoolSize);
      initializeTaskExecutor(taskThreadPoolSize);
      initializeTaskMonitor();
    }
    return new TaskStateModel(_manager, _taskFactoryRegistry, _taskExecutor, _timerTaskExecutor);
  }

  public void shutdown() {
    if (_monitor != null) {
      _monitor.unregister();
    }
    _taskExecutor.shutdown();
    _timerTaskExecutor.shutdown();
    if (_monitor != null) {
      _monitor.unregister();
    }
  }

  public boolean isShutdown() {
    return _taskExecutor.isShutdown();
  }

  public boolean isTerminated() {
    return _taskExecutor.isTerminated();
  }

  /*
   * Get target thread pool size from InstanceConfig first; if that fails, get it from
   * ClusterConfig; if that fails, fall back to the default value.
   */
  private int getTaskThreadPoolSize() {
    ConfigAccessor configAccessor = _manager.getConfigAccessor();
    // Check instance config first for thread pool size
    InstanceConfig instanceConfig =
        configAccessor.getInstanceConfig(_manager.getClusterName(), _manager.getInstanceName());
    if (instanceConfig != null) {
      int targetTaskThreadPoolSize = instanceConfig.getTargetTaskThreadPoolSize();
      if (verifyTargetThreadPoolSize(targetTaskThreadPoolSize)) {
        return targetTaskThreadPoolSize;
      }
    }

    // Fallback to cluster config since instance config doesn't provide the value
    ClusterConfig clusterConfig = configAccessor.getClusterConfig(_manager.getClusterName());
    if (clusterConfig != null) {
      int targetTaskThreadPoolSize = clusterConfig.getDefaultTargetTaskThreadPoolSize();
      if (verifyTargetThreadPoolSize(targetTaskThreadPoolSize)) {
        return targetTaskThreadPoolSize;
      }
    }

    return TaskConstants.DEFAULT_TASK_THREAD_POOL_SIZE;
  }

  /*
   * Checks against the default values of -1 when pool sizes are not defined; we don't want -1's
   */
  private static boolean verifyTargetThreadPoolSize(int targetTaskThreadPoolSize) {
    return targetTaskThreadPoolSize > 0;
  }

  /*
   * Update LiveInstance with the current used thread pool size
   */
  private void updateLiveInstanceWithThreadPoolSize(int taskThreadPoolSize) {
    HelixDataAccessor zkHelixDataAccessor = _manager.getHelixDataAccessor();
    LiveInstance liveInstance = zkHelixDataAccessor
        .getProperty(zkHelixDataAccessor.keyBuilder().liveInstance(_manager.getInstanceName()));
    liveInstance.setCurrentTaskThreadPoolSize(taskThreadPoolSize);
    zkHelixDataAccessor
        .setProperty(zkHelixDataAccessor.keyBuilder().liveInstance(_manager.getInstanceName()), liveInstance);
  }

  private void initializeTaskExecutor(int taskThreadPoolSize) {
    _taskExecutor = Executors.newScheduledThreadPool(taskThreadPoolSize, new ThreadFactory() {
      private AtomicInteger threadId = new AtomicInteger(0);

      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, "TaskStateModelFactory-task_thread-" + threadId.getAndIncrement());
      }
    });
  }

  private void initializeTaskMonitor() {
    if (_taskExecutor instanceof ThreadPoolExecutor) {
      try {
        _monitor = new ThreadPoolExecutorMonitor(TaskConstants.STATE_MODEL_NAME,
            (ThreadPoolExecutor) _taskExecutor);
      } catch (JMException e) {
        LOG.warn("Error in creating ThreadPoolExecutorMonitor for TaskStateModelFactory.");
      }
    }
  }
}
