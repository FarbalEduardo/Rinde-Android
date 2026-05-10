# Best Practices: Firebase & Coroutines

## ⚠️ The "Fire-and-Forget" Pitfall
In Android, Firebase operations (Firestore, Auth, Storage) return a `com.google.android.gms.tasks.Task`. If called inside a coroutine without awaiting its result, the coroutine may complete before the asynchronous Firebase operation finishes, leading to data loss or inconsistent states.

### ❌ Incorrect Pattern (Silent failure)
```kotlin
// Failure: .set() is async and returns a Task that is never awaited.
suspend fun addItem(item: Item) {
    firestore.collection("items").document(item.id).set(item)
}
```

### ✅ Correct Pattern (Using await)
Requires the dependency: `org.jetbrains.kotlinx:kotlinx-coroutines-play-services`.

```kotlin
import kotlinx.coroutines.tasks.await

suspend fun addItem(item: Item) = withContext(Dispatchers.IO) {
    try {
        firestore.collection("items")
            .document(item.id)
            .set(item)
            .await() // Properly waits for the operation to complete
    } catch (e: Exception) {
        // Handle sync failure
    }
}
```

## Audit Rules
1. **Never** use `.set()`, `.update()`, or `.delete()` inside a `suspend` function or `viewModelScope.launch` without:
   - Calling `.await()` (preferred).
   - Attaching `.addOnCompleteListener` or similar (only if not using coroutines).
2. Always wrap repository writes in `withContext(Dispatchers.IO)`.
