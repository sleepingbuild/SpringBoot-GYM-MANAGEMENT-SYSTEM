# Danh sách User Story của hệ thống quản lý phòng gym (GMS)

Nguồn: các endpoint trong `API_DESIGN.md`, vai trò trong `REQUIREMENTS.md` và thay đổi mới nhất về điểm danh PT, luật no-show, lương PT theo buổi. Tên use case theo dạng **Động từ + Danh từ**. Dấu (*) xem mục 3.

## 1. Tác nhân

| Tác nhân | Vai trò trong hệ thống | Giới hạn quyền hạn |
|---|---|---|
| Khách | Chưa có tài khoản | Chưa đăng nhập. Chỉ được đăng ký, đăng nhập và xem gói tập. Không truy cập được dữ liệu cá nhân hay nghiệp vụ nào. |
| Hội viên | `MEMBER` | Chỉ thao tác trên dữ liệu của chính mình (hồ sơ, gói tập, lịch tập, thanh toán, điểm danh). Không xem được dữ liệu hội viên khác, không quản lý gói tập hay sản phẩm. |
| Huấn luyện viên | `TRAINER` | Quản lý ca làm việc và lịch dạy của chính mình, xác nhận và hoàn thành buổi dạy, xem lương của mình. Không bán gói, không xem báo cáo hệ thống. |
| Lễ tân | `RECEPTIONIST` | Vận hành tại quầy: bán và gia hạn gói, xác nhận thanh toán, đặt lịch hộ, điểm danh kiosk, đăng ký khuôn mặt hộ, bán lẻ. Không cấu hình hệ thống, không xem báo cáo tài chính toàn hệ thống. |
| Nhân viên Sales | `SALES` | Quản lý khách tiềm năng (lead), chăm sóc hội viên sắp hết hạn và chấm công bằng khuôn mặt cho chính mình (lương cứng, không có hoa hồng). Không bán gói trực tiếp, không xem thanh toán hay báo cáo. |
| Quản trị viên | `SUPER_ADMIN` | Toàn quyền cấu hình (gói tập, lương PT, chi nhánh, nhân sự) và xem mọi báo cáo toàn hệ thống. |

Chức năng dùng chung cho mọi tác nhân đã đăng nhập (C1) nằm ở mục 2. Việc hệ thống tự chuyển trạng thái theo thời gian (hết hạn gói, huỷ lịch quá hạn xác nhận, ghi nhận vắng mặt) chạy khi có người đọc dữ liệu, không phải một tác nhân.

## 2. User story theo tác nhân

### Mọi người dùng đã đăng nhập (1 story)

| ID | Use case | User story |
|---|---|---|
| C1 | Đăng xuất hệ thống | Là người dùng đã đăng nhập, tôi muốn đăng xuất khỏi hệ thống để phiên đăng nhập hiện tại không thể bị dùng lại. |

### Khách (3 story)

| ID | Use case | User story |
|---|---|---|
| G1 | Đăng ký tài khoản | Là Khách, tôi muốn đăng ký tài khoản hội viên bằng email và mật khẩu để mua được gói tập và đặt lịch. |
| G2 | Đăng nhập hệ thống | Là Khách, tôi muốn đăng nhập bằng email và mật khẩu để dùng các chức năng đúng với vai trò của mình. |
| G3 | Xem danh sách gói tập (*) | Là Khách, tôi muốn xem các gói tập đang mở bán cùng giá để cân nhắc trước khi đăng ký. |

### Hội viên (MEMBER) (17 story)

