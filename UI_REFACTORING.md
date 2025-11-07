# UI Refactoring - Multi-Cluster Tab Design

## Overview

This document describes the major UI refactoring implemented to support a multi-cluster tab-based interface.

## Design Changes

### Before (Old Design)
```
┌─────────────────────────────────────────────────────┐
│  Menu Bar | Toolbar                                  │
├──────────┬──────────────────────────────────────────┤
│          │                                          │
│  Cluster │         Functional Tabs                  │
│  Tree    │  ┌───────────────────────────────────┐  │
│  View    │  │ Topics | Producer | Query | ...   │  │
│          │  └───────────────────────────────────┘  │
│  - Cluster1│                                        │
│  - Cluster2│         Content Area                   │
│  - Cluster3│                                        │
│          │                                          │
└──────────┴──────────────────────────────────────────┘
```

**Limitations:**
- Only one cluster could be viewed at a time
- Switching clusters required selecting from tree
- No easy way to compare data across clusters

### After (New Design)
```
┌─────────────────────────────────────────────────────┐
│  Menu Bar | Toolbar                                  │
├──────┬──────────────────────────────────────────────┤
│      │                                              │
│ C    │  Open Cluster Tabs (closable)               │
│ l    │  ┌────────────────────────────────────────┐ │
│ u    │  │ Cluster1 | Cluster2 | ...              │ │
│ s    │  └────────────────────────────────────────┘ │
│ t    │                                              │
│ e    │  Cluster Function Tabs                       │
│ r    │  ┌────────────────────────────────────────┐ │
│      │  │ Overview | Brokers | Topics | ...     │ │
│ T    │  └────────────────────────────────────────┘ │
│ a    │                                              │
│ b    │         Content Area                         │
│ s    │                                              │
│      │                                              │
│      │                                              │
└──────┴──────────────────────────────────────────────┘
```

**Improvements:**
- Multiple clusters can be open simultaneously
- Each cluster has its own set of function tabs
- Easy switching between open clusters via horizontal tabs
- Each cluster tab can be closed independently

## Component Structure

### Left Side - Cluster List (Vertical Tabs)
- Displays all configured clusters as vertical tabs
- Clicking a cluster tab opens it in the right panel
- Tabs are not closable (permanent cluster list)

### Right Side - Open Clusters (Horizontal Tabs)
- Shows currently open clusters as horizontal tabs
- Each tab displays the cluster name
- Tabs are closable (close cluster view)
- Automatically switches to newly opened cluster

### Cluster Content - Nested Function Tabs
Each open cluster contains the following nested tabs:

#### 1. Overview Tab
- Cluster name and connection details
- Broker count
- Topic count
- Other summary statistics

#### 2. Brokers Tab
- List of all brokers in the cluster
- Broker ID, host, port, rack information
- Broker health status (future enhancement)

#### 3. Topics Tab
- Topic list with partition and replication info
- Topic details view
- Create/delete topic functionality
- Topic configuration viewing

#### 4. Consumer Groups Tab
- Consumer group list
- Group members and assignments
- Lag information per partition

#### 5. ACL Tab
- Access Control List management (placeholder)
- Future: Add/modify/delete ACL rules

## Technical Implementation

### Key Classes

#### MainController
- Main controller managing the overall UI
- Handles cluster selection from left panel
- Creates and manages open cluster tabs
- Maintains map of open cluster content

#### ClusterTabContent (Inner Class)
- Encapsulates all content for a single cluster
- Creates nested function tabs
- Manages data loading for each tab
- Handles UI events within cluster context

### FXML Structure
```xml
<BorderPane>
  <top>MenuBar & Toolbar</top>
  <center>
    <SplitPane>
      <left>
        <TabPane fx:id="clusterTabPane" side="LEFT">
          <!-- Cluster tabs added dynamically -->
        </TabPane>
      </left>
      <right>
        <TabPane fx:id="openClustersTabPane">
          <!-- Open cluster tabs added dynamically -->
          <!-- Each contains nested TabPane with function tabs -->
        </TabPane>
      </right>
    </SplitPane>
  </center>
  <bottom>Status Bar</bottom>
</BorderPane>
```

## Internationalization (i18n)

New i18n keys added:
- `tab.overview` - Overview tab title
- `tab.brokers` - Brokers tab title
- `tab.acl` - ACL tab title
- `overview.title` - Overview section title
- `overview.brokerCount` - Broker count label
- `overview.topicCount` - Topic count label
- `broker.list` - Broker list title
- `broker.id` - Broker ID column
- `broker.host` - Broker host column
- `broker.port` - Broker port column
- `broker.rack` - Broker rack column
- `acl.*` - ACL related labels

## User Workflow

### Opening a Cluster
1. User clicks a cluster tab on the left
2. System connects to the cluster
3. New horizontal tab is created on the right
4. Cluster content loads with Overview tab selected
5. User can switch between function tabs

### Working with Multiple Clusters
1. User opens Cluster A (tab appears on right)
2. User opens Cluster B (new tab appears on right)
3. User can switch between Cluster A and B tabs
4. Each cluster maintains its own state
5. User can close cluster tabs as needed

### Managing Cluster Data
1. Topics: View, create, delete topics
2. Brokers: View broker information
3. Consumer Groups: Monitor consumer lag
4. ACL: Future implementation

## Future Enhancements

1. **Producer Tab**
   - Add message producer functionality to cluster tabs
   - Send messages to topics in the specific cluster

2. **Query Tab**
   - Add message query functionality
   - Query and search messages by topic/partition/offset

3. **Configuration Tab**
   - Cluster-specific configuration management
   - Broker and topic configuration editing

4. **Broker Management**
   - Real-time broker health monitoring
   - Broker configuration details
   - Partition leader distribution

5. **ACL Management**
   - Complete ACL functionality
   - Add/modify/delete ACL rules
   - ACL testing and validation

6. **Context Menus**
   - Right-click menus for cluster tabs
   - Quick actions: edit, delete, refresh

7. **Visual Enhancements**
   - Custom icons for cluster tabs
   - Status indicators (connected/disconnected)
   - Theme support (light/dark)

## Migration Notes

### For Developers
- Old `TreeView` based cluster selection replaced with `TabPane`
- Main content area split into two `TabPane` instances (cluster tabs and content tabs)
- All cluster-specific UI now created dynamically
- Each cluster maintains independent data and state

### For Users
- No breaking changes in functionality
- Existing clusters automatically appear in new tab layout
- All previous features preserved (topics, consumer groups, etc.)
- New features: multiple cluster viewing, overview, brokers

## Testing Recommendations

1. Test with multiple clusters configured
2. Verify opening/closing multiple clusters
3. Test data refresh for each cluster independently
4. Verify cluster add/edit/delete functionality
5. Test all function tabs for each open cluster
6. Test i18n with both English and Chinese
7. Verify memory management with many open clusters
