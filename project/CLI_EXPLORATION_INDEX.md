# Cashu Client CLI - Exploration Index

## Overview

This index documents the exploration of the cashu-client CLI architecture to understand command patterns, wallet service integration, and configuration for implementing NUT-13 wallet recovery features.

**Exploration Date**: 2025-11-03

## Key Documents Generated

### 1. CASHU_CLIENT_CLI_STRUCTURE.md (660 lines)
Comprehensive analysis of the cashu-client project structure covering:
- Project overview and components
- Command architecture using PicoCLI framework
- Four command pattern templates with examples
- Command registration and configuration mechanisms
- Wallet service integration patterns
- Identity management patterns
- Maven build configuration
- Existing recovery/backup features (EncryptCmd, ChangePassphraseCmd, ListProofsCmd)
- Missing recovery features for NUT-13

**Location**: `/home/eric/IdeaProjects/cashu-lib/project/CASHU_CLIENT_CLI_STRUCTURE.md`

**Key Sections**:
- Section 2.3: Command Pattern Templates (most useful for implementation)
- Section 3: Command Registration and Configuration
- Section 4: Wallet Command Implementation Patterns
- Section 5: Wallet Service Integration
- Section 8: Existing Recovery/Backup Features

### 2. NUT13_CLI_INTEGRATION_GUIDE.md (464 lines)
Practical implementation guide for adding NUT-13 features to cashu-client CLI:
- Key findings from CLI architecture
- Proposed NUT-13 commands (Recover, Backup, Voucher, Restore)
- Integration steps with code examples
- Implementation considerations
- Testing patterns
- References to key source files

**Location**: `/home/eric/IdeaProjects/cashu-lib/project/NUT13_CLI_INTEGRATION_GUIDE.md`

**Key Sections**:
- Section "Proposed NUT-13 Commands": Specific command designs
- Section "Integration Steps": Step-by-step implementation
- Section "Implementation Considerations": Important design decisions

## Key Files Examined

### CLI Entry Points
- `WalletMain.java` - Primary wallet CLI entry point and command registration
- `IdentityMain.java` - Identity CLI entry point and command registration
- `WalletCliConfiguration.java` - Dependency injection and configuration

### Command Examples
- `SendCmd.java` - Complex command with use cases (1000+ lines, well-structured)
- `ReceiveCmd.java` - Command with Nostr integration
- `EncryptCmd.java` - Security-sensitive command
- `ChangePassphraseCmd.java` - Encryption passphrase management
- `ListProofsCmd.java` - Proof inspection with filters
- `GenerateCommand.java` (identity-cli) - Key generation pattern
- `ImportCommand.java` (identity-cli) - Key import with validation

### Base Classes and Infrastructure
- `WalletServiceCommand.java` - Base class for simple wallet commands
- `WalletCliConfiguration.java` - Custom PicoCLI IFactory for DI

### Configuration
- `wallet-cli/pom.xml` - Maven build configuration
- Dependency structure showing wallet-core integration

## PicoCLI Framework

