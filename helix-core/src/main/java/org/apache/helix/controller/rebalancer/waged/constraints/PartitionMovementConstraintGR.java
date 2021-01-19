//package org.apache.helix.controller.rebalancer.waged.constraints;
//
//import java.util.Collections;
//import java.util.Map;
//
//import org.apache.helix.controller.rebalancer.waged.model.AssignableNode;
//import org.apache.helix.controller.rebalancer.waged.model.AssignableReplica;
//import org.apache.helix.controller.rebalancer.waged.model.ClusterContext;
//import org.apache.helix.model.Partition;
//import org.apache.helix.model.ResourceAssignment;
//
//
//public class PartitionMovementConstraintGR extends SoftConstraint {
//  private static final double MAX_SCORE = 1f;
//  private static final double MIN_SCORE = 0f;
//  // The scale factor to adjust score when the proposed allocation partially matches the assignment
//  // plan but will require a state transition (with partition movement).
//  // TODO: these factors will be tuned based on user's preference
//  private static final double STATE_TRANSITION_COST_FACTOR = 0.5;
//  private static final double MOVEMENT_COST_FACTOR = 0.25;
//
//  PartitionMovementConstraintGR() {
//    super(MAX_SCORE, MIN_SCORE);
//  }
//
//  @Override
//  protected double getAssignmentScore(AssignableNode node, AssignableReplica replica,
//      ClusterContext clusterContext) {
//    Map<String, String> previousAssignment =
//        getStateMap(replica, clusterContext.getPreviousAssignmentForGR());
//    String nodeName = node.getInstanceName();
//    String state = replica.getReplicaState();
//    double score = calculateAssignmentScore(nodeName, state, previousAssignment);
////    if (score > 0) {
////      System.out.println("PartitionMovementConstraintGR " + score);
////    }
//    return score;
//  }
//
//  private Map<String, String> getStateMap(AssignableReplica replica,
//      Map<String, ResourceAssignment> assignment) {
//    String resourceName = replica.getResourceName();
//    String partitionName = replica.getPartitionName();
//    if (assignment == null || !assignment.containsKey(resourceName)) {
//      return Collections.emptyMap();
//    }
//    return assignment.get(resourceName).getReplicaMap(new Partition(partitionName));
//  }
//
//  private double calculateAssignmentScore(String nodeName, String state,
//      Map<String, String> instanceToStateMap) {
//    if (instanceToStateMap.containsKey(nodeName)) {
//      return state.equals(instanceToStateMap.get(nodeName)) ?
//          1 : // if state matches, no state transition required for the proposed assignment
//          STATE_TRANSITION_COST_FACTOR; // if state does not match,
//      // then the proposed assignment requires state transition.
//    }
//    return 0;
//  }
//
//  @Override
//  protected NormalizeFunction getNormalizeFunction() {
//    // PartitionMovementConstraint already scale the score properly.
//    return (score) -> score;
//  }
//}
