# ACTIVITY_DIAGRAMS.md — Luồng nghiệp vụ của hai chức năng phức tạp nhất

> Phục vụ **Bước 1, tiêu chí "Luồng nghiệp vụ" (15%)**: mô hình hoá 1 đến 2 chức năng phức tạp nhất, có xử lý nhánh ngoại lệ.
> Nguồn quy tắc: `REQUIREMENTS.md` mục 2, 3 và `docs/CHANGE_PT_ATTENDANCE_AND_PAY.md`. Ký hiệu: **S** là giờ bắt đầu buổi, **E** là giờ kết thúc; hình bầu dục là điểm kết thúc của luồng; nhánh đỏ ghi mã lỗi là ngoại lệ bị từ chối.
> Sơ đồ vẽ bằng Mermaid (GitHub tự hiển thị). Muốn xuất ảnh cho slide: dán phần code vào https://mermaid.live rồi chọn xuất PNG hoặc SVG.

## Sơ đồ 1 — Đặt lịch PT và vòng đời booking

Chức năng này có nhiều nhánh kiểm tra tuần tự (8 điều kiện từ chối) và nhiều trạng thái kết thúc theo thời gian.

```mermaid
flowchart TD
    A([Bắt đầu: hội viên chọn PT, ngày, giờ]) --> B{"Hôm nay và giờ đã qua hoặc còn dưới 30 phút?"}
    B -->|"Có"| X1["Từ chối: BOOKING_TOO_SOON"]
    B -->|"Không"| C{"Khung giờ của PT còn trống, không chồng giờ?"}
    C -->|"Không"| X2["Từ chối: BOOKING_SLOT_TAKEN"]
    C -->|"Có"| D{"Hội viên chưa có lịch trùng giờ với PT khác?"}
    D -->|"Không"| X3["Từ chối: BOOKING_DOUBLE_BOOKED"]
    D -->|"Có"| E{"Giờ nằm trong ca làm việc của PT?"}
    E -->|"Không"| X4["Từ chối: BOOKING_OUT_OF_WORKING_HOURS"]
    E -->|"Có"| F{"Hội viên có gói đang ACTIVE?"}
    F -->|"Không"| X5["Từ chối: MEMBERSHIP_NOT_FOUND"]
    F -->|"Có"| G{"Ngày đặt không vượt end_date của gói?"}
    G -->|"Không"| X6["Từ chối: MEMBERSHIP_EXPIRED"]
    G -->|"Có"| H{"Chưa vượt giới hạn buổi mỗi tuần của gói?"}
    H -->|"Không"| X7["Từ chối: BOOKING_WEEKLY_LIMIT_EXCEEDED"]
    H -->|"Có"| I{"Số buổi còn lại trừ số booking đang mở lớn hơn 0?"}
    I -->|"Không"| X8["Từ chối: MEMBERSHIP_NO_SESSION"]
    I -->|"Có"| J["Tạo booking, trạng thái PENDING"]
    J --> K{"PT xác nhận trước khi còn 1 giờ đến S?"}
    K -->|"Không"| Y1(["CANCELLED: hệ thống tự huỷ"])
    K -->|"Có"| L["Booking chuyển CONFIRMED"]
    L --> M{"Hội viên huỷ lịch?"}
    M -->|"Huỷ khi còn từ 2 giờ trở lên"| Y2(["CANCELLED: không phạt"])
    M -->|"Huỷ khi còn dưới 2 giờ"| Y3(["LATE_CANCELLED: mất 1 buổi, PT vẫn có lương"])
    M -->|"Không huỷ"| N{"Đến S + 15 phút: PT đã quét mặt vào?"}
    N -->|"Chưa"| Y4(["PT_NO_SHOW: PT không có lương, hội viên không mất buổi"])
    N -->|"Rồi"| O{"Hội viên đã quét mặt vào?"}
    O -->|"Chưa"| Y5(["NO_SHOW: hội viên mất 1 buổi, PT vẫn có lương"])
    O -->|"Rồi"| Z(["Buổi diễn ra: tiếp tục ở Sơ đồ 2"])
```

