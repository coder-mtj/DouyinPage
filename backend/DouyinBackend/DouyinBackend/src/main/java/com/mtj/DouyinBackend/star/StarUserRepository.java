package com.mtj.DouyinBackend.star;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class StarUserRepository {
    // 定义明星类数据库mapper

    private final JdbcTemplate jdbcTemplate;

    // 匿名内部类方式
    private final RowMapper<StarUser> rowMapper = new RowMapper<StarUser>() {
        @Override
        public StarUser mapRow(ResultSet rs, int rowNum) throws SQLException {
            StarUser user = new StarUser();
            user.setId(rs.getLong("id"));
            user.setName(rs.getString("name"));
            user.setAvatarIndex(rs.getInt("avatar_index"));
            user.setVerified(rs.getInt("verified") != 0);
            user.setFollowed(rs.getInt("followed") != 0);
            user.setDouyinId(rs.getString("douyin_id"));
            user.setSpecialFollow(rs.getInt("special_follow") != 0);
            user.setRemark(rs.getString("remark"));
            return user;
        }
    };

    public StarUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 获取所有的元素
    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM star_user", Long.class);
        return count == null ? 0L : count;
    }
    // 分页方式获取元素以List的方式返回
    public List<StarUser> findPage(int page, int size) {
        int offset = page * size;
        String sql = "SELECT id, name, avatar_index, verified, followed, douyin_id, special_follow, remark " +
                "FROM star_user ORDER BY id LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, rowMapper, size, offset);
    }
    // 批量插入
    public void insertBatch(List<StarUser> users) {
        String sql = "INSERT INTO star_user (id, name, avatar_index, verified, followed, douyin_id, special_follow, remark) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, users, users.size(), (ps, user) -> {
            ps.setLong(1, user.getId());
            ps.setString(2, user.getName());
            ps.setInt(3, user.getAvatarIndex());
            ps.setInt(4, Boolean.TRUE.equals(user.getVerified()) ? 1 : 0);
            ps.setInt(5, Boolean.TRUE.equals(user.getFollowed()) ? 1 : 0);
            ps.setString(6, user.getDouyinId());
            ps.setInt(7, Boolean.TRUE.equals(user.getSpecialFollow()) ? 1 : 0);
            ps.setString(8, user.getRemark());
        });
    }
// 更新单个user
    public void updateUser(long id, Boolean specialFollow, Boolean followed, String remark) {
        String sql = "UPDATE star_user SET special_follow = ?, followed = ?, remark = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                Boolean.TRUE.equals(specialFollow) ? 1 : 0,
                Boolean.TRUE.equals(followed) ? 1 : 0,
                remark,
                id);
    }
}
