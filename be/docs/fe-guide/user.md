# User Domain & Legal Profiles - FE Guide

Tài liệu hướng dẫn tích hợp Frontend cho module **User Domain & Hồ sơ pháp lý** (`vn.mentify.be.modules.user`).

---

## 1. Tổng quan & Bảo mật

- **Base URLs**: Hỗ trợ đồng thời cả hai prefix:
  - Chuẩn: `/api/v1/users`
  - Alias: `/api/users`
- **Xác thực (Authentication)**: Tất cả API thuộc module này đều yêu cầu người dùng **đã xác thực** bằng JWT Bearer token (`Authorization: Bearer <token>`).
  - Chưa gửi token hoặc token hết hạn sẽ nhận phản hồi lỗi: `401 Unauthorized` (`UNAUTHENTICATED`).
- **Định dạng dữ liệu**:
  - Request format: `Content-Type: application/json`
  - Response envelope: Chuẩn `ApiSuccessResponse` với `timestamp` offset `+07:00` (Asia/Ho_Chi_Minh) và `requestId`.

---

## 2. API Endpoints

### 2.1. Lấy thông tin cá nhân & Hồ sơ (Get Profile)

- **Method**: `GET`
- **Path**: `/api/v1/users/profile` (hoặc `/api/users/profile`)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Mô tả**: Trích xuất thông tin tài khoản và chi tiết hồ sơ cá nhân của người dùng hiện tại từ JWT context.

#### Response thành công (`200 OK`):

```json
{
  "success": true,
  "data": {
    "id": "2d263f40-8cef-4390-bb43-e524f571e1b9",
    "email": "student@mentify.vn",
    "userType": "SCHOOL_STUDENT",
    "role": "USER",
    "accountStatus": "ACTIVE",
    "fullName": "Nguyễn Văn An",
    "bio": "Học sinh THPT đam mê công nghệ phần mềm",
    "avatarUrl": "https://cdn.mentify.vn/avatars/an.png",
    "school": {
      "id": "e229d494-b789-4fa2-bceb-fcead6151740",
      "code": "QSK",
      "admissionCode": "QSK",
      "name": "Trường Đại học Bách khoa - ĐHQG TP.HCM",
      "shortName": "ĐH Bách khoa TP.HCM",
      "commonAbbreviation": "HCMUT",
      "campusType": "MAIN_CAMPUS",
      "parentSchoolId": null,
      "region": "TP.HCM",
      "status": "ACTIVE",
      "dataVersion": "2026"
    },
    "majorGroup": {
      "id": "c1f7a810-745a-4062-8178-5dbf9e1f57b1",
      "code": "4801",
      "name": "Máy tính",
      "fieldCode": "48",
      "regulationVersion": "TT_74_2026",
      "description": "MAY TINH VA CONG NGHE THONG TIN",
      "isActive": true
    },
    "customMajorName": "Kỹ thuật phần mềm"
  },
  "meta": null,
  "timestamp": "2026-10-01T20:30:00+07:00",
  "requestId": "90cf85b2-29ee-473d-82d8-79e51c890bd9"
}
```

> [!NOTE]
> Khi tài khoản mới tạo và chưa cập nhật profile, các trường `fullName`, `bio`, `avatarUrl`, `school`, `majorGroup`, `customMajorName` sẽ mang giá trị `null`.

---

### 2.2. Cập nhật hồ sơ cá nhân & Thăng hạng UserType (Update Profile)

- **Method**: `PUT`
- **Path**: `/api/v1/users/profile` (hoặc `/api/users/profile`)
- **Headers**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **Mô tả**: Cập nhật thông tin profile của người dùng và chuyển đổi `userType` nếu có nhu cầu.

#### Request Body (`UpdateProfileRequest`):

```json
{
  "fullName": "Nguyễn Văn An",
  "bio": "Học sinh THPT đam mê công nghệ phần mềm",
  "avatarUrl": "https://cdn.mentify.vn/avatars/an.png",
  "schoolId": "e229d494-b789-4fa2-bceb-fcead6151740",
  "majorGroupCode": "4801",
  "customMajorName": "Kỹ thuật phần mềm",
  "newUserType": "UNIVERSITY_STUDENT"
}
```

