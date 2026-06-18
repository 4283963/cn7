package com.textadventure.editor.ui;

import com.textadventure.editor.model.Condition;
import com.textadventure.editor.model.StoryNode;
import com.textadventure.editor.service.StoryNodeService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class MainController {

    @FXML private TreeView<StoryNode> storyTree;
    @FXML private TextField searchField;
    @FXML private TextField titleField;
    @FXML private ComboBox<String> nodeTypeCombo;
    @FXML private ComboBox<StoryNode> parentNodeCombo;
    @FXML private Label idLabel;
    @FXML private TextArea contentArea;
    @FXML private ListView<Condition> conditionsList;
    @FXML private ComboBox<String> conditionTypeCombo;
    @FXML private TextField conditionDescField;
    @FXML private ComboBox<StoryNode> targetNodeCombo;
    @FXML private TextField conditionKeyField;
    @FXML private ComboBox<String> operatorCombo;
    @FXML private TextField conditionValueField;
    @FXML private Button addChildBtn;
    @FXML private Button deleteBtn;
    @FXML private Button saveBtn;
    @FXML private Button addConditionBtn;
    @FXML private Button deleteConditionBtn;
    @FXML private Label statusLabel;
    @FXML private VBox detailPanel;

    private StoryNodeService nodeService;
    private Stage primaryStage;
    private StoryNode currentNode;
    private Condition currentCondition;
    private Map<Integer, StoryNode> allNodesMap;
    private TreeItem<StoryNode> rootItem;

    private List<StoryNode> fullTreeCache;

    @FXML
    public void initialize() {
        nodeService = new StoryNodeService();
        allNodesMap = new HashMap<>();
        rootItem = new TreeItem<>();
        rootItem.setValue(new StoryNode());
        rootItem.getValue().setTitle("剧情根目录");
        rootItem.setExpanded(true);

        storyTree.setRoot(rootItem);
        storyTree.setShowRoot(true);

        storyTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null && newVal.getValue().getId() > 0) {
                onNodeSelected(newVal.getValue());
            } else {
                clearDetailPanel();
            }
        });

        conditionsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                onConditionSelected(newVal);
            }
        });

        setupCustomTreeCell();

        refreshTree();
    }

    private void setupCustomTreeCell() {
        storyTree.setCellFactory(new Callback<TreeView<StoryNode>, TreeCell<StoryNode>>() {
            @Override
            public TreeCell<StoryNode> call(TreeView<StoryNode> param) {
                return new TreeCell<StoryNode>() {
                    @Override
                    protected void updateItem(StoryNode item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            getStyleClass().removeAll("start", "branch", "ending", "normal");
                        } else {
                            setText(item.toString());
                            getStyleClass().removeAll("start", "branch", "ending", "normal");
                            if (item.getNodeType() != null) {
                                getStyleClass().add(item.getNodeType());
                            }
                        }
                    }
                };
            }
        });
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    private void onNodeSelected(StoryNode node) {
        this.currentNode = node;
        titleField.setText(node.getTitle());
        nodeTypeCombo.setValue(node.getNodeType());
        idLabel.setText(String.valueOf(node.getId()));
        contentArea.setText(node.getContent());

        ObservableList<Condition> conditions = FXCollections.observableArrayList(node.getConditions());
        conditionsList.setItems(conditions);

        updateTargetNodeCombo();
        updateParentNodeCombo();

        addChildBtn.setDisable(false);
        deleteBtn.setDisable(false);
        saveBtn.setDisable(false);
        addConditionBtn.setDisable(false);
        deleteConditionBtn.setDisable(true);

        clearConditionEditor();
        setStatus("已选中节点: " + node.getTitle());
    }

    private void onConditionSelected(Condition condition) {
        this.currentCondition = condition;
        conditionTypeCombo.setValue(condition.getConditionType());
        conditionDescField.setText(condition.getDescription());
        conditionKeyField.setText(condition.getConditionKey());
        operatorCombo.setValue(condition.getOperator());
        conditionValueField.setText(condition.getConditionValue());

        if (condition.getTargetNodeId() != null && allNodesMap.containsKey(condition.getTargetNodeId())) {
            targetNodeCombo.setValue(allNodesMap.get(condition.getTargetNodeId()));
        } else {
            targetNodeCombo.setValue(null);
        }

        deleteConditionBtn.setDisable(false);
    }

    private void clearDetailPanel() {
        this.currentNode = null;
        titleField.clear();
        nodeTypeCombo.setValue("normal");
        idLabel.setText("-");
        contentArea.clear();
        conditionsList.setItems(FXCollections.emptyObservableList());
        addChildBtn.setDisable(true);
        deleteBtn.setDisable(true);
        saveBtn.setDisable(true);
        addConditionBtn.setDisable(true);
        deleteConditionBtn.setDisable(true);
        clearConditionEditor();
    }

    private void clearConditionEditor() {
        this.currentCondition = null;
        conditionTypeCombo.setValue("prerequisite");
        conditionDescField.clear();
        conditionKeyField.clear();
        operatorCombo.setValue("equals");
        conditionValueField.clear();
        targetNodeCombo.setValue(null);
        deleteConditionBtn.setDisable(true);
    }

    private void updateTargetNodeCombo() {
        ObservableList<StoryNode> items = FXCollections.observableArrayList();
        items.add(null);
        if (currentNode != null) {
            List<StoryNode> otherNodes = allNodesMap.values().stream()
                    .filter(n -> n.getId() != currentNode.getId())
                    .sorted(Comparator.comparing(StoryNode::getTitle))
                    .collect(Collectors.toList());
            items.addAll(otherNodes);
        } else {
            items.addAll(allNodesMap.values().stream()
                    .sorted(Comparator.comparing(StoryNode::getTitle))
                    .collect(Collectors.toList()));
        }
        targetNodeCombo.setItems(items);
        targetNodeCombo.setButtonCell(new ListCell<StoryNode>() {
            @Override
            protected void updateItem(StoryNode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "无" : item.getTitle());
            }
        });
        targetNodeCombo.setCellFactory(param -> new ListCell<StoryNode>() {
            @Override
            protected void updateItem(StoryNode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "无" : item.getTitle());
            }
        });
    }

    private void updateParentNodeCombo() {
        ObservableList<StoryNode> items = FXCollections.observableArrayList();
        items.add(null);
        if (currentNode != null) {
            Set<Integer> excludedIds = new HashSet<>();
            collectAllChildIds(currentNode, excludedIds);
            excludedIds.add(currentNode.getId());

            List<StoryNode> validParents = allNodesMap.values().stream()
                    .filter(n -> !excludedIds.contains(n.getId()))
                    .sorted(Comparator.comparing(StoryNode::getTitle))
                    .collect(Collectors.toList());
            items.addAll(validParents);
        } else {
            items.addAll(allNodesMap.values().stream()
                    .sorted(Comparator.comparing(StoryNode::getTitle))
                    .collect(Collectors.toList()));
        }
        parentNodeCombo.setItems(items);
        parentNodeCombo.setButtonCell(new ListCell<StoryNode>() {
            @Override
            protected void updateItem(StoryNode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "无（根节点）" : item.getTitle());
            }
        });
        parentNodeCombo.setCellFactory(param -> new ListCell<StoryNode>() {
            @Override
            protected void updateItem(StoryNode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "无（根节点）" : item.getTitle());
            }
        });

        if (currentNode != null && currentNode.getParentId() != null) {
            parentNodeCombo.setValue(allNodesMap.get(currentNode.getParentId()));
        } else {
            parentNodeCombo.setValue(null);
        }
    }

    private void collectAllChildIds(StoryNode node, Set<Integer> ids) {
        if (node == null || node.getChildren() == null) return;
        for (StoryNode child : node.getChildren()) {
            ids.add(child.getId());
            collectAllChildIds(child, ids);
        }
    }

    @FXML
    public void refreshTree() {
        try {
            fullTreeCache = nodeService.loadFullStoryTree();
            allNodesMap = nodeService.getAllNodesMap(fullTreeCache);

            rootItem.getChildren().clear();
            buildTreeItems(fullTreeCache, rootItem);

            updateTargetNodeCombo();
            updateParentNodeCombo();
            setStatus("已刷新剧情树，共 " + allNodesMap.size() + " 个节点");
        } catch (SQLException e) {
            showError("刷新失败", e.getMessage());
            e.printStackTrace();
        }
    }

    private void buildTreeItems(List<StoryNode> nodes, TreeItem<StoryNode> parent) {
        for (StoryNode node : nodes) {
            TreeItem<StoryNode> treeItem = new TreeItem<>(node);
            treeItem.setExpanded(true);
            parent.getChildren().add(treeItem);
            if (!node.getChildren().isEmpty()) {
                buildTreeItems(node.getChildren(), treeItem);
            }
        }
    }

    @FXML
    public void createRootNode() {
        TextInputDialog dialog = new TextInputDialog("新剧情");
        dialog.setTitle("新建根节点");
        dialog.setHeaderText("输入根节点标题");
        dialog.setContentText("标题:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(title -> {
            try {
                StoryNode newNode = nodeService.createChildNode(null, title);
                refreshTree();
                selectNodeById(newNode.getId());
                setStatus("已创建根节点: " + title);
            } catch (SQLException e) {
                showError("创建失败", e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    public void addChildNode() {
        if (currentNode == null) return;

        TextInputDialog dialog = new TextInputDialog("新节点");
        dialog.setTitle("添加子节点");
        dialog.setHeaderText("为 \"" + currentNode.getTitle() + "\" 添加子节点");
        dialog.setContentText("子节点标题:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(title -> {
            try {
                StoryNode newNode = nodeService.createChildNode(currentNode, title);
                refreshTree();
                selectNodeById(newNode.getId());
                setStatus("已创建子节点: " + title);
            } catch (SQLException e) {
                showError("创建失败", e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    public void deleteNode() {
        if (currentNode == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("删除节点 \"" + currentNode.getTitle() + "\"");
        alert.setContentText("警告：删除此节点将同时删除所有子节点和相关条件，此操作不可撤销！");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            try {
                nodeService.deleteNode(currentNode);
                refreshTree();
                setStatus("已删除节点: " + currentNode.getTitle());
            } catch (SQLException e) {
                showError("删除失败", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void saveCurrentNode() {
        if (currentNode == null) return;

        currentNode.setTitle(titleField.getText().trim());
        currentNode.setNodeType(nodeTypeCombo.getValue());
        currentNode.setContent(contentArea.getText());

        StoryNode selectedParent = parentNodeCombo.getValue();
        Integer newParentId = selectedParent != null ? selectedParent.getId() : null;
        boolean parentChanged = (currentNode.getParentId() == null && newParentId != null)
                || (currentNode.getParentId() != null && !currentNode.getParentId().equals(newParentId))
                || (currentNode.getParentId() != null && newParentId != null && !currentNode.getParentId().equals(newParentId));
        currentNode.setParentId(newParentId);

        if (currentNode.getTitle().isEmpty()) {
            showError("保存失败", "节点标题不能为空");
            return;
        }

        try {
            nodeService.saveNode(currentNode);
            refreshTree();
            selectNodeById(currentNode.getId());
            if (parentChanged) {
                setStatus("已保存节点并更新父节点: " + currentNode.getTitle());
            } else {
                setStatus("已保存节点: " + currentNode.getTitle());
            }
        } catch (SQLException e) {
            showError("保存失败", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void addCondition() {
        if (currentNode == null) return;

        try {
            Condition newCondition = nodeService.createCondition(currentNode.getId());
            currentNode.getConditions().add(newCondition);
            conditionsList.getItems().add(newCondition);
            conditionsList.getSelectionModel().select(newCondition);
            setStatus("已添加新条件");
        } catch (SQLException e) {
            showError("添加失败", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void deleteCondition() {
        if (currentCondition == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("删除条件");
        alert.setContentText("确定要删除此条件吗？");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            try {
                nodeService.deleteCondition(currentCondition);
                currentNode.getConditions().remove(currentCondition);
                conditionsList.getItems().remove(currentCondition);
                clearConditionEditor();
                setStatus("已删除条件");
            } catch (SQLException e) {
                showError("删除失败", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void saveCondition() {
        if (currentCondition == null || currentNode == null) return;

        currentCondition.setConditionType(conditionTypeCombo.getValue());
        currentCondition.setDescription(conditionDescField.getText().trim());
        currentCondition.setConditionKey(conditionKeyField.getText().trim());
        currentCondition.setOperator(operatorCombo.getValue());
        currentCondition.setConditionValue(conditionValueField.getText().trim());

        StoryNode target = targetNodeCombo.getValue();
        currentCondition.setTargetNodeId(target != null ? target.getId() : null);

        try {
            nodeService.saveCondition(currentCondition);
            int savedNodeId = currentNode.getId();
            int savedConditionId = currentCondition.getId();
            refreshTree();
            selectNodeById(savedNodeId);
            if (currentNode != null) {
                for (Condition c : currentNode.getConditions()) {
                    if (c.getId() == savedConditionId) {
                        conditionsList.getSelectionModel().select(c);
                        break;
                    }
                }
            }
            setStatus("已保存条件");
        } catch (SQLException e) {
            showError("保存失败", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void onSearch(KeyEvent event) {
        String keyword = searchField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            refreshTree();
            return;
        }

        try {
            List<StoryNode> filtered = allNodesMap.values().stream()
                    .filter(n -> n.getTitle().toLowerCase().contains(keyword) ||
                            (n.getContent() != null && n.getContent().toLowerCase().contains(keyword)))
                    .collect(Collectors.toList());

            rootItem.getChildren().clear();
            for (StoryNode node : filtered) {
                TreeItem<StoryNode> treeItem = new TreeItem<>(node);
                treeItem.setExpanded(true);
                rootItem.getChildren().add(treeItem);
            }

            setStatus("搜索到 " + filtered.size() + " 个匹配节点");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void importSampleData() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("导入示例数据");
        alert.setHeaderText("确认导入示例数据");
        alert.setContentText("这将清空现有数据并导入示例剧情，确定要继续吗？");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() != ButtonType.OK) return;

        try {
            for (StoryNode node : allNodesMap.values()) {
                nodeService.deleteNode(node);
            }

            StoryNode prologue = new StoryNode();
            prologue.setTitle("序章：神秘的来信");
            prologue.setNodeType("start");
            prologue.setContent("阳光透过窗帘洒进房间，你醒来发现床头放着一封神秘的来信。信封上没有署名，只有一个奇怪的符文印记。\n\n\"亲爱的冒险者：\n当你读到这封信时，命运的齿轮已经开始转动。在城市的边缘，有一座被遗忘的古老庄园，传说那里藏着改变一切的秘密...\"\n\n你决定：");
            nodeService.saveNode(prologue);

            Condition prologueCond = new Condition();
            prologueCond.setNodeId(prologue.getId());
            prologueCond.setConditionType("flag");
            prologueCond.setConditionKey("game_started");
            prologueCond.setConditionValue("true");
            prologueCond.setDescription("游戏已开始");
            nodeService.saveCondition(prologueCond);
            prologue.getConditions().add(prologueCond);

            StoryNode accept = new StoryNode();
            accept.setParentId(prologue.getId());
            accept.setTitle("分支A：接受邀请");
            accept.setNodeType("branch");
            accept.setContent("你决定接受这个神秘的邀请。按照信中的指示，你来到了城市边缘的古老庄园。\n\n庄园的大门缓缓打开，一个穿着黑色斗篷的人出现在门口...\n\n\"欢迎，勇敢的冒险者。我等你很久了。\"");
            nodeService.saveNode(accept);

            Condition acceptCond = new Condition();
            acceptCond.setNodeId(accept.getId());
            acceptCond.setConditionType("prerequisite");
            acceptCond.setTargetNodeId(prologue.getId());
            acceptCond.setDescription("必须先阅读序章");
            nodeService.saveCondition(acceptCond);
            accept.getConditions().add(acceptCond);

            StoryNode refuse = new StoryNode();
            refuse.setParentId(prologue.getId());
            refuse.setTitle("分支B：拒绝邀请");
            refuse.setNodeType("branch");
            refuse.setContent("你觉得这封信太可疑了，决定不予理会。你把信扔进了垃圾桶，继续过着平凡的生活。\n\n然而，奇怪的事情开始接连发生...\n\n你开始不断做同一个噩梦，梦里总有一个声音在呼唤你...");
            nodeService.saveNode(refuse);

            Condition refuseCond = new Condition();
            refuseCond.setNodeId(refuse.getId());
            refuseCond.setConditionType("prerequisite");
            refuseCond.setTargetNodeId(prologue.getId());
            refuseCond.setDescription("必须先阅读序章");
            nodeService.saveCondition(refuseCond);
            refuse.getConditions().add(refuseCond);

            StoryNode ending1 = new StoryNode();
            ending1.setParentId(accept.getId());
            ending1.setTitle("结局1：真相大白");
            ending1.setNodeType("ending");
            ending1.setContent("在黑衣人的引导下，你逐渐揭开了庄园的秘密。原来你是古老魔法家族的后裔，而这座庄园是你祖先留下的遗产。\n\n黑衣人是你家族的守护者，他等待这一天已经等了三百年。\n\n你接受了自己的命运，成为了新一代的守护者，守护着这个世界与魔法世界之间的平衡。\n\n【好结局 - 守护者之路】");
            nodeService.saveNode(ending1);

            Condition ending1Cond = new Condition();
            ending1Cond.setNodeId(ending1.getId());
            ending1Cond.setConditionType("variable");
            ending1Cond.setConditionKey("courage");
            ending1Cond.setOperator("greater_or_equal");
            ending1Cond.setConditionValue("50");
            ending1Cond.setDescription("勇气值 >= 50");
            nodeService.saveCondition(ending1Cond);
            ending1.getConditions().add(ending1Cond);

            StoryNode ending2 = new StoryNode();
            ending2.setParentId(accept.getId());
            ending2.setTitle("结局2：迷失黑暗");
            ending2.setNodeType("ending");
            ending2.setContent("你进入了庄园深处，却被黑暗力量所迷惑。原来黑衣人并不是什么守护者，而是被封印的恶魔。\n\n你成为了他的容器，黑暗重新降临人间...\n\n【坏结局 - 堕落之路】");
            nodeService.saveNode(ending2);

            Condition ending2Cond = new Condition();
            ending2Cond.setNodeId(ending2.getId());
            ending2Cond.setConditionType("variable");
            ending2Cond.setConditionKey("wisdom");
            ending2Cond.setOperator("less_than");
            ending2Cond.setConditionValue("30");
            ending2Cond.setDescription("智慧值 < 30");
            nodeService.saveCondition(ending2Cond);
            ending2.getConditions().add(ending2Cond);

            StoryNode ending3 = new StoryNode();
            ending3.setParentId(refuse.getId());
            ending3.setTitle("结局3：平凡人生");
            ending3.setNodeType("ending");
            ending3.setContent("你最终选择了逃避，继续过着平凡的生活。那些奇怪的噩梦渐渐消失了，就像从未发生过一样。\n\n多年以后，当你老去，偶尔还会想起那封神秘的来信。你常常会想，如果当时做出了不同的选择，人生会不会不一样？\n\n但这就是你的选择，平凡而安稳。\n\n【普通结局 - 平凡之路】");
            nodeService.saveNode(ending3);

            Condition ending3Cond = new Condition();
            ending3Cond.setNodeId(ending3.getId());
            ending3Cond.setConditionType("prerequisite");
            ending3Cond.setTargetNodeId(refuse.getId());
            ending3Cond.setDescription("必须选择拒绝邀请");
            nodeService.saveCondition(ending3Cond);
            ending3.getConditions().add(ending3Cond);

            refreshTree();
            expandAll(rootItem);
            selectNodeById(prologue.getId());
            setStatus("示例数据导入成功！");

        } catch (SQLException e) {
            showError("导入失败", e.getMessage());
            e.printStackTrace();
        }
    }

    private void expandAll(TreeItem<StoryNode> item) {
        if (item == null) return;
        item.setExpanded(true);
        for (TreeItem<StoryNode> child : item.getChildren()) {
            expandAll(child);
        }
    }

    private void selectNodeById(int nodeId) {
        TreeItem<StoryNode> found = findNodeItem(rootItem, nodeId);
        if (found != null) {
            storyTree.getSelectionModel().select(found);
            storyTree.scrollTo(storyTree.getSelectionModel().getSelectedIndex());
        }
    }

    private TreeItem<StoryNode> findNodeItem(TreeItem<StoryNode> parent, int nodeId) {
        if (parent.getValue() != null && parent.getValue().getId() == nodeId) {
            return parent;
        }
        for (TreeItem<StoryNode> child : parent.getChildren()) {
            TreeItem<StoryNode> found = findNodeItem(child, nodeId);
            if (found != null) return found;
        }
        return null;
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
        Platform.runLater(() -> {
            try {
                Thread.sleep(3000);
                if (statusLabel.getText().equals(message)) {
                    statusLabel.setText("就绪");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
