# JWT Authentication Contract

Mentify API hiện dùng JWT stateless. API không dùng `JSESSIONID`, CSRF cookie hoặc CSRF header để xác thực request.

## Login

Password login và dev-login trả về `AuthMeResponse` kèm token:

```json
{
  "data": {
    "id": "...",
    "email": "...",
    "role": "USER",
    "token": {
      "value": "eyJ...",
      "tokenType": "Bearer",
      "expiresAt": "2026-10-03T10:15:00Z"
    }
  }
}
```

Mọi API protected phải gửi:

```http
Authorization: Bearer <token.value>
X-Request-Id: <uuid>
```

FE chỉ lưu token trong memory. Không lưu vào `localStorage`, `sessionStorage` hoặc cookie.

## Google OAuth2

Sau khi Google xác thực thành công, BE redirect về:

```text
/auth/callback?code=<one-time-code>
```

Code có hiệu lực 60 giây và chỉ dùng một lần. FE đổi code lấy JWT:

```http
POST /api/v1/auth/oauth2/exchange
Content-Type: application/json

{ "code": "..." }
```

FE phải xóa code khỏi URL ngay sau khi exchange thành công.

## Logout và token hết hạn

Logout là thao tác xóa token ở FE và clear auth/query state. Khi API trả `401`, FE cũng xóa token và đưa user về trạng thái chưa đăng nhập.

Access token hiện có TTL 15 phút. Refresh token chưa thuộc phạm vi phiên bản này; refresh toàn trang sẽ yêu cầu đăng nhập lại.

## Quên và đặt lại mật khẩu

FE sử dụng các endpoint public sau:

```http
POST /api/v1/auth/password/forgot
Content-Type: application/json

{ "email": "user@example.com" }
```

Endpoint luôn trả response thành công ở mức contract để không tiết lộ email có tồn tại hay không.

Link trong email có dạng `/reset-password?token=<reset-token>`. FE gửi token cùng mật khẩu mới:

```http
POST /api/v1/auth/password/reset
Content-Type: application/json

{ "token": "...", "newPassword": "correct horse battery" }
```

Reset token chỉ dùng một lần, hết hạn sau 30 phút và không được lưu vào storage hoặc cookie.
