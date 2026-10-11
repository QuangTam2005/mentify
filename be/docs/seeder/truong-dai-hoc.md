# Seeder: Truong dai hoc tieu bieu

## Pham vi

File nay la dataset curated cho MVP, hop nhat toan bo seeder truong dai hoc theo dia phuong. Dataset hien gom 110 record:

- Can Tho: 6 record.
- Da Nang va Hue: 10 record.
- Thanh pho Ho Chi Minh: 44 record.
- Ha Noi: 40 record.
- Mien Bac ngoai Ha Noi: 10 record.

Danh sach khong phai danh muc day du tat ca co so giao duc dai hoc. Co the bo sung record theo cung schema.

## Quy uoc du lieu

- `schoolCode` la ma noi bo on dinh cua co so/campus.
- `admissionCode` la ma truong dung tren he thong dang ky nguyen vong; ma co the thay doi theo nam tuyen sinh.
- `campusType`: `MAIN_CAMPUS` cho co so chinh; `BRANCH_CAMPUS` cho phan hieu/campus.
- Record `BRANCH_CAMPUS` phai co `parentSchoolCode`. Khong tu tao ma tuyen sinh rieng neu nguon chinh thuc cong bo dung ma truong me.
- `provinceOrCity` la dia phuong dung cho tim kiem/hien thi trong MVP.
- `sourceVersion` la nam tuyen sinh dung de doi chieu ma truong. Khi ma thay doi, tao dataset version moi, khong ghi de lich su.
- `shortName` va `commonAbbreviation` la du lieu tim kiem/hien thi, khong thay the ten phap ly.
- Ma nganh khong nam trong file nay; ma nganh la dataset rieng theo Thong tu 74/2026/TT-BGDDT.

## Recommended record shape

```json
{
  "schoolCode": "TCT",
  "admissionCode": "TCT",
  "schoolName": "Trường Đại học Cần Thơ",
  "shortName": "ĐH Cần Thơ",
  "commonAbbreviation": "CTU",
  "campusType": "MAIN_CAMPUS",
  "parentSchoolCode": null,
  "provinceOrCity": "Cần Thơ",
  "sourceVersion": "2026",
  "status": "ACTIVE"
}
```

## Seed records: Can Tho

| # | schoolCode | admissionCode | schoolName | shortName | commonAbbreviation | campusType | parentSchoolCode | provinceOrCity | sourceVersion |
|---:|---|---|---|---|---|---|---|---|---|
| 1 | TCT | TCT | Trường Đại học Cần Thơ | ĐH Cần Thơ | CTU | MAIN_CAMPUS | - | Cần Thơ | 2026 |
| 2 | YCT | YCT | Trường Đại học Y Dược Cần Thơ | ĐH Y Dược Cần Thơ | CTUMP | MAIN_CAMPUS | - | Cần Thơ | 2026 |
| 3 | KCC | KCC | Trường Đại học Kỹ thuật - Công nghệ Cần Thơ | ĐH Kỹ thuật - Công nghệ Cần Thơ | CTUT | MAIN_CAMPUS | - | Cần Thơ | 2026 |
| 4 | DNC | DNC | Trường Đại học Nam Cần Thơ | ĐH Nam Cần Thơ | NCTU | MAIN_CAMPUS | - | Cần Thơ | 2026 |
| 5 | DTD | DTD | Trường Đại học Tây Đô | ĐH Tây Đô | TDU | MAIN_CAMPUS | - | Cần Thơ | 2026 |
| 6 | FPT-CAN-THO | FPT | Trường Đại học FPT - Phân hiệu Cần Thơ | ĐH FPT Cần Thơ | FPTU Cần Thơ | BRANCH_CAMPUS | FPT | Cần Thơ | 2026 |

## Seed records: Da Nang va Hue

