# Design Document: Email Password Authentication

## Overview

This design document specifies the technical implementation of email and password authentication for the Rinde Android application. The system follows Clean Architecture principles and the MVVM pattern using Kotlin and Jetpack Compose.

The authentication system provides a login screen displayed on first launch, validates user credentials, manages authentication state, and persists sessions securely. The implementation is structured in three distinct layers (data, domain, and UI) to ensure separation of concerns, testability, and maintainability.

### Key Design Goals

- Implement Clean Architecture with clear layer boundaries
- Follow MVVM pattern for UI layer
- Ensure secure credential handling and storage
- Provide comprehensive input validation
- Support accessibility requirements
- Enable session persistence across app restarts

## Architecture

The system follows Clean Architecture with three main layers:

### Layer Structure

```
┌─────────────────────────────────────────────────────────┐
│                      UI Layer                           │
│  ┌──────────────┐         ┌─────────────────┐         │
│  │ LoginScreen  │────────▶│ LoginViewModel  │         │
│  │ (Composable) │         │                 │         │
│  └──────────────┘         └─────────────────┘         │
└────────────────────────────────┬────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────┐
│                    Domain Layer                         │
│  ┌──────────────────┐    ┌──────────────────┐         │
│  │ LoginUseCase     │    │ ValidateEmail    │         │
│  │                  │    │ UseCase          │         │
│  └──────────────────┘    └──────────────────┘         │
│  ┌──────────────────┐    ┌──────────────────┐         │
│  │ ValidatePassword │    │ GetAuthState     │         │
│  │ UseCase          │    │ UseCase          │         │
│  └──────────────────┘    └──────────────────┘         │
│                                                         │
│  ┌──────────────────────────────────────────┐         │
│  │ AuthRepository (Interface)               │         │
│  └──────────────────────────────────────────┘         │
└────────────────────────────────┬────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────┐
│                     Data Layer                          │
│  ┌──────────────────────────────────────────┐         │
│  │ AuthRepositoryImpl                       │         │
│  └──────────────────────────────────────────┘         │
│           │                        │                   │
│           ▼                        ▼                   │
│  ┌─────────────────┐    ┌─────────────────┐          │
│  │ RemoteDataSource│    │ LocalDataSource │          │
│  │ (API)           │    │ (Encrypted      │          │
│  │                 │    │  SharedPrefs)   │          │
│  └─────────────────┘    └─────────────────┘          │
└─────────────────────────────────────────────────────────┘
```

### Dependency Rule

- UI Layer depends on Domain Layer
- Domain Layer has no dependencies on other layers
- Data Layer depends on Domain Layer (implements interfaces)
- Dependencies point inward (toward domain)

### Navigation Flow

```
App Launch
    │
    ▼
Check Authentication State
    │
    ├─── Authenticated ────▶ Main Screen
    │
    └─── Not Authenticated ─▶ Login Screen
                                  │
                                  ▼
                            User Enters Credentials
                                  │
                                  ▼
                            Validation Passes
                                  │
                                  ▼
                            Authentication Success
                                  │
                                  ▼
                            Store Token
                                  │
                                  ▼
                            Navigate to Main Screen
```

## Components and Interfaces

### UI Layer

#### LoginScreen (Composable)

Jetpack Compose UI component that displays the login interface.

**Responsibilities:**
- Render email and password input fields
- Display validation errors
- Show loading state during authentication
- Handle user interactions
- Announce accessibility events

**State Observation:**
- Observes `LoginUiState` from `LoginViewModel`

**User Actions:**
- Email text changed
- Password text changed
- Login button clicked

#### LoginViewModel

ViewModel that manages UI state and coordinates use cases.

**Properties:**
```kotlin
val uiState: StateFlow<LoginUiState>
```

**Methods:**
```kotlin
fun onEmailChanged(email: String)
fun onPasswordChanged(password: String)
fun onLoginClicked()
```

