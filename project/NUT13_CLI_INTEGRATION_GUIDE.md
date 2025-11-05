# NUT-13 CLI Integration Guide

Based on the cashu-client CLI architecture analysis, this guide outlines how to implement NUT-13 wallet recovery and voucher commands.

## Key Findings from CLI Architecture

### 1. Command Registration Pattern

Commands are registered in `WalletMain` using the `@Command` annotation with a `subcommands` list.

**Location**: `/home/eric/IdeaProjects/cashu-client/wallet-cli/src/main/java/xyz/tcheeric/wallet/cli/WalletMain.java`

```java
@Command(
    name = "wallet",
    subcommands = {
        Init.class,
        BalanceCmd.class,
        // ... existing commands ...
        // NEW NUT-13 COMMANDS WOULD BE ADDED HERE
    }
)
public class WalletMain implements Runnable { }
```

### 2. Dependency Injection Pattern

Commands receive dependencies through constructor injection via custom PicoCLI factory.

**Location**: `/home/eric/IdeaProjects/cashu-client/wallet-cli/src/main/java/xyz/tcheeric/wallet/cli/WalletCliConfiguration.java`

The `WalletCliConfiguration.create()` method detects constructor signatures and injects appropriate services:

```java
@Override
public <K> K create(Class<K> targetClass) throws Exception {
    // Example: detect constructor with SendUseCase
    if (hasConstructor(targetClass, SendUseCase.class, boolean.class)) {
        return targetClass.getDeclaredConstructor(SendUseCase.class, boolean.class)
            .newInstance((SendUseCase) ensureApplicationService(), requiresH2Backend());
    }
    // New pattern for recovery could be:
    // if (hasConstructor(targetClass, RecoveryService.class)) { ... }
}
```

### 3. Command Base Classes

Two patterns exist:

#### Pattern A: WalletServiceCommand Base Class
For simple commands that need a service instance:

```java
public abstract class WalletServiceCommand<T extends BalanceService> implements Runnable {
    protected final T service;
    protected WalletServiceCommand(T service) {
        this.service = service;
    }
}
```

#### Pattern B: Direct Constructor Injection
For complex commands needing specific use cases:

```java
public class SendCmd implements Runnable {
    private final SendUseCase sendUseCase;
    private final boolean requireH2Backend;
    
    public SendCmd(SendUseCase sendUseCase, boolean requireH2Backend) {
        this.sendUseCase = Objects.requireNonNull(sendUseCase);
        this.requireH2Backend = requireH2Backend;
    }
}
```

### 4. Command Structure Template

All commands follow this pattern:

```java
@Command(name = "command-name", description = "Description...")
public class MyCommand implements Runnable {
    
    // CLI options
    @Option(names = {"--mint"}, description = "Mint base URL")
    String mint;
    
    @Option(names = {"--unit"}, description = "Unit")
    String unit;
    
    // Access to command context and parent
    @Spec
    CommandSpec spec;
    
    @CommandLine.ParentCommand
    WalletMain parent;
    
    // Constructor with dependencies
    public MyCommand(SomeService service) {
        this.service = Objects.requireNonNull(service);
    }
    
    @Override
    public void run() {
        // 1. Load configuration
        WalletConfig config = WalletConfig.load();
        String mintUrl = resolveOrDefault(mint, config.defaultMintUrl());
        
        // 2. Validate inputs
        if (invalid) {
            throw new ParameterException(spec.commandLine(), "Error message");
        }
        
        // 3. Execute business logic
        try {
            Result result = service.doSomething(...);
            
            // 4. Format output
            if (parent != null && parent.jsonOutput) {
                System.out.println(toJson(result));
            } else {
                System.out.println(result);
            }
        } catch (Exception e) {
            throw new CommandLine.ExecutionException(commandLine(), "User message", e);
        }
    }
    
    private String resolveOrDefault(String value, String defaultValue) {
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
```

---

## Proposed NUT-13 Commands

### 1. RecoverCmd - Wallet Recovery from Seed

```
wallet recover --from-seed "seed words here" [--name "wallet name"] [--mint MINT_URL]
```

