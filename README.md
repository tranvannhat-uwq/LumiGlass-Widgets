# LumiGlass Widgets – Premium Android Liquid Glass

Ứng dụng thiết kế, tùy biến và đặt các **Widget Kính Lỏng (Liquid Glass)** cao cấp lên màn hình chính Android, phát triển bằng **Kotlin**, **Jetpack Compose**, **Material 3** và **Procedural Liquid Glass Optical Engine**.

## ✨ Tính năng nổi bật

1. **Procedural Liquid Glass Optical Renderer**:
   - Mặt kính bán trong suốt đa tầng với độ trong suốt tùy chỉnh `0–100%`.
   - Đường viền phản chiếu ánh sáng bất đối xứng (`Sweep Specular Rim`).
   - Giả lập khúc xạ ánh sáng Fresnel dọc theo mép kính cong (`Edge Refraction Rim`).
   - Tán xạ sương mờ satin (`Frosted Satin Diffusion`) và ánh sáng phản xạ mặt kính (`Caustic Sheen`).
   - Chữ và biểu tượng nằm trực tiếp trên bề mặt kính với độ tương phản thông minh, không có khối nền chữ nhật thô kệch.

2. **6 Module Thông tin Độc lập (Bật/Tắt & Sắp xếp thứ tự)**:
   - **Đồng hồ kỹ thuật số**: Dùng `TextClock` native của Android (1 dòng hoặc tách 2 dòng Giờ/Phút, 12h/24h, hiện/ẩn giây, 5 bộ font, phát sáng mềm & đổ bóng), tự động cập nhật chính xác mà **không tốn pin chạy ngầm**.
   - **Thứ, ngày, tháng, năm**: Tiếng Việt đầy đủ (*"Thứ Năm, 19 tháng 11 năm 2026"*) và rút gọn (*"THỨ NĂM • 19/11"*).
   - **Âm lịch Việt Nam**: Thuật toán thiên văn **Hồ Ngọc Đức (UTC+7)** hoạt động 100% ngoại tuyến, hỗ trợ tháng nhuận, Can Chi, ngày Rằm, Mùng 1 và Lễ Tết Việt Nam.
   - **Thời tiết thực (Open-Meteo API)**: Hiển thị nhiệt độ hiện tại, Cao/Thấp, Độ ẩm, Khả năng mưa, Cảm giác như; hỗ trợ vị trí gần đúng hoặc 24 tỉnh/thành phố Việt Nam; có bộ nhớ đệm ngoại tuyến và tuyệt đối không dùng nhiệt độ giả.
   - **Phần trăm pin**: 4 phong cách biểu tượng kính (*Viên thuốc Pill, Vòng tròn Circle, Thanh tiến trình Bar, Biểu tượng + Số*), trạng thái sạc `⚡` và đổi màu theo mức pin.
   - **Nhiệt độ pin thực tế (°C)**: Đọc trực tiếp từ `BatteryManager.EXTRA_TEMPERATURE`, cảnh báo màu sắc khi pin ấm/nóng.

3. **5 Kích thước Widget Responsive & 5 Preset Kính Lỏng**:
   - Kích thước: **2×1 Mini Clock**, **2×2 Compact Glass**, **4×2 Essential Glass**, **4×3 Premium Glass**, **4×4 Dashboard Glass** (tự động co giãn bố cục khi kéo khung widget trên Launcher).
   - Phong cách kính: **01 Crystal Clear**, **02 Aurora Glass**, **03 Midnight Glass**, **04 Frosted Pearl**, **05 Adaptive Glass** + lưu Preset cá nhân.

4. **Cập nhật trực tiếp trong ứng dụng (GitHub In-App Updater)**:
   - Tự động kiểm tra bản phát hành mới từ **GitHub Releases API**.
   - Tải gói `.apk` với thanh tiến trình trực tiếp bên trong ứng dụng và kích hoạt trình cài đặt hệ thống qua `FileProvider`.

5. **Apple-style Liquid Glass In-App UI (v1.3.0)**:
   - Toàn bộ giao diện ứng dụng được nâng cấp bằng công nghệ Liquid Glass cao cấp:
     - **Thanh Navigation Bar nổi**: Khúc xạ trực tiếp màu sắc của các widget và thẻ nội dung cuộn bên dưới; con trỏ chuyển tab mượt mà bằng lò xo vật lý.
     - **Nút bấm kính đàn hồi**: Biến dạng xúc giác đàn hồi tự nhiên với hàm cản trở `tanh` và phản quang viền sáng.
     - **Thẻ vật liệu & Điều khiển kính**: Các thẻ Telemetry, thanh trượt Slider và công tắc Toggle kính lỏng tương tác sang trọng, hiện đại.
     - **Tương thích & Fallback 100%**: Hỗ trợ AGSL RuntimeShader trên Android 13+ (API 33+) và tự động suy biến an toàn sang acrylic frosted trên các thiết bị thấp hơn.

## 📦 Cài đặt & Build

- Tải file `.apk` mới nhất tại mục **[Releases](https://github.com/tranvannhat-uwq/LumiGlass-Widgets/releases)**.
- Hoặc build từ mã nguồn:
  ```bash
  ./gradlew testDebugUnitTest assembleDebug
  ```
