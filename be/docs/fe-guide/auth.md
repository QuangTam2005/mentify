# Auth & Security Handshake - FE Guide

Tài liệu hướng dẫn tích hợp Frontend cho module **Xác thực & Bảo mật (Auth & Security - Stateless JWT)** (`vn.mentify.be.modules.auth`).

---

## 1. Cơ chế xác thực & Bảo mật (Security Architecture)

### 1.1. Luồng đăng nhập Google OAuth2 (Production & Staging)
1. **Khởi tạo đăng nhập**:
   - Frontend chuyển hướng trình duyệt tới endpoint:
     ```
     GET http://localhost:8080/oauth2/authorization/google
     ```
2. **Google Handshake**:
   - Người dùng đăng nhập tài khoản Google và chấp thuận cấp quyền.
3. **Backend Callback & Cấp mã một lần (One-Time Login Code)**:
   - Google redirect về Backend với authorization code.
   - `OAuth2LoginSuccessHandler` xử lý:
     - Trích xuất profile (`email`, `name`, `picture`).
     - Chỉ chấp nhận email Google đã xác minh; tự động tạo user mới hoặc cập nhật user đã có Google identity tương ứng trong DB.
     - Không tự link Google account vào local account chỉ vì email trùng; user phải dùng flow link explicit sau khi đăng nhập.
     - Cấp một mã đăng nhập dùng một lần (`one-time-code`) có hiệu lực trong 60 giây.
     - Redirect trình duyệt về URL Frontend được cấu hình qua biến `OAUTH2_REDIRECT_URI` (mặc định: `http://localhost:3000/auth/callback?code=<one-time-code>`).
4. **Frontend đổi mã lấy JWT Token**:
   - Tại trang `/auth/callback`, Frontend đọc tham số `code` từ URL và gọi API:
     ```http
     POST /api/v1/auth/oauth2/exchange
     Content-Type: application/json

     { "code": "<one-time-code>" }
     ```
   - Xóa tham số `code` khỏi URL ngay sau khi đổi mã thành công.
   - Nhận về `AuthMeResponse` kèm đối tượng `token` và lưu vào bộ nhớ RAM (Zustand / React Context).

---

### 1.2. Cơ chế Xác thực Stateless JWT & Bảo mật Client
- **Phương thức bảo vệ**: Stateless Bearer JWT Token.
- **Không dùng Session Cookie & CSRF**: Hệ thống API hoạt động hoàn toàn stateless, không cấp phát cookie `JSESSIONID`, không yêu cầu cookie `XSRF-TOKEN` và không cần header `X-XSRF-TOKEN`.
- **Thời gian sống của Token (TTL)**: Access token có TTL 15 phút.
- **Yêu cầu đối với Frontend**:
  - Lưu token trong bộ nhớ ứng dụng (In-Memory). **Tuyệt đối không lưu token vào `localStorage`, `sessionStorage` hoặc cookie** nhằm triệt tiêu rủi ro tấn công XSS trích xuất token lâu dài.
  - Đối với tất cả request yêu cầu xác thực, Frontend đính kèm token vào HTTP header:
    ```http
    Authorization: Bearer <token.value>
    X-Request-Id: <uuid>
    ```
  - Khi token hết hạn hoặc Backend trả lỗi `401 UNAUTHENTICATED`, Frontend tự động xóa token khỏi bộ nhớ và đưa người dùng về trạng thái đăng nhập lại.

---

## 2. API Endpoints

### 2.1. Đổi OAuth2 Code lấy JWT Token (Exchange OAuth2 Code)

- **Method**: `POST`
- **Path**: `/api/v1/auth/oauth2/exchange` (hoặc `/api/auth/oauth2/exchange`)
- **Quyền truy cập**: Public (`permitAll`).
- **Headers**:
  - `Content-Type: application/json`
  - `X-Request-Id: <uuid>`

#### Request Body (`OAuth2CodeExchangeRequest`):

