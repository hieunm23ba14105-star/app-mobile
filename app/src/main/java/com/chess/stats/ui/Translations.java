package com.chess.stats.ui;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Translations {
    private Translations() {}

    public static String text(String key) {
        switch (key) {
            case "games": return "Ván đấu";
            case "overview": return "Tổng quan";
            case "current": return "Điểm hiện tại";
            case "peak": return "Điểm cao nhất";
            case "updated": return "Cập nhật";
            case "results": return "Kết quả ván đấu";
            case "wins": return "Thắng";
            case "draws": return "Hòa";
            case "losses": return "Thua";
            case "games_count": return "ván";
            case "rating": return "điểm";
            case "peak_game": return "Xem ván đạt mốc ↗";
            case "loading": return "Đang tải thống kê…";
            case "empty": return "Nhập tên tài khoản Chess.com để bắt đầu.";
            case "not_found": return "Không tìm thấy hồ sơ. Kiểm tra tên tài khoản rồi thử lại.";
            case "rate_limited": return "Chess.com đang giới hạn yêu cầu. Vui lòng thử lại sau.";
            case "http_error": return "Chess.com chưa thể trả dữ liệu (mã lỗi %s). Hãy thử lại.";
            case "connection": return "Không thể kết nối Chess.com. Kiểm tra mạng rồi thử lại.";
            case "invalid_data": return "Hồ sơ chưa có dữ liệu ván đấu để hiển thị.";
            case "time_per_move": return "Thời gian / nước";
            case "timeout": return "Hết giờ · 90 ngày";
            case "tournaments": return "Giải đấu";
            case "points": return "Điểm giải";
            case "highest_finish": return "Thứ hạng cao nhất";
            case "daily": return "Cờ hằng ngày";
            case "rapid": return "Cờ nhanh";
            case "blitz": return "Cờ chớp";
            case "bullet": return "Cờ siêu chớp";
            case "chess": return "Cờ vua";
            case "chess960": return "Cờ Fischer 960";
            case "bughouse": return "Cờ đôi";
            case "kingofthehill": return "Vua của đồi";
            case "threecheck": return "Ba lần chiếu";
            case "crazyhouse": return "Cờ hỗn hợp";
            default: return key;
        }
    }

    public static String date(Long epochSeconds) {
        if (epochSeconds == null || epochSeconds <= 0) return "—";
        return new SimpleDateFormat("d MMM yyyy", new Locale("vi", "VN"))
                .format(new Date(epochSeconds * 1000L));
    }

    public static String rating(Integer rating) {
        if (rating == null) return "—";
        return String.format(Locale.getDefault(), "%,d", rating).replace(',', '.');
    }

    public static String playerSubtitle(String username, int gameTypeCount) {
        return "@" + username + " · " + gameTypeCount + " thể loại cờ";
    }
}