| ID | Use case | User story |
|---|---|---|
| M1 | Xem hồ sơ cá nhân | Là Hội viên, tôi muốn xem thông tin hồ sơ của mình để kiểm tra thông tin đang lưu. |
| M2 | Cập nhật hồ sơ cá nhân | Là Hội viên, tôi muốn cập nhật thông tin thể chất và mục tiêu tập luyện để PT hiểu đúng tình trạng của tôi. |
| M3 | Tải lên ảnh đại diện | Là Hội viên, tôi muốn tải lên ảnh đại diện để tài khoản dễ nhận diện hơn. |
| M4 | Mua gói tập | Là Hội viên, tôi muốn chọn và mua một gói tập để được sử dụng phòng tập sau khi thanh toán được xác nhận. |
| M5 | Xem gói tập hiện tại | Là Hội viên, tôi muốn xem gói đang hoạt động, ngày hết hạn và số buổi còn lại để biết khi nào cần gia hạn. |
| M6 | Xem lịch sử gói tập | Là Hội viên, tôi muốn xem các gói đã đăng ký, kể cả đã hết hạn hoặc đã huỷ để đối chiếu quá trình sử dụng. |
| M7 | Nâng cấp gói tập | Là Hội viên, tôi muốn đổi sang gói có giá cao hơn để hưởng quyền lợi tốt hơn (gói cũ huỷ ngay, gói mới có hiệu lực khi thanh toán xong). |
| M8 | Hạ cấp gói tập | Là Hội viên, tôi muốn chuyển sang gói giá thấp hơn kể từ khi gói hiện tại hết hạn để giảm chi phí mà không mất thời gian đã mua. |
| M9 | Xem lịch sử thanh toán | Là Hội viên, tôi muốn xem các giao dịch thanh toán của mình để theo dõi chi tiêu. |
| M10 | Xem lịch trống của PT | Là Hội viên, tôi muốn xem các khung giờ còn trống của một PT theo ngày để chọn giờ phù hợp. |
| M11 | Đặt lịch tập với PT | Là Hội viên, tôi muốn đặt một buổi tập với PT vào khung giờ trống để có buổi tập đúng giờ (hệ thống chặn trùng giờ, quá giới hạn buổi mỗi tuần hoặc hết buổi). |
| M12 | Sửa lịch tập | Là Hội viên, tôi muốn đổi giờ buổi tập đã đặt để linh hoạt khi thay đổi kế hoạch (vẫn áp đủ quy tắc đặt lịch). |
| M13 | Hủy lịch tập | Là Hội viên, tôi muốn huỷ buổi tập đã đặt để không giữ chỗ khi không đến được (huỷ muộn dưới 2 giờ bị tính mất 1 buổi). |
| M14 | Xem lịch tập của tôi | Là Hội viên, tôi muốn xem các buổi đã đặt cùng trạng thái để nắm được lịch tập. |
| M15 | Đăng ký khuôn mặt | Là Hội viên, tôi muốn đăng ký khuôn mặt của mình để điểm danh bằng khuôn mặt. |
| M16 | Điểm danh vào phòng tập bằng khuôn mặt | Là Hội viên, tôi muốn quét mặt để điểm danh vào và ra để ghi nhận lượt tập không cần thẻ (với buổi PT thì xác nhận có mặt). |
| M17 | Xem lịch sử check-in (*) | Là Hội viên, tôi muốn xem lịch sử các lượt vào phòng tập để theo dõi mức độ chuyên cần. |

### Huấn luyện viên (TRAINER) (14 story)

| ID | Use case | User story |
|---|---|---|
| T1 | Xem hồ sơ cá nhân | Là Huấn luyện viên, tôi muốn xem thông tin hồ sơ của mình để kiểm tra thông tin đang lưu. |
| T2 | Cập nhật hồ sơ cá nhân | Là Huấn luyện viên, tôi muốn cập nhật hồ sơ và ảnh đại diện để thông tin của tôi luôn đúng. |
| T3 | Tạo ca làm việc | Là Huấn luyện viên, tôi muốn đăng ký ca làm việc theo ngày và giờ cụ thể để hội viên đặt lịch trong khung giờ của tôi (không cho chọn ngày quá khứ). |
| T4 | Sửa ca làm việc | Là Huấn luyện viên, tôi muốn sửa giờ hoặc đánh dấu tạm nghỉ một ca để lịch phản ánh đúng khả năng của tôi. |
| T5 | Xóa ca làm việc | Là Huấn luyện viên, tôi muốn xoá ca không còn làm để không còn khung giờ trống ảo. |
| T6 | Xem lịch dạy | Là Huấn luyện viên, tôi muốn xem các buổi được đặt theo ngày để chuẩn bị buổi dạy. |
| T7 | Xác nhận lịch tập | Là Huấn luyện viên, tôi muốn xác nhận buổi hội viên vừa đặt để cam kết dạy (buổi chưa xác nhận sẽ tự huỷ khi sát giờ). |
| T8 | Hủy lịch tập | Là Huấn luyện viên, tôi muốn huỷ buổi khi không thể dạy để hội viên được báo và không bị trừ buổi. |
| T9 | Điểm danh khuôn mặt buổi dạy | Là Huấn luyện viên, tôi muốn quét mặt khi vào và khi ra buổi dạy để có bằng chứng đã dạy làm căn cứ trả công. |
| T10 | Nhập lý do rời buổi sớm | Là Huấn luyện viên, tôi muốn nhập lý do khi ra trước giờ kết thúc để buổi vẫn được tính công khi có lý do chính đáng. |
| T11 | Hoàn thành buổi tập | Là Huấn luyện viên, tôi muốn đánh dấu hoàn thành buổi đã dạy (sau khi hai bên đã điểm danh) để hệ thống trừ buổi của hội viên và tính công cho tôi. |
| T12 | Đăng ký khuôn mặt | Là Huấn luyện viên, tôi muốn đăng ký khuôn mặt của mình để điểm danh bằng khuôn mặt. |
| T13 | Xem lương theo buổi | Là Huấn luyện viên, tôi muốn xem các buổi được tính công và số tiền để đối chiếu lương. |
| T14 | Xem chỉ số hội viên đang huấn luyện | Là Huấn luyện viên, tôi muốn xem thông tin thể chất và mục tiêu của hội viên có lịch với tôi để xây dựng giáo án phù hợp. |

