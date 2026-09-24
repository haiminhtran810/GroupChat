# GroupChat Architecture Overview

This document describes the high-level architecture of the **GroupChat** Android application. The project is structured using **Clean Architecture** principles and leverages modern Android development tools including **Jetpack Compose** (with Adaptive Layouts), **Coroutines/Flows**, and **Manual Dependency Injection**.

It simulates an End-to-End Encrypted (E2EE) messaging system.

## High-Level Architecture

The application is divided into three primary layers to separate concerns, improve testability, and keep the UI independent of data sources and business logic:

1.  **UI / Presentation Layer**: Handles rendering the UI and user interactions.
2.  **Domain Layer**: Contains the core business logic, use cases, and enterprise models.
3.  **Data Layer**: Manages data retrieval and storage, implementing the repository interfaces defined in the Domain layer.

### System Diagram

```mermaid
graph TD
    subgraph UI [UI / Presentation Layer]
        MainAdaptiveApp
        ConversationListScreen
        ChatScreen
        KeyVerificationScreen
        ConversationListViewModel
        ChatViewModel
        KeyVerificationViewModel
    end

    subgraph Domain [Domain Layer]
        ChatRepository_Interface((ChatRepository))
        ContactRepository_Interface((ContactRepository))
        SecurityRepository_Interface((SecurityRepository))
        
        EncryptMessageUseCase
        DecryptMessageUseCase
        GenerateSafetyNumberUseCase
        
        Models[Domain Models: Message, Contact, Conversation]
    end

    subgraph Data [Data Layer]
        ChatRepositoryImpl
        ContactRepositoryImpl
        SecurityRepositoryImpl
        
        LocalMessageDataSource[(Local Messages)]
        LocalContactDataSource[(Local Contacts)]
        PeerMessagingDataSource((Peer Messaging API))
    end
    
    subgraph Crypto [Security / Crypto]
        CryptoManager
        KeyStoreManager
        SafetyNumberGenerator
    end

    %% UI to Domain
    ConversationListScreen --> ConversationListViewModel
    ChatScreen --> ChatViewModel
    KeyVerificationScreen --> KeyVerificationViewModel
    
    ConversationListViewModel --> ChatRepository_Interface
    ConversationListViewModel --> ContactRepository_Interface
    ChatViewModel --> ChatRepository_Interface
    ChatViewModel --> ContactRepository_Interface
    ChatViewModel --> SecurityRepository_Interface
    KeyVerificationViewModel --> SecurityRepository_Interface
    KeyVerificationViewModel --> ContactRepository_Interface

    %% Domain to Crypto
    ChatRepository_Interface -.-> EncryptMessageUseCase
    ChatRepository_Interface -.-> DecryptMessageUseCase

    %% Data implements Domain
    ChatRepositoryImpl -.->|Implements| ChatRepository_Interface
    ContactRepositoryImpl -.->|Implements| ContactRepository_Interface
    SecurityRepositoryImpl -.->|Implements| SecurityRepository_Interface

    %% Data to Data Sources
    ChatRepositoryImpl --> LocalMessageDataSource
    ChatRepositoryImpl --> PeerMessagingDataSource
    ChatRepositoryImpl --> CryptoManager
    ContactRepositoryImpl --> LocalContactDataSource
    SecurityRepositoryImpl --> KeyStoreManager
    SecurityRepositoryImpl --> SafetyNumberGenerator
    
    %% Crypto internals
    EncryptMessageUseCase --> CryptoManager
    DecryptMessageUseCase --> CryptoManager
    GenerateSafetyNumberUseCase --> SafetyNumberGenerator
```

## Layer Breakdown

### 1. UI / Presentation Layer
-   **Jetpack Compose**: The entire UI is built using Jetpack Compose.
-   **Adaptive Layouts**: Uses `androidx.compose.adaptive` (`ListDetailPaneScaffold`) to automatically adjust the layout for different screen sizes (e.g., side-by-side list and detail on tablets, single screen on phones).
-   **ViewModels**: Each screen has a corresponding ViewModel (e.g., `ChatViewModel`) that manages UI state using `StateFlow`. ViewModels interact directly with Domain Repositories and map the results to UI states.

### 2. Domain Layer
-   **Models**: Pure Kotlin data classes representing core concepts (`Message`, `Contact`, `Conversation`, `EncryptedPayload`, `SafetyNumber`).
-   **Interfaces**: Defines repository interfaces (`ChatRepository`, `ContactRepository`, `SecurityRepository`) to apply the Dependency Inversion principle.
-   **Use Cases / Crypto**: Encapsulates specific business rules. `EncryptMessageUseCase` and `DecryptMessageUseCase` manage the simulated E2EE logic.

### 3. Data Layer
-   **Repositories**: Implementations of Domain interfaces (`ChatRepositoryImpl`, etc.). They orchestrate data between local storage and the network.
-   **Data Sources**:
    -   `LocalMessageDataSource` / `LocalContactDataSource`: Simulates local database storage for caching messages and contacts.
    -   `PeerMessagingDataSource`: Simulates a real-time network layer (like WebSockets or WebRTC) using Kotlin `Channel` and `Flow`. It handles "sending" messages and mocking "incoming" encrypted replies.

### 4. Security & Cryptography Module
Handles the End-to-End Encryption simulation:
-   **`KeyStoreManager`**: Manages generating, storing, and retrieving asymmetric keys (RSA) for users.
-   **`CryptoManager`**: Handles the actual encryption and decryption of message payloads.
-   **`SafetyNumberGenerator`**: Generates a reproducible fingerprint (safety number) from public keys, allowing users to verify identities (similar to Signal/WhatsApp).

## Message Flow Example

The following sequence diagram shows the flow of sending a message, from the UI down to the simulated network, demonstrating the encryption step.

```mermaid
sequenceDiagram
    actor User
    participant ChatScreen
    participant ChatViewModel
    participant ChatRepositoryImpl
    participant CryptoManager
    participant PeerMessagingDataSource
    
    User->>ChatScreen: Types message & taps Send
    ChatScreen->>ChatViewModel: sendMessage(text)
    ChatViewModel->>ChatRepositoryImpl: sendMessage(text, recipientId)
    
    Note over ChatRepositoryImpl, CryptoManager: 1. Get Recipient's Public Key
    ChatRepositoryImpl->>CryptoManager: encrypt(text, recipientPublicKey)
    CryptoManager-->>ChatRepositoryImpl: EncryptedPayload (ciphertext)
    
    Note over ChatRepositoryImpl: 2. Create Message Object
    
    ChatRepositoryImpl->>PeerMessagingDataSource: sendOverNetwork(Message)
    PeerMessagingDataSource-->>ChatRepositoryImpl: MessageStatus.SENT
    
    Note over ChatRepositoryImpl: 3. Save locally
    ChatRepositoryImpl-->>ChatViewModel: update Flow
    ChatViewModel-->>ChatScreen: Render New Message
```

## Dependency Injection
The project uses **Manual Dependency Injection** via an `AppContainer` class. This container creates and provides singletons of the repositories, data sources, and managers to the ViewModels, keeping the architecture decoupled without the overhead of Dagger/Hilt for this specific scope.
