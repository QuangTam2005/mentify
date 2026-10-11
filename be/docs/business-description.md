# Business Description - Mentify MVP

## 1. Muc dich va pham vi

Mentify ket noi hoc sinh THPT, sinh vien dai hoc/cao dang va nguoi da di lam voi mentor de tim hieu truong, nhom nganh, nganh hoc va nghe nghiep.

Nguoi hoc va mentor su dung mien phi. Doanh thu den tu noi dung tai tro cua truong. Truong lam viec voi Marketing ngoai ung dung; khong co tai khoan truong, CRM hop dong hay cong thanh toan trong MVP.

MVP uu tien:

1. Dang ky/dang nhap.
2. Dang ky va duyet mentor.
3. Tao lich ranh, sinh slot, booking, chat, danh gia.
4. Dien dan hoi dap/chia se co hau kiem.
5. Hien thi banner/landing page tai tro theo thoi gian hieu luc.
6. Bao ve du lieu, consent, bao cao va audit.

Khong xay video call noi bo, thanh toan, hoa hong mentor, role Marketing/IT rieng hoac cong tu phuc vu cho truong.

## 2. Actor va quyen

- `SCHOOL_STUDENT`: hoc sinh THPT.
- `UNIVERSITY_STUDENT`: sinh vien dai hoc/cao dang.
- `WORKING_PROFESSIONAL`: nguoi da di lam.
- `ADMIN`: role quan tri duy nhat.
- Mentor khong phai role dang nhap rieng. Quyen nhan booking mo theo `mentorStatus`.

`mentorStatus`: `NOT_APPLIED`, `PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED`.

Chi tai khoan co `mentorStatus = APPROVED`, `acceptingBookings = true` va khong bi cam booking moi moi duoc nhan/xac nhan booking.

## 3. Tai khoan va consent

- Dang nhap bang Google OAuth; cho phep thiet lap them mat khau cho cung tai khoan.
- Email dang nhap phai verified. Khong lien ket Google account vao tai khoan khac tuy tien.
- Doi email phai xac minh email moi va bao ve bang xac thuc JWT hien tai/phuong thuc khoi phuc.
- Mot tai khoan chi co mot ho so mentor; ho so bi tu choi duoc cap nhat va nop lai, luu lich su xet duyet.
- `userType` co the tu chuyen theo chieu hoc sinh -> sinh vien -> nguoi di lam. Luu `oldType`, `newType`, `changedAt`. Chuyen type khong cap quyen mentor.
- Luu consent theo `consentType`, `policyVersion`, `acceptedAt`, `subject`. Khi phap luat yeu cau doi voi nguoi chua thanh nien, phai co buoc xac nhan cua nguoi dai dien; khong coi checkbox cua hoc sinh la thay the.
- Quy trinh xoa tai khoan: xac thuc, khoa dang nhap va an ho so trong 30 ngay; sau do xoa thong tin nhan dien, auth, token, file minh chung va R2 file; an danh du lieu lich su can giu. Bao cao/audit va tranh chap giu theo chinh sach cong bo, de xuat 12 thang booking da an danh va 24 thang report/audit.

## 4. Du lieu truong va nhom nganh

School toi thieu: `schoolCode`, `schoolName`, `provinceOrCity`, `abbreviation`.

Seed ban dau: TP.HCM, Can Tho, Dong Nai, Da Nang, Ha Noi. Schema phai cho phep them dia phuong.

Mentor profile luu truong da/dang hoc, nhom nganh tu danh muc chuan va ten nganh tu nhap. Nhom nganh va nganh cu the la hai khai niem khac nhau.

Danh muc nhom nganh seed tu Thong tu `74/2026/TT-BGDDT` va phu luc; luu `code`, `name`, `sourceVersion`. Khong ghi de du lieu lich su khi cap nhat danh muc.

## 5. Mentor verification

Ung vien phai la sinh vien dai hoc/cao dang hoac nguoi da di lam tung hoc tai co so do. Minh chung:

- Sinh vien: the sinh vien/giay xac nhan.
- Nguoi da di lam: bang cap hoac minh chung tung theo hoc.

Admin duyet thu cong, luu ly do tu choi, cho phep nop lai va luu lich su. File minh chung, email, dien thoai va thong tin dinh danh khong cong khai.

Khong tai xac minh dinh ky. Admin co the `SUSPEND` quyen nhan booking sau khi xem xet phan anh. Mentor bi suspend van co the tham gia dien dan va hoan thanh booking da xac nhan, tru khi admin can thiep vi ly do an toan. Khong hoat dong lau chi dat `acceptingBookings = false`, khong tu dong suspend.

## 6. Availability, slot va booking

Mentor tao khoang ranh va co the tao lich lap theo chu ky hai tuan. Thoi luong slot chi nhan `15`, `30`, `60`, `90` phut. BE sinh slot nam tron trong khoang ranh, khong trung nhau trong lich cua cung mentor.

Mot slot co toi da 3 booking `PENDING`. Sau yeu cau thu ba, slot ngung nhan them. Mentor chi xac nhan mot yeu cau; cac yeu cau con lai tu dong `DECLINED` voi ly do slot da duoc nhan. Neu qua gio bat dau ma chua xac nhan, cac yeu cau `PENDING` thanh `EXPIRED` va thong bao trong ung dung. Khong giu slot doc quyen cho nguoi gui dau tien.

BE phai kiem tra overlap atomically/transactionally khi confirm; mentor khong duoc confirm hai booking chong thoi gian, ke ca tu hai availability khac nhau.

Booking states:

`PENDING`, `CONFIRMED`, `DECLINED`, `CANCELLED_BY_LEARNER`, `CANCELLED_BY_MENTOR`, `RESCHEDULE_REQUESTED`, `COMPLETED`, `NO_SHOW`, `EXPIRED`.