### Lễ tân (RECEPTIONIST) (23 story)

| ID | Use case | User story |
|---|---|---|
| R1 | Bán gói tập cho hội viên | Là Lễ tân, tôi muốn tạo đăng ký gói tập cho hội viên tại quầy để hội viên bắt đầu dùng gói sau khi tôi xác nhận thanh toán. |
| R2 | Gia hạn gói tập | Là Lễ tân, tôi muốn đăng ký gói mới cho hội viên có gói đã hết hạn để hội viên tiếp tục tập. |
| R3 | Nâng cấp gói tập hộ hội viên | Là Lễ tân, tôi muốn nâng cấp gói cho hội viên ngay tại quầy để hội viên không phải tự thao tác. |
| R4 | Hạ cấp gói tập hộ hội viên | Là Lễ tân, tôi muốn lên lịch hạ cấp gói cho hội viên để hội viên đổi gói rẻ hơn khi gói cũ hết hạn. |
| R5 | Xác nhận thanh toán tại quầy | Là Lễ tân, tôi muốn xác nhận đã thu tiền một giao dịch để gói tập hoặc đơn bán lẻ được ghi nhận hoàn tất. |
| R6 | Xem danh sách giao dịch theo ngày | Là Lễ tân, tôi muốn xem các giao dịch trong khoảng ngày để đối soát tiền thu cuối ngày. |
| R7 | Đặt lịch tập hộ hội viên | Là Lễ tân, tôi muốn đặt lịch PT thay cho hội viên để hội viên có lịch mà không cần dùng ứng dụng. |
| R8 | Sửa lịch tập hộ hội viên | Là Lễ tân, tôi muốn đổi giờ buổi tập thay cho hội viên để xử lý yêu cầu đổi lịch tại quầy. |
| R9 | Hủy lịch tập hộ hội viên | Là Lễ tân, tôi muốn huỷ buổi tập thay cho hội viên để giải phóng khung giờ (huỷ muộn dưới 2 giờ vẫn bị tính mất buổi). |
| R10 | Xem danh sách lịch tập theo ngày | Là Lễ tân, tôi muốn xem toàn bộ lịch tập theo ngày và chi nhánh để điều phối hoạt động phòng tập. |
| R11 | Đăng ký khuôn mặt hộ hội viên | Là Lễ tân, tôi muốn đăng ký khuôn mặt cho hội viên từ ảnh tĩnh để hội viên điểm danh được mà không cần thiết bị cá nhân. |
| R12 | Xem hồ sơ khuôn mặt | Là Lễ tân, tôi muốn kiểm tra hội viên đã có hồ sơ khuôn mặt chưa để biết có cần đăng ký thêm hay không. |
| R13 | Xóa hồ sơ khuôn mặt | Là Lễ tân, tôi muốn xoá hồ sơ khuôn mặt khi sai hoặc hội viên yêu cầu để dữ liệu sinh trắc không còn được lưu. |
| R14 | Điểm danh hội viên tại kiosk | Là Lễ tân, tôi muốn dùng camera tại quầy nhận diện hội viên vào và ra để ghi nhận lượt vào phòng mà hội viên không phải thao tác. |
| R15 | Chấm công bằng khuôn mặt | Là Lễ tân, tôi muốn quét mặt vào và ra ca làm của mình để có chấm công chính xác (đúng giờ, đi muộn, về sớm). |
| R16 | Xem bảng chấm công của tôi | Là Lễ tân, tôi muốn xem lịch sử chấm công của mình để đối chiếu giờ làm. |
| R17 | Thêm sản phẩm bán lẻ | Là Lễ tân, tôi muốn thêm sản phẩm mới (tên, loại, giá, tồn kho) để có hàng để bán tại quầy. |
| R18 | Cập nhật sản phẩm bán lẻ | Là Lễ tân, tôi muốn sửa giá, tồn kho hoặc ngừng bán sản phẩm để thông tin bán hàng luôn đúng. |
| R19 | Xem danh sách sản phẩm | Là Lễ tân, tôi muốn xem các sản phẩm đang bán cùng tồn kho để biết còn hàng hay không. |
| R20 | Tạo đơn bán lẻ | Là Lễ tân, tôi muốn lập đơn bán đồ uống hoặc thực phẩm bổ sung cho hội viên hoặc khách vãng lai để hệ thống tự trừ tồn kho và ghi nhận thanh toán. |
| R21 | Xem danh sách đơn bán lẻ | Là Lễ tân, tôi muốn xem các đơn đã bán để đối soát doanh thu bán lẻ. |
| R22 | Tạo tài khoản hội viên tại quầy (*) | Là Lễ tân, tôi muốn tạo tài khoản cho khách mới đến đăng ký tại quầy để bán được gói ngay khi khách có mặt. |
| R23 | Đăng ký khuôn mặt | Là Lễ tân, tôi muốn đăng ký khuôn mặt của mình để chấm công bằng khuôn mặt. |