```json
{
  "code": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### Response thành công (`200 OK`):

```json
{
  "success": true,
  "data": {
    "id": "fa3327aa-10bc-4664-a585-6ba9fc167c7f",
    "email": "user@mentify.vn",
    "userType": "UNIVERSITY_STUDENT",
    "role": "USER",
    "accountStatus": "ACTIVE",
    "mentorStatus": "NOT_APPLIED",
    "acceptingBookings": false,
    "fullName": "Nguyễn Văn A",
    "avatarUrl": "https://cdn.mentify.vn/avatars/user.png",
    "token": {
      "value": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "tokenType": "Bearer",
      "expiresAt": "2026-10-11T07:15:00Z"
    }
  },
  "meta": null,
  "timestamp": "2026-10-11T14:00:00+07:00",
  "requestId": "8fcdbb5a-480d-4c01-9cf7-7dcbd3277ba9"
}
```

---

### 2.2. Dev-only Mock Login (Dành riêng cho Local / Dev / Test)

- **Method**: `POST`
- **Path**: `/api/v1/auth/dev-login` (hoặc `/api/auth/dev-login`)
- **Quyền truy cập**: Chỉ khả dụng trên các profile `local`, `dev`, `test` (vô hiệu hóa trên `prod`).
- **Headers**:
  - `Content-Type: application/json`
  - `X-Request-Id: <uuid>`
- **Mô tả**: Cấp JWT token ngay lập tức cho bất kỳ role (`USER`, `ADMIN`) và `userType` (`SCHOOL_STUDENT`, `UNIVERSITY_STUDENT`, `WORKING_PROFESSIONAL`) nào để phát triển Frontend hoặc kiểm thử nhanh mà không cần tài khoản Google thật.

#### Request Body (`DevLoginRequest`):

```json
{
  "email": "dev-tester@mentify.vn",
  "userType": "UNIVERSITY_STUDENT",
  "role": "USER",
  "fullName": "Trần Dev"
}
```

*Tất cả các trường đều là optional:*
- `email`: Mặc định `dev@mentify.vn` nếu không truyền.
- `userType`: `SCHOOL_STUDENT`, `UNIVERSITY_STUDENT` (mặc định), `WORKING_PROFESSIONAL`.
- `role`: `USER` (mặc định), `ADMIN`.
- `fullName`: Mặc định `Dev User` nếu không truyền.

#### Response thành công (`200 OK`):

```json
{
  "success": true,
  "data": {
    "id": "fa3327aa-10bc-4664-a585-6ba9fc167c7f",
    "email": "dev-tester@mentify.vn",
    "userType": "UNIVERSITY_STUDENT",
    "role": "USER",
    "accountStatus": "ACTIVE",
    "mentorStatus": "NOT_APPLIED",
    "acceptingBookings": false,
    "fullName": "Trần Dev",
    "avatarUrl": null,
    "token": {
      "value": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "tokenType": "Bearer",
      "expiresAt": "2026-10-11T07:15:00Z"
    }
  },
  "meta": null,
  "timestamp": "2026-10-11T14:00:15+07:00",
  "requestId": "8fcdbb5a-480d-4c01-9cf7-7dcbd3277ba9"
}
```

---

### 2.3. Kiểm tra thông tin phiên hiện tại (Get Me)

- **Method**: `GET`
- **Path**: `/api/v1/auth/me` (hoặc `/api/auth/me`)
- **Quyền truy cập**: Yêu cầu đã xác thực (`authenticated`).
- **Headers**:
  - `Authorization: Bearer <token.value>`
  - `X-Request-Id: <uuid>`
- **Mô tả**: Trích xuất thông tin tóm tắt của user đang đăng nhập từ JWT subject, bao gồm trạng thái mentor (`mentorStatus`, `acceptingBookings`) để Frontend render thanh điều hướng (navbar/header) và kiểm soát quyền truy cập trang.

#### Response thành công (`200 OK`):

```json
{
  "success": true,
  "data": {
    "id": "fa3327aa-10bc-4664-a585-6ba9fc167c7f",
    "email": "dev-tester@mentify.vn",
    "userType": "UNIVERSITY_STUDENT",
    "role": "USER",
    "accountStatus": "ACTIVE",
    "mentorStatus": "NOT_APPLIED",
    "acceptingBookings": false,
    "fullName": "Trần Dev",
    "avatarUrl": "https://cdn.mentify.vn/avatars/dev.png"
  },
  "meta": null,
  "timestamp": "2026-10-11T14:00:30+07:00",
  "requestId": "221b34cd-f001-4475-b6d8-410fbde6a152"
}
```

#### Giá trị `mentorStatus`:
- `NOT_APPLIED`: Chưa nộp hồ sơ mentor.
- `PENDING`: Đang chờ admin duyệt minh chứng.
- `APPROVED`: Đã được duyệt làm mentor.
- `REJECTED`: Hồ sơ bị từ chối (có thể cập nhật và nộp lại).
- `SUSPENDED`: Bị tạm ngưng quyền nhận booking.

#### Lỗi khi chưa xác thực hoặc token hết hạn (`401 Unauthorized`):

```json
{
  "success": false,
  "error": {
    "code": "UNAUTHENTICATED",
    "message": "Yêu cầu đăng nhập hoặc token đã hết hạn",
    "details": []
  },
  "timestamp": "2026-10-11T14:00:35+07:00",
  "requestId": "e1f13b6c-389f-4bc2-84f9-2169b2d69f0e"
}
```

---

### 2.4. Đăng xuất (Logout)

- **Method**: `POST`
- **Path**: `/api/v1/auth/logout` (hoặc `/api/auth/logout`)
- **Headers**:
  - `Authorization: Bearer <token.value>`
  - `X-Request-Id: <uuid>`
- **Mô tả**: Thông báo máy chủ kết thúc phiên. Phía Frontend lập tức xóa token khỏi bộ nhớ RAM và làm sạch toàn bộ cache queries.

#### Response thành công (`200 OK`):

```json
{
  "success": true,
  "data": null,
  "meta": null,
  "timestamp": "2026-10-11T14:00:40+07:00",
  "requestId": "3a18a5cb-22fa-45cf-a739-16e6eb1b1dc5"
}
```

---

### 2.5. Đăng nhập bằng mật khẩu (Password Login)

- **Method**: `POST`
- **Path**: `/api/v1/auth/password/login`
- **Quyền truy cập**: Public (`permitAll`).
- **Headers**:
  - `Content-Type: application/json`
  - `X-Request-Id: <uuid>`
- **Request Body**:
  ```json
  {
    "email": "user@example.com",
    "password": "mySecurePassword123"
  }
  ```
- **Response**: Trả về `AuthMeResponse` kèm JWT token trong `data.token`.
- **Bảo mật**: Sai thông tin đăng nhập trả `401 UNAUTHENTICATED` với thông báo chung (generic message) để chống rò rỉ danh sách tài khoản.

---

### 2.6. Thiết lập hoặc đổi mật khẩu (Set / Change Password)

- **Method**: `PUT`
- **Path**: `/api/v1/auth/password`
- **Quyền truy cập**: Authenticated (`Authorization: Bearer <token>`).
- **Headers**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>`
- **Request Body**:
  ```json
  {
    "currentPassword": "oldPassword123",
    "newPassword": "newSecurePassword456"
  }
  ```