| # | schoolCode | admissionCode | schoolName | shortName | commonAbbreviation | campusType | parentSchoolCode | provinceOrCity | sourceVersion |
|---:|---|---|---|---|---|---|---|---|---|
| 7 | DDK | DDK | Trường Đại học Bách khoa, Đại học Đà Nẵng | ĐH Bách khoa - ĐH Đà Nẵng | DUT | MAIN_CAMPUS | - | Thành phố Đà Nẵng | 2026 |
| 8 | DDT | DDT | Trường Đại học Kinh tế, Đại học Đà Nẵng | ĐH Kinh tế - ĐH Đà Nẵng | DUE | MAIN_CAMPUS | - | Thành phố Đà Nẵng | 2026 |
| 9 | DDS | DDS | Trường Đại học Sư phạm, Đại học Đà Nẵng | ĐH Sư phạm - ĐH Đà Nẵng | UED | MAIN_CAMPUS | - | Thành phố Đà Nẵng | 2026 |
| 10 | DDF | DDF | Trường Đại học Ngoại ngữ, Đại học Đà Nẵng | ĐH Ngoại ngữ - ĐH Đà Nẵng | UFL | MAIN_CAMPUS | - | Thành phố Đà Nẵng | 2026 |
| 11 | VKU | VKU | Trường Đại học Công nghệ Thông tin và Truyền thông Việt - Hàn, Đại học Đà Nẵng | ĐH CNTT và Truyền thông Việt - Hàn | VKU | MAIN_CAMPUS | - | Thành phố Đà Nẵng | 2026 |
| 12 | DHT | DHT | Trường Đại học Khoa học, Đại học Huế | ĐH Khoa học - ĐH Huế | HUSC | MAIN_CAMPUS | - | Thành phố Huế | 2026 |
| 13 | DHS | DHS | Trường Đại học Sư phạm, Đại học Huế | ĐH Sư phạm - ĐH Huế | HUEUED | MAIN_CAMPUS | - | Thành phố Huế | 2026 |
| 14 | DHK | DHK | Trường Đại học Kinh tế, Đại học Huế | ĐH Kinh tế - ĐH Huế | HCE | MAIN_CAMPUS | - | Thành phố Huế | 2026 |
| 15 | DHY | DHY | Trường Đại học Y - Dược, Đại học Huế | ĐH Y - Dược - ĐH Huế | HUUMED | MAIN_CAMPUS | - | Thành phố Huế | 2026 |
| 16 | DHL | DHL | Trường Đại học Nông Lâm, Đại học Huế | ĐH Nông Lâm - ĐH Huế | HUAF | MAIN_CAMPUS | - | Thành phố Huế | 2026 |

## Seed records: Thanh pho Ho Chi Minh