### Nhân viên Sales/CSKH (SALES) (8 story)

| ID | Use case | User story |
|---|---|---|
| S1 | Thêm khách tiềm năng | Là Nhân viên Sales, tôi muốn ghi nhận một khách tiềm năng (tên, số điện thoại, nguồn) để không bỏ sót khách cần chăm sóc. |
| S2 | Cập nhật thông tin khách tiềm năng | Là Nhân viên Sales, tôi muốn cập nhật trạng thái và ghi chú chăm sóc của lead để theo dõi tiến trình chốt khách. |
| S3 | Xem danh sách khách tiềm năng | Là Nhân viên Sales, tôi muốn xem các lead được giao cho tôi để ưu tiên việc cần liên hệ. |
| S4 | Chuyển đổi khách tiềm năng thành hội viên | Là Nhân viên Sales, tôi muốn chuyển một lead đã chốt thành hội viên thật để lead có tài khoản và có thể mua gói. |
| S5 | Xem hội viên sắp hết hạn | Là Nhân viên Sales, tôi muốn xem danh sách hội viên sắp hết hạn gói để chủ động liên hệ gia hạn. |
| S6 | Chấm công bằng khuôn mặt | Là Nhân viên Sales, tôi muốn quét mặt vào và ra ca làm của mình để có chấm công chính xác (đúng giờ, đi muộn, về sớm). |
| S7 | Xem bảng chấm công của tôi | Là Nhân viên Sales, tôi muốn xem lịch sử chấm công của mình để đối chiếu giờ làm. |
| S8 | Đăng ký khuôn mặt | Là Nhân viên Sales, tôi muốn đăng ký khuôn mặt của mình để chấm công bằng khuôn mặt. |

### Quản trị viên (SUPER_ADMIN) (22 story)

