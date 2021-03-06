package org.springframework.amqp.rabbit.stocks.dao.sqlfire;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.jdbc.query.QueryDslJdbcTemplate;
import org.springframework.data.jdbc.query.SqlUpdateCallback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.amqp.rabbit.stocks.dao.SequenceDao;
import org.springframework.amqp.rabbit.stocks.domain.Sequence;
import org.springframework.amqp.rabbit.stocks.generated.domain.QSequence;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.dml.SQLUpdateClause;
import com.mysema.query.types.Expression;
import com.mysema.query.types.MappingProjection;

@Transactional
public class SqlFireSequenceDao implements SequenceDao {

	private final QSequence qSequence = QSequence.sequence;

	private QueryDslJdbcTemplate template;

	public void setDataSource(DataSource dataSource) {
		this.template = new QueryDslJdbcTemplate(dataSource);
	}
	
	public int getNextTradeId() throws DataAccessException {
		return getNextId(TRADE_SEQ);
	}
	
	/**
	 * This is a generic sequence ID generator that is based on a database table
	 * called 'SEQUENCE', which contains two columns (NAME, NEXTID). This
	 * approach should work with any database.
	 * 
	 * @param name
	 *            the name of the sequence
	 * @return the next ID
	 */
	public int getNextId(final String name) throws DataAccessException {
		SQLQuery sqlQuery = template.newSqlQuery()
				.from(qSequence)
				.where(qSequence.name.eq(name));
		final Sequence sequence = template.queryForObject(sqlQuery, 
				new MappingSequenceProjection(qSequence.name, qSequence.nextid));
		
		if (sequence == null) {
			throw new DataRetrievalFailureException(
					"Could not get next value of sequence '" + name
							+ "': sequence does not exist");
		}
		
		template.update(qSequence, new SqlUpdateCallback() {
			public long doInSqlUpdateClause(SQLUpdateClause sqlUpdateClause) {
				return sqlUpdateClause.where(qSequence.name.eq(name))
						.set(qSequence.nextid, sequence.getNextId() + 1)
						.execute();
			}
		});
		
		return sequence.getNextId();
	}
	
	private class MappingSequenceProjection extends MappingProjection<Sequence> {

		private static final long serialVersionUID = 1900523652057218147L;

		public MappingSequenceProjection(Expression<?>... args) {
			super(Sequence.class, args);
		}

		@Override
		protected Sequence map(Tuple tuple) {
			Sequence sequence = new Sequence();
			
			sequence.setName(tuple.get(qSequence.name));
			sequence.setNextId(tuple.get(qSequence.nextid));
			
			return sequence;
		}
		
	}
}