| # | schoolCode | admissionCode | schoolName | shortName | commonAbbreviation | campusType | parentSchoolCode | provinceOrCity | sourceVersion |
|---:|---|---|---|---|---|---|---|---|---|
| 17 | QSB | QSB | Trường Đại học Bách khoa, Đại học Quốc gia Thành phố Hồ Chí Minh | ĐH Bách khoa - ĐHQG-HCM | HCMUT | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 18 | QST | QST | Trường Đại học Khoa học Tự nhiên, Đại học Quốc gia Thành phố Hồ Chí Minh | ĐH Khoa học Tự nhiên - ĐHQG-HCM | HCMUS | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 19 | QSX | QSX | Trường Đại học Khoa học Xã hội và Nhân văn, Đại học Quốc gia Thành phố Hồ Chí Minh | ĐH KHXH&NV - ĐHQG-HCM | HCMUSSH | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 20 | QSC | QSC | Trường Đại học Công nghệ Thông tin, Đại học Quốc gia Thành phố Hồ Chí Minh | ĐH Công nghệ Thông tin - ĐHQG-HCM | UIT | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 21 | QSQ | QSQ | Trường Đại học Quốc tế, Đại học Quốc gia Thành phố Hồ Chí Minh | ĐH Quốc tế - ĐHQG-HCM | IU | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 22 | QSK | QSK | Trường Đại học Kinh tế - Luật, Đại học Quốc gia Thành phố Hồ Chí Minh | ĐH Kinh tế - Luật - ĐHQG-HCM | UEL | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 23 | QSY | QSY | Trường Đại học Sức khỏe, Đại học Quốc gia Thành phố Hồ Chí Minh | ĐH Sức khỏe - ĐHQG-HCM | UHS | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 24 | KSA | KSA | Đại học Kinh tế Thành phố Hồ Chí Minh | ĐH Kinh tế TP.HCM | UEH | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 25 | SPS | SPS | Trường Đại học Sư phạm Thành phố Hồ Chí Minh | ĐH Sư phạm TP.HCM | HCMUE | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 26 | SPK | SPK | Trường Đại học Sư phạm Kỹ thuật Thành phố Hồ Chí Minh | ĐH Sư phạm Kỹ thuật TP.HCM | HCMUTE | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 27 | NLS | NLS | Trường Đại học Nông Lâm Thành phố Hồ Chí Minh | ĐH Nông Lâm TP.HCM | NLU | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 28 | LPS | LPS | Trường Đại học Luật Thành phố Hồ Chí Minh | ĐH Luật TP.HCM | ULAW | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 29 | IUH | IUH | Trường Đại học Công nghiệp Thành phố Hồ Chí Minh | ĐH Công nghiệp TP.HCM | IUH | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 30 | GTS | GTS | Trường Đại học Giao thông vận tải Thành phố Hồ Chí Minh | ĐH Giao thông vận tải TP.HCM | UTH | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 31 | NHS | NHS | Trường Đại học Ngân hàng Thành phố Hồ Chí Minh | ĐH Ngân hàng TP.HCM | HUB | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 32 | DMS | DMS | Trường Đại học Tài chính - Marketing | ĐH Tài chính - Marketing | UFM | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 33 | TDT | TDT | Trường Đại học Tôn Đức Thắng | ĐH Tôn Đức Thắng | TDTU | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 34 | DVL | DVL | Trường Đại học Văn Lang | ĐH Văn Lang | VLU | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 35 | NTT | NTT | Trường Đại học Nguyễn Tất Thành | ĐH Nguyễn Tất Thành | NTTU | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 36 | DKC | DKC | Trường Đại học Công nghệ Thành phố Hồ Chí Minh | ĐH Công nghệ TP.HCM | HUTECH | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 37 | DTH | DTH | Trường Đại học Hoa Sen | ĐH Hoa Sen | HSU | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 38 | KTS | KTS | Trường Đại học Kiến trúc Thành phố Hồ Chí Minh | ĐH Kiến trúc TP.HCM | UAH | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 39 | MTS | MTS | Trường Đại học Mỹ thuật Thành phố Hồ Chí Minh | ĐH Mỹ thuật TP.HCM | HCMUFA | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 40 | FPT-HCM | FPT | Trường Đại học FPT - Phân hiệu Thành phố Hồ Chí Minh | ĐH FPT TP.HCM | FPTU HCM | BRANCH_CAMPUS | FPT | Thành phố Hồ Chí Minh | 2026 |
| 41 | BVS-HCM | BVS | Học viện Công nghệ Bưu chính Viễn thông - Cơ sở Thành phố Hồ Chí Minh | PTIT cơ sở TP.HCM | PTIT HCM | BRANCH_CAMPUS | BVS | Thành phố Hồ Chí Minh | 2026 |
| 42 | HHK | HHK | Học viện Hàng không Việt Nam | Học viện Hàng không | VAA | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 43 | DSG | DSG | Trường Đại học Công nghệ Sài Gòn | ĐH Công nghệ Sài Gòn | STU | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 44 | UMT | UMT | Trường Đại học Quản lý và Công nghệ Thành phố Hồ Chí Minh | ĐH Quản lý và Công nghệ TP.HCM | UMT | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 45 | HIU | HIU | Trường Đại học Quốc tế Hồng Bàng | ĐH Quốc tế Hồng Bàng | HIU | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 46 | SIU | SIU | Trường Đại học Quốc tế Sài Gòn | ĐH Quốc tế Sài Gòn | SIU | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 47 | SGD | SGD | Trường Đại học Sài Gòn | ĐH Sài Gòn | SGU | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 48 | DSD | DSD | Trường Đại học Sân khấu - Điện ảnh Thành phố Hồ Chí Minh | ĐH Sân khấu - Điện ảnh TP.HCM | UAHC | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 49 | STS | STS | Trường Đại học Sư phạm Thể dục Thể thao Thành phố Hồ Chí Minh | ĐH Sư phạm TDTT TP.HCM | HCMUPES | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 50 | TDS | TDS | Trường Đại học Thể dục Thể thao Thành phố Hồ Chí Minh | ĐH TDTT TP.HCM | USH | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 51 | DTM | DTM | Trường Đại học Tài nguyên và Môi trường Thành phố Hồ Chí Minh | ĐH Tài nguyên và Môi trường TP.HCM | HCMUNRE | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 52 | TLS-HCM | TLS | Trường Đại học Thủy lợi - Cơ sở 2 | ĐH Thủy lợi cơ sở phía Nam | TLU HCM | BRANCH_CAMPUS | TLA | Thành phố Hồ Chí Minh | 2026 |
| 53 | DVH | DVH | Trường Đại học Văn Hiến | ĐH Văn Hiến | VHU | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 54 | VHS | VHS | Trường Đại học Văn hóa Thành phố Hồ Chí Minh | ĐH Văn hóa TP.HCM | HUC | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 55 | YDS | YDS | Đại học Y Dược Thành phố Hồ Chí Minh | ĐH Y Dược TP.HCM | UMP | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 56 | TYS | TYS | Trường Đại học Y khoa Phạm Ngọc Thạch | ĐH Y khoa Phạm Ngọc Thạch | PNTU | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 57 | DCT | DCT | Trường Đại học Công Thương Thành phố Hồ Chí Minh | ĐH Công Thương TP.HCM | HUIT | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 58 | DNT | DNT | Trường Đại học Ngoại ngữ - Tin học Thành phố Hồ Chí Minh | ĐH Ngoại ngữ - Tin học TP.HCM | HUFLIT | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 59 | UEF | UEF | Trường Đại học Kinh tế - Tài chính Thành phố Hồ Chí Minh | ĐH Kinh tế - Tài chính TP.HCM | UEF | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |
| 60 | FUV | FUV | Trường Đại học Fulbright Việt Nam | ĐH Fulbright Việt Nam | FUV | MAIN_CAMPUS | - | Thành phố Hồ Chí Minh | 2026 |