Moi transition phai khai bao actor, dieu kien truoc, side effect va dieu kien loi. `RESCHEDULE_REQUESTED` phai giu trang thai truoc do va ket qua chap nhan/tu choi.

Mentor cung cap Google Meet/Zoom khi confirm. Co the co link mac dinh trong profile va override theo booking. Link chi tra ve cho hai nguoi tham gia booking `CONFIRMED`.

Huy/d doi lich truoc it nhat 4 gio la hop le. Huy trong 4 gio van dung trang thai `CANCELLED_*` nhung ghi mot vi pham tuong duong no-show; khong doi trang thai booking thanh `NO_SHOW`.

Sau gio hen, mot ben co the bao no-show. Ben bi bao co 8 gio de phan hoi/khieu nai. Khong phan hoi: he thong tu ket luan va ghi 1 gay. Co phan hoi: chuyen admin xem xet. Khong cong trung gay cho cung mot booking do huy sat gio va bao no-show.

Gay tinh theo thang duong lich, timezone `Asia/Ho_Chi_Minh`. Dat 3 gay trong cung thang: cam tao/nhan booking moi 14 ngay. Booking da confirm duoc phep hoan thanh neu an toan hoac admin can thiep. Dien dan khong bi cam theo hinh phat booking. Luu lich su gay; gay khong tinh nguong khi sang thang moi.

## 7. Review

Sau `COMPLETED`, hai ben duoc danh gia trong 7 ngay; duoc sua mot lan trong 24 gio dau. Learner danh gia mentor bang diem va noi dung; hien tren mentor profile va tinh rating. Mentor danh gia learner de ho tro chat luong/xu ly vi pham, khong hien nhu rating mentor. Admin co the an review vi pham, bat buoc luu ly do va audit.

## 8. Chat va bao cao

Khi booking dau tien giua learner va mentor duoc confirm, tao mot chat dung chung cho cap nguoi dung. Booking sau dung lai chat nay. Huy/complete khong tu dong khoa chat.

Block chan tin nhan moi hai chieu; khong tu xoa lich su. Cho phep bao cao tin nhan. Khi admin mo chat theo report, luu `adminId`, `reason`, `viewedAt`; khong co man hinh doc hang loat chat.

## 9. Dien dan va moderation

Loai bai: `QUESTION` (Hoi dap), `SHARING` (Chia se). Co the gan school va nhom nganh de loc/tim kiem. Khong an danh; role nguoi dang hien ro.

Bai hop le dang ngay va hau kiem. Admin quan ly tu/cum tu cam. So khop khong phan biet hoa thuong va dau tieng Viet; chua can xu ly bien the viet sai. Khop tu cam -> `PENDING_REVIEW`, khong cong khai; tac gia thay trang thai, duoc sua va gui lai, khong xem tu nao kich hoat.

Qua 3 bao cao tu cac tai khoan khac nhau trong 15 phut -> an cong khai va `PENDING_REVIEW`. Moi user chi tinh 1 luot trong moi dot xem xet; co the bao lai sau khi report cu da xu ly.

## 10. Noi dung tai tro

IT tao template; admin nhap/chinh noi dung da Marketing chot. Backend luu noi dung, khong hard-code tung campaign o frontend.

Toi thieu: `title`, `content`, `image`, `targetUrl`, `position`, `startAt`, `endAt`, `sponsorLabel`, preview, publish/hide. Chi tra noi dung dang hieu luc; auto hide theo `Asia/Ho_Chi_Minh`. Moi vi tri phai hien nhan `Tai tro`/`Quang cao`. Khong cho phep truong sua/chi phoi noi dung mentor.

## 11. File va thong bao

- File luu Cloudflare R2 bucket private.
- Minh chung mentor chi truy cap qua signed URL ngan han sau khi authz admin; gioi han mime/size; ghi audit truy cap.
- In-app notification co `read/unread`, read one/read all; giu 12 thang va cleanup dinh ky.
- Email thong bao nghiep vu MVP chi gui khi learner tao booking request (toi mentor) va mentor confirm (toi learner); email bao mat bat buoc cho password reset va xac minh doi email la ngoai le. Cac su kien khac dung in-app notification.

## 12. Runtime constraint

MVP chay tren VPS 6 CPU, RAM 8 GB, SSD 120 GB. Uu tien open source, kien truc gon, backup, bao mat va dung luong R2. Timezone nghiep vu `Asia/Ho_Chi_Minh`, giao dien ban dau tieng Viet.

## 12.1. Muc tieu thiet ke Backend

- Backend duoc thiet ke de de bao tri, linh hoat va chay on dinh trong pham vi MVP.
- Uu tien modular monolith, PostgreSQL, transaction ro rang va cac giai phap don gian co the van hanh tot.
- Khong overengineer hoac chuan bi san kien truc phan tan khi chua co nhu cau thuc te.
- Quy mo muc tieu ban dau khoang 500-1000 CCU; moi toi uu mo rong phai dua tren do luong va bottleneck thuc te.
- Khong them microservice, Redis, queue hoac outbox neu chua co yeu cau van hanh cu the.

## 13. Nguyen tac API/BE bat buoc

- API phai the hien ro authn/authz, precondition, postcondition, state transition, loi validation va conflict/concurrency.
- Khong tra file minh chung, link hop hay du lieu nhay cam cho user khong du quyen.
- Cac tac vu slot/confirm/report threshold/no-show/ban phai idempotent va an toan khi retry.
- Luu audit cho admin action, moderation, mentor review, report/no-show, xoa/an danh du lieu.
- Neu phat hien mau thuan voi tai lieu nay, dung implement phan mau thuan va neu ro mau thuan trong PR/issue truoc khi thay doi business.
