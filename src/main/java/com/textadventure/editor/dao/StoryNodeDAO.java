package com.textadventure.editor.dao;

import com.textadventure.editor.db.DatabaseConnection;
import com.textadventure.editor.model.StoryNode;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoryNodeDAO {

    public List<StoryNode> findAll() throws SQLException {
        String sql = "SELECT * FROM story_nodes ORDER BY parent_id, position";
        List<StoryNode> nodes = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                nodes.add(mapRow(rs));
            }
        }
        return buildTree(nodes);
    }

    public StoryNode findById(int id) throws SQLException {
        String sql = "SELECT * FROM story_nodes WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public StoryNode save(StoryNode node) throws SQLException {
        if (node.getId() > 0) {
            return update(node);
        } else {
            return insert(node);
        }
    }

    private StoryNode insert(StoryNode node) throws SQLException {
        String sql = "INSERT INTO story_nodes (parent_id, title, content, node_type, position, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            if (node.getParentId() != null) {
                stmt.setInt(1, node.getParentId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setString(2, node.getTitle());
            stmt.setString(3, node.getContent());
            stmt.setString(4, node.getNodeType());
            stmt.setInt(5, node.getPosition());
            
            stmt.executeUpdate();
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    node.setId(generatedKeys.getInt(1));
                }
            }
        }
        return node;
    }

    private StoryNode update(StoryNode node) throws SQLException {
        String sql = "UPDATE story_nodes SET parent_id = ?, title = ?, content = ?, node_type = ?, position = ?, " +
                "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            if (node.getParentId() != null) {
                stmt.setInt(1, node.getParentId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setString(2, node.getTitle());
            stmt.setString(3, node.getContent());
            stmt.setString(4, node.getNodeType());
            stmt.setInt(5, node.getPosition());
            stmt.setInt(6, node.getId());
            
            stmt.executeUpdate();
        }
        return node;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM story_nodes WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private List<StoryNode> buildTree(List<StoryNode> flatNodes) {
        Map<Integer, StoryNode> nodeMap = new HashMap<>();
        List<StoryNode> rootNodes = new ArrayList<>();
        
        for (StoryNode node : flatNodes) {
            nodeMap.put(node.getId(), node);
        }
        
        for (StoryNode node : flatNodes) {
            if (node.getParentId() == null) {
                rootNodes.add(node);
            } else {
                StoryNode parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.addChild(node);
                }
            }
        }
        
        return rootNodes;
    }

    private StoryNode mapRow(ResultSet rs) throws SQLException {
        StoryNode node = new StoryNode();
        node.setId(rs.getInt("id"));
        
        int parentId = rs.getInt("parent_id");
        if (!rs.wasNull()) {
            node.setParentId(parentId);
        }
        
        node.setTitle(rs.getString("title"));
        node.setContent(rs.getString("content"));
        node.setNodeType(rs.getString("node_type"));
        node.setPosition(rs.getInt("position"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            node.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            node.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return node;
    }
}
