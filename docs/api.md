# REST API Documentation

Base URL: `http://localhost:8080` (via API Gateway)

All authenticated endpoints require an `Authorization: Bearer <JWT_ACCESS_TOKEN>` header.

---

## 1. Authentication (`user-service`)

### POST `/api/v1/auth/register`
Create a new user account.
```json
{
  "username": "alex",
  "email": "alex@example.com",
  "password": "Password123!",
  "fullName": "Alex Rivera"
}
```

### POST `/api/v1/auth/login`
Authenticate user and return JWT tokens.
```json
{
  "emailOrUsername": "alex@example.com",
  "password": "Password123!"
}
```

### POST `/api/v1/auth/refresh`
Rotate refresh token and issue a fresh short-lived access token.
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsIn..."
}
```

---

## 2. Workbooks & Sheets (`sheet-service`)

### GET `/api/v1/workbooks`
Retrieve all workbooks owned or shared with the current user.

### POST `/api/v1/workbooks`
Create a new workbook.
```json
{
  "title": "Q3 Financial Model",
  "visibility": "PRIVATE"
}
```

### GET `/api/v1/workbooks/{id}`
Retrieve workbook details with sheet hierarchy.

### POST `/api/v1/workbooks/{id}/sheets`
Add a sheet to a workbook.
```json
{
  "name": "Expenses",
  "rowCount": 100,
  "colCount": 26
}
```

### GET `/api/v1/sheets/{sheetId}/cells`
Retrieve all initialized cells for a sheet.

### POST `/api/v1/sheets/{sheetId}/export/xlsx`
Export entire workbook to Excel `.xlsx` format.

### POST `/api/v1/sheets/{sheetId}/export/csv`
Export active sheet to CSV format.

### POST `/api/v1/workbooks/import`
Import `.xlsx` or `.csv` multipart file to create a workbook.

---

## 3. Comments (`comment-service`)

### POST `/api/v1/sheets/{sheetId}/comments`
Create a cell comment thread.
```json
{
  "row": 4,
  "col": 2,
  "content": "Please verify this Q3 forecast @maria"
}
```

### POST `/api/v1/comments/{threadId}/reply`
Add a reply to an existing comment thread.

### POST `/api/v1/comments/{threadId}/resolve`
Resolve a comment thread.

---

## 4. Audit & Version History (`audit-service`)

### GET `/api/v1/audit/workbook/{workbookId}`
Get paginated changelog of all cell edits and structure changes.

### POST `/api/v1/audit/workbook/{workbookId}/snapshot`
Create a named version snapshot checkpoint.
```json
{
  "label": "Approved Final Version",
  "description": "Signed off by finance"
}
```

### POST `/api/v1/audit/workbook/{workbookId}/restore`
Compute point-in-time state restoration.
