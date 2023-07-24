package com.fzk.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author zhike.feng
 * @datetime 2023-07-24 22:43
 */
public class Operate {
    public static void select(Connection conn) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT id,json_col FROM t2 WHERE id=?");
            stmt.setInt(1, 1);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    System.out.printf("id: %d, json_col: %s\n", resultSet.getInt("id"), resultSet.getString("json_col"));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
