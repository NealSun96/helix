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

import org.apache.helix.AccessOption;
import org.apache.helix.ConfigAccessor;
import org.apache.helix.integration.task.TaskTestBase;
import org.apache.helix.model.ClusterConfig;
import org.apache.helix.model.InstanceConfig;
import org.apache.helix.monitoring.mbeans.JobMonitor;
import org.apache.helix.task.assigner.AssignableInstance;
import org.apache.helix.zookeeper.datamodel.ZNRecord;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class TestTaskDriver extends TaskTestBase {
  // Use a thread pool size that's different from the default value for test
  private static final int TEST_THREAD_POOL_SIZE = TaskConstants.DEFAULT_TASK_THREAD_POOL_SIZE + 1;
  private static final String NON_EXISTENT_INSTANCE_NAME = "NON_EXISTENT_INSTANCE_NAME";
  private static final String TEST_JOB_TYPE = AssignableInstance.DEFAULT_QUOTA_TYPE;

  private TaskDriver _taskDriver;
  private ConfigAccessor _configAccessor;

  @BeforeClass
  public void beforeClass() throws Exception {
    super.beforeClass();

    _taskDriver = new TaskDriver(_controller);
    _configAccessor = _controller.getConfigAccessor();
  }

  @Test
  public void testGetTargetTaskThreadPoolSize() {
    String validInstanceName = _participants[0].getInstanceName();
    InstanceConfig instanceConfig =
        _configAccessor.getInstanceConfig(CLUSTER_NAME, validInstanceName);
    instanceConfig.setTargetTaskThreadPoolSize(TEST_THREAD_POOL_SIZE);
    _configAccessor.setInstanceConfig(CLUSTER_NAME, validInstanceName, instanceConfig);

    Assert.assertEquals(_taskDriver.getTargetTaskThreadPoolSize(validInstanceName),
        TEST_THREAD_POOL_SIZE);
  }

  @Test(dependsOnMethods = "testGetTargetTaskThreadPoolSize", expectedExceptions = IllegalArgumentException.class)
  public void testGetTargetTaskThreadPoolSizeWrongInstanceName() {
    _taskDriver.getTargetTaskThreadPoolSize(NON_EXISTENT_INSTANCE_NAME);
  }

  @Test(dependsOnMethods = "testGetTargetTaskThreadPoolSizeWrongInstanceName")
  public void testSetTargetTaskThreadPoolSize() {
    String validInstanceName = _participants[0].getInstanceName();
    _taskDriver.setTargetTaskThreadPoolSize(validInstanceName, TEST_THREAD_POOL_SIZE);
    InstanceConfig instanceConfig =
        _configAccessor.getInstanceConfig(CLUSTER_NAME, validInstanceName);

    Assert.assertEquals(instanceConfig.getTargetTaskThreadPoolSize(), TEST_THREAD_POOL_SIZE);
  }

  @Test(dependsOnMethods = "testSetTargetTaskThreadPoolSize", expectedExceptions = IllegalArgumentException.class)
  public void testSetTargetTaskThreadPoolSizeWrongInstanceName() {
    _taskDriver.setTargetTaskThreadPoolSize(NON_EXISTENT_INSTANCE_NAME, TEST_THREAD_POOL_SIZE);
  }

  @Test(dependsOnMethods = "testSetTargetTaskThreadPoolSizeWrongInstanceName")
  public void testGetGlobalTargetTaskThreadPoolSize() {
    ClusterConfig clusterConfig = _configAccessor.getClusterConfig(CLUSTER_NAME);
    clusterConfig.setGlobalTargetTaskThreadPoolSize(TEST_THREAD_POOL_SIZE);
    _configAccessor.setClusterConfig(CLUSTER_NAME, clusterConfig);

    Assert.assertEquals(_taskDriver.getGlobalTargetTaskThreadPoolSize(), TEST_THREAD_POOL_SIZE);
  }

  @Test(dependsOnMethods = "testGetGlobalTargetTaskThreadPoolSize")
  public void testSetGlobalTargetTaskThreadPoolSize() {
    _taskDriver.setGlobalTargetTaskThreadPoolSize(TEST_THREAD_POOL_SIZE);
    ClusterConfig clusterConfig = _configAccessor.getClusterConfig(CLUSTER_NAME);

    Assert.assertEquals(clusterConfig.getGlobalTargetTaskThreadPoolSize(), TEST_THREAD_POOL_SIZE);
  }

  @Test(dependsOnMethods = "testSetGlobalTargetTaskThreadPoolSize")
  public void testGetCurrentTaskThreadPoolSize() {
    String validInstanceName = _participants[0].getInstanceName();

    Assert.assertEquals(_taskDriver.getCurrentTaskThreadPoolSize(validInstanceName),
        TaskConstants.DEFAULT_TASK_THREAD_POOL_SIZE);
  }

  @Test(dependsOnMethods = "testGetCurrentTaskThreadPoolSize", expectedExceptions = IllegalArgumentException.class)
  public void testGetCurrentTaskThreadPoolSizeWrongInstanceName() {
    _taskDriver.getCurrentTaskThreadPoolSize(NON_EXISTENT_INSTANCE_NAME);
  }

  @Test(dependsOnMethods = "testGetCurrentTaskThreadPoolSizeWrongInstanceName")
  public void testGetSubmissionToProcessDelay() {
    String zkPath = JobMonitor.buildZkPathForJobMonitorMetric(TEST_JOB_TYPE);
    _baseAccessor.update(zkPath, currentData -> {
      if (currentData == null) {
        currentData = new ZNRecord(TEST_JOB_TYPE);
      }
      currentData
          .setSimpleField(JobMonitor.JobMonitorMetricZnodeField.SUBMISSION_TO_PROCESS_DELAY.name(),
              Long.toString(100L));
      return currentData;
    }, AccessOption.PERSISTENT);

    Assert.assertEquals(_taskDriver.getSubmissionToProcessDelay(TEST_JOB_TYPE), 100L);
  }

  @Test(dependsOnMethods = "testGetSubmissionToProcessDelay", expectedExceptions = IllegalArgumentException.class)
  public void testGetSubmissionToProcessDelayIllegalArgument() {
    _taskDriver.getSubmissionToProcessDelay("ILLEGAL_JOB_TYPE");
  }

  @Test(dependsOnMethods = "testGetSubmissionToProcessDelayIllegalArgument")
  public void testGetSubmissionToScheduleDelay() {
    String zkPath = JobMonitor.buildZkPathForJobMonitorMetric(TEST_JOB_TYPE);
    _baseAccessor.update(zkPath, currentData -> {
      if (currentData == null) {
        currentData = new ZNRecord(TEST_JOB_TYPE);
      }
      currentData
          .setSimpleField(JobMonitor.JobMonitorMetricZnodeField.SUBMISSION_TO_SCHEDULE_DELAY.name(),
              Long.toString(100L));
      return currentData;
    }, AccessOption.PERSISTENT);

    Assert.assertEquals(_taskDriver.getSubmissionToScheduleDelay(TEST_JOB_TYPE), 100L);
  }

  @Test(dependsOnMethods = "testGetSubmissionToScheduleDelay", expectedExceptions = IllegalArgumentException.class)
  public void testGetSubmissionToScheduleDelayIllegalArgument() {
    _taskDriver.getSubmissionToScheduleDelay("ILLEGAL_JOB_TYPE");
  }

  @Test(dependsOnMethods = "testGetSubmissionToScheduleDelayIllegalArgument")
  public void testControllerInducedDelay() {
    String zkPath = JobMonitor.buildZkPathForJobMonitorMetric(TEST_JOB_TYPE);
    _baseAccessor.update(zkPath, currentData -> {
      if (currentData == null) {
        currentData = new ZNRecord(TEST_JOB_TYPE);
      }
      currentData.setSimpleField(
          JobMonitor.JobMonitorMetricZnodeField.CONTROLLER_INDUCED_PROCESS_DELAY.name(),
          Long.toString(100L));
      return currentData;
    }, AccessOption.PERSISTENT);

    Assert.assertEquals(_taskDriver.getControllerInducedDelay(TEST_JOB_TYPE), 100L);
  }

  @Test(dependsOnMethods = "testControllerInducedDelay", expectedExceptions = IllegalArgumentException.class)
  public void testControllerInducedDelayIllegalArgument() {
    _taskDriver.getControllerInducedDelay("ILLEGAL_JOB_TYPE");
  }
}
