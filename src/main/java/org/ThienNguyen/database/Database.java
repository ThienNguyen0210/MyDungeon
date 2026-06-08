package org.ThienNguyen.database;

import org.ThienNguyen.Main;
import java.sql.*;
import java.util.UUID;

public class Database {
    private Connection connection;

    public Database() {
        try {
            // Kết nối đến file sqlite trong folder plugin
            String url = "jdbc:sqlite:" + Main.getInstance().getDataFolder() + "/database.db";
            connection = DriverManager.getConnection(url);
            createTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTable() {
        try (Statement statement = connection.createStatement()) {
            // Giữ nguyên bảng cũ của bạn
            statement.execute("CREATE TABLE IF NOT EXISTS tower_cooldowns (" +
                    "uuid TEXT," +
                    "stage_num INTEGER," +
                    "last_win LONG," +
                    "PRIMARY KEY (uuid, stage_num))");

            // Bảng 1: Lưu thông tin Party (Leader)
            statement.execute("CREATE TABLE IF NOT EXISTS parties (" +
                    "party_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "party_name TEXT," + // Thêm cột này để lưu tên
                    "leader_uuid TEXT NOT NULL)");

            // Bảng 2: Lưu thành viên gắn với party_id
            statement.execute("CREATE TABLE IF NOT EXISTS party_members (" +
                    "member_uuid TEXT PRIMARY KEY," +
                    "party_id INTEGER," +
                    "FOREIGN KEY(party_id) REFERENCES parties(party_id) ON DELETE CASCADE)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // Lưu hoặc cập nhật thời gian thắng tầng
    public void setLastWin(UUID uuid, int stageNum, long time) {
        String sql = "INSERT OR REPLACE INTO tower_cooldowns(uuid, stage_num, last_win) VALUES(?,?,?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setInt(2, stageNum);
            pstmt.setLong(3, time);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void resetPlayerCooldown(UUID uuid, int stageNum) {
        String sql = "DELETE FROM tower_cooldowns WHERE uuid = ? AND stage_num = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setInt(2, stageNum);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // Lấy thời gian thắng lần cuối
    public long getLastWin(UUID uuid, int stageNum) {
        String sql = "SELECT last_win FROM tower_cooldowns WHERE uuid = ? AND stage_num = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setInt(2, stageNum);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("last_win");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    // Thêm vào class Database
    public int createParty(UUID leaderUuid, String name) {
        // 1. Thêm party_name vào câu lệnh INSERT
        String sql = "INSERT INTO parties(leader_uuid, party_name) VALUES(?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // 2. Set các tham số tương ứng
            pstmt.setString(1, leaderUuid.toString());
            pstmt.setString(2, name); // Lưu cái tên người chơi đặt vào đây

            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void addMemberToParty(UUID memberUuid, int partyId) {
        String sql = "INSERT OR REPLACE INTO party_members(member_uuid, party_id) VALUES(?,?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, memberUuid.toString());
            pstmt.setInt(2, partyId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteParty(int partyId) {
        String sql = "DELETE FROM parties WHERE party_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, partyId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public Connection getConnection() {
        try {
            // Kiểm tra nếu kết nối bị đóng thì khởi tạo lại
            if (connection == null || connection.isClosed()) {
                String url = "jdbc:sqlite:" + org.ThienNguyen.Main.getInstance().getDataFolder() + "/database.db";
                connection = java.sql.DriverManager.getConnection(url);
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }
}
