/*
 * Copyright 2017 LinkedIn Corp. Licensed under the BSD 2-Clause License (the "License"). See License in the project root for license information.
 */

package com.linkedin.kafka.cruisecontrol.analyzer;

import com.linkedin.kafka.cruisecontrol.CruiseControlUnitTestUtils;
import com.linkedin.kafka.cruisecontrol.analyzer.goals.CpuCapacityGoal;
import com.linkedin.kafka.cruisecontrol.analyzer.goals.CpuUsageDistributionGoal;
import com.linkedin.kafka.cruisecontrol.analyzer.goals.DiskCapacityGoal;
import com.linkedin.kafka.cruisecontrol.analyzer.goals.DiskUsageDistributionGoal;
import com.linkedin.kafka.cruisecontrol.analyzer.goals.NetworkInboundCapacityGoal;
import com.linkedin.kafka.cruisecontrol.analyzer.goals.NetworkInboundUsageDistributionGoal;
import com.linkedin.kafka.cruisecontrol.analyzer.goals.NetworkOutboundCapacityGoal;
import com.linkedin.kafka.cruisecontrol.analyzer.goals.NetworkOutboundUsageDistributionGoal;
import com.linkedin.kafka.cruisecontrol.analyzer.goals.PotentialNwOutGoal;
import com.linkedin.kafka.cruisecontrol.analyzer.goals.RackAwareGoal;
import com.linkedin.kafka.cruisecontrol.analyzer.goals.ReplicaCapacityGoal;
import com.linkedin.kafka.cruisecontrol.analyzer.goals.ReplicaDistributionGoal;
import com.linkedin.kafka.cruisecontrol.analyzer.goals.TopicReplicaDistributionGoal;
import com.linkedin.kafka.cruisecontrol.config.KafkaCruiseControlConfig;
import com.linkedin.kafka.cruisecontrol.common.ClusterProperty;
import com.linkedin.kafka.cruisecontrol.common.RandomCluster;
import com.linkedin.kafka.cruisecontrol.common.TestConstants;
import com.linkedin.kafka.cruisecontrol.exception.AnalysisInputException;
import com.linkedin.kafka.cruisecontrol.model.ClusterModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import java.util.Properties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;


@RunWith(Parameterized.class)
public class RandomSelfHealingTest {
  private static final Logger LOG = LoggerFactory.getLogger(RandomSelfHealingTest.class);

