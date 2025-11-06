package com.kafkadesk.ui.constants;

/**
 * I18n Keys Constants for UI components
 */
public final class I18nKeys {
    
    // Private constructor to prevent instantiation
    private I18nKeys() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // I18n Keys - Menu
    public static final String MENU_FILE = "menu.file";
    public static final String MENU_FILE_ADD_CLUSTER = "menu.file.addCluster";
    public static final String MENU_FILE_EXIT = "menu.file.exit";
    public static final String MENU_VIEW = "menu.view";
    public static final String MENU_VIEW_REFRESH = "menu.view.refresh";
    public static final String MENU_TOOLS = "menu.tools";
    public static final String MENU_TOOLS_SETTINGS = "menu.tools.settings";
    public static final String MENU_HELP = "menu.help";
    public static final String MENU_HELP_ABOUT = "menu.help.about";

    // I18n Keys - Toolbar
    public static final String TOOLBAR_ADD_CLUSTER = "toolbar.addCluster";
    public static final String TOOLBAR_REFRESH = "toolbar.refresh";
    public static final String TOOLBAR_SETTINGS = "toolbar.settings";

    // I18n Keys - Cluster
    public static final String CLUSTER_LIST = "cluster.list";
    public static final String CLUSTER_ADD_TITLE = "cluster.add.title";
    public static final String CLUSTER_ADD_HEADER = "cluster.add.header";
    public static final String CLUSTER_ADD_NAME = "cluster.add.name";
    public static final String CLUSTER_ADD_SERVERS = "cluster.add.servers";
    public static final String CLUSTER_ADD_SUCCESS = "cluster.add.success";
    public static final String CLUSTER_CONNECTING = "cluster.connecting";
    public static final String CLUSTER_CONNECTED = "cluster.connected";
    public static final String CLUSTER_FAILED = "cluster.failed";
    public static final String CLUSTER_DELETE_TITLE = "cluster.delete.title";
    public static final String CLUSTER_DELETE_CONFIRM = "cluster.delete.confirm";
    public static final String CLUSTER_DELETE_SUCCESS = "cluster.delete.success";
    public static final String CLUSTER_EDIT_TITLE = "cluster.edit.title";
    public static final String CLUSTER_EDIT_HEADER = "cluster.edit.header";
    public static final String CLUSTER_EDIT_HOST = "cluster.edit.host";
    public static final String CLUSTER_EDIT_PROTOCOL = "cluster.edit.protocol";
    public static final String CLUSTER_EDIT_PORT = "cluster.edit.port";
    public static final String CLUSTER_EDIT_SUCCESS = "cluster.edit.success";
    public static final String CLUSTER_EDIT_ERROR_NAME_EMPTY = "cluster.edit.error.nameEmpty";
    public static final String CLUSTER_EDIT_ERROR_HOST_EMPTY = "cluster.edit.error.hostEmpty";
    public static final String CLUSTER_EDIT_ERROR_PORT_EMPTY = "cluster.edit.error.portEmpty";
    public static final String CLUSTER_EDIT_ERROR_PORT_INVALID = "cluster.edit.error.portInvalid";

    // I18n Keys - Tabs
    public static final String TAB_TOPICS = "tab.topics";
    public static final String TAB_PRODUCER = "tab.producer";
    public static final String TAB_QUERY = "tab.query";
    public static final String TAB_CONSUMER_GROUPS = "tab.consumerGroups";
    public static final String TAB_CONFIGURATION = "tab.configuration";

    // I18n Keys - Configuration
    public static final String CONFIG_TITLE = "config.title";
    public static final String CONFIG_CURRENT_CLUSTER = "config.currentCluster";
    public static final String CONFIG_NO_CLUSTER_SELECTED = "config.noClusterSelected";
    public static final String CONFIG_PROPERTIES = "config.properties";
    public static final String CONFIG_PARAM_NAME = "config.parameter.name";
    public static final String CONFIG_PARAM_RANGE = "config.parameter.range";
    public static final String CONFIG_PARAM_DEFAULT = "config.parameter.default";
    public static final String CONFIG_PARAM_CURRENT = "config.parameter.current";
    public static final String CONFIG_PARAM_OPERATION = "config.parameter.operation";
    public static final String CONFIG_PARAM_EDIT = "config.parameter.edit";
    public static final String CONFIG_PARAM_DESC = "config.parameter.description";
    
