package com.textadventure.editor.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StoryNode {
    private int id;
    private Integer parentId;
    private String title;
    private String content;
    private String nodeType;
    private int position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<StoryNode> children;
    private List<Condition> conditions;

    public StoryNode() {
        this.children = new ArrayList<>();
        this.conditions = new ArrayList<>();
        this.nodeType = "normal";
    }

    public StoryNode(int id, Integer parentId, String title, String content, String nodeType) {
        this();
        this.id = id;
        this.parentId = parentId;
        this.title = title;
        this.content = content;
        this.nodeType = nodeType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<StoryNode> getChildren() {
        return children;
    }

    public void setChildren(List<StoryNode> children) {
        this.children = children;
    }

    public void addChild(StoryNode child) {
        this.children.add(child);
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public void addCondition(Condition condition) {
        this.conditions.add(condition);
    }

    @Override
    public String toString() {
        return title;
    }
}