The CLI uses **PicoCLI** (https://picocli.info/) for command-line parsing:

Key annotations used:
- `@Command` - Defines a command
- `@Option` - Defines command options (flags)
- `@Parameters` - Defines positional parameters
- `@ParentCommand` - Access parent command context
- `@Spec` - Inject CommandSpec for command context

Command interfaces:
- `Runnable` - For simple commands
- `Callable<Integer>` - For commands that return exit codes

## Command Architecture Summary

### Registration Pattern
Commands are registered in the main CLI class via `@Command.subcommands`:

```java
@Command(
    name = "wallet",
    subcommands = {Init.class, BalanceCmd.class, SendCmd.class, ...}
)
public class WalletMain implements Runnable { }
```

### Dependency Injection Pattern
Custom `CommandLine.IFactory` implementation detects constructor signatures:

```java
public <K> K create(Class<K> targetClass) throws Exception {
    if (hasConstructor(targetClass, SendUseCase.class)) {
        return targetClass.getDeclaredConstructor(SendUseCase.class)
            .newInstance((SendUseCase) ensureApplicationService());
    }
    // ... more patterns
}
```

### Command Execution Flow
```
WalletMain.main(args)
  → WalletCliConfiguration.fromDefaults()
  → WalletCliConfiguration.commandLine()
  → commandLine.execute(args)
  → PicoCLI parses and routes to subcommand
  → Custom factory injects dependencies
  → Command.run() executes
  → Output to stdout/stderr
  → System.exit(code)
```

## Existing Features (Not NUT-13)

Current wallet commands available:
- `init` - Initialize wallet
- `balance` - Show balances per unit
- `info` - Fetch mint information
- `mint`, `melt` - Cashu operations
- `mint-quote`, `melt-quote` - Request quotes
- `send`, `receive` - Send/receive proofs
- `consolidate` - Consolidate proofs
- `status`, `verify` - Wallet status and verification
- `encrypt`, `change-passphrase` - Encryption management
- `list-proofs` - List proofs with filters
- And 10+ more utility commands

## Gaps for NUT-13 Implementation

Features NOT currently implemented:
- Mnemonic/seed phrase generation and recovery
- Wallet state backup/restore
- BIP-32 derivation path support
- NUT-13 voucher operations
- Recovery from keyset rotation
- Wallet state portability

## Recommended Implementation Approach

1. **Follow existing patterns**: Use `@Command`, `WalletServiceCommand` base class, constructor injection
2. **Create service layer**: Implement `RecoveryService`, `BackupService` in wallet-core-app
3. **Use cashu-lib dependencies**: DerivationPath, KeysetId, BIP-32 utilities
4. **Register commands**: Add `RecoverCmd`, `BackupCmd`, `RestoreCmd`, `VoucherCmd` to WalletMain
5. **Update DI**: Add constructor detection patterns in WalletCliConfiguration
6. **Error handling**: Follow existing error handling patterns with user-friendly messages
7. **Output support**: Implement `--json` flag support for machine readability
8. **Testing**: Unit test commands with mocked services, integration tests with real services

## File Locations

### Project Root
- `/home/eric/IdeaProjects/cashu-client` - Cashu client project

### Wallet CLI
- `/home/eric/IdeaProjects/cashu-client/wallet-cli/src/main/java/xyz/tcheeric/wallet/cli/` - CLI source
- `/home/eric/IdeaProjects/cashu-client/wallet-cli/src/main/java/xyz/tcheeric/wallet/cli/commands/` - Command implementations

### Identity CLI
- `/home/eric/IdeaProjects/cashu-client/identity-cli/src/main/java/xyz/tcheeric/identity/cli/` - Identity CLI source
- `/home/eric/IdeaProjects/cashu-client/identity-cli/src/main/java/xyz/tcheeric/identity/cli/commands/` - Identity commands

### Wallet Core
- `/home/eric/IdeaProjects/cashu-client/wallet-plugin/wallet-core-app/` - Application services
- `/home/eric/IdeaProjects/cashu-client/wallet-plugin/wallet-core-base/` - Base classes

### Documentation
- `/home/eric/IdeaProjects/cashu-client/DEVELOPMENT.md` - Development tracker
- Generated docs:
  - `/home/eric/IdeaProjects/cashu-lib/project/CASHU_CLIENT_CLI_STRUCTURE.md`
  - `/home/eric/IdeaProjects/cashu-lib/project/NUT13_CLI_INTEGRATION_GUIDE.md`

## Next Steps

1. **Review** the generated documentation (CASHU_CLIENT_CLI_STRUCTURE.md)
2. **Study** example commands: SendCmd, EncryptCmd, GenerateCommand
3. **Understand** DI pattern: WalletCliConfiguration.create()
4. **Design** NUT-13 commands using templates from guide
5. **Implement** services in wallet-core-app
6. **Register** commands in WalletMain
7. **Test** with unit and integration tests

## Notes

- The codebase is well-organized and follows clear patterns
- Error handling is comprehensive with user-friendly messages
- Support for both plain text and JSON output is standard
- Security is considered (encryption, passphrase handling)
- Architecture supports incremental feature addition
- Identity management provides key generation/import patterns to reference

## Related Projects

From DEVELOPMENT.md references:
- cashu-lib: Core library with DerivationPath, KeysetId
- cashu-mint: Backend mint server
- cashu-vault: Vault implementation
- cashu-gateway: Gateway service
- phoenixd-java: Lightning integration
- nostr-cashu: Nostr protocol integration