**Dependencies:**
- `LoginUseCase`
- `ValidateEmailUseCase`
- `ValidatePasswordUseCase`

#### LoginUiState

Data class representing the UI state.

```kotlin
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val isLoginEnabled: Boolean = false,
    val authError: String? = null
)
```

### Domain Layer

#### LoginUseCase

Use case that orchestrates the authentication process.

**Method:**
```kotlin
suspend operator fun invoke(email: String, password: String): Result<AuthToken>
```

**Dependencies:**
- `AuthRepository` (interface)

**Behavior:**
- Delegates authentication to repository
- Returns success with token or failure with error

#### ValidateEmailUseCase

Use case that validates email format against RFC 5322.

**Method:**
```kotlin
operator fun invoke(email: String): ValidationResult
```

**Validation Rules:**
- Must match RFC 5322 email format
- Uses regex pattern for validation

**Returns:**
```kotlin
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}
```

#### ValidatePasswordUseCase

Use case that validates password requirements.

**Method:**
```kotlin
operator fun invoke(password: String): ValidationResult
```

**Validation Rules:**
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit

#### GetAuthStateUseCase

Use case that checks current authentication state.

**Method:**
```kotlin
suspend operator fun invoke(): AuthState
```

**Returns:**
```kotlin
sealed class AuthState {
    data class Authenticated(val token: AuthToken) : AuthState()
    object NotAuthenticated : AuthState()
}
```

#### AuthRepository (Interface)

Repository interface for authentication operations.

**Methods:**
```kotlin
suspend fun login(email: String, password: String): Result<AuthToken>
suspend fun getStoredToken(): AuthToken?
suspend fun saveToken(token: AuthToken)
suspend fun clearToken()
suspend fun isTokenValid(token: AuthToken): Boolean
```

### Data Layer

#### AuthRepositoryImpl

Implementation of `AuthRepository` interface.

**Dependencies:**
- `RemoteDataSource` (API client)
- `LocalDataSource` (secure storage)

**Behavior:**
- Coordinates between remote and local data sources
- Handles error mapping from data layer to domain layer
- Ensures HTTPS-only communication

#### RemoteDataSource

Handles network communication for authentication.

**Methods:**
```kotlin
suspend fun authenticate(email: String, password: String): ApiResponse<TokenDto>
```

**Implementation:**
- Uses Retrofit or Ktor for HTTP client
- Enforces HTTPS-only connections
- Maps API responses to domain models

#### LocalDataSource

Handles secure local storage of authentication tokens.

**Methods:**
```kotlin
suspend fun saveToken(token: String)
suspend fun getToken(): String?
suspend fun clearToken()
```

**Implementation:**
- Uses `EncryptedSharedPreferences` for API 23+
- Uses Android Keystore for encryption keys
- Never stores passwords

## Data Models

### Domain Models

#### AuthToken

```kotlin
data class AuthToken(
    val value: String,
    val expiresAt: Long
)
```

Represents an authentication token with expiration.

#### User

```kotlin
data class User(
    val id: String,
    val email: String
)
```

Represents an authenticated user (future use).

### Data Transfer Objects (DTOs)

#### LoginRequestDto

```kotlin
data class LoginRequestDto(
    val email: String,
    val password: String
)
```

Request payload for authentication API.

#### TokenResponseDto

```kotlin
data class TokenResponseDto(
    val token: String,
    val expiresIn: Long
)
```

Response from authentication API.

### Error Models

#### AuthError

```kotlin
sealed class AuthError : Exception() {
    object InvalidCredentials : AuthError()
    object NetworkError : AuthError()
    object ServerError : AuthError()
    object TokenExpired : AuthError()
}
```

Domain-level authentication errors.

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*


### Property 1: Email Format Validation

*For any* string input, the email validator should return valid only if the string matches RFC 5322 email format, and invalid otherwise.

