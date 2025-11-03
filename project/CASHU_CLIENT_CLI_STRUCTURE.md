# Cashu Client CLI Project Structure and Patterns

## Project Overview

The **cashu-client** project is a comprehensive Cashu wallet implementation for Java with CLI and identity management capabilities. It's located at `/home/eric/IdeaProjects/cashu-client` and consists of multiple integrated modules.

### Main Components

1. **wallet-cli** - Primary wallet command-line interface
2. **identity-cli** - Nostr identity management CLI
3. **wallet-plugin** - Core wallet services and infrastructure
4. **identity-plugin** - Identity management infrastructure
5. **wallet-storage-h2** - H2 database storage backend
6. **wallet-storage-file** - File-based storage backend
7. **wallet-infra-nip44/nip04/nip42** - Nostr protocol implementations

---

## 1. Project Structure

### Directory Layout

```
cashu-client/
├── wallet-cli/                          # Primary wallet CLI
│   ├── src/main/java/xyz/tcheeric/wallet/cli/
│   │   ├── WalletMain.java             # Entry point, command registration
│   │   ├── WalletCliConfiguration.java # DI and configuration
│   │   ├── commands/                    # Command implementations
│   │   ├── config/                      # Configuration classes
│   │   ├── events/                      # Event subscribers
│   │   ├── logging/                     # Logging setup
│   │   ├── nostr/                       # Nostr utilities
│   │   └── observability/               # Tracing/monitoring
│   └── pom.xml                          # Maven configuration
│
├── identity-cli/                        # Identity management CLI
│   ├── src/main/java/xyz/tcheeric/identity/cli/
│   │   ├── IdentityMain.java           # Entry point
│   │   ├── commands/                    # Command implementations
│   │   └── util/                        # Utilities (password prompts, etc.)
│   └── pom.xml
│
├── wallet-plugin/                       # Core wallet implementation
│   ├── wallet-core-base/               # Base interfaces and security
│   ├── wallet-core-app/                # Application services
│   ├── wallet-core-cashu/              # Cashu protocol implementation
│   ├── wallet-core-nostr/              # Nostr integration
│   └── wallet-core-observability/      # Monitoring
│
├── identity-plugin/                    # Identity infrastructure
├── wallet-storage-h2/                  # H2 database storage
├── wallet-storage-file/                # File storage
├── wallet-infra-nip04/                 # NIP-04 encryption
├── wallet-infra-nip44/                 # NIP-44 encryption
├── wallet-infra-nip42/                 # NIP-42 auth
├── identity-storage-file/              # Identity file storage
└── pom.xml                             # Parent POM
```

---

## 2. Command Architecture and Patterns

### 2.1 PicoCLI Framework

