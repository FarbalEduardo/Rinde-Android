# Fix Replies Not Showing in Post Detail

The user reported that replies to comments are successfully uploaded to Firebase but are not visible in the `PostDetail` screen. Investigation revealed that the `repliesCount` for comments is not being incremented correctly because the `postId` is missing in the `Reply` and `Comment` objects during creation in their respective UseCases.

## User Review Required

> [!IMPORTANT]
> The fix will correctly increment `repliesCount` for **new** replies. Existing comments that already have replies but a `repliesCount` of 0 in Firebase will still not show the "Ver respuestas" button unless their `repliesCount` is manually corrected in the database or a fallback is added to the UI.

## Proposed Changes

### Domain Layer

#### [MODIFY] [AddCommentUseCase.kt](file:///D:/PROYECTOSANDROID/Rinde/app/src/main/java/com/farbalapps/rinde/domain/usecase/AddCommentUseCase.kt)
- Set the `postId` field when creating the `Comment` object.

#### [MODIFY] [AddReplyUseCase.kt](file:///D:/PROYECTOSANDROID/Rinde/app/src/main/java/com/farbalapps/rinde/domain/usecase/AddReplyUseCase.kt)
- Set both `commentId` and `postId` fields when creating the `Reply` object.

---

### UI Layer (Optional/Fallback)

#### [MODIFY] [SharedCommentThread.kt](file:///D:/PROYECTOSANDROID/Rinde/app/src/main/java/com/farbalapps/rinde/ui/screen/home/community/components/SharedCommentThread.kt)
- As a safety measure, I could add a way to load replies even if `repliesCount` is 0, but fixing the data flow is preferred. I will stick to fixing the UseCases first.

## Verification Plan

### Automated Tests
- I will check if there are existing tests for these UseCases and update them if necessary, or create new ones to ensure the objects are created with the correct IDs.

### Manual Verification
1. Add a new comment to a post.
2. Add a reply to that comment.
3. Verify that the "Ver 1 respuesta" button appears.
4. Verify that clicking the button loads and displays the reply.