**Validates: Requirements 2.1**

### Property 2: Password Requirements Validation

*For any* string input, the password validator should return valid only if the string has at least 8 characters AND contains at least one uppercase letter AND at least one lowercase letter AND at least one digit.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4**

### Property 3: Login Button State

*For any* combination of email and password inputs, the login button should be enabled if and only if both the email validation returns valid AND the password validation returns valid.

**Validates: Requirements 2.3, 2.5, 3.6**

### Property 4: Authentication Initiation

*For any* valid email and password combination, when the login button is clicked, the authentication use case should be invoked with those credentials.

**Validates: Requirements 4.1**

### Property 5: Loading State During Authentication

*For any* authentication attempt, while the authentication is in progress, the UI state should have isLoading set to true and all inputs should be disabled.

**Validates: Requirements 4.2, 4.3**

### Property 6: Token Storage Round-Trip

*For any* authentication token, if the token is saved to storage, then retrieving from storage should return an equivalent token value.

**Validates: Requirements 4.5, 6.5, 7.4**

### Property 7: Navigation on Authentication Success

*For any* successful authentication result, the system should navigate to the main screen and the navigation state should no longer point to the login screen.

**Validates: Requirements 4.6**

### Property 8: UI Recovery After Authentication Error

*For any* authentication error (invalid credentials, network error, or server error), the UI state should have isLoading set to false and all inputs should be re-enabled.

**Validates: Requirements 5.4, 5.5**

### Property 9: Navigation Based on Token Validity

*For any* stored authentication token, if the token is valid and not expired, then the initial navigation should go to the main screen; if the token is invalid or expired, then the initial navigation should go to the login screen.

**Validates: Requirements 7.2, 7.3**

### Property 10: Email Error Display

*For any* invalid email input, the UI state should contain a non-null email error message.

**Validates: Requirements 2.2**

### Property 11: Email Error Clearing

*For any* transition from an invalid email to a valid email, the UI state should have the email error message set to null.

**Validates: Requirements 2.4**

### Property 12: Password Error Display

*For any* invalid password input, the UI state should contain a non-null password error message.

**Validates: Requirements 3.5**

## Error Handling

### Error Categories

The system handles three categories of authentication errors:

1. **Validation Errors**: Client-side validation failures
   - Invalid email format
   - Password doesn't meet requirements
   - Handled synchronously before API call

2. **Authentication Errors**: Server-side authentication failures
   - Invalid credentials (401)
   - Handled after API response
   - Generic error message for security

3. **System Errors**: Infrastructure failures
   - Network connectivity issues
   - Server errors (5xx)
   - Timeout errors

### Error Flow

```
User Input
    │
    ▼
Validation
    │
    ├─── Invalid ────▶ Display Validation Error
    │                 (No API Call)
    │
    └─── Valid ──────▶ Call API
                          │
                          ├─── 401 ────▶ Display "Invalid email or password"
                          │
                          ├─── Network Error ──▶ Display "Connection problem"
                          │
                          ├─── 5xx ────▶ Display "Service temporarily unavailable"
                          │
                          └─── Success ──▶ Store Token & Navigate
```

### Error Messages

All error messages are user-facing and localized:

- **Email Validation**: "Please enter a valid email address"
- **Password Length**: "Password must be at least 8 characters"
- **Password Uppercase**: "Password must contain at least one uppercase letter"
- **Password Lowercase**: "Password must contain at least one lowercase letter"
- **Password Digit**: "Password must contain at least one digit"
- **Invalid Credentials**: "Invalid email or password" (generic for security)
- **Network Error**: "Unable to connect. Please check your internet connection"
- **Server Error**: "Service temporarily unavailable. Please try again later"

### Error Recovery

- Validation errors: User can immediately correct input
- Authentication errors: All inputs remain editable, user can retry
- Network errors: User can retry when connection is restored
- No automatic retry mechanism (user-initiated only)