The CLI uses **PicoCLI** (https://picocli.info/) for command-line parsing and execution.

- Annotation-based: `@Command`, `@Option`, `@Parameters`
- Built-in help generation with `--help` and version support
- Parent-child command relationships via `@ParentCommand`
- Custom factory for dependency injection

### 2.2 Main CLI Entry Points

#### WalletMain (wallet-cli)

**File**: `/home/eric/IdeaProjects/cashu-client/wallet-cli/src/main/java/xyz/tcheeric/wallet/cli/WalletMain.java`

```java
@Command(name = "wallet", mixinStandardHelpOptions = true, version = "cashu-client 0.1",
        description = "Cashu wallet CLI...",
        subcommands = {
            Init.class,
            BalanceCmd.class,
            InfoCmd.class,
            MintQuoteCmd.class,
            // ... 15+ more commands
        })
public class WalletMain implements Runnable {
    @Option(names = {"--json"}, description = "Output JSON where applicable")
    public boolean jsonOutput;
    
    // Dependency injection via constructor
    private final BalanceService service;
    
    public static void main(String[] args) {
        WalletCliConfiguration configuration = WalletCliConfiguration.fromDefaults();
        int exit = configuration.commandLine().execute(args);
        System.exit(exit);
    }
}
```

#### IdentityMain (identity-cli)

**File**: `/home/eric/IdeaProjects/cashu-client/identity-cli/src/main/java/xyz/tcheeric/identity/cli/IdentityMain.java`

```java
@Command(name = "identity", 
        version = "identity-cli 1.2.0-SNAPSHOT",
        description = "Nostr identity management CLI...",
        subcommands = {
            GenerateCommand.class,
            ListCommand.class,
            ExportCommand.class,
            ImportCommand.class,
            DeleteCommand.class,
            SetDefaultCommand.class,
            SignCommand.class,
            CommandLine.HelpCommand.class
        })
public class IdentityMain implements Callable<Integer> {
    @Option(names = {"-v", "--verbose"})
    private boolean verbose;
}
```

### 2.3 Command Pattern Templates

#### Pattern 1: WalletServiceCommand Base Class

**File**: `/home/eric/IdeaProjects/cashu-client/wallet-cli/src/main/java/xyz/tcheeric/wallet/cli/commands/WalletServiceCommand.java`

Simple generic base class for wallet service commands:

```java
public abstract class WalletServiceCommand<T extends BalanceService> implements Runnable {
    protected final T service;
    protected WalletServiceCommand(T service) {
        this.service = service;
    }
}
```

#### Pattern 2: Simple Command (Init.java)

**File**: `/home/eric/IdeaProjects/cashu-client/wallet-cli/src/main/java/xyz/tcheeric/wallet/cli/commands/Init.java`

```java
@Command(name = "init", description = "Initialize wallet profile")
public class Init extends WalletServiceCommand<BalanceService> {
    @Option(names = {"--mint"}, description = "Mint base URL")
    String mint;
    
    @Option(names = {"--unit"}, description = "Default unit (e.g., sat)")
    String unit;

    public Init(BalanceService service) {
        super(service);
    }

    @Override
    public void run() {
        WalletConfig src = WalletConfig.load();
        String mintUrl = mint != null ? mint : src.defaultMintUrl();
        String u = unit != null ? unit : src.defaultUnit();
        service.init(new WalletConfig(mintUrl, u));
        System.out.println("Initialized wallet profile");
    }
}
```

#### Pattern 3: Complex Command (SendCmd.java)

**File**: `/home/eric/IdeaProjects/cashu-client/wallet-cli/src/main/java/xyz/tcheeric/wallet/cli/commands/SendCmd.java`

Complex commands with advanced features:

```java
@Command(name = "send",
        description = "Pay a Lightning invoice or deliver proofs via Nostr P2PK...")
public class SendCmd implements Runnable {
    private final SendUseCase sendUseCase;
    private final boolean requireH2Backend;

    @Spec
    CommandSpec spec;  // Access to command execution context

    @Option(names = {"--mint"}, description = "Mint base URL")
    String mint;
    
    @Option(names = {"--invoice"}, description = "BOLT11 invoice to pay")
    String invoice;
    
    @Option(names = {"--recipient"}, description = "Recipient Nostr pubkey")
    String recipient;

    public SendCmd(SendUseCase sendUseCase, boolean requireH2Backend) {
        this.sendUseCase = sendUseCase;
        this.requireH2Backend = requireH2Backend;
    }

    @Override
    public void run() {
        // Parameter validation
        boolean hasInvoice = invoice != null && !invoice.isBlank();
        boolean hasRecipient = recipient != null && !recipient.isBlank();
        if (hasInvoice == hasRecipient) {
            throw new ParameterException(spec.commandLine(), 
                "Specify exactly one of --invoice or --recipient");
        }

        // Delegate to use case
        if (hasInvoice) {
            handleLightning(flowConfig);
        } else {
            handleP2pk(flowConfig, relays);
        }
    }

    // Helper methods for error handling, output formatting
}
```

#### Pattern 4: Callable Commands (GenerateCommand)

**File**: `/home/eric/IdeaProjects/cashu-client/identity-cli/src/main/java/xyz/tcheeric/identity/cli/commands/GenerateCommand.java`

For commands returning exit codes:

```java
@Command(name = "generate",
        description = "Generate a new Nostr identity with encrypted storage")
public class GenerateCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Name/label for the identity")
    private String name;

    @Option(names = {"-d", "--set-default"})
    private boolean setAsDefault;

    @Override
    public Integer call() {
        try {
            // Generate keypair
            KeyGenerator.KeyPair keyPair = KeyGenerator.generateKeyPair();
            
            // Encrypt and store
            EncryptedFileStorage keyStorage = new EncryptedFileStorage(...);
            keyStorage.save(identityId, domainPrivateKey);
            
            System.out.println("✓ Identity generated successfully!");
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
```

---

## 3. Command Registration and Configuration

### 3.1 WalletCliConfiguration (Dependency Injection)

**File**: `/home/eric/IdeaProjects/cashu-client/wallet-cli/src/main/java/xyz/tcheeric/wallet/cli/WalletCliConfiguration.java`

Implements `CommandLine.IFactory` for custom dependency injection:

```java
public class WalletCliConfiguration implements CommandLine.IFactory {
    private final BalanceService service;
    private final NostrGatewayService nostrGateway;
    private final TokenCodec tokenCodec;

    public CommandLine commandLine() {
        CommandLine cmd = new CommandLine(new WalletMain(service), this);
        cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            commandLine.getErr().println("Error: " + ex.getMessage());
            return 2;
        });
        return cmd;
    }

    @Override
    public <K> K create(Class<K> targetClass) throws Exception {
        // Smart constructor detection and dependency injection
        if (hasConstructor(targetClass, SendUseCase.class, boolean.class)) {
            return targetClass.getDeclaredConstructor(SendUseCase.class, boolean.class)
                .newInstance((SendUseCase) ensureApplicationService(), requiresH2Backend());
        }
        // ... More constructor patterns
        return defaultFactory.create(targetClass);
    }

    private WalletApplicationService ensureApplicationService() { /* ... */ }
    private TransferPublisher ensureTransferPublisher() { /* ... */ }
    private DomainEventPublisher ensureDomainEventPublisher() { /* ... */ }
    private DmService ensureDmService() { /* ... */ }
}
```

### 3.2 Command List (wallet-cli)

Commands registered in `WalletMain.subcommands`:

```
Init                    - Initialize wallet profile
BalanceCmd              - Show balances per unit
InfoCmd                 - Fetch mint /v1/info
MintQuoteCmd            - Request mint quote
MeltQuoteCmd            - Request melt quote
MintCmd                 - Execute mint
MeltCmd                 - Execute melt
SendCmd                 - Pay Lightning invoice or send via Nostr P2PK
ReceiveCmd              - Import token proofs or listen for Nostr transfers
DmSendCmd               - Send direct message
DmListCmd               - List direct messages
ConsolidateCmd          - Consolidate proofs
StatusCmd               - Show wallet status
VerifyCmd               - Verify proofs
EncryptCmd              - Enable encryption for secrets
ChangePassphraseCmd     - Change encryption passphrase
VerifyEncryptionCmd     - Verify encryption setup
ListProofsCmd           - List proofs with filters
ApiVersionCmd           - Show API version
MintRegistryCmd         - Manage mint registry
NostrGatewayCmd         - Nostr gateway operations
```

### 3.3 Command List (identity-cli)

Commands registered in `IdentityMain.subcommands`:

```
GenerateCommand         - Generate new Nostr identity
ImportCommand           - Import identity from nsec
ExportCommand           - Export identity
ListCommand             - List stored identities
DeleteCommand           - Delete identity
SetDefaultCommand       - Set default identity
SignCommand             - Sign Nostr events
HelpCommand             - Built-in help (auto-added by PicoCLI)
```

---

## 4. Wallet Command Implementation Patterns

### 4.1 Configuration and Initialization

All commands follow a pattern of:

1. **Load Config**: `WalletConfig.load()` - reads from files/environment
2. **Resolve Options**: Use provided flags or fallback to config defaults
3. **Initialize Service**: Call `service.init(config)` with resolved config
4. **Execute Business Logic**: Call service methods
5. **Format Output**: Plain text or JSON

**Example from SendCmd**:

```java
@Override
public void run() {
    WalletConfig config = WalletConfig.load();
    String mintUrl = resolveOrDefault(mint, config.defaultMintUrl());
    String resolvedUnit = resolveOrDefault(unit, config.defaultUnit());
    WalletConfig flowConfig = new WalletConfig(mintUrl, resolvedUnit);
    
    // Validation
    if (hasInvoice == hasRecipient) {
        throw new ParameterException(...);
    }
    
    // Backend check
    if (requireH2Backend) {
        CliUtils.ensureH2Backend();
    }
    
    // Execution
    if (hasInvoice) {
        handleLightning(flowConfig);
    } else {
        handleP2pk(flowConfig, normalizedRelays);
    }
}
```

### 4.2 JSON Output Support

Commands access parent's `jsonOutput` flag via `@ParentCommand`:

```java
@Command(name = "balance")
public class BalanceCmd extends WalletServiceCommand<BalanceService> {
    @CommandLine.ParentCommand WalletMain parent;

    @Override
    public void run() {
        Balance bal = service.balance();
        if (parent != null && parent.jsonOutput) {
            // Output JSON
            System.out.print("{\"balances\":{...}}");
        } else {
            // Output plain text
            for (Map.Entry<String, Long> e : bal.totalsByUnit().entrySet()) {
                System.out.println(e.getValue() + " " + e.getKey());
            }
        }
    }
}
```

### 4.3 Error Handling

Multi-level error handling pattern:

```java
try {
    PayLightningResult result = sendUseCase.payLightning(command);
    // Success handling
} catch (ConstraintViolationException e) {
    throw new ParameterException(spec.commandLine(), violationMessage(e), e);
} catch (QuoteExpiredException e) {
    PrintWriter err = error();
    err.printf("Quote expired while paying %d %s...%n", amount, unit);
    throw new CommandLine.ExecutionException(commandLine(), e.getUserMessage(), e);
} catch (WalletOperationException e) {
    PrintWriter err = error();
    err.printf("Failed to pay invoice: %s (code=%s)%n", 
        e.getUserMessage(), e.getErrorCode());
    if (e.isRetryable()) {
        err.println("The operation may succeed if you retry...");
    }
    throw new CommandLine.ExecutionException(commandLine(), e.getUserMessage(), e);
}
```

### 4.4 Backend Requirements

Some commands require H2 backend (for encrypted storage):

```java
@Command(name = "encrypt", 
         description = "Enable encryption... Requires H2 backend.")
public class EncryptCmd extends WalletServiceCommand<BalanceService> {
    @Override
    public void run() {
        if (!"h2".equalsIgnoreCase(System.getenv().getOrDefault("WALLET_STORAGE", "file"))) {
            throw new CommandLine.ExecutionException(
                new CommandLine(this), "encrypt requires WALLET_STORAGE=h2");
        }
        // ...
    }
}
```

---

## 5. Wallet Service Integration

### 5.1 Service Interfaces

Located in `/home/eric/IdeaProjects/cashu-client/wallet-plugin/wallet-core-app/src/main/java/xyz/tcheeric/wallet/core/`

Key services:

- **BalanceService** - Base service for balance queries
- **SendService** - Send proofs (extends BalanceService)
- **ReceiveService** - Receive and import proofs (extends BalanceService)
- **MintingService** - Mint operations
- **MeltService** - Melt operations

### 5.2 Command to Service Mapping

```
Command                 → Service Method
------                    ---------------
Init                    → BalanceService.init(config)
BalanceCmd              → BalanceService.balance()
SendCmd                 → SendUseCase.payLightning(...) / deliverP2pk(...)
ReceiveCmd              → ReceiveUseCase.importToken(...) / listenForTransfers(...)
MintCmd                 → MintingService.mint(...)
MeltCmd                 → MeltService.melt(...)
ChangePassphraseCmd     → H2WalletService.changeEncryptionPassphrase(...)
ListProofsCmd           → H2WalletService.listProofSummaries(...)
EncryptCmd              → H2WalletService.migrateToEncryption(...)
```

---

## 6. Identity Management Patterns

### 6.1 Identity Commands Pattern

Identity commands follow a similar pattern but with different concerns (key generation, encryption, storage):

**GenerateCommand**:
1. Prompt for name
2. Prompt for password
3. Generate keypair
4. Encrypt and store private key
5. Save metadata
6. Display success

**ImportCommand**:
1. Validate nsec input
2. Prompt for name
3. Check for duplicates
4. Prompt for password
5. Encrypt and store
6. Save metadata

### 6.2 Identity Storage

Uses encrypted file storage with metadata:

```
~/.cashu/identities/
├── keys/
│   └── {identity-id}.enc       # Encrypted private key
└── metadata.json               # Identity names and public keys
```

---

## 7. Build and Configuration (Maven)

### 7.1 POM Structure

**Main dependencies** in wallet-cli pom.xml:

```xml
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>wallet-core-app</artifactId>
</dependency>
<dependency>
    <groupId>info.picocli</groupId>
    <artifactId>picocli</artifactId>
</dependency>
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>wallet-storage-h2</artifactId>
</dependency>
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>wallet-storage-file</artifactId>
</dependency>
```

### 7.2 Build Output

Maven shade plugin creates fat JAR with:
- Main class: `xyz.tcheeric.wallet.cli.WalletMain`
- All dependencies included
- Service loader configuration preserved
- Bouncycastle handled separately for signature verification

---

## 8. Existing Wallet Recovery/Backup Features

### Current Implementation

#### EncryptCmd - Encryption Management
- Enables encryption for wallet secrets
- Migrates existing secrets to encrypted storage
- Uses PBKDF2 + AES-256-GCM

#### ChangePassphraseCmd - Passphrase Management
- Changes encryption passphrase
- Requires H2 backend
- Supports KDF parameter (`--kdf` system property)

#### ListProofsCmd - Proof Inspection
- Lists all stored proofs
- Supports filtering by amount, keyset, spent status
- Shows proof commitment hash and timestamps
- H2 backend only

### Missing Recovery Features

Based on code analysis, the following recovery/backup features are **NOT YET IMPLEMENTED**:

- No mnemonic seed phrase generation/recovery
- No wallet backup export/restore
- No proof state versioning/rollback
- No recovery from keyset rotation
- No exported wallet state portability
- No recovery phrase or seed support

---

## 9. Key Configuration and Utilities

### 9.1 CliUtils

**File**: `/home/eric/IdeaProjects/cashu-client/wallet-cli/src/main/java/xyz/tcheeric/wallet/cli/CliUtils.java`

Provides utilities for:
- H2 backend validation
- Keyset ID normalization
- Common CLI checks

### 9.2 WalletConfig

Loads wallet configuration from:
- Environment variables
- Configuration files
- Command-line options
- Defaults

### 9.3 Command Execution Flow

```
WalletMain.main()
    ↓
WalletCliConfiguration.fromDefaults()
    ↓
WalletCliConfiguration.commandLine()
    ↓
commandLine.execute(args)
    ↓
PicoCLI parses args → finds subcommand → creates instance via IFactory
    ↓
Custom IFactory (WalletCliConfiguration.create()) → injects dependencies
    ↓
Command.run() executes
    ↓
Output to stdout/stderr
    ↓
System.exit(code)
```

---

## 10. Summary

### Strengths of Current Architecture

1. **Clean Separation**: CLI concerns separated from wallet core logic
2. **Type-Safe DI**: Custom factory with compile-time safety
3. **Consistent Patterns**: All commands follow similar structure
4. **Error Handling**: Multi-level exception handling with user-friendly messages
5. **Testability**: Commands can be unit tested with injected services
6. **Extensibility**: New commands easily added by extending base classes
7. **Output Flexibility**: JSON and plain text output modes
8. **Security**: Encrypted storage, passphrase management

### Areas for NUT-13 Implementation

1. **Recovery Commands**: New `recover` command with mnemonic support
2. **Backup Commands**: Export/import wallet state
3. **Key Derivation**: BIP-32 derivation path support (from cashu-lib)
4. **Voucher Commands**: NUT-13 specific operations (spending conditions, backup)
5. **State Management**: Track wallet state versions for recovery

