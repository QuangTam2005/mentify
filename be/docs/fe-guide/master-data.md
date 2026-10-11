# Master Data Domain (Trường Đại học & Nhóm ngành TT 74/2026) - FE Guide

Tài liệu hướng dẫn tích hợp Frontend cho module **Master Data** (`vn.mentify.be.modules.masterdata`).

---

## 1. Tổng quan & Bảo mật

- **Base URLs**: Hỗ trợ đồng thời cả hai prefix:
  - Chuẩn: `/api/v1/master-data`
  - Alias: `/api/master-data`
- **Quyền truy cập (Public APIs)**: Tất cả API thuộc Master Data Domain là **công khai**, không yêu cầu đăng nhập.
- **Tối ưu hiệu năng (Caching)**: Dữ liệu danh mục trường đại học và nhóm ngành được cache sẵn tại tầng ứng dụng (`@Cacheable`), đảm bảo tốc độ phản hồi cực nhanh cho các dropdown và thanh tìm kiếm autocomplete ở Frontend.

---

## 2. API Endpoints

### 2.1. Tra cứu danh sách Trường Đại học (Get Schools)

- **Method**: `GET`
- **Path**: `/api/v1/master-data/schools` (hoặc `/api/master-data/schools`)
- **Query Parameters**:
  | Tham số | Kiểu | Mặc định | Mô tả |
  | :--- | :--- | :--- | :--- |
  | `search` | `String` | `null` | Tìm kiếm theo tên trường, tên viết tắt, mã định danh, hoặc mã tuyển sinh |
  | `region` | `String` | `null` | Lọc theo vùng miền: `TP.HCM`, `Hà Nội`, `Cần Thơ`, `Đà Nẵng - Huế`, `Miền Bắc ngoài Hà Nội` |
  | `campusType` | `String` | `null` | `MAIN_CAMPUS` (Cơ sở chính) hoặc `BRANCH_CAMPUS` (Phân hiệu) |
  | `page` | `Integer`| `0` | Chỉ số trang (bắt đầu từ 0) |
  | `size` | `Integer`| `50` | Số lượng phần tử mỗi trang (tối đa 200) |

#### Response mẫu (`200 OK`):

```json
{
  "success": true,
  "data": [
    {
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
    {
      "id": "2daefcf0-f1c7-43ca-a3ba-8b80fc930491",
      "code": "TLS-HCM",
      "admissionCode": "TLS",
      "name": "Trường Đại học Thủy lợi - Phân hiệu miền Nam",
      "shortName": "ĐH Thủy lợi miền Nam",
      "commonAbbreviation": "TLU-HCM",
      "campusType": "BRANCH_CAMPUS",
      "parentSchoolId": "38a3c50a-bc0d-4074-a694-82245b0a3314",
      "region": "TP.HCM",
      "status": "ACTIVE",
      "dataVersion": "2026"
    }
  ],
  "meta": {
    "page": 0,
    "size": 50,
    "totalItems": 110,
    "totalPages": 3
  },
  "timestamp": "2026-10-01T20:15:00+07:00",
  "requestId": "e1f13b6c-389f-4bc2-84f9-2169b2d69f0e"
}
```

---

### 2.2. Tra cứu Nhóm ngành chuẩn theo Thông tư 74/2026 (Get Major Groups)

- **Method**: `GET`
- **Path**: `/api/v1/master-data/major-groups` (hoặc `/api/master-data/major-groups`)
- **Query Parameters**:
  | Tham số | Kiểu | Mặc định | Mô tả |
  | :--- | :--- | :--- | :--- |
  | `search` | `String` | `null` | Tìm kiếm theo tên nhóm ngành, mã 4 số, hoặc tên lĩnh vực |
  | `isActive` | `Boolean` | `true` | Lọc nhóm ngành đang hoạt động (`true`/`false`) |

#### Response mẫu (`200 OK`):

```json
{
  "success": true,
  "data": [
    {
      "id": "c1f7a810-745a-4062-8178-5dbf9e1f57b1",
      "code": "4801",
      "name": "Máy tính",
      "fieldCode": "48",
      "regulationVersion": "TT_74_2026",
      "description": "MAY TINH VA CONG NGHE THONG TIN",
      "isActive": true
    },
    {
      "id": "b3e94a10-252a-4375-9c88-9226cb1288c3",
      "code": "4802",
      "name": "Công nghệ thông tin",
      "fieldCode": "48",
      "regulationVersion": "TT_74_2026",
      "description": "MAY TINH VA CONG NGHE THONG TIN",
      "isActive": true
    }
  ],
  "meta": null,
  "timestamp": "2026-10-01T20:15:00+07:00",
  "requestId": "221b34cd-f001-4475-b6d8-410fbde6a152"
}
```

> [!TIP]
> Mã nhóm ngành `code` luôn là chuỗi 4 ký tự số (ví dụ: `"4801"`, `"4802"`). Frontend nên gửi trường `code` này khi gọi API cập nhật hồ sơ người dùng (`UpdateProfileRequest.majorGroupCode`).
