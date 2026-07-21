package game.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ScoreRepositoryTest {
	@Test
	public void createsSharedScoreSchema() throws Exception {
		Path file = Files.createTempFile("score-", ".db");
		new ScoreRepository(file.toString());
		try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file);
			 Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
					"SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='score_record'")) {
			assertEquals(1, rs.getInt(1));
		}
		Files.deleteIfExists(file);
	}
}