## Seed records: Ha Noi

| # | schoolCode | admissionCode | schoolName | shortName | commonAbbreviation | campusType | parentSchoolCode | provinceOrCity | sourceVersion |
|---:|---|---|---|---|---|---|---|---|---|
| 61 | HBT | HBT | Học viện Báo chí và Tuyên truyền | Học viện Báo chí và Tuyên truyền | VJOL | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 62 | BVH | BVH | Học viện Công nghệ Bưu chính Viễn thông - cơ sở phía Bắc | Học viện Công nghệ Bưu chính Viễn thông | PTIT | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 63 | HQT | HQT | Học viện Ngoại giao | Học viện Ngoại giao | DAV | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 64 | NHH | NHH | Học viện Ngân hàng | Học viện Ngân hàng | BA | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 65 | HVN | HVN | Học viện Nông nghiệp Việt Nam | Học viện Nông nghiệp Việt Nam | VNUA | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 66 | HPN | HPN | Học viện Phụ nữ Việt Nam | Học viện Phụ nữ Việt Nam | VNUWA | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 67 | HTC | HTC | Học viện Tài chính | Học viện Tài chính | AOF | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 68 | HYD | HYD | Học viện Y - Dược học cổ truyền Việt Nam | Học viện Y - Dược học cổ truyền Việt Nam | VUTM | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 69 | QHL | QHL | Khoa Luật, Đại học Quốc gia Hà Nội | Luật - ĐHQGHN | VNU-UL | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 70 | QHD | QHD | Trường Quản trị và Kinh doanh, Đại học Quốc gia Hà Nội | Quản trị và Kinh doanh - ĐHQGHN | VNU-CQE | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 71 | BKA | BKA | Đại học Bách khoa Hà Nội | Bách khoa Hà Nội | HUST | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 72 | CMC | CMC | Đại học CMC | Đại học CMC | CMC | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 73 | QHI | QHI | Trường Đại học Công nghệ, Đại học Quốc gia Hà Nội | Công nghệ - ĐHQGHN | VNU-UET | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 74 | GTA | GTA | Trường Đại học Công nghệ Giao thông vận tải | Công nghệ Giao thông vận tải | UTT | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 75 | LDA | LDA | Trường Đại học Công đoàn | Đại học Công đoàn | TUU | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 76 | DKH | DKH | Trường Đại học Dược Hà Nội | Đại học Dược Hà Nội | HUP | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 77 | GHA | GHA | Trường Đại học Giao thông vận tải - cơ sở phía Bắc | Giao thông vận tải | UTC | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 78 | QHS | QHS | Trường Đại học Giáo dục, Đại học Quốc gia Hà Nội | Giáo dục - ĐHQGHN | VNU-UED | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 79 | NHF | NHF | Trường Đại học Hà Nội | Đại học Hà Nội | HANU | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 80 | KCN | KCN | Trường Đại học Khoa học và Công nghệ Hà Nội | Đại học Việt Pháp | USTH | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 81 | QHX | QHX | Trường Đại học Khoa học Xã hội và Nhân văn, Đại học Quốc gia Hà Nội | KHXH&NV - ĐHQGHN | VNU-USSH | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 82 | QHT | QHT | Trường Đại học Khoa học Tự nhiên, Đại học Quốc gia Hà Nội | Khoa học Tự nhiên - ĐHQGHN | VNU-HUS | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 83 | QHE | QHE | Trường Đại học Kinh tế, Đại học Quốc gia Hà Nội | Kinh tế - ĐHQGHN | VNU-UEB | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 84 | KHA | KHA | Trường Đại học Kinh tế Quốc dân | Kinh tế Quốc dân | NEU | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 85 | KTA | KTA | Trường Đại học Kiến trúc Hà Nội | Kiến trúc Hà Nội | HAU | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 86 | LPH | LPH | Trường Đại học Luật Hà Nội | Luật Hà Nội | HLU | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 87 | MDA | MDA | Trường Đại học Mỏ - Địa chất | Mỏ - Địa chất | HUMG | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 88 | MHN | MHN | Trường Đại học Mở Hà Nội | Đại học Mở Hà Nội | HOU | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 89 | QHF | QHF | Trường Đại học Ngoại ngữ, Đại học Quốc gia Hà Nội | Ngoại ngữ - ĐHQGHN | VNU-ULIS | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 90 | NTH | NTH | Trường Đại học Ngoại thương - cơ sở phía Bắc | Ngoại thương | FTU | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 91 | SPH | SPH | Trường Đại học Sư phạm Hà Nội | Sư phạm Hà Nội | HNUE | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 92 | TMA | TMA | Trường Đại học Thương mại | Thương mại | TMU | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 93 | HNM | HNM | Trường Đại học Thủ đô Hà Nội | Thủ đô Hà Nội | HNMU | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 94 | TLA | TLA | Trường Đại học Thủy lợi - cơ sở 1 | Thủy lợi | TLU | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 95 | VJU | VJU | Trường Đại học Việt Nhật, Đại học Quốc gia Hà Nội | Việt Nhật - ĐHQGHN | VJU | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 96 | VHH | VHH | Trường Đại học Văn hóa Hà Nội | Văn hóa Hà Nội | HUC | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 97 | XDA | XDA | Trường Đại học Xây dựng Hà Nội | Xây dựng Hà Nội | HUCE | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 98 | QHY | QHY | Trường Đại học Y Dược, Đại học Quốc gia Hà Nội | Y Dược - ĐHQGHN | VNU-UMP | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 99 | YHB | YHB | Trường Đại học Y Hà Nội | Y Hà Nội | HMU | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |
| 100 | DDL | DDL | Trường Đại học Điện lực | Điện lực | EPU | MAIN_CAMPUS | - | Thành phố Hà Nội | 2026 |

