package com.textadventure.editor.service;

import com.textadventure.editor.model.Condition;
import com.textadventure.editor.model.StoryNode;

import java.util.*;

public class StoryAnalyzer {

    public static class AnalysisResult {
        private final Set<Integer> deadEndNodeIds;
        private final Map<Integer, String> deadEndReasons;

        public AnalysisResult() {
            this.deadEndNodeIds = new HashSet<>();
            this.deadEndReasons = new HashMap<>();
        }

        public void addDeadEnd(int nodeId, String reason) {
            deadEndNodeIds.add(nodeId);
            deadEndReasons.put(nodeId, reason);
        }

        public Set<Integer> getDeadEndNodeIds() {
            return deadEndNodeIds;
        }

        public String getReason(int nodeId) {
            return deadEndReasons.get(nodeId);
        }

        public int getDeadEndCount() {
            return deadEndNodeIds.size();
        }

        public boolean isDeadEnd(int nodeId) {
            return deadEndNodeIds.contains(nodeId);
        }
    }

    public AnalysisResult analyze(List<StoryNode> rootNodes, Map<Integer, StoryNode> allNodes) {
        AnalysisResult result = new AnalysisResult();

        if (allNodes.isEmpty()) {
            return result;
        }

        Set<Integer> visited = new HashSet<>();
        Set<Integer> recursionStack = new HashSet<>();

        for (StoryNode node : allNodes.values()) {
            checkDeadEnd(node, allNodes, result, visited, recursionStack);
        }

        return result;
    }

    private boolean checkDeadEnd(StoryNode node, Map<Integer, StoryNode> allNodes,
                                 AnalysisResult result, Set<Integer> visited, Set<Integer> recursionStack) {
        int nodeId = node.getId();

        if (result.isDeadEnd(nodeId)) {
            return true;
        }

        if (visited.contains(nodeId)) {
            return false;
        }

        if (recursionStack.contains(nodeId)) {
            result.addDeadEnd(nodeId, "检测到循环依赖：此节点的前置条件形成了无限循环");
            return true;
        }

        recursionStack.add(nodeId);

        boolean isDead = false;
        String deadReason = null;

        if (node.getConditions() != null && !node.getConditions().isEmpty()) {
            for (Condition condition : node.getConditions()) {
                String conditionResult = validateCondition(condition, allNodes);
                if (conditionResult != null) {
                    isDead = true;
                    deadReason = conditionResult;
                    break;
                }

                if ("prerequisite".equals(condition.getConditionType()) && condition.getTargetNodeId() != null) {
                    StoryNode targetNode = allNodes.get(condition.getTargetNodeId());
                    if (targetNode != null) {
                        if (checkDeadEnd(targetNode, allNodes, result, visited, recursionStack)) {
                            isDead = true;
                            deadReason = "前置节点 \"" + targetNode.getTitle() + "\" 本身是死路节点";
                            break;
                        }
                    }
                }
            }

            if (!isDead) {
                String conflictResult = checkVariableConflicts(node.getConditions());
                if (conflictResult != null) {
                    isDead = true;
                    deadReason = conflictResult;
                }
            }

            if (!isDead) {
                String branchConflict = checkBranchConflicts(node, allNodes);
                if (branchConflict != null) {
                    isDead = true;
                    deadReason = branchConflict;
                }
            }
        }

        if (!isDead && node.getParentId() != null) {
            StoryNode parent = allNodes.get(node.getParentId());
            if (parent != null && checkDeadEnd(parent, allNodes, result, visited, recursionStack)) {
                isDead = true;
                deadReason = "父节点 \"" + parent.getTitle() + "\" 本身是死路节点";
            }
        }

        recursionStack.remove(nodeId);
        visited.add(nodeId);

        if (isDead && !result.isDeadEnd(nodeId)) {
            result.addDeadEnd(nodeId, deadReason);
        }

        return isDead;
    }

    private String validateCondition(Condition condition, Map<Integer, StoryNode> allNodes) {
        if ("prerequisite".equals(condition.getConditionType())) {
            if (condition.getTargetNodeId() == null) {
                return "前置条件缺少目标节点";
            }
            if (!allNodes.containsKey(condition.getTargetNodeId())) {
                return "前置条件引用的节点ID " + condition.getTargetNodeId() + " 不存在";
            }
        }

        if ("variable".equals(condition.getConditionType())) {
            if (condition.getConditionKey() == null || condition.getConditionKey().trim().isEmpty()) {
                return "变量条件缺少变量名";
            }
            if (condition.getOperator() == null || condition.getOperator().trim().isEmpty()) {
                return "变量条件缺少运算符";
            }
            if (condition.getConditionValue() == null || condition.getConditionValue().trim().isEmpty()) {
                return "变量条件缺少值";
            }

            if (isNumericOperator(condition.getOperator())) {
                try {
                    Double.parseDouble(condition.getConditionValue());
                } catch (NumberFormatException e) {
                    return "数值比较运算符需要数值，但值是 \"" + condition.getConditionValue() + "\"";
                }
            }
        }

        if ("flag".equals(condition.getConditionType())) {
            if (condition.getConditionKey() == null || condition.getConditionKey().trim().isEmpty()) {
                return "标记条件缺少标记名";
            }
        }

        return null;
    }