    // I18n Keys - Kafka Configuration Descriptions
    public static final String CONFIG_DESC_MIN_INSYNC_REPLICAS = "config.desc.min.insync.replicas";
    public static final String CONFIG_DESC_UNCLEAN_LEADER_ELECTION = "config.desc.unclean.leader.election.enable";
    public static final String CONFIG_DESC_LOG_RETENTION_HOURS = "config.desc.log.retention.hours";
    public static final String CONFIG_DESC_LOG_RETENTION_BYTES = "config.desc.log.retention.bytes";
    public static final String CONFIG_DESC_LOG_SEGMENT_BYTES = "config.desc.log.segment.bytes";
    public static final String CONFIG_DESC_COMPRESSION_TYPE = "config.desc.compression.type";
    public static final String CONFIG_DESC_NUM_PARTITIONS = "config.desc.num.partitions";
    public static final String CONFIG_DESC_DEFAULT_REPLICATION_FACTOR = "config.desc.default.replication.factor";
    public static final String CONFIG_DESC_MAX_MESSAGE_BYTES = "config.desc.max.message.bytes";
    public static final String CONFIG_DESC_REPLICA_LAG_TIME_MAX_MS = "config.desc.replica.lag.time.max.ms";

    // I18n Keys - Topic
    public static final String TOPIC_LIST = "topic.list";
    public static final String TOPIC_DETAILS = "topic.details";
    public static final String TOPIC_NAME = "topic.name";
    public static final String TOPIC_PARTITIONS = "topic.partitions";
    public static final String TOPIC_REPLICATION = "topic.replication";
    public static final String TOPIC_LOADING = "topic.loading";
    public static final String TOPIC_LOADED = "topic.loaded";
    public static final String TOPIC_CREATE = "topic.create";
    public static final String TOPIC_DELETE = "topic.delete";
    public static final String TOPIC_CREATE_TITLE = "topic.create.title";
    public static final String TOPIC_CREATE_HEADER = "topic.create.header";
    public static final String TOPIC_CREATE_SUCCESS = "topic.create.success";
    public static final String TOPIC_CREATE_ERROR = "topic.create.error";
    public static final String TOPIC_DELETE_TITLE = "topic.delete.title";
    public static final String TOPIC_DELETE_CONFIRM = "topic.delete.confirm";
    public static final String TOPIC_DELETE_SUCCESS = "topic.delete.success";
    public static final String TOPIC_DELETE_ERROR = "topic.delete.error";
    public static final String TOPIC_DELETE_NO_SELECTION = "topic.delete.noSelection";

    // I18n Keys - Producer
    public static final String PRODUCER_TITLE = "producer.title";
    public static final String PRODUCER_TOPIC = "producer.topic";
    public static final String PRODUCER_TOPIC_PROMPT = "producer.topic.prompt";
    public static final String PRODUCER_KEY = "producer.key";
    public static final String PRODUCER_KEY_PROMPT = "producer.key.prompt";
    public static final String PRODUCER_VALUE = "producer.value";
    public static final String PRODUCER_VALUE_PROMPT = "producer.value.prompt";
    public static final String PRODUCER_SEND = "producer.send";
    public static final String PRODUCER_SENDING = "producer.sending";
    public static final String PRODUCER_SUCCESS = "producer.success";
    public static final String PRODUCER_FAILED = "producer.failed";
    public static final String PRODUCER_ERROR_TITLE = "producer.error.title";
    public static final String PRODUCER_ERROR_REQUIRED = "producer.error.required";