#### Quy tắc trường (Validation Rules):
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc / Mô tả |
| :--- | :--- | :---: | :--- |
| `fullName` | `String` | Không | Tối đa 128 ký tự |
| `bio` | `String` | Không | Tối đa 1000 ký tự |
| `avatarUrl` | `String` | Không | Tối đa 512 ký tự |
| `schoolId` | `UUID` | Không | Phải tồn tại trong danh mục `schools` |
| `majorGroupCode` | `String` | Không | Tối đa 4 ký tự; phải tồn tại trong danh mục `major_groups` |
| `customMajorName`| `String` | Không | Tối đa 255 ký tự; tên ngành cụ thể tự nhập |
| `newUserType` | `String` | Không | `SCHOOL_STUDENT`, `UNIVERSITY_STUDENT`, `WORKING_PROFESSIONAL` |

> [!IMPORTANT]
> **Quy tắc chuyển đổi `UserType` một chiều (Bắt buộc)**:
> - Cấp bậc chỉ được phép **giữ nguyên hoặc tăng lên**:
>   $$\text{SCHOOL\_STUDENT (0)} \longrightarrow \text{UNIVERSITY\_STUDENT (1)} \longrightarrow \text{WORKING\_PROFESSIONAL (2)}$$
> - **Cấm hạ cấp**: Nếu người dùng đang là `WORKING_PROFESSIONAL` mà gửi `newUserType = UNIVERSITY_STUDENT` hoặc `SCHOOL_STUDENT`, API sẽ từ chối với lỗi **409 Conflict** (`CONFLICT`).
> - Chuyển đổi thành công sẽ được hệ thống tự động ghi nhật ký audit vào bảng `user_type_histories`.
> - Việc đổi sang `WORKING_PROFESSIONAL` **không tự động cấp quyền Mentor** (quyền nhận booking cần quy trình đăng ký & duyệt riêng).

#### Response thành công (`200 OK`):
Trả về đối tượng `UserProfileResponse` cập nhật hoàn chỉnh trong `data`.

#### Các mã lỗi thường gặp:
- **`400 VALIDATION_ERROR`**: Dữ liệu vượt quá độ dài quy định.
- **`401 UNAUTHENTICATED`**: Thiếu token, token không hợp lệ hoặc đã hết hạn.
- **`403 FORBIDDEN`**: Không có quyền truy cập thao tác này.
- **`404 RESOURCE_NOT_FOUND`**: `schoolId` hoặc `majorGroupCode` không tồn tại trong danh mục Master Data.
- **`409 CONFLICT`**: Cố tình chuyển lùi loại người dùng.

---

### 2.3. Ghi nhận chấp thuận điều khoản pháp lý (Record Consent)

- **Method**: `POST`
- **Path**: `/api/v1/users/consent` (hoặc `/api/users/consent`)
- **Headers**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **Mô tả**: Lưu vết sự chấp thuận của người dùng đối với các chính sách, điều khoản dịch vụ (TOS), bảo vệ dữ liệu cá nhân (PDPA) hoặc xác nhận của người giám hộ đối với học sinh THPT.

#### Request Body (`RecordConsentRequest`):

```json
{
  "consentType": "DATA_PROTECTION_AND_TOS",
  "policyVersion": "2026.1",
  "guardianName": "Nguyễn Phụ Huynh",
  "guardianContact": "0987654321"
}
```

#### Quy tắc trường (Validation Rules):
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc / Mô tả |
| :--- | :--- | :---: | :--- |
| `consentType` | `String` | **Có** | Tối đa 64 ký tự (ví dụ: `TOS`, `PRIVACY_POLICY`, `DATA_PROTECTION_AND_TOS`) |
| `policyVersion` | `String` | **Có** | Tối đa 32 ký tự (ví dụ: `2026.1`, `v1.0`) |
| `guardianName` | `String` | Phụ thuộc | Tối đa 128 ký tự. Khuyến nghị gửi kèm cho học sinh THPT (`SCHOOL_STUDENT`) |
| `guardianContact`| `String` | Phụ thuộc | Tối đa 128 ký tự (số điện thoại hoặc email liên hệ của người giám hộ) |

#### Response thành công (`200 OK`):

```json
{
  "success": true,
  "data": {
    "id": "713ba40a-d8cb-4ee4-90a6-8167f2b9829e",
    "consentType": "DATA_PROTECTION_AND_TOS",
    "policyVersion": "2026.1",
    "acceptedAt": "2026-10-01T20:30:15.123+07:00",
    "guardianName": "Nguyễn Phụ Huynh",
    "guardianContact": "0987654321"
  },
  "meta": null,
  "timestamp": "2026-10-01T20:30:15+07:00",
  "requestId": "50e41fd8-ec37-4d7a-af48-735f1cbdb86d"
}
```

Hệ thống tự động ghi nhận địa chỉ IP của Client (qua `X-Forwarded-For` hoặc Remote Addr) và chuỗi `User-Agent` phục vụ cho mục đích pháp lý & kiểm toán dữ liệu.