**Ghi chú:** hội viên huỷ booking còn `PENDING` luôn miễn phí (PT chưa cam kết); PT huỷ thì `CANCELLED`, không lương, không trừ buổi. Việc chuyển trạng thái theo thời gian dùng lazy-check, chạy mỗi khi có người đọc booking hoặc có lượt quét mặt, không cần cron.

## Sơ đồ 2 — PT điểm danh, chốt buổi, trừ buổi và tính lương

Chức năng này nối Booking với Membership (trừ buổi) và Lương PT qua một sự kiện chốt buổi, có nhánh ra sớm có hoặc không có lý do.

```mermaid
flowchart TD
    A([Buổi CONFIRMED, đến giờ bắt đầu S]) --> B{"PT quét mặt vào trong khoảng từ S - 30 phút đến S + 15 phút?"}
    B -->|"Không"| X1(["PT_NO_SHOW: không lương, không trừ buổi"])
    B -->|"Có"| C["Ghi pt_check_in_time"]
    C --> D{"Hội viên quét mặt vào trước S + 15 phút?"}
    D -->|"Không"| X2["NO_SHOW: phát sự kiện MEMBER_NO_SHOW, fullDuration = true"]
    D -->|"Có"| E["Ghi check_in_time của hội viên"]
    E --> F["PT dạy hết buổi và quét mặt ra"]
    F --> G{"Quét ra trước giờ kết thúc E?"}
    G -->|"Không"| H["fullDuration = true"]
    G -->|"Có"| I["Hệ thống yêu cầu PT nhập lý do, reasonRequired"]
    I --> J{"PT nhập lý do?"}
    J -->|"Có"| H
    J -->|"Không"| K["fullDuration = false: coi là không dạy đủ giờ"]
    H --> L{"PT bấm Hoàn thành: booking CONFIRMED, hội viên và PT đã quét đủ?"}
    K --> L
    L -->|"Không"| X3["Từ chối: BOOKING_NOT_CHECKED_IN hoặc BOOKING_PT_NOT_CHECKED_IN"]
    L -->|"Có"| M["Booking chuyển COMPLETED, phát sự kiện COMPLETED kèm fullDuration"]
    A2([Hội viên huỷ muộn dưới 2 giờ]) --> X4["LATE_CANCELLED: phát sự kiện LATE_CANCEL, fullDuration = true"]
    X2 --> N["Xử lý sự kiện chốt buổi"]
    M --> N
    X4 --> N
    N --> O{"Gói ACTIVE của hội viên còn buổi?"}
    O -->|"Còn"| P["Membership trừ 1 buổi remaining_sessions"]
    O -->|"Hết buổi hoặc gói đã hết hạn"| Q["Ghi cảnh báo, không chặn việc chốt buổi"]
    P --> R{"Điều kiện tính lương PT"}
    Q --> R
    R -->|"fullDuration = true và PT đã có mức lương"| S(["Ghi commissions: số tiền lấy từ trainer_pay_rates"])
    R -->|"fullDuration = false"| T(["Không ghi lương"])
    R -->|"PT chưa có mức lương"| U(["Ghi cảnh báo, bỏ qua"])
```

**Ghi chú:** bước trừ buổi và ghi lương chạy đồng bộ trong cùng transaction với việc chốt buổi, nên chúng **không được ném lỗi** (nếu ném, PT không chốt được buổi chỉ vì hội viên hết buổi hay PT chưa có mức lương), chỉ ghi cảnh báo. Mỗi booking tối đa một bản ghi lương (`commissions.booking_id` là UNIQUE), nên chốt hai lần không trả lương hai lần. Quy tắc cho trường hợp PT không bấm "Hoàn thành" sau giờ kết thúc đang chờ Phi quyết định, chưa vẽ.
