package com.textadventure.editor.model;

public class Condition {
    private int id;
    private int nodeId;
    private String conditionType;
    private Integer targetNodeId;
    private String conditionKey;
    private String conditionValue;
    private String operator;
    private String description;

    public Condition() {
        this.conditionType = "prerequisite";
        this.operator = "equals";
    }

    public Condition(int id, int nodeId, String conditionType, String description) {
        this();
        this.id = id;
        this.nodeId = nodeId;
        this.conditionType = conditionType;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public String getConditionType() {
        return conditionType;
    }

    public void setConditionType(String conditionType) {
        this.conditionType = conditionType;
    }

    public Integer getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(Integer targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public String getConditionKey() {
        return conditionKey;
    }

    public void setConditionKey(String conditionKey) {
        this.conditionKey = conditionKey;
    }

    public String getConditionValue() {
        return conditionValue;
    }

    public void setConditionValue(String conditionValue) {
        this.conditionValue = conditionValue;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        if (description != null && !description.isEmpty()) {
            return description;
        }
        return conditionType + ": " + (conditionKey != null ? conditionKey + " " + operator + " " + conditionValue : "");
    }
}