## Seed records: Mien Bac ngoai Ha Noi

| # | schoolCode | admissionCode | schoolName | shortName | commonAbbreviation | campusType | parentSchoolCode | provinceOrCity | sourceVersion |
|---:|---|---|---|---|---|---|---|---|---|
| 101 | HHA | HHA | Trường Đại học Hàng hải Việt Nam | ĐH Hàng hải Việt Nam | VMU | MAIN_CAMPUS | - | Thành phố Hải Phòng | 2026 |
| 102 | THP | THP | Trường Đại học Hải Phòng | ĐH Hải Phòng | UHP | MAIN_CAMPUS | - | Thành phố Hải Phòng | 2026 |
| 103 | YPB | YPB | Trường Đại học Y Dược Hải Phòng | ĐH Y Dược Hải Phòng | HPMU | MAIN_CAMPUS | - | Thành phố Hải Phòng | 2026 |
| 104 | DTS | DTS | Trường Đại học Sư phạm, Đại học Thái Nguyên | ĐH Sư phạm - ĐH Thái Nguyên | TNUE | MAIN_CAMPUS | - | Tỉnh Thái Nguyên | 2026 |
| 105 | DTK | DTK | Trường Đại học Kỹ thuật Công nghiệp, Đại học Thái Nguyên | ĐH Kỹ thuật Công nghiệp - ĐH Thái Nguyên | TNUT | MAIN_CAMPUS | - | Tỉnh Thái Nguyên | 2026 |
| 106 | DTC | DTC | Trường Đại học Công nghệ Thông tin và Truyền thông, Đại học Thái Nguyên | ĐH CNTT và Truyền thông - ĐH Thái Nguyên | ICTU | MAIN_CAMPUS | - | Tỉnh Thái Nguyên | 2026 |
| 107 | HLU | HLU | Trường Đại học Hạ Long | ĐH Hạ Long | UHL | MAIN_CAMPUS | - | Tỉnh Quảng Ninh | 2026 |
| 108 | HDT | HDT | Trường Đại học Hồng Đức | ĐH Hồng Đức | HDU | MAIN_CAMPUS | - | Tỉnh Thanh Hóa | 2026 |
| 109 | TDV | TDV | Trường Đại học Vinh | ĐH Vinh | VU | MAIN_CAMPUS | - | Tỉnh Nghệ An | 2026 |
| 110 | DKY | DKY | Trường Đại học Kỹ thuật Y tế Hải Dương | ĐH Kỹ thuật Y tế Hải Dương | HMTU | MAIN_CAMPUS | - | Tỉnh Hải Dương | 2026 |