### Error Logging

- Validation errors: Not logged (expected user behavior)
- Authentication errors: Log error type and timestamp (no credentials)
- System errors: Log full error details for debugging
- Never log passwords or tokens

## Testing Strategy

### Dual Testing Approach

The authentication system requires both unit tests and property-based tests for comprehensive coverage:

- **Unit tests**: Verify specific examples, edge cases, and integration points
- **Property-based tests**: Verify universal properties across randomized inputs

### Property-Based Testing

**Framework**: Kotest Property Testing for Kotlin

**Configuration**:
- Minimum 100 iterations per property test
- Each test tagged with feature name and property reference
- Tag format: `Feature: email-password-authentication, Property {number}: {property_text}`

**Property Test Coverage**:

1. **Email Validation Property** (Property 1)
   - Generate random valid and invalid email strings
   - Verify validator correctly identifies format compliance
   - Include edge cases: empty, special characters, multiple @, etc.

2. **Password Validation Property** (Property 2)
   - Generate random passwords with varying characteristics
   - Verify all four requirements are enforced
   - Include edge cases: exactly 8 chars, all uppercase, all lowercase, etc.

3. **Login Button State Property** (Property 3)
   - Generate random email/password combinations
   - Verify button state matches validation results
   - Test all four combinations: both valid, both invalid, mixed

4. **Token Storage Round-Trip Property** (Property 6)
   - Generate random token strings
   - Verify save then retrieve returns equivalent value
   - Test with various token formats and lengths

5. **UI State Properties** (Properties 5, 8, 10, 11, 12)
   - Generate random UI state transitions
   - Verify state updates match expected behavior
   - Test error states, loading states, validation states

### Unit Testing

**Focus Areas**:

1. **Specific Examples**
   - Login screen displays required UI elements (1.3, 1.4, 1.5)
   - First launch shows login screen (1.1)
   - Specific error messages for each error type (5.1, 5.2, 5.3)
   - Generic error message for invalid credentials (5.6)

2. **Security Requirements**
   - Password masking in UI (6.1)
   - HTTPS-only configuration (6.2)
   - EncryptedSharedPreferences usage (6.4)

3. **Accessibility**
   - Content descriptions present (10.1)
   - Minimum touch target sizes (10.3)
   - Color contrast ratios (10.4)
   - Error announcements (10.5)

4. **Integration Tests**
   - ViewModel coordinates use cases correctly
   - Repository coordinates data sources correctly
   - Navigation flow from login to main screen
   - Token validation on app restart

### Test Organization

```
test/
├── domain/
│   ├── usecase/
│   │   ├── LoginUseCaseTest.kt (unit)
│   │   ├── ValidateEmailUseCaseTest.kt (unit + property)
│   │   ├── ValidatePasswordUseCaseTest.kt (unit + property)
│   │   └── GetAuthStateUseCaseTest.kt (unit)
│   └── repository/
│       └── AuthRepositoryTest.kt (unit)
├── data/
│   ├── repository/
│   │   └── AuthRepositoryImplTest.kt (unit)
│   └── source/
│       ├── RemoteDataSourceTest.kt (unit)
│       └── LocalDataSourceTest.kt (unit + property)
└── ui/
    ├── viewmodel/
    │   └── LoginViewModelTest.kt (unit + property)
    └── screen/
        └── LoginScreenTest.kt (unit + accessibility)
```

### Testing Guidelines

- Property tests handle comprehensive input coverage
- Unit tests focus on specific scenarios and integration
- Mock external dependencies (API, storage) in unit tests
- Use test doubles for use cases in ViewModel tests
- Test accessibility with Compose testing utilities
- Verify security configurations in integration tests

### Continuous Testing

- Run unit tests on every commit
- Run property tests in CI pipeline
- Minimum 80% code coverage for domain layer
- Minimum 70% code coverage for data and UI layers
- All properties must pass before merge