**Implementation Pattern**:
```java
@Command(name = "recover", 
         description = "Recover wallet from seed phrase (NUT-13)")
public class RecoverCmd implements Runnable {
    @Option(names = {"--from-seed"}, required = true, 
            description = "Seed phrase (12/24 words)")
    String seedPhrase;
    
    @Option(names = {"--name"}, description = "Wallet name")
    String name;
    
    @Option(names = {"--mint"}, description = "Mint URL")
    String mint;
    
    @Option(names = {"--derivation-path"}, 
            description = "BIP-32 derivation path (default: m/32'/0'/0')")
    String derivationPath;
    
    public RecoverCmd(RecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }
    
    @Override
    public void run() {
        // 1. Validate seed phrase format
        // 2. Derive keys using BIP-32 paths (from cashu-lib)
        // 3. Initialize wallet with recovered keys
        // 4. Display recovery summary
    }
}
```

**Dependencies Needed**:
- Create `RecoveryService` interface in wallet-core-app
- Use `DerivationPath` from cashu-lib
- Implement seed phrase validation (BIP-39)

### 2. BackupCmd - Export Wallet State

```
wallet backup [--mint MINT_URL] [--output backup.json] [--include-private-keys]
```

**Implementation Pattern**:
```java
@Command(name = "backup",
         description = "Export wallet backup (NUT-13)")
public class BackupCmd extends WalletServiceCommand<BalanceService> {
    @Option(names = {"--output"}, description = "Output file")
    String outputFile;
    
    @Option(names = {"--include-private-keys"},
            description = "Include encrypted private keys")
    boolean includePrivateKeys;
    
    @Override
    public void run() {
        // 1. Collect all wallet state (proofs, config, keys)
        // 2. Serialize to backup format (JSON/binary)
        // 3. Optionally encrypt backup
        // 4. Write to file
    }
}
```

### 3. VoucherCmd - NUT-13 Voucher Operations

```
wallet voucher create --amount 100 [--conditions "conditions..."]
wallet voucher verify VOUCHER_STRING
wallet voucher redeem VOUCHER_STRING
```

**Implementation Pattern**:
```java
@Command(name = "voucher", description = "NUT-13 voucher operations")
public class VoucherCmd implements Runnable {
    // Sub-commands via nested @Command classes or separate commands
    
    // Option 1: Single command with subcommand pattern
    @Spec CommandSpec spec;
    
    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}
```

### 4. RestoreCmd - Import Wallet from Backup

```
wallet restore --from-backup backup.json [--passphrase secret]
```

---

## Integration Steps

### Step 1: Create New Services

Create service interfaces in `wallet-core-app`:

```
wallet-plugin/wallet-core-app/src/main/java/
  xyz/tcheeric/wallet/core/
    ├── RecoveryService.java
    ├── BackupService.java
    ├── VoucherService.java
    └── application/
        ├── RecoveryUseCase.java
        └── ...
```

### Step 2: Update Dependency Injection

Modify `WalletCliConfiguration.create()`:

```java
if (hasConstructor(targetClass, RecoveryService.class)) {
    return targetClass.getDeclaredConstructor(RecoveryService.class)
        .newInstance(ensureRecoveryService());
}

private RecoveryService ensureRecoveryService() {
    if (recoveryService == null) {
        recoveryService = new RecoveryServiceImpl(
            ensureSendService(),
            ensureReceiveService(),
            keyDerivationService
        );
    }
    return recoveryService;
}
```

### Step 3: Register Commands

Add to `WalletMain.subcommands`:

```java
@Command(
    subcommands = {
        // ... existing ...
        RecoverCmd.class,
        BackupCmd.class,
        RestoreCmd.class,
        VoucherCmd.class,
        // ... other new commands ...
    }
)
public class WalletMain implements Runnable { }
```

### Step 4: Add to Maven Dependencies

Update `wallet-cli/pom.xml`:

```xml
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-lib-common</artifactId>
    <!-- For DerivationPath, BIP-39, etc. -->
</dependency>
```

### Step 5: Implement Commands

Create command files in:
```
wallet-cli/src/main/java/xyz/tcheeric/wallet/cli/commands/
  ├── RecoverCmd.java
  ├── BackupCmd.java
  ├── RestoreCmd.java
  ├── VoucherCmd.java
  └── VoucherSubcommands.java (if using nested structure)
```

---

## Implementation Considerations

