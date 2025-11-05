# KafkaDesk Changelog

## Version 1.1.0 (2025-11-05)

### New Features

#### Internationalization (i18n)
- Full multi-language support (English and Chinese)
- Language switcher in Settings dialog
- Auto-detection of system language
- All UI strings externalized to resource bundles

#### Message Query
- Replaced message consumer with advanced query functionality
- Filter by topic, partition, and offset range
- Configurable max records limit
- Real-time search results
- Message details viewer
- Export capability (coming soon)

#### Consumer Group Management
- List all consumer groups
- View group state and coordinator information
- Drill down into group members
- Display member details (ID, client ID, host, assignments)
- Show lag information per partition
- Real-time lag calculation

#### Settings & Configuration
- Settings dialog for runtime configuration
- Language switching without restart
- Persistent configuration storage
- Window state preservation

### Improvements

- All code comments converted to English
- Enhanced cross-platform compatibility
- Improved error handling and user feedback
- Better UI organization with new tabs
- Performance optimizations

### Technical Details

- Java 17+
- JavaFX 17+
- Apache Kafka Clients 3.6.0
- Maven multi-module architecture
- ResourceBundle for i18n
- Platform-independent paths

### Cross-Platform Support

- macOS ✅
- Windows ✅
- Linux ✅

Configuration directory: `~/.kafkadesk/`

## Version 1.0.0 (Initial Release)

### Core Features

- Cluster management
- Topic browsing and details
- Message producer
- Configuration persistence
- Professional JavaFX UI
- Light theme styling