## Source references

- Cổng tuyển sinh Bộ Giáo dục và Đào tạo: https://tuyensinh.moet.gov.vn/ts/
- Các nguồn tuyển sinh địa phương và trường đã được ghi trong các seeder ban đầu; khi cập nhật dataset, admin/BA phải đối chiếu lại website tuyển sinh chính thức của từng cơ sở.
- Đà Nẵng: https://ts.udn.vn/DHCD/Chinhquy/DHTbao/19360
- Huế: https://tuyensinh.hueuni.edu.vn/News/Detail/thong-tin-tuyen-sinh-dai-hoc-cao-dang-he-chinh-quy-cua-dai-hoc-hue_20260501150542
- Hà Nội: https://vietnamnet.vn/giao-duc/diem-thi/tra-cuu-diem-chuan-cd-dh-2026-page0?province=000001
- Đại học Thái Nguyên: https://tnu.edu.vn/
- Đại học Vinh: https://phongdaotao.vinhuni.edu.vn/tuyen-sinh

## Seed validation rules

1. `admissionCode` phai duy nhat trong cung `sourceVersion`, tru truong hop nguon chinh thuc cong bo ro ma dung chung cho nhieu campus.
2. `schoolCode` phai duy nhat trong toan bo dataset, ke ca giua cac dia phuong.
3. Record `BRANCH_CAMPUS` phai co `parentSchoolCode`; parent co the nam trong dataset ngoai pham vi file nay.
4. Khong ghi de dataset lich su khi ma truong, ten, dia chi hoac trang thai hoat dong thay doi; tao version moi.
5. Truoc moi mua tuyen sinh, admin/BA phai doi chieu lai ma truong, ten phap ly, dia chi va trang thai hoat dong tu website tuyen sinh chinh thuc.
6. Khong dung danh sach nay de suy dien ma nganh; ma nganh la dataset rieng theo Thong tu 74/2026/TT-BGDDT.