### 1. Key Derivation

The cashu-lib provides `DerivationPath` classes:
- `DerivationPath` - generic path
- `SecretDerivationPath` - secret key derivation
- `RDerivationPath` - recovery path
- `KeysetId` - keyset identifiers

Use these for BIP-32 derivation paths:
```java
DerivationPath path = DerivationPath.fromString("m/32'/0'/0'");
// Use with key derivation service
```

### 2. Seed Phrase Handling

Options:
1. Use existing BIP-39 library (e.g., `bitcoinj` or `bip39-java`)
2. Implement custom seed phrase support
3. Support multiple mnemonic standards

### 3. State Persistence

Leverage existing storage layers:
- **H2Backend**: Store recovered keys, backup metadata
- **FileBackend**: Store backup files, recovery state
- **EncryptedStorage**: Protect sensitive recovery data

### 4. Error Handling Pattern

Follow existing error handling:

```java
try {
    RecoveryResult result = recoveryService.recover(command);
    System.out.println("Recovery successful");
} catch (InvalidSeedPhraseException e) {
    throw new ParameterException(spec.commandLine(), 
        "Invalid seed phrase: " + e.getMessage());
} catch (WalletOperationException e) {
    throw new CommandLine.ExecutionException(commandLine(), 
        "Recovery failed: " + e.getMessage(), e);
}
```

### 5. JSON Output Support

Commands should support `--json` flag via parent context:

```java
if (parent != null && parent.jsonOutput) {
    ObjectMapper mapper = new ObjectMapper();
    System.out.println(mapper.writeValueAsString(result));
} else {
    // Pretty print
}
```

### 6. Configuration Management

Recovery operations may need special config:
```java
WalletConfig config = WalletConfig.load();
WalletConfig recoveryConfig = new WalletConfig(
    mint != null ? mint : config.defaultMintUrl(),
    config.defaultUnit()
);
service.init(recoveryConfig);
```

---

## Testing Pattern

Based on existing tests in `wallet-cli`:

```java
// Unit test for command
class RecoverCmdTest {
    @Test
    void testValidSeedPhraseRecovery() {
        RecoveryService mockService = mock(RecoveryService.class);
        RecoverCmd cmd = new RecoverCmd(mockService);
        cmd.seedPhrase = "valid twelve word seed phrase here";
        
        cmd.run();
        
        verify(mockService).recover(any());
    }
}

// Integration test for end-to-end
class RecoverCmdIT {
    @Test
    void testRecoveryIntegration() {
        // Create command via WalletCliConfiguration
        // Execute with real services
        // Verify wallet state
    }
}
```

---

## References

### Key Files to Study

1. **Command Examples**:
   - `/home/eric/IdeaProjects/cashu-client/wallet-cli/src/main/java/xyz/tcheeric/wallet/cli/commands/SendCmd.java` - Complex command
   - `/home/eric/IdeaProjects/cashu-client/wallet-cli/src/main/java/xyz/tcheeric/wallet/cli/commands/EncryptCmd.java` - Security-sensitive command
   - `/home/eric/IdeaProjects/cashu-client/identity-cli/src/main/java/xyz/tcheeric/identity/cli/commands/GenerateCommand.java` - Key generation pattern

2. **Infrastructure**:
   - `/home/eric/IdeaProjects/cashu-client/wallet-cli/src/main/java/xyz/tcheeric/wallet/cli/WalletCliConfiguration.java` - DI setup
   - `/home/eric/IdeaProjects/cashu-client/wallet-cli/src/main/java/xyz/tcheeric/wallet/cli/WalletMain.java` - Command registration

3. **Cashu-lib Classes**:
   - `DerivationPath`, `SecretDerivationPath`, `RDerivationPath`
   - `KeysetId`
   - BIP-32 utilities

---

## Summary

The cashu-client CLI is well-architected for extension. NUT-13 support can be added by:

1. Creating new service classes following existing patterns
2. Implementing command classes extending `WalletServiceCommand` or using constructor injection
3. Registering commands in `WalletMain.subcommands`
4. Adding DI support in `WalletCliConfiguration`
5. Following error handling, output formatting, and testing patterns

The architecture supports incremental implementation, allowing NUT-13 features to be added without disrupting existing functionality.