- **Quy tắc**: Mật khẩu dài từ 12 đến 128 ký tự. Người dùng Google lần đầu thiết lập mật khẩu có thể bỏ trống `currentPassword`.

---

### 2.7. Quên và đặt lại mật khẩu (Forgot / Reset Password)

- **Yêu cầu đặt lại**:
  - `POST /api/v1/auth/password/forgot`
  - Body: `{ "email": "user@example.com" }`
  - Luôn trả về phản hồi thành công generic để tránh email enumeration.
- **Xác nhận đặt lại**:
  - `POST /api/v1/auth/password/reset`
  - Body: `{ "token": "...", "newPassword": "..." }`
  - Token đặt lại chỉ sử dụng 1 lần và hết hạn sau 30 phút.

---

### 2.8. Đổi email (Email Change)

- **Yêu cầu đổi**:
  - `POST /api/v1/auth/email-change`
  - Headers: `Authorization: Bearer <token>`
  - Body: `{ "newEmail": "newemail@example.com" }`
  - Gửi link xác nhận kèm token tới email mới.
- **Xác nhận đổi**:
  - `POST /api/v1/auth/email-change/confirm`
  - Body: `{ "token": "..." }`
  - Email tài khoản chỉ được cập nhật sau khi token được xác nhận hợp lệ.
- **Liên kết tài khoản Google**:
  - Thực hiện qua flow explicit tại `GET /api/v1/auth/google/link` (yêu cầu đã đăng nhập). Hệ thống không tự động liên kết chỉ vì email Google trùng với email local.
