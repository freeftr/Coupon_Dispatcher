package com.freeftr.batch.couponhistory.domain.repository;

import com.freeftr.batch.couponhistory.dto.event.CouponHistoryEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CouponHistoryJdbcRepository {

	private final JdbcTemplate jdbcTemplate;

	public void bulkInsert(List<CouponHistoryEvent> couponHistoryEvents) {
		String sql = """
				INSERT INTO coupon_history
				(coupon_member_id, history_type, issued_date, created_at, modified_at)
				VALUES
				(?, ?, ?, NOW(), NOW())
				""";

		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps, int idx) throws SQLException {
				CouponHistoryEvent couponHistory = couponHistoryEvents.get(idx);
				ps.setLong(1, couponHistory.couponMemberId());
				ps.setString(2, couponHistory.type().toString());
				ps.setTimestamp(3, Timestamp.valueOf(couponHistory.issuedDate()));
			}

			@Override
			public int getBatchSize() {
				return couponHistoryEvents.size();
			}
		});
	}
}
