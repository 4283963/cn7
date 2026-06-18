package com.textadventure.editor.service;

import com.textadventure.editor.dao.ConditionDAO;
import com.textadventure.editor.dao.StoryNodeDAO;
import com.textadventure.editor.model.Condition;
import com.textadventure.editor.model.StoryNode;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoryNodeService {
    private final StoryNodeDAO nodeDAO;
    private final ConditionDAO conditionDAO;

    public StoryNodeService() {
        this.nodeDAO = new StoryNodeDAO();
        this.conditionDAO = new ConditionDAO();
    }

    public List<StoryNode> loadFullStoryTree() throws SQLException {
        List<StoryNode> rootNodes = nodeDAO.findAll();
        loadAllConditions(rootNodes);
        return rootNodes;
    }

    private void loadAllConditions(List<StoryNode> nodes) {
        for (StoryNode node : nodes) {
            try {
                List<Condition> conditions = conditionDAO.findByNodeId(node.getId());
                node.setConditions(conditions);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                loadAllConditions(node.getChildren());
            }
        }
    }

    public Map<Integer, StoryNode> getAllNodesMap(List<StoryNode> rootNodes) {
        Map<Integer, StoryNode> nodeMap = new HashMap<>();
        collectAllNodes(rootNodes, nodeMap);
        return nodeMap;
    }

    private void collectAllNodes(List<StoryNode> nodes, Map<Integer, StoryNode> nodeMap) {
        for (StoryNode node : nodes) {
            nodeMap.put(node.getId(), node);
            if (!node.getChildren().isEmpty()) {
                collectAllNodes(node.getChildren(), nodeMap);
            }
        }
    }

    public StoryNode saveNode(StoryNode node) throws SQLException {
        return nodeDAO.save(node);
    }

    public StoryNode createChildNode(StoryNode parent, String title) throws SQLException {
        StoryNode newNode = new StoryNode();
        newNode.setTitle(title);
        newNode.setNodeType("normal");
        if (parent != null) {
            newNode.setParentId(parent.getId());
            newNode.setPosition(parent.getChildren().size());
        }
        return nodeDAO.save(newNode);
    }

    public void deleteNode(StoryNode node) throws SQLException {
        conditionDAO.deleteByNodeId(node.getId());
        deleteChildrenRecursively(node);
        nodeDAO.delete(node.getId());
    }

    private void deleteChildrenRecursively(StoryNode node) throws SQLException {
        for (StoryNode child : node.getChildren()) {
            conditionDAO.deleteByNodeId(child.getId());
            deleteChildrenRecursively(child);
            nodeDAO.delete(child.getId());
        }
    }

    public StoryNode getNodeById(int id) throws SQLException {
        StoryNode node = nodeDAO.findById(id);
        if (node != null) {
            node.setConditions(conditionDAO.findByNodeId(id));
        }
        return node;
    }

    public Condition saveCondition(Condition condition) throws SQLException {
        return conditionDAO.save(condition);
    }

    public void deleteCondition(Condition condition) throws SQLException {
        conditionDAO.delete(condition.getId());
    }

    public Condition createCondition(int nodeId) throws SQLException {
        Condition condition = new Condition();
        condition.setNodeId(nodeId);
        condition.setDescription("新条件");
        return conditionDAO.save(condition);
    }

    public void updateNodeParent(StoryNode node, StoryNode newParent) throws SQLException {
        node.setParentId(newParent != null ? newParent.getId() : null);
        nodeDAO.save(node);
    }
}
