---
description: create a new feature following Clean Architecture
---
1. Create the feature directory in `ui`.
2. Define the `Domain` layer:
   - Create Use Cases.
   - Define Repository interface.
3. Define the `Data` layer:
   - Implement Repository.
   - Add Room/Firebase logic.
4. Define the `UI` layer:
   - Create Compose Screen.
   - Create ViewModel.
5. Register in DI (Hilt).