| ID | Use case | User story |
|---|---|---|
| A1 | Thêm gói tập | Là Quản trị viên, tôi muốn tạo gói tập mới (loại, giá, thời hạn, số buổi, giới hạn buổi mỗi tuần) để có sản phẩm để bán. |
| A2 | Sửa gói tập | Là Quản trị viên, tôi muốn sửa thông tin gói tập để gói luôn đúng chính sách hiện hành. |
| A3 | Ngừng bán gói tập | Là Quản trị viên, tôi muốn ngừng bán một gói (không xoá cứng) để hội viên cũ vẫn giữ được lịch sử gói. |
| A4 | Xem toàn bộ gói tập | Là Quản trị viên, tôi muốn xem mọi gói tập kể cả đã ngừng bán để quản lý đầy đủ danh mục. |
| A5 | Xem báo cáo doanh thu | Là Quản trị viên, tôi muốn xem doanh thu theo kỳ để đánh giá kết quả kinh doanh. |
| A6 | Xem báo cáo tỷ lệ gia hạn | Là Quản trị viên, tôi muốn xem tỷ lệ hội viên gia hạn gói để đánh giá mức giữ chân hội viên. |
| A7 | Xem báo cáo lưu lượng check-in | Là Quản trị viên, tôi muốn xem số lượt vào phòng tập theo thời gian để sắp xếp nhân sự và giờ mở cửa. |
| A8 | Xem báo cáo gói bán chạy | Là Quản trị viên, tôi muốn xem các gói được mua nhiều nhất để điều chỉnh danh mục gói. |
| A9 | Xem báo cáo tổng lương PT | Là Quản trị viên, tôi muốn xem tổng lương theo buổi của các PT để kiểm soát chi phí nhân sự. |
| A10 | Cấu hình mức lương theo buổi cho PT | Là Quản trị viên, tôi muốn đặt mức lương mỗi buổi cho từng PT để tiền công được tính tự động và đúng. |
| A11 | Xem lương PT theo tháng | Là Quản trị viên, tôi muốn xem lương của một PT trong tháng để chi trả đúng. |
| A12 | Xem báo cáo chấm công nhân viên | Là Quản trị viên, tôi muốn xem chấm công theo ngày của nhân viên để đánh giá chuyên cần. |
| A13 | Xem danh sách lịch tập toàn hệ thống | Là Quản trị viên, tôi muốn xem mọi lịch tập theo ngày và chi nhánh để giám sát vận hành. |
| A14 | Xem danh sách giao dịch toàn hệ thống | Là Quản trị viên, tôi muốn xem mọi giao dịch trong khoảng ngày để đối soát tài chính. |
| A15 | Quản lý ca làm việc của PT | Là Quản trị viên, tôi muốn xem và sửa ca làm việc của bất kỳ PT nào để xử lý khi PT không tự thao tác được. |
| A16 | Quản lý hồ sơ khuôn mặt | Là Quản trị viên, tôi muốn đăng ký hộ, xem và xoá hồ sơ khuôn mặt của hội viên và nhân viên để dữ liệu sinh trắc được kiểm soát tập trung. |
| A17 | Quản lý sản phẩm bán lẻ | Là Quản trị viên, tôi muốn thêm, sửa, ngừng bán sản phẩm để danh mục bán lẻ luôn đúng. |
| A18 | Tạo tài khoản nhân viên (*) | Là Quản trị viên, tôi muốn tạo tài khoản cho lễ tân, PT, sales để nhân sự mới có thể làm việc. |
| A19 | Phân quyền tài khoản (*) | Là Quản trị viên, tôi muốn gán hoặc đổi vai trò của một tài khoản để mỗi người chỉ có đúng quyền cần thiết. |
| A20 | Khóa tài khoản (*) | Là Quản trị viên, tôi muốn khoá hoặc mở khoá một tài khoản để chặn người dùng vi phạm hoặc nhân viên đã nghỉ. |
| A21 | Quản lý chi nhánh (*) | Là Quản trị viên, tôi muốn thêm, sửa hoặc đóng chi nhánh để hệ thống hỗ trợ nhiều cơ sở. |
| A22 | Xem danh sách khách tiềm năng | Là Quản trị viên, tôi muốn xem toàn bộ khách tiềm năng và trạng thái chăm sóc để theo dõi hiệu quả của đội Sales. |

## 3. Ghi chú

(*) Story chưa có endpoint tại thời điểm viết. Đã đưa vào phạm vi theo quyết định của Agent 1 và Phi ngày 2026-10-16 (`DECISIONS_DB_V2.md`):

| Story | Endpoint | Ai làm |
|---|---|---|
| G3 Xem danh sách gói tập | `GET /api/v1/packages` và `GET /api/v1/packages/{id}` công khai, chỉ trả gói đang bán | Agent 2 (mã), Agent 1 (quy tắc mở trong `SecurityConfig`) |
| M17 Xem lịch sử check-in | `GET /api/v1/check-ins/me` (hợp của `gym_check_ins` và `bookings`) | Agent 4, cùng thay đổi CH2 |
| R22 Tạo tài khoản hội viên tại quầy | `createMemberAccount` | Agent 1 |
| A18 Tạo tài khoản nhân viên | `POST /users` | Agent 1 |
| A19 Phân quyền tài khoản | `PUT /users/{id}/roles` | Agent 1 |
| A20 Khóa tài khoản | `PATCH /users/{id}/status` | Agent 1 |
| A21 Quản lý chi nhánh | CRUD `/branches` | Agent 1 |

Story A10 (cấu hình lương PT) đã có endpoint `GET/PUT /api/v1/trainer-pay-rates/{trainerId}` (Agent 5) nên không còn đánh dấu (*).

**Ngoài phạm vi phiên bản 1.0:** lớp tập nhóm (Group X), nhắc gia hạn qua Zalo/SMS, chốt ca thu ngân, báo cáo nâng cao theo chi nhánh. Hoa hồng cho Sales đã bỏ, chỉ PT được tính lương theo buổi.

Tổng cộng **88 user story**: Người dùng đã đăng nhập 1, Khách 3, Hội viên 17, Huấn luyện viên 14, Lễ tân 23, Nhân viên Sales 8, Quản trị viên 22. Trong đó 87 story thao tác trên dữ liệu trong bảng, riêng C1 (Đăng xuất) chỉ dùng Redis.
