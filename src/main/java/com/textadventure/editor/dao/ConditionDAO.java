package com.textadventure.editor.dao;

import com.textadventure.editor.db.DatabaseConnection;
import com.textadventure.editor.model.Condition;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConditionDAO {

    public List<Condition> findByNodeId(int nodeId) throws SQLException {
        String sql = "SELECT * FROM conditions WHERE node_id = ? ORDER BY id";
        List<Condition> conditions = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, nodeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    conditions.add(mapRow(rs));
                }
            }
        }
        return conditions;
    }

    public Condition findById(int id) throws SQLException {
        String sql = "SELECT * FROM conditions WHERE id = ?";
        
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

    public Condition save(Condition condition) throws SQLException {
        if (condition.getId() > 0) {
            return update(condition);
        } else {
            return insert(condition);
        }
    }

    private Condition insert(Condition condition) throws SQLException {
        String sql = "INSERT INTO conditions (node_id, condition_type, target_node_id, condition_key, " +
                "condition_value, operator, description) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, condition.getNodeId());
            stmt.setString(2, condition.getConditionType());
            
            if (condition.getTargetNodeId() != null) {
                stmt.setInt(3, condition.getTargetNodeId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            
            stmt.setString(4, condition.getConditionKey());
            stmt.setString(5, condition.getConditionValue());
            stmt.setString(6, condition.getOperator());
            stmt.setString(7, condition.getDescription());
            
            stmt.executeUpdate();
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    condition.setId(generatedKeys.getInt(1));
                }
            }
        }
        return condition;
    }

    private Condition update(Condition condition) throws SQLException {
        String sql = "UPDATE conditions SET condition_type = ?, target_node_id = ?, condition_key = ?, " +
                "condition_value = ?, operator = ?, description = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, condition.getConditionType());
            
            if (condition.getTargetNodeId() != null) {
                stmt.setInt(2, condition.getTargetNodeId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            
            stmt.setString(3, condition.getConditionKey());
            stmt.setString(4, condition.getConditionValue());
            stmt.setString(5, condition.getOperator());
            stmt.setString(6, condition.getDescription());
            stmt.setInt(7, condition.getId());
            
            stmt.executeUpdate();
        }
        return condition;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM conditions WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public void deleteByNodeId(int nodeId) throws SQLException {
        String sql = "DELETE FROM conditions WHERE node_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, nodeId);
            stmt.executeUpdate();
        }
    }

    private Condition mapRow(ResultSet rs) throws SQLException {
        Condition condition = new Condition();
        condition.setId(rs.getInt("id"));
        condition.setNodeId(rs.getInt("node_id"));
        condition.setConditionType(rs.getString("condition_type"));
        
        int targetNodeId = rs.getInt("target_node_id");
        if (!rs.wasNull()) {
            condition.setTargetNodeId(targetNodeId);
        }
        
        condition.setConditionKey(rs.getString("condition_key"));
        condition.setConditionValue(rs.getString("condition_value"));
        condition.setOperator(rs.getString("operator"));
        condition.setDescription(rs.getString("description"));
        
        return condition;
    }
}
