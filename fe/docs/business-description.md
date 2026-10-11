# Business Description - FE Integration Contract (MVP)

Tai lieu nay la nguon business cho Next.js + TypeScript. FE khong tu suy dien business rule ngoai noi dung duoi day. API contract chi tiet phai doc them tai lieu trong `be/docs/fe-guide/`.

## 1. Product rules

- Nguoi hoc va mentor mien phi.
- Mentor khong phai role login; hien thi theo `mentorStatus` cua user.
- Chi mentor `APPROVED` va `acceptingBookings = true` nhan/xac nhan booking.
- Admin la role quan tri duy nhat.
- Truong khong co tai khoan trong app; noi dung tai tro do admin/IT dua len.
- Noi dung tai tro phai co nhan `Tai tro` hoac `Quang cao`, khong tron voi chia se doc lap cua mentor.

## 2. User & Auth UI

User types: `SCHOOL_STUDENT`, `UNIVERSITY_STUDENT`, `WORKING_PROFESSIONAL`.

Mentor status: `NOT_APPLIED`, `PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED`.

Dang nhap Google OAuth; user co the dat them password. Email phai verified. Doi email can verify email moi. FE phai hien thi loi email da duoc lien ket vao tai khoan khac, khong tu tao tai khoan moi.

User duoc doi userType theo chieu hoc sinh -> sinh vien -> nguoi di lam. Khong hien thi thong bao rang doi userType se tu cap quyen mentor.

Consent UI phai hien thi policy version va luu ket qua tu API. Neu can consent cua nguoi dai dien, FE phai hien thi buoc rieng; checkbox cua hoc sinh khong duoc coi la thay the.

## 3. Mentor UI

Mot user chi co mot mentor profile. Profile co truong, nhom nganh chuan, nganh tu nhap, kinh nghiem, chu de tu van, link meeting mac dinh va `acceptingBookings`.

Khong hien thi public: file minh chung, email ca nhan, dien thoai, thong tin dinh danh. Profile phai hien role/userType va trang thai mentor phu hop.

Application co the `PENDING`, `REJECTED` (hien ly do), `APPROVED`, `SUSPENDED`. Cho phep sua va nop lai sau reject. `SUSPENDED` co the van dung forum va booking da confirm, nhung khong nhan booking moi.

School seed ban dau: TP.HCM, Can Tho, Dong Nai, Da Nang, Ha Noi. Nhom nganh co code/name/version nguon; khong hien thi nhu nganh cu the neu API tra ve nhom nganh.

## 4. Availability and booking UI

Mentor tao khoang ranh theo chu ky hai tuan; chon thoi luong 15/30/60/90 phut. FE chi hien slot duoc BE tra ve; khong tu tao slot bang logic rieng.

Slot co toi da 3 request `PENDING`. Sau request thu ba, UI phai chan submit va xu ly conflict neu API tu choi.

Booking statuses:

`PENDING`, `CONFIRMED`, `DECLINED`, `CANCELLED_BY_LEARNER`, `CANCELLED_BY_MENTOR`, `RESCHEDULE_REQUESTED`, `COMPLETED`, `NO_SHOW`, `EXPIRED`.

FE phai render action theo status va actor do BE tra ve. Khong cho confirm neu mentor dang overlap; xu ly `409 Conflict` bang reload slot/booking. `PENDING` qua gio bat dau co the hien la het han.

Huy/d oi truoc 4 gio la hop le. Huy trong 4 gio van hien dung `CANCELLED_*`, dong thoi hien canh bao vi pham/no-show equivalent theo response; khong doi nhan thanh `NO_SHOW` neu user chu dong huy.

No-show: sau gio hen, mot ben report; ben kia co 8 gio phan hoi/khiu nai. FE can hien countdown, ket qua dang cho admin hoac ket luan tu dong. Khong hien thi hai lan penalty cho cung booking.

Dat 3 gay trong thang `Asia/Ho_Chi_Minh` -> cam booking moi 14 ngay. Hien thi remaining ban duration neu API tra ve. Dien dan van dung duoc.

Meeting link chi hien khi booking `CONFIRMED` va current user la learner/mentor cua booking. Mentor co the dung link mac dinh hoac override link rieng.

## 5. Review UI

Sau `COMPLETED`, moi ben co 7 ngay de review; duoc sua mot lan trong 24 gio dau. Learner review mentor hien tren mentor profile/rating. Mentor review learner khong hien nhu public mentor rating. Review bi admin an phai hien trang thai/phu hop voi error response, khong tu xoa khoi cache neu API cho biet ly do.

## 6. Chat/report UI

Chat duoc tao khi booking dau tien giua learner va mentor duoc confirm; cac booking sau dung chung thread. Huy/complete khong tu khoa chat.

Block chan gui tin nhan hai chieu, lich su khong tu xoa. Cho phep report message. Chi hien moderation details khi API cap quyen admin; khong co man hinh doc hang loat chat.

## 7. Forum UI

Post types: `QUESTION`, `SHARING`. Co filter school va nhom nganh. Khong co anonymous post; hien role/userType ro rang.

Bai hop le hien ngay. Bai khop tu/cum tu cam hoac vuot nguong report hien `PENDING_REVIEW`, khong public; author thay trang thai va duoc edit/resubmit. Khong hien tu/cum tu nao da match.

Nguong tu an: hon 3 tai khoan khac nhau trong 15 phut; moi tai khoan chi tinh 1 lan trong moi dot xem xet.

## 8. Sponsored content UI

FE lay tu API cac banner/landing page dang hieu luc theo `Asia/Ho_Chi_Minh`. Khong hard-code campaign. Toi thieu co `title`, `content`, `image`, `targetUrl`, `position`, `startAt`, `endAt`, `sponsorLabel`.

Luon render nhan `Tai tro`/`Quang cao`. Noi dung het han khong hien. Khong co flow dang ky truong, hop dong, thanh toan hay campaign self-service.

## 9. Notifications and account deletion

In-app notification co unread/read, read one/read all; du lieu hien trong 12 thang. Email MVP chi cho booking request moi va booking confirm. Cac su kien booking khac dung in-app.

Delete account la quy trinh 30 ngay: xac nhan, khoa login/an profile, cho phep huy yeu cau trong thoi gian cho. Sau do API co the tra ve du lieu da an danh; FE khong duoc coi account id cu la profile public.

R2 file minh chung khong bao gio render public URL; FE chi dung signed URL ngan han tu endpoint co authz.

## 10. FE error and timezone rules

- Hien thi thoi gian theo `Asia/Ho_Chi_Minh`.
- Khong tu doi status dua tren timer client; goi API/refresh de xac nhan.
- Xu ly `401` bang refresh/login; `403` bang access denied; `404` bang not found; `409` bang conflict va reload; validation error hien theo field; business error hien message an toan tu API.
- Sau mutation quan trong (confirm, cancel, report, review, moderation), invalidate query cache lien quan.
- Khong log access token, meeting link, signed URL hoac thong tin minh chung vao client log.

## 11. Source of truth

Business rule nam trong file nay va `be/docs/business-description.md`. API field, endpoint, DTO, permission, error code va vi du TypeScript phai duoc cap nhat trong `be/docs/fe-guide/` moi khi BE thay doi.
