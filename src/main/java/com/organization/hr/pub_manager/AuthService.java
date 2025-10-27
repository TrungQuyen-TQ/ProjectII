package com.organization.hr.pub_manager;

public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    /**
     * Xác thực người dùng bằng cách truy vấn vào database.
     * @return "ADMIN" hoặc "EMPLOYEE" nếu thành công, null nếu thất bại.
     */
    public String authenticate(String username, String password) {
        // 1. Tìm user trong database
        User user = userDAO.findByUsername(username);

        // 2. Kiểm tra user có tồn tại và mật khẩu có khớp không
        // Chú ý: trong thực tế, bạn nên mã hóa mật khẩu thay vì so sánh trực tiếp
        if (user != null && user.getPassword().equals(password)) {
            // Xác thực thành công, trả về vai trò
            return user.getRole();
        }

        // 3. Xác thực thất bại
        return null;
    }
}