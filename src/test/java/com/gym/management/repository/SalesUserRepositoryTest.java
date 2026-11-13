package com.gym.management.repository;

import com.gym.management.dto.response.SalesUserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesUserRepositoryTest {

    @Mock private NamedParameterJdbcTemplate jdbc;
    @InjectMocks private SalesUserRepository repository;

    @SuppressWarnings("unchecked")
    @Test
    void findActiveSales_queriesActiveSalesSortedByNameAndMapsRows() throws SQLException {
        UUID id = UUID.randomUUID();
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        repository.findActiveSales();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<RowMapper<SalesUserResponse>> mapper = ArgumentCaptor.forClass(RowMapper.class);
        verify(jdbc).query(sql.capture(), mapper.capture());
        assertThat(sql.getValue()).contains("r.name = 'SALES'", "u.status = 'ACTIVE'", "ORDER BY u.full_name ASC");

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("id")).thenReturn(id.toString());
        when(rs.getString("full_name")).thenReturn("An");
        when(rs.getString("phone")).thenReturn("0900000001");
        assertThat(mapper.getValue().mapRow(rs, 0)).isEqualTo(new SalesUserResponse(id, "An", "0900000001"));
    }

    @Test
    void isActiveSales_trueOnlyWhenTheCountIsPositive() {
        UUID id = UUID.randomUUID();
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Integer.class)))
                .thenReturn(1, 0, null);

        assertThat(repository.isActiveSales(id)).isTrue();
        assertThat(repository.isActiveSales(id)).isFalse();
        assertThat(repository.isActiveSales(id)).isFalse();
    }
}