    // I18n Keys - Query
    public static final String QUERY_TOPIC = "query.topic";
    public static final String QUERY_TOPIC_PROMPT = "query.topic.prompt";
    public static final String QUERY_PARTITION = "query.partition";
    public static final String QUERY_PARTITION_ALL = "query.partition.all";
    public static final String QUERY_OFFSET_FROM = "query.offset.from";
    public static final String QUERY_OFFSET_TO = "query.offset.to";
    public static final String QUERY_MAX_RECORDS = "query.maxRecords";
    public static final String QUERY_SEARCH = "query.search";
    public static final String QUERY_CLEAR = "query.clear";
    public static final String QUERY_EXPORT = "query.export";
    public static final String QUERY_RESULTS = "query.results";
    public static final String QUERY_DETAILS = "query.details";
    public static final String QUERY_OFFSET = "query.offset";
    public static final String QUERY_KEY = "query.key";
    public static final String QUERY_VALUE = "query.value";
    public static final String QUERY_TIMESTAMP = "query.timestamp";
    public static final String QUERY_SEARCHING = "query.searching";
    public static final String QUERY_FOUND = "query.found";
    public static final String QUERY_NO_CONNECTION = "query.noConnection";

    // I18n Keys - Consumer Group
    public static final String CONSUMER_GROUP_LIST = "consumerGroup.list";
    public static final String CONSUMER_GROUP_DETAILS = "consumerGroup.details";
    public static final String CONSUMER_GROUP_ID = "consumerGroup.groupId";
    public static final String CONSUMER_GROUP_STATE = "consumerGroup.state";
    public static final String CONSUMER_GROUP_COORDINATOR = "consumerGroup.coordinator";
    public static final String CONSUMER_GROUP_MEMBERS = "consumerGroup.members";
    public static final String CONSUMER_GROUP_MEMBER_ID = "consumerGroup.member.memberId";
    public static final String CONSUMER_GROUP_MEMBER_CLIENT_ID = "consumerGroup.member.clientId";
    public static final String CONSUMER_GROUP_MEMBER_HOST = "consumerGroup.member.host";
    public static final String CONSUMER_GROUP_MEMBER_ASSIGNMENTS = "consumerGroup.member.assignments";
    public static final String CONSUMER_GROUP_OFFSET = "consumerGroup.offset";
    public static final String CONSUMER_GROUP_LAG = "consumerGroup.lag";
    public static final String CONSUMER_GROUP_LOADING = "consumerGroup.loading";
    public static final String CONSUMER_GROUP_LOADED = "consumerGroup.loaded";

    // I18n Keys - Settings
    public static final String SETTINGS_TITLE = "settings.title";
    public static final String SETTINGS_LANGUAGE = "settings.language";

    // I18n Keys - Status
    public static final String STATUS_READY = "status.ready";

    // I18n Keys - Common
    public static final String COMMON_DELETE = "common.delete";
    public static final String COMMON_EDIT = "common.edit";
    public static final String COMMON_SUCCESS = "common.success";
    public static final String COMMON_INFO = "common.info";
    public static final String COMMON_ERROR = "common.error";

    // I18n Keys - Dialog
    public static final String DIALOG_ERROR_TITLE = "dialog.error.title";
    public static final String DIALOG_ABOUT_TITLE = "dialog.about.title";
    public static final String DIALOG_ABOUT_HEADER = "dialog.about.header";
    public static final String DIALOG_ABOUT_CONTENT = "dialog.about.content";

    // I18n Keys - Placeholders
    public static final String PLACEHOLDER_NO_TOPICS = "placeholder.noTopics";
    public static final String PLACEHOLDER_NO_MESSAGES = "placeholder.noMessages";
    public static final String PLACEHOLDER_NO_CONSUMER_GROUPS = "placeholder.noConsumerGroups";
    public static final String PLACEHOLDER_NO_DATA = "placeholder.noData";

    // I18n Keys - Error
    public static final String ERROR_QUERY_FAILED = "error.queryFailed";
}
