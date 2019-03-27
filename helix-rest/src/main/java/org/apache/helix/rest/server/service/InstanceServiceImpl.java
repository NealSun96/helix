package org.apache.helix.rest.server.service;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.helix.ConfigAccessor;
import org.apache.helix.HelixDataAccessor;
import org.apache.helix.HelixException;
import org.apache.helix.model.CurrentState;
import org.apache.helix.model.InstanceConfig;
import org.apache.helix.model.LiveInstance;
import org.apache.helix.rest.server.json.instance.InstanceInfo;
import org.apache.helix.util.InstanceValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstanceServiceImpl implements InstanceService {
  private static final Logger _logger = LoggerFactory.getLogger(InstanceServiceImpl.class);

  private final HelixDataAccessor _dataAccessor;
  private final ConfigAccessor _configAccessor;

  public InstanceServiceImpl(HelixDataAccessor dataAccessor, ConfigAccessor configAccessor) {
    _dataAccessor = dataAccessor;
    _configAccessor = configAccessor;
  }

  @Override
  public Map<String, Boolean> getInstanceHealthStatus(String clusterId, String instanceName,
      List<HealthCheck> healthChecks) {
    Map<String, Boolean> healthStatus = new HashMap<>();
    for (HealthCheck healthCheck : healthChecks) {
      switch (healthCheck) {
      case INVALID_CONFIG:
        healthStatus.put(HealthCheck.INVALID_CONFIG.name(),
            InstanceValidationUtil.hasValidConfig(_dataAccessor, clusterId, instanceName));
        if (!healthStatus.get(HealthCheck.INVALID_CONFIG.name())) {
          _logger.error("The instance {} doesn't have valid configuration", instanceName);
          return healthStatus;
        }
      case INSTANCE_NOT_ENABLED:
        healthStatus.put(HealthCheck.INSTANCE_NOT_ENABLED.name(), InstanceValidationUtil
            .isEnabled(_dataAccessor, _configAccessor, clusterId, instanceName));
        break;
      case INSTANCE_NOT_ALIVE:
        healthStatus.put(HealthCheck.INSTANCE_NOT_ALIVE.name(),
            InstanceValidationUtil.isAlive(_dataAccessor, clusterId, instanceName));
        break;
      case INSTANCE_NOT_STABLE:
        try {
          boolean isStable = InstanceValidationUtil.isInstanceStable(_dataAccessor, instanceName);
          healthStatus.put(HealthCheck.INSTANCE_NOT_STABLE.name(), isStable);
        } catch (HelixException e) {
          _logger.error("Failed to check instance is stable, message: {}", e.getMessage());
          // TODO action on the stable check exception
        }
        break;
      case HAS_ERROR_PARTITION:
        healthStatus.put(HealthCheck.HAS_ERROR_PARTITION.name(),
            !InstanceValidationUtil.hasErrorPartitions(_dataAccessor, clusterId, instanceName));
        break;
      case HAS_DISABLED_PARTITION:
        healthStatus.put(HealthCheck.HAS_DISABLED_PARTITION.name(),
            !InstanceValidationUtil.hasDisabledPartitions(_dataAccessor, clusterId, instanceName));
        break;
      case EMPTY_RESOURCE_ASSIGNMENT:
        healthStatus.put(HealthCheck.EMPTY_RESOURCE_ASSIGNMENT.name(),
            InstanceValidationUtil.hasResourceAssigned(_dataAccessor, clusterId, instanceName));
        break;
      default:
        _logger.error("Unsupported health check: {}", healthCheck);
        break;
      }
    }

    return healthStatus;
  }

  @Override
  public InstanceInfo getInstanceInfo(String clusterId, String instanceName,
      List<HealthCheck> healthChecks) {
    InstanceInfo.Builder instanceInfoBuilder = new InstanceInfo.Builder(instanceName);

    InstanceConfig instanceConfig =
        _dataAccessor.getProperty(_dataAccessor.keyBuilder().instanceConfig(instanceName));
    LiveInstance liveInstance =
        _dataAccessor.getProperty(_dataAccessor.keyBuilder().liveInstance(instanceName));
    if (instanceConfig != null) {
      instanceInfoBuilder.instanceConfig(instanceConfig.getRecord());
    }
    if (liveInstance != null) {
      instanceInfoBuilder.liveInstance(liveInstance.getRecord());
      String sessionId = liveInstance.getSessionId();

      List<String> resourceNames = _dataAccessor
          .getChildNames(_dataAccessor.keyBuilder().currentStates(instanceName, sessionId));
      instanceInfoBuilder.resources(resourceNames);
      List<String> partitions = new ArrayList<>();
      for (String resourceName : resourceNames) {
        CurrentState currentState = _dataAccessor.getProperty(
            _dataAccessor.keyBuilder().currentState(instanceName, sessionId, resourceName));
        if (currentState != null && currentState.getPartitionStateMap() != null) {
          partitions.addAll(currentState.getPartitionStateMap().keySet());
        }
      }
      instanceInfoBuilder.partitions(partitions);
    }
    instanceInfoBuilder
        .healthStatus(getInstanceHealthStatus(clusterId, instanceName, healthChecks));

    return instanceInfoBuilder.build();
  }
}