    private boolean isNumericOperator(String operator) {
        return "greater_than".equals(operator)
                || "less_than".equals(operator)
                || "greater_or_equal".equals(operator)
                || "less_or_equal".equals(operator);
    }

    private String checkVariableConflicts(List<Condition> conditions) {
        Map<String, List<Condition>> variableConditions = new HashMap<>();

        for (Condition condition : conditions) {
            if ("variable".equals(condition.getConditionType()) && condition.getConditionKey() != null) {
                variableConditions.computeIfAbsent(condition.getConditionKey(), k -> new ArrayList<>())
                        .add(condition);
            }
        }

        for (Map.Entry<String, List<Condition>> entry : variableConditions.entrySet()) {
            String varName = entry.getKey();
            List<Condition> varConds = entry.getValue();

            if (varConds.size() >= 2) {
                String conflict = findNumericConflict(varName, varConds);
                if (conflict != null) {
                    return conflict;
                }

                conflict = findEqualsConflict(varName, varConds);
                if (conflict != null) {
                    return conflict;
                }
            }
        }

        return null;
    }

    private String findNumericConflict(String varName, List<Condition> conditions) {
        Double maxLowerBound = null;
        Double minUpperBound = null;
        boolean lowerInclusive = false;
        boolean upperInclusive = false;

        for (Condition cond : conditions) {
            if (!isNumericOperator(cond.getOperator())) continue;

            try {
                double value = Double.parseDouble(cond.getConditionValue());

                switch (cond.getOperator()) {
                    case "greater_than":
                        if (maxLowerBound == null || value > maxLowerBound) {
                            maxLowerBound = value;
                            lowerInclusive = false;
                        }
                        break;
                    case "greater_or_equal":
                        if (maxLowerBound == null || value > maxLowerBound || (value == maxLowerBound && !lowerInclusive)) {
                            maxLowerBound = value;
                            lowerInclusive = true;
                        }
                        break;
                    case "less_than":
                        if (minUpperBound == null || value < minUpperBound) {
                            minUpperBound = value;
                            upperInclusive = false;
                        }
                        break;
                    case "less_or_equal":
                        if (minUpperBound == null || value < minUpperBound || (value == minUpperBound && !upperInclusive)) {
                            minUpperBound = value;
                            upperInclusive = true;
                        }
                        break;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (maxLowerBound != null && minUpperBound != null) {
            if (lowerInclusive && upperInclusive) {
                if (maxLowerBound > minUpperBound) {
                    return "变量条件矛盾：\"" + varName + "\" 需要同时 >= " + maxLowerBound + " 和 <= " + minUpperBound;
                }
            } else {
                if (maxLowerBound >= minUpperBound) {
                    return "变量条件矛盾：\"" + varName + "\" 需要同时 " + (lowerInclusive ? ">=" : ">") + " " + maxLowerBound
                            + " 和 " + (upperInclusive ? "<=" : "<") + " " + minUpperBound;
                }
            }
        }

        return null;
    }

    private String findEqualsConflict(String varName, List<Condition> conditions) {
        Set<String> equalsValues = new HashSet<>();
        Set<String> notEqualsValues = new HashSet<>();

        for (Condition cond : conditions) {
            if ("equals".equals(cond.getOperator())) {
                equalsValues.add(cond.getConditionValue());
            } else if ("not_equals".equals(cond.getOperator())) {
                notEqualsValues.add(cond.getConditionValue());
            }
        }

        if (equalsValues.size() > 1) {
            return "变量条件矛盾：\"" + varName + "\" 需要同时等于多个不同的值: " + equalsValues;
        }

        for (String eqValue : equalsValues) {
            if (notEqualsValues.contains(eqValue)) {
                return "变量条件矛盾：\"" + varName + "\" 需要同时等于和不等于 \"" + eqValue + "\"";
            }
        }

        return null;
    }

    private String checkBranchConflicts(StoryNode node, Map<Integer, StoryNode> allNodes) {
        if (node.getConditions() == null || node.getConditions().size() < 2) {
            return null;
        }

        List<Integer> prerequisiteNodeIds = new ArrayList<>();
        for (Condition condition : node.getConditions()) {
            if ("prerequisite".equals(condition.getConditionType()) && condition.getTargetNodeId() != null) {
                prerequisiteNodeIds.add(condition.getTargetNodeId());
            }
        }

        if (prerequisiteNodeIds.size() >= 2) {
            for (int i = 0; i < prerequisiteNodeIds.size(); i++) {
                for (int j = i + 1; j < prerequisiteNodeIds.size(); j++) {
                    StoryNode nodeA = allNodes.get(prerequisiteNodeIds.get(i));
                    StoryNode nodeB = allNodes.get(prerequisiteNodeIds.get(j));
                    if (nodeA != null && nodeB != null && areInDifferentBranches(nodeA, nodeB, allNodes)) {
                        return "前置条件冲突：需要同时访问 \"" + nodeA.getTitle() + "\" 和 \""
                                + nodeB.getTitle() + "\"，但它们位于不同的分支，无法同时到达";
                    }
                }
            }
        }

        return null;
    }

    private boolean areInDifferentBranches(StoryNode nodeA, StoryNode nodeB, Map<Integer, StoryNode> allNodes) {
        Set<Integer> ancestorsOfA = getAllAncestors(nodeA, allNodes);
        Set<Integer> ancestorsOfB = getAllAncestors(nodeB, allNodes);

        ancestorsOfA.add(nodeA.getId());
        ancestorsOfB.add(nodeB.getId());

        if (ancestorsOfA.contains(nodeB.getId()) || ancestorsOfB.contains(nodeA.getId())) {
            return false;
        }

        StoryNode parentA = nodeA.getParentId() != null ? allNodes.get(nodeA.getParentId()) : null;
        StoryNode parentB = nodeB.getParentId() != null ? allNodes.get(nodeB.getParentId()) : null;

        while (parentA != null && parentB != null) {
            if (parentA.getId() == parentB.getId()) {
                return areSiblingsInExclusiveBranch(nodeA, nodeB, parentA, allNodes);
            }

            Set<Integer> ancestorsB = getAllAncestors(parentB, allNodes);
            if (ancestorsB.contains(parentA.getId())) {
                return areSiblingsInExclusiveBranch(nodeA, getSiblingAncestor(nodeB, parentA.getId(), allNodes),
                        parentA, allNodes);
            }

            Set<Integer> ancestorsA = getAllAncestors(parentA, allNodes);
            if (ancestorsA.contains(parentB.getId())) {
                return areSiblingsInExclusiveBranch(getSiblingAncestor(nodeA, parentB.getId(), allNodes), nodeB,
                        parentB, allNodes);
            }

            parentA = parentA.getParentId() != null ? allNodes.get(parentA.getParentId()) : null;
            parentB = parentB.getParentId() != null ? allNodes.get(parentB.getParentId()) : null;
        }

        return false;
    }

    private StoryNode getSiblingAncestor(StoryNode node, int ancestorId, Map<Integer, StoryNode> allNodes) {
        StoryNode current = node;
        while (current != null && current.getParentId() != null && current.getParentId() != ancestorId) {
            current = allNodes.get(current.getParentId());
        }
        return current;
    }

    private boolean areSiblingsInExclusiveBranch(StoryNode nodeA, StoryNode nodeB,
                                                  StoryNode commonParent, Map<Integer, StoryNode> allNodes) {
        if (nodeA == null || nodeB == null || commonParent == null) return false;
        if (nodeA.getId() == nodeB.getId()) return false;

        return (commonParent.getChildren().contains(nodeA) || isDescendant(nodeA, commonParent, allNodes))
                && (commonParent.getChildren().contains(nodeB) || isDescendant(nodeB, commonParent, allNodes));
    }

    private boolean isDescendant(StoryNode node, StoryNode ancestor, Map<Integer, StoryNode> allNodes) {
        StoryNode current = node;
        while (current != null && current.getParentId() != null) {
            if (current.getParentId() == ancestor.getId()) {
                return true;
            }
            current = allNodes.get(current.getParentId());
        }
        return false;
    }

    private Set<Integer> getAllAncestors(StoryNode node, Map<Integer, StoryNode> allNodes) {
        Set<Integer> ancestors = new HashSet<>();
        Integer parentId = node.getParentId();
        while (parentId != null) {
            ancestors.add(parentId);
            StoryNode parent = allNodes.get(parentId);
            if (parent == null) break;
            parentId = parent.getParentId();
        }
        return ancestors;
    }
}