  /**
   * Populate parameters for the {@link OptimizationVerifier}. All brokers are alive.
   *
   * @return Parameters for the {@link OptimizationVerifier}.
   */
  @Parameters
  public static Collection<Object[]> data() throws AnalysisInputException {
    Collection<Object[]> params = new ArrayList<>();

    Map<Integer, String> goalNameByPriority = new HashMap<>();
    goalNameByPriority.put(1, RackAwareGoal.class.getName());
    goalNameByPriority.put(2, ReplicaCapacityGoal.class.getName());
    goalNameByPriority.put(3, CpuCapacityGoal.class.getName());
    goalNameByPriority.put(4, DiskCapacityGoal.class.getName());
    goalNameByPriority.put(5, NetworkInboundCapacityGoal.class.getName());
    goalNameByPriority.put(6, NetworkOutboundCapacityGoal.class.getName());
    goalNameByPriority.put(7, PotentialNwOutGoal.class.getName());
    goalNameByPriority.put(8, DiskUsageDistributionGoal.class.getName());
    goalNameByPriority.put(9, NetworkInboundUsageDistributionGoal.class.getName());
    goalNameByPriority.put(10, NetworkOutboundUsageDistributionGoal.class.getName());
    goalNameByPriority.put(11, CpuUsageDistributionGoal.class.getName());
    goalNameByPriority.put(12, TopicReplicaDistributionGoal.class.getName());
    goalNameByPriority.put(13, ReplicaDistributionGoal.class.getName());


    Properties props = CruiseControlUnitTestUtils.getCruiseControlProperties();
    props.setProperty(KafkaCruiseControlConfig.MAX_REPLICAS_PER_BROKER_CONFIG, Long.toString(2000L));
    BalancingConstraint balancingConstraint = new BalancingConstraint(new KafkaCruiseControlConfig(props));
    balancingConstraint.setBalancePercentage(TestConstants.LOW_BALANCE_PERCENTAGE);
    balancingConstraint.setCapacityThreshold(TestConstants.MEDIUM_CAPACITY_THRESHOLD);

    // -- TEST DECK #1: SINGLE DEAD BROKER.
    // Test: Single Goal.
    Map<ClusterProperty, Number> singleDeadBroker = new HashMap<>();
    singleDeadBroker.put(ClusterProperty.NUM_DEAD_BROKERS, 1);
    for (Map.Entry<Integer, String> entry : goalNameByPriority.entrySet()) {
      Object[] singleDeadSingleSoftParams = {singleDeadBroker, Collections.singletonMap(entry.getKey(),
          entry.getValue()), balancingConstraint};
      params.add(singleDeadSingleSoftParams);
    }
    props.setProperty(KafkaCruiseControlConfig.MAX_REPLICAS_PER_BROKER_CONFIG, Long.toString(5000L));
    balancingConstraint = new BalancingConstraint(new KafkaCruiseControlConfig(props));
    balancingConstraint.setBalancePercentage(TestConstants.LOW_BALANCE_PERCENTAGE);
    balancingConstraint.setCapacityThreshold(TestConstants.MEDIUM_CAPACITY_THRESHOLD);

    // Test: All Goals.
    Object[] singleDeadMultiAllGoalsParams = {singleDeadBroker, goalNameByPriority, balancingConstraint};
    params.add(singleDeadMultiAllGoalsParams);

    // -- TEST DECK #2: MULTIPLE DEAD BROKERS.
    // Test: Single Goal.
    Map<ClusterProperty, Number> multipleDeadBrokers = new HashMap<>();
    multipleDeadBrokers.put(ClusterProperty.NUM_DEAD_BROKERS, 5);
    for (Map.Entry<Integer, String> entry : goalNameByPriority.entrySet()) {
      Object[] multiDeadSingleSoftParams = {multipleDeadBrokers, Collections.singletonMap(entry.getKey(),
                                                                                          entry.getValue()),
                                                                                          balancingConstraint};
      params.add(multiDeadSingleSoftParams);
    }
    // Test: All Goals.
    Object[] multiDeadMultiAllGoalsParams = {multipleDeadBrokers, goalNameByPriority, balancingConstraint};
    params.add(multiDeadMultiAllGoalsParams);

    return params;
  }

  private Map<ClusterProperty, Number> _modifiedProperties;
  private Map<Integer, String> _goalNameByPriority;
  private BalancingConstraint _balancingConstraint;

  /**
   * Constructor of Self Healing Test.
   *
   * @param modifiedProperties Modified cluster properties over the {@link TestConstants#BASE_PROPERTIES}.
   * @param goalNameByPriority Goal name by priority.
   */
  public RandomSelfHealingTest(Map<ClusterProperty, Number> modifiedProperties,
                               Map<Integer, String> goalNameByPriority,
                               BalancingConstraint balancingConstraint) {
    _modifiedProperties = modifiedProperties;
    _goalNameByPriority = goalNameByPriority;
    _balancingConstraint = balancingConstraint;
  }

  @Test
  public void test() throws Exception {
    // Create cluster properties by applying modified properties to base properties.
    Map<ClusterProperty, Number> clusterProperties = new HashMap<>(TestConstants.BASE_PROPERTIES);
    clusterProperties.putAll(_modifiedProperties);

    LOG.debug("Replica distribution: {}.", TestConstants.Distribution.UNIFORM);
    ClusterModel clusterModel = RandomCluster.generate(clusterProperties);
    RandomCluster.populate(clusterModel, clusterProperties, TestConstants.Distribution.UNIFORM, true);

    assertTrue("Self Healing Test failed to improve the existing state.",
               OptimizationVerifier.executeGoalsFor(_balancingConstraint, clusterModel, _goalNameByPriority));

  }
}
