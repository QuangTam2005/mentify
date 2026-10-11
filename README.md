# Mentify

Nền tảng kết nối học sinh THPT, sinh viên đại học/cao đẳng và người đi làm với mentor để định hướng trường học, nhóm ngành, ngành đào tạo và lộ trình sự nghiệp tại Việt Nam.

---

## 1. Cấu trúc dự án (Monorepo)

- **`be/`**: Backend Modular Monolith xây dựng trên nền tảng **Java 21** và **Spring Boot 4.1.1**, cơ sở dữ liệu **PostgreSQL 16**, quản lý migration bằng **Flyway**, kiểm thử tích hợp bằng **Testcontainers**.
- **`fe/`**: Frontend xây dựng trên nền tảng **Next.js 15 (App Router)**, **React 19**, **TypeScript**, **Tailwind CSS v4**, **TanStack Query v5** và **Zustand v5**.

---

## 2. Kiến trúc & Cơ chế Xác thực

- **Cơ chế xác thực**: Hoàn toàn **Stateless JWT** (`Authorization: Bearer <token>`). Không sử dụng cookie session (`JSESSIONID`) hay token CSRF.
- **Thời hạn Token**: Access Token có TTL 15 phút, lưu trữ an toàn trong bộ nhớ RAM của ứng dụng (In-Memory AuthStore).
- **Đăng nhập Google OAuth2**: Luồng đổi mã một lần (one-time authorization code, TTL 60s) thông qua endpoint `/api/v1/auth/oauth2/exchange` để nhận JWT.
- **Chuẩn hóa API Envelope**: Mọi API trả về định dạng chuẩn `ApiSuccessResponse<T>` hoặc `ApiErrorResponse` kèm `requestId` và múi giờ `Asia/Ho_Chi_Minh` (`+07:00`).

---

## 3. Khởi chạy cục bộ (Local Development)

### Backend (`be/`):

```bash
# Khởi động PostgreSQL
docker compose up -d postgres

# Chạy backend
./mvnw spring-boot:run
# Trên Windows:
.\mvnw.cmd spring-boot:run
```

API khả dụng tại: `http://localhost:8080` (Health check: `GET /actuator/health`).

### Frontend (`fe/`):

```bash
cd fe
npm install
npm run dev
```

Ứng dụng web khả dụng tại: `http://localhost:3000`.

---

## 4. Tài liệu tham khảo chi tiết

- [Backend Documentation (`be/docs/`)](be/docs/)
  - [Business Description](be/docs/business-description.md)
  - [MVP Contracts & State Machines](be/docs/architecture/mvp-contracts.md)
  - [FE Integration Guide - Auth (JWT)](be/docs/fe-guide/auth.md)
  - [FE Integration Guide - Backend Foundation](be/docs/fe-guide/backend-foundation.md)
  - [FE Integration Guide - Master Data](be/docs/fe-guide/master-data.md)
  - [FE Integration Guide - User Domain](be/docs/fe-guide/user.md)
- [Frontend Documentation (`fe/docs/`)](fe/docs/)
  - [FE - BE Communication Contract](fe/docs/be-communication.md)
  - [JWT Authentication Specification](fe/docs/jwt-auth.md)
  - [Frontend Business Rules](fe/docs/business-description.md)
